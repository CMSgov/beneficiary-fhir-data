import json
import os
import re
import socket
from base64 import b64decode
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import Enum
from io import StringIO
from typing import TYPE_CHECKING, Any
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

# Solve typing issues in Lambda as mypy_boto3 will not be included in the Lambda
if TYPE_CHECKING:
    from mypy_boto3_ssm.client import SSMClient
else:
    SSMClient = object

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
BUCKET = os.environ.get("BUCKET", "")
BUCKET_ROOT_DIR = os.environ.get("BUCKET_ROOT_DIR", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

logger = Logger()


class S3EventType(str, Enum):
    """Represents the types of S3 events that this Lambda is invoked by and supports. The value of
    each Enum is a substring that is matched for on the "eventName" property of an invocation
    event"""

    OBJECT_CREATED = "ObjectCreated"

    @classmethod
    def from_event_name(cls, event_name: str) -> "S3EventType":
        try:
            return next(x for x in S3EventType if x in event_name)
        except StopIteration as ex:
            raise ValueError(
                f"Invalid event name {event_name}; no corresponding, supported event found"
            ) from ex


class BaseTransferError(Exception):
    def __init__(self, message: str, s3_object_key: str, partner: str | None):
        super(BaseTransferError, self).__init__(message, s3_object_key, partner)

        self.message = message
        self.s3_object_key = s3_object_key
        self.partner = partner

    def __str__(self) -> str:
        return json.dumps(
            {"message": self.message, "s3_object_key": self.s3_object_key, "partner": self.partner}
        )


class UnknownPartnerError(BaseTransferError): ...


class InvalidObjectKeyError(BaseTransferError): ...


class InvalidPendingDirError(BaseTransferError): ...


class UnrecognizedFileError(BaseTransferError): ...


class SFTPConnectionError(BaseTransferError): ...


class SFTPTransferError(BaseTransferError): ...




@dataclass(frozen=True, eq=True)
class GlobalSsmConfig:
    sftp_connect_timeout: int
    sftp_hostname: str
    sftp_host_pub_key: str
    sftp_username: str
    sftp_user_priv_key: str
    enrolled_partners: list[str]
    home_dirs_to_partner: dict[str, str]

    @classmethod
    def from_ssm(cls, ssm_client: SSMClient) -> "GlobalSsmConfig":
        sftp_connect_timeout = int(
            get_ssm_parameter(
                ssm_client=ssm_client,
                path=f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/timeout",
                with_decrypt=True,
            )
        )
        sftp_hostname = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/host",
            with_decrypt=True,
        )
        sftp_host_pub_key = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/trusted_host_key",
            with_decrypt=True,
        )
        sftp_username = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/username",
            with_decrypt=True,
        )
        sftp_user_priv_key = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/user_priv_key",
            with_decrypt=True,
        )
        enrolled_partners: list[str] = json.loads(
            get_ssm_parameter(
                ssm_client=ssm_client,
                path=f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/partners_list_json",
                with_decrypt=True,
            )
        )
        home_dirs_to_partner = {
            get_ssm_parameter(
                ssm_client=ssm_client,
                path=f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/bucket_home_dir",
                with_decrypt=True,
            ): partner
            for partner in enrolled_partners
        }

        return GlobalSsmConfig(
            sftp_connect_timeout=sftp_connect_timeout,
            sftp_hostname=sftp_hostname,
            sftp_host_pub_key=sftp_host_pub_key,
            sftp_username=sftp_username,
            sftp_user_priv_key=sftp_user_priv_key,
            enrolled_partners=enrolled_partners,
            home_dirs_to_partner=home_dirs_to_partner,
        )


@dataclass(frozen=True, eq=True)
class RecognizedFile:
    filename_pattern: str
    destination_folder: str


@dataclass(frozen=True, eq=True)
class PartnerSsmConfig:
    partner: str
    bucket_home_dir: str
    pending_files_dir: str
    recognized_files: list[RecognizedFile]

    @classmethod
    def from_partner(cls, ssm_client: SSMClient, partner: str) -> "PartnerSsmConfig":
        bucket_home_dir = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/bucket_home_dir",
            with_decrypt=True,
        )
        pending_files_dir = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/outbound/pending_dir",
            with_decrypt=True,
        )
        recognized_files = [
            RecognizedFile(**file_dict)
            for file_dict in json.loads(
                get_ssm_parameter(
                    ssm_client=ssm_client,
                    path=(
                        f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/outbound/recognized_files_json"
                    ),
                    with_decrypt=True,
                )
            )
        ]

        return PartnerSsmConfig(
            partner=partner,
            bucket_home_dir=bucket_home_dir,
            pending_files_dir=pending_files_dir,
            recognized_files=recognized_files,
        )

    @property
    def bucket_home_path(self):
        return f"{BUCKET_ROOT_DIR}/{self.bucket_home_dir}"

    @property
    def pending_files_full_path(self):
        return f"{self.bucket_home_path}/{self.pending_files_dir}"


def get_ssm_parameter(ssm_client: SSMClient, path: str, with_decrypt: bool = False) -> str:
    response = ssm_client.get_parameter(Name=path, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]
    except KeyError as exc:
        raise ValueError(f'SSM parameter "{path}" not found or empty') from exc


def _handle_s3_event(s3_object_key: str):
    s3_client = boto3.client("s3", config=BOTO_CONFIG)  # type: ignore
    ssm_client = boto3.client("ssm", config=BOTO_CONFIG)  # type: ignore

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

    global_config = GlobalSsmConfig.from_ssm(ssm_client=ssm_client)

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

    partner_config = PartnerSsmConfig.from_partner(ssm_client=ssm_client, partner=partner)
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
                data=b64decode(global_config.sftp_host_pub_key.removeprefix("ssh-rsa "))
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
                    s3_client.download_fileobj(Bucket=BUCKET, Key=s3_object_key, Fileobj=f)  # type: ignore
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


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext):
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
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
