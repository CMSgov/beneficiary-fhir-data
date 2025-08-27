import itertools
import json
import os
import re
import sys
from datetime import UTC, datetime
from io import BytesIO, StringIO
from typing import Any
from urllib.parse import unquote_plus

import boto3
import botocore
import botocore.exceptions
import paramiko
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parser import envelopes
from aws_lambda_powertools.utilities.parser.models import S3Model
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
    FileDiscoveredDetails,
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


def _handle_s3_event(s3_object_key: str) -> None:
    s3_client = boto3.client("s3", config=BOTO_CONFIG)
    ssm_client = boto3.client("ssm", config=BOTO_CONFIG)
    sns_resource = boto3.resource("sns", config=BOTO_CONFIG)

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
            message=f"The file {filename} did not match any of {partner}'s recognized files: "
            + json.dumps(
                [
                    {"type": file.type, "pattern": file.filename_pattern}
                    for file in partner_config.recognized_files
                ]
            ),
            s3_object_key=s3_object_key,
            partner=partner,
        )

    logger.info(
        "%s recognized as %s file type using pattern %s; will be sent to %s",
        filename,
        recognized_file.type,
        recognized_file.filename_pattern,
        global_config.sftp_hostname,
    )
    logger.append_keys(
        file_type=recognized_file.type,
        matched_pattern=recognized_file.filename_pattern,
        staging_folder=recognized_file.staging_folder,
        input_folder=recognized_file.input_folder,
    )

    file_discovered_time = datetime.now(UTC)
    bfd_topic = sns_resource.Topic(BFD_SNS_TOPIC_ARN)
    discovered_notif = StatusNotification(
        details=FileDiscoveredDetails(
            partner=partner,
            timestamp=file_discovered_time,
            object_key=s3_object_key,
            file_type=recognized_file.type,
        )
    )

    send_notification(topic=bfd_topic, notification=discovered_notif)

    topic_arns_by_partner: dict[str, str] = json.loads(SNS_TOPIC_ARNS_BY_PARTNER_JSON)
    partner_topic = (
        sns_resource.Topic(topic_arns_by_partner[partner])
        if partner in topic_arns_by_partner
        else None
    )
    if partner_topic:
        send_notification(topic=partner_topic, notification=discovered_notif)

    logger.info(
        'Preconditions checked. Connecting to SFTP host "%s" as user "%s"',
        global_config.sftp_hostname,
        global_config.sftp_username,
    )
    with paramiko.SSHClient() as ssh_client:
        try:
            sftp_priv_key = paramiko.RSAKey.from_private_key(
                file_obj=StringIO(global_config.sftp_user_priv_key)
            )
            for sftp_host_key in global_config.sftp_host_pub_keys:
                ssh_client.get_host_keys().add(
                    hostname=global_config.sftp_hostname,
                    keytype=sftp_host_key.key_type,
                    key=paramiko.PKey.from_type_string(
                        key_type=sftp_host_key.key_type, key_bytes=sftp_host_key.key_bytes
                    ),
                )
            ssh_client.connect(
                hostname=global_config.sftp_hostname,
                username=global_config.sftp_username,
                pkey=sftp_priv_key,
                look_for_keys=False,
                allow_agent=False,
                timeout=global_config.sftp_connect_timeout,
            )
        except (
            OSError,
            BadHostKeyException,
            AuthenticationException,
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
            recognized_file.input_folder,
            global_config.sftp_hostname,
        )

        try:
            with ssh_client.open_sftp() as sftp_client:
                # Clean the provided paths of any whitespace or trailing/leading slashes
                staging_dir_path = f"/{recognized_file.staging_folder.strip().strip('/')}"
                input_dir_path = f"/{recognized_file.input_folder.strip().strip('/')}"

                # First, upload the file to the staging folder. This avoids the SWEEPS automation
                # from sweeping partially uploaded files as we will move this file to the SWEEPS
                # input folder only once it has fully uploaded
                logger.info(
                    "Starting initial download from %s and upload to the staging path %s",
                    filename,
                    staging_dir_path,
                )
                staging_file_path = f"{staging_dir_path}/{filename}"

                with BytesIO() as bo:
                    logger.info("Starting initial download of object %s", filename)
                    s3_client.download_fileobj(Bucket=BUCKET, Key=s3_object_key, Fileobj=bo)
                    bo.seek(0)
                    logger.info("Starting upload to %s", staging_dir_path)
                    transfer = sftp_client.putfo(fl=bo, remotepath=staging_file_path)

                logger.info(
                    "Upload of %s to %s successful: %s",
                    filename,
                    staging_dir_path,
                    str(transfer.st_size),
                )

                # Once uploaded, we must modify the file permissions such that the SWEEPS automation
                # user can interact with the file
                logger.info(
                    "Modifying file permissons of %s on %s to 664",
                    staging_file_path,
                    global_config.sftp_hostname,
                )
                sftp_client.chmod(path=staging_file_path, mode=0o664)
                logger.info(
                    "File permissions of %s modified to 664 successfully", staging_file_path
                )

                # Once the permissions have been modified, we need to move the file to the actual
                # SWEEPS input directory. (SFTP calls this a "rename")
                logger.info(
                    "Moving %s to SWEEPS input directory path %s", staging_file_path, input_dir_path
                )
                input_file_path = f"{input_dir_path}/{filename}"
                sftp_client.rename(oldpath=staging_file_path, newpath=input_file_path)
                logger.info(
                    "%s moved to %s successfully; %s has been transferred successfully",
                    filename,
                    input_file_path,
                    filename,
                )
        except (OSError, SSHException, botocore.exceptions.ClientError) as exc:
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
            recognized_file.input_folder,
            global_config.sftp_hostname,
        )

        success_time = datetime.now(UTC)
        success_notif = StatusNotification(
            details=TransferSuccessDetails(
                partner=partner,
                timestamp=datetime.now(UTC),
                object_key=s3_object_key,
                file_type=recognized_file.type,
                transfer_duration=round((success_time - file_discovered_time).total_seconds()),
            )
        )
        send_notification(topic=bfd_topic, notification=success_notif)
        if partner_topic:
            send_notification(topic=partner_topic, notification=success_notif)


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext) -> None:  # noqa: ARG001
    try:
        if not all(
            [
                REGION,
                BFD_ENVIRONMENT,
                BUCKET,
                BUCKET_ROOT_DIR,
            ]
        ):
            raise RuntimeError("Not all necessary environment variables were defined")

        s3_event_records = list(
            itertools.chain.from_iterable(
                event.Records
                for event in envelopes.SnsSqsEnvelope().parse(data=event, model=S3Model)
                if event is not None
            )
        )
        if not s3_event_records:
            raise ValueError("Invalid invocation event, no records")

        for s3_record in s3_event_records:
            s3_event_time = s3_record.eventTime.astimezone(UTC)
            s3_object_key = unquote_plus(s3_record.s3.object.key)
            s3_event_name = s3_record.eventName
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
                    timestamp=datetime.now(UTC),
                    object_key=exc.s3_object_key,
                    error_name=exc.__class__.__name__,
                    reason=exc.message,
                )
            )

            topic_arns_by_partner: dict[str, str] = json.loads(SNS_TOPIC_ARNS_BY_PARTNER_JSON)
            if exc.partner and exc.partner in topic_arns_by_partner:
                partner_topic = sns_resource.Topic(topic_arns_by_partner[exc.partner])
                logger.info(
                    "%s status notification topic configured. Sending error notification",
                    exc.partner,
                )
                send_notification(topic=partner_topic, notification=notification)
        else:
            notification = StatusNotification(
                details=UnknownErrorDetails(
                    timestamp=datetime.now(UTC), error_name=exc.__class__.__name__, reason=str(exc)
                )
            )

        logger.info("Sending error notification to BFD catch-all topic %s", BFD_SNS_TOPIC_ARN)
        send_notification(topic=sns_resource.Topic(BFD_SNS_TOPIC_ARN), notification=notification)

        raise
    finally:
        _, exception, _ = sys.exc_info()
        if exception:
            logger.exception("Unrecoverable exception raised")
