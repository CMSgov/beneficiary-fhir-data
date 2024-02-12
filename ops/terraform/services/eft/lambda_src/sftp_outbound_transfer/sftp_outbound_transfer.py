import json
import os
import re
import socket
from base64 import b64decode
from datetime import datetime, timezone
from io import StringIO
from typing import Any
from urllib.parse import unquote

import boto3
import botocore
import botocore.exceptions
import paramiko
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.data_classes import S3Event, SNSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from paramiko.ssh_exception import (
    AuthenticationException,
    BadHostKeyException,
    NoValidConnectionsError,
    SSHException,
)

from errors import (
    BaseTransferError,
    InvalidObjectKeyError,
    InvalidPendingDirError,
    SFTPConnectionError,
    SFTPTransferError,
    UnknownPartnerError,
    UnrecognizedFileError,
)
from s3 import S3EventType
from sns import (
    StatusNotification,
    TransferFailedDetails,
    TransferSuccessDetails,
    UnknownErrorDetails,
    send_notification,
)
from ssm import GlobalSsmConfig, PartnerSsmConfig

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
BUCKET = os.environ.get("BUCKET", "")
BUCKET_ROOT_DIR = os.environ.get("BUCKET_ROOT_DIR", "")
BFD_SNS_TOPIC_ARN = os.environ.get("BFD_SNS_TOPIC_ARN", "")
SNS_TOPIC_ARNS_BY_PARTNER_JSON = os.environ.get("SNS_TOPIC_ARNS_BY_PARTNER_JSON", "{}")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

logger = Logger()


def _handle_s3_event(s3_object_key: str):
    s3_client = boto3.client("s3", config=BOTO_CONFIG)
    ssm_client = boto3.client("ssm", config=BOTO_CONFIG)

    object_key_pattern = rf"^{BUCKET_ROOT_DIR}/([\w_]+)/([\w_]+)/(.*)$"
    if (
        match := re.search(
            pattern=object_key_pattern,
            string=s3_object_key,
            flags=re.IGNORECASE,
        )
    ) is None:
        raise InvalidObjectKeyError(
            message=(
                f"Invocation event invalid, object key {s3_object_key} does not match expected"
                f" pattern: {object_key_pattern}"
            ),
            s3_object_key=s3_object_key,
            partner=None,
        )

    global_config = GlobalSsmConfig.from_ssm(ssm_client=ssm_client, env=BFD_ENVIRONMENT)

    partner_home_dir = match.group(1)
    try:
        partner = global_config.home_dirs_to_partner[partner_home_dir]
    except KeyError as exc:
        raise UnknownPartnerError(
            message=(
                f"{partner_home_dir} does not match configured home directory for any enrolled"
                " partner"
            ),
            s3_object_key=s3_object_key,
            partner=None,
        ) from exc
    subfolder = match.group(2)
    filename = match.group(3)

    logger.append_keys(partner=partner, filename=filename)

    partner_config = PartnerSsmConfig.from_ssm(
        ssm_client=ssm_client, partner=partner, env=BFD_ENVIRONMENT
    )
    if subfolder != partner_config.pending_files_dir:
        # This should be impossible, as the S3 Event Notification configuration is configured to
        # send notifications only from valid pending paths
        raise InvalidPendingDirError(
            message=(
                f"{partner}'s pending files directory, {partner_config.pending_files_dir}, does not"
                f" match event notification object key: {s3_object_key}"
            ),
            s3_object_key=s3_object_key,
            partner=partner,
        )
    logger.append_keys(
        sftp_hostname=global_config.sftp_hostname, sftp_username=global_config.sftp_username
    )

    recognized_file = next(
        (
            recognized_file
            for recognized_file in partner_config.recognized_files
            if re.search(pattern=recognized_file.filename_pattern, string=filename)
        ),
        None,
    )
    if not recognized_file:
        raise UnrecognizedFileError(
            message=(
                f"The file {filename} did not match any recognized files"
                f" ({json.dumps(partner_config.recognized_files, default=str)}) for partner"
                f" {partner}"
            ),
            s3_object_key=s3_object_key,
            partner=partner,
        )

    logger.info(
        "%s recognized using pattern %s; will be sent to %s",
        filename,
        recognized_file.filename_pattern,
        recognized_file.destination_folder,
    )
    logger.append_keys(
        matched_pattern=recognized_file.filename_pattern,
        destination_folder=recognized_file.destination_folder,
    )

    logger.info(
        'Preconditions checked. Connecting to SFTP host "%s" as user "%s"',
        global_config.sftp_hostname,
        global_config.sftp_username,
    )
    with paramiko.SSHClient() as ssh_client:
        try:
            sftp_host_key = paramiko.RSAKey(
                data=b64decode(global_config.sftp_host_pub_key.removeprefix("ssh-rsa").strip())
            )
            sftp_priv_key = paramiko.RSAKey.from_private_key(
                StringIO(global_config.sftp_user_priv_key)
            )
            ssh_client.get_host_keys().add(
                hostname=global_config.sftp_hostname, keytype="ssh-rsa", key=sftp_host_key
            )
            ssh_client.connect(
                global_config.sftp_hostname,
                username=global_config.sftp_username,
                pkey=sftp_priv_key,
                look_for_keys=False,
                allow_agent=False,
                timeout=global_config.sftp_connect_timeout,
            )
        except (
            BadHostKeyException,
            AuthenticationException,
            socket.error,
            SSHException,
            NoValidConnectionsError,
        ) as exc:
            raise SFTPConnectionError(
                message=(
                    "An unrecoverable error occurred when attempting to connect to CMS EFT SFTP"
                    " server"
                ),
                s3_object_key=s3_object_key,
                partner=partner,
            ) from exc

        logger.info(
            "Connected successfully to %s. Starting upload of %s to %s on %s",
            global_config.sftp_hostname,
            filename,
            recognized_file.destination_folder,
            global_config.sftp_hostname,
        )

        try:
            with ssh_client.open_sftp() as sftp_client:
                current_dir = ""
                for dir_part in recognized_file.destination_folder.split("/"):
                    if not dir_part:
                        continue
                    current_dir += f"/{dir_part}"
                    try:
                        sftp_client.listdir(current_dir)
                    except IOError:
                        logger.info("%s does not exist, creating it", current_dir)
                        sftp_client.mkdir(current_dir)

                with sftp_client.open(
                    f"{recognized_file.destination_folder}/{filename}", "wb", 32768
                ) as f:
                    s3_client.download_fileobj(Bucket=BUCKET, Key=s3_object_key, Fileobj=f)
        except (SSHException, IOError, botocore.exceptions.ClientError) as exc:
            raise SFTPTransferError(
                message=(
                    f"An unrecoverable error occurred when attempting to transfer {filename} to the"
                    " CMS EFT SFTP Server"
                ),
                s3_object_key=s3_object_key,
                partner=partner,
            ) from exc

        logger.info(
            "%s uploaded successfully to %s on %s",
            s3_object_key,
            recognized_file.destination_folder,
            global_config.sftp_hostname,
        )

        sns_resource = boto3.resource("sns", config=BOTO_CONFIG)
        bfd_topic = sns_resource.Topic(BFD_SNS_TOPIC_ARN)
        notification = StatusNotification(
            details=TransferSuccessDetails(
                partner=partner,
                timestamp=datetime.utcnow(),
                object_key=s3_object_key,
                destination=recognized_file.destination_folder,
            )
        )
        send_notification(topic=bfd_topic, notification=notification)
        topic_arns_by_partner: dict[str, str] = json.loads(SNS_TOPIC_ARNS_BY_PARTNER_JSON)
        if partner in topic_arns_by_partner:
            partner_topic = sns_resource.Topic(topic_arns_by_partner[partner])
            send_notification(topic=partner_topic, notification=notification)


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext):  # pylint: disable=unused-argument
    try:
        if not all([
            REGION,
            BFD_ENVIRONMENT,
            BUCKET,
            BUCKET_ROOT_DIR,
        ]):
            raise RuntimeError("Not all necessary environment variables were defined")

        sns_event = SNSEvent(event)
        if next(sns_event.records, None) is None:
            raise ValueError(
                "Invalid SNS event with empty records:"
                f" {json.dumps(sns_event.raw_event, default=str)}"
            )

        for sns_record in sns_event.records:
            s3_event = S3Event(json.loads(sns_record.sns.message))
            if next(s3_event.records, None) is None:
                raise ValueError(
                    "Invalid inner S3 event with empty records:"
                    f" {json.dumps(s3_event.raw_event, default=str)}"
                )

            for s3_record in s3_event.records:
                s3_event_time = datetime.fromisoformat(
                    s3_record.event_time.removesuffix("Z")
                ).replace(tzinfo=timezone.utc)
                s3_object_key = unquote(s3_record.s3.get_object.key)
                s3_event_name = s3_record.event_name
                s3_event_type = S3EventType.from_event_name(s3_event_name)

                # Append to all future logs information about the S3 Event
                logger.append_keys(
                    s3_object_key=s3_object_key,
                    s3_event_name=s3_event_name,
                    s3_event_type=s3_event_type.name,
                    s3_event_time=s3_event_time.isoformat(),
                )

                _handle_s3_event(s3_object_key=s3_object_key)
    except Exception as exc:
        sns_resource = boto3.resource("sns", config=BOTO_CONFIG)

        notification: StatusNotification
        if isinstance(exc, BaseTransferError):
            notification = StatusNotification(
                details=TransferFailedDetails(
                    partner=exc.partner or "unknown",
                    timestamp=datetime.utcnow(),
                    object_key=exc.s3_object_key,
                    error_name=exc.__class__.__name__,
                    reason=exc.message,
                )
            )

            topic_arns_by_partner: dict[str, str] = json.loads(SNS_TOPIC_ARNS_BY_PARTNER_JSON)
            if exc.partner and exc.partner in topic_arns_by_partner:
                partner_topic = sns_resource.Topic(topic_arns_by_partner[exc.partner])
                send_notification(topic=partner_topic, notification=notification)
        else:
            notification = StatusNotification(
                details=UnknownErrorDetails(
                    timestamp=datetime.utcnow(), error_name=exc.__class__.__name__, reason=str(exc)
                )
            )

        send_notification(topic=sns_resource.Topic(BFD_SNS_TOPIC_ARN), notification=notification)

        raise
    finally:
        logger.exception("Unrecoverable exception raised")
