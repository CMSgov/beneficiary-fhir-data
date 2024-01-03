import json
import logging
import os
import re
import socket
import sys
from base64 import b64decode
from dataclasses import dataclass
from datetime import datetime
from enum import Enum, StrEnum, auto
from io import StringIO
from typing import Any
from urllib.parse import unquote

import boto3
import botocore
import botocore.exceptions
import paramiko
from botocore.config import Config
from paramiko.ssh_exception import (
    AuthenticationException,
    BadHostKeyException,
    NoValidConnectionsError,
    SSHException,
)

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

logging.basicConfig(level=logging.INFO, force=True)
logger = logging.getLogger()
try:
    s3_client = boto3.client("s3", config=BOTO_CONFIG)  # type: ignore
    ssm_client = boto3.client("ssm", config=BOTO_CONFIG)  # type: ignore
except Exception:
    logger.error(
        "Unrecoverable exception occurred when attempting to create boto3 clients/resources: ",
        exc_info=True,
    )
    sys.exit(0)


class S3EventType(str, Enum):
    """Represents the types of S3 events that this Lambda is invoked by and supports. The value of
    each Enum is a substring that is matched for on the "eventName" property of an invocation
    event"""

    OBJECT_CREATED = "ObjectCreated"


class FileTransferError(StrEnum):
    UNRECOGNIZED_FILE = auto()
    SFTP_CONNECTION_ERROR = auto()
    SFTP_TRANSFER_ERROR = auto()

@dataclass(frozen=True, eq=True)
class GlobalSsmConfig:
    sftp_connect_timeout: int
    sftp_hostname: str
    sftp_host_pub_key: str
    sftp_username: str
    sftp_user_priv_key: str

    @classmethod
    def from_ssm(cls) -> "GlobalSsmConfig":
        sftp_connect_timeout = int(get_ssm_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/timeout", with_decrypt=True
        ))
        sftp_hostname = get_ssm_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/host", with_decrypt=True
        )
        sftp_host_pub_key = get_ssm_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/trusted_host_key", with_decrypt=True
        )
        sftp_username = get_ssm_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/username", with_decrypt=True
        )
        sftp_user_priv_key = get_ssm_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sftp/user_priv_key", with_decrypt=True
        )

        return GlobalSsmConfig(
            sftp_connect_timeout=sftp_connect_timeout,
            sftp_hostname=sftp_hostname,
            sftp_host_pub_key=sftp_host_pub_key,
            sftp_username=sftp_username,
            sftp_user_priv_key=sftp_user_priv_key
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
    sent_files_dir: str
    failed_files_dir: str
    recognized_files: list[RecognizedFile]

    @classmethod
    def from_partner(cls, partner: str) -> "PartnerSsmConfig":
        bucket_home_dir = get_ssm_parameter(
            f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/bucket_home_dir", with_decrypt=True
        )
        pending_files_dir = get_ssm_parameter(
            f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/outbound/pending_dir", with_decrypt=True
        )
        sent_files_dir = get_ssm_parameter(
            f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/outbound/sent_dir", with_decrypt=True
        )
        failed_files_dir = get_ssm_parameter(
            f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/outbound/failed_dir", with_decrypt=True
        )
        recognized_files = [
            RecognizedFile(**file_dict)
            for file_dict in json.loads(
                get_ssm_parameter(
                    f"/{partner}/{BFD_ENVIRONMENT}/eft/sensitive/outbound/recognized_files_json",
                    with_decrypt=True,
                )
            )
        ]

        return PartnerSsmConfig(
            partner=partner,
            bucket_home_dir=bucket_home_dir,
            pending_files_dir=pending_files_dir,
            sent_files_dir=sent_files_dir,
            failed_files_dir=failed_files_dir,
            recognized_files=recognized_files,
        )

    @property
    def bucket_home_path(self):
        return f"{BUCKET_ROOT_DIR}/{self.bucket_home_dir}"

    @property
    def pending_files_full_path(self):
        return f"{self.bucket_home_path}/{self.pending_files_dir}"

    @property
    def sent_files_full_path(self):
        return f"{self.bucket_home_path}/{self.sent_files_dir}"

    @property
    def failed_files_full_path(self):
        return f"{self.bucket_home_path}/{self.failed_files_dir}"


def get_ssm_parameter(name: str, with_decrypt: bool = False) -> str:
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]
    except KeyError as exc:
        raise ValueError(f'SSM parameter "{name}" not found or empty') from exc


def move_s3_object(object_key: str, destination: str) -> None:
    try:
        s3_client.copy(
            CopySource={"Bucket": BUCKET, "Key": object_key},
            Bucket=BUCKET,
            Key=destination,
        )
        s3_client.delete_object(Bucket=BUCKET, Key=object_key)
    except botocore.exceptions.ClientError as exc:
        raise RuntimeError(f"Unable to copy or delete {object_key}") from exc


def handler(event: Any, context: Any):
    if not all(
        [
            REGION,
            BFD_ENVIRONMENT,
            BUCKET,
            BUCKET_ROOT_DIR,
        ]
    ):
        logger.error("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record: dict[str, Any] = event["Records"][0]
    except KeyError:
        logger.error("The incoming event was invalid: ", exc_info=True)
        return
    except IndexError:
        logger.error("Invalid event notification, no records found: ", exc_info=True)
        return

    try:
        sns_message_json: str = record["Sns"]["Message"]
        sns_message = json.loads(sns_message_json)
    except KeyError:
        logger.error("No message found in SNS notification: ", exc_info=True)
        return
    except json.JSONDecodeError:
        logger.error("SNS message body was not valid JSON: ", exc_info=True)
        return

    try:
        s3_event = sns_message["Records"][0]
    except KeyError:
        logger.error("Invalid S3 event, no records found: ", exc_info=True)
        return
    except IndexError:
        logger.error("Invalid event notification, no records found: ", exc_info=True)
        return

    try:
        event_type_str = s3_event["eventName"]
        event_type: S3EventType
        if S3EventType.OBJECT_CREATED in event_type_str:
            event_type = S3EventType.OBJECT_CREATED
        else:
            logger.error("Event type %s is unsupported. Exiting...", event_type_str)
            return
    except KeyError:
        logger.error(
            "The incoming event record did not contain the type of S3 event: ", exc_info=True
        )
        return

    try:
        file_key: str = s3_event["s3"]["object"]["key"]
        decoded_file_key = unquote(file_key)
    except KeyError:
        logger.error("No bucket file found in event notification: ", exc_info=True)
        return

    # Log the various bits of data extracted from the invoking event to aid debugging:
    logger.info("Invoked at: %s UTC", datetime.utcnow().isoformat())
    logger.info("S3 Object Key: %s", decoded_file_key)
    logger.info("S3 Event Type: %s, Specific Event Name: %s", event_type.name, event_type_str)

    object_key_pattern = rf"^{BUCKET_ROOT_DIR}/([\w_]+)/([\w_]+)/(.*)$"
    match = re.search(
        pattern=object_key_pattern,
        string=decoded_file_key,
        flags=re.IGNORECASE,
    )
    if match is None:
        logger.error(
            "Invocation event unsupported, object key does not match expected pattern: %s. See log"
            " for additional detail. Exiting...",
            object_key_pattern,
        )
        return

    partner = match.group(1)
    subfolder = match.group(2)
    filename = match.group(3)

    try: 
        global_config = GlobalSsmConfig.from_ssm()
    except (ValueError, botocore.exceptions.ClientError):
        logger.error(
            "An unrecoverable error occurred when attempting to retrieve "
            "global outbound SSM configuration: ",
            exc_info=True,
        )
        return

    try:
        partner_config = PartnerSsmConfig.from_partner(partner=partner)
    except (ValueError, botocore.exceptions.ClientError):
        logger.error(
            "An unrecoverable error occurred when attempting to retrieve SSM configuration for"
            " partner %s: ",
            partner,
            exc_info=True,
        )
        return

    if subfolder != partner_config.pending_files_dir:
        logger.error(
            "%s pending files directory, %s, does not match object key: %s, exiting...",
            partner,
            partner_config.pending_files_dir,
            decoded_file_key,
        )
        return

    destination_folder = next(
        (
            recognized_file.destination_folder
            for recognized_file in partner_config.recognized_files
            if re.search(pattern=recognized_file.filename_pattern, string=filename)
        ),
        None,
    )
    if not destination_folder:
        logger.error(
            "The file %s did not match any recognized files (%s) for partner %s",
            filename,
            str(partner_config.recognized_files),
            partner,
        )
        failed_full_path = "/".join(
            [
                partner_config.failed_files_full_path,
                FileTransferError.UNRECOGNIZED_FILE,
                filename,
            ]
        )
        logger.error(
            'The file "%s" will be moved to %s in the %s bucket to indicate this failure',
            decoded_file_key,
            failed_full_path,
            BUCKET,
        )

        try:
            move_s3_object(object_key=decoded_file_key, destination=failed_full_path)
        except RuntimeError:
            logger.error(
                "Unrecoverable error occurred when attempting to move %s to %s",
                decoded_file_key,
                failed_full_path,
                exc_info=True,
            )
        return

    logger.info(
        'Preconditions checked. Connecting to SFTP host "%s" as user "%s" with provided private key'
        " and server host public key...",
        global_config.sftp_hostname,
        global_config.sftp_username,
    )

    with paramiko.SSHClient() as ssh_client:
        try:
            sftp_host_key = paramiko.RSAKey(
                data=b64decode(
                    global_config.sftp_host_pub_key.removeprefix("ssh-rsa ")
                )
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
        ):
            logger.error(
                "An unrecoverable exception occurred when attempting to connect to SFTP server %s",
                global_config.sftp_hostname,
                exc_info=True,
            )
            failed_full_path = "/".join(
                [
                    partner_config.failed_files_full_path,
                    FileTransferError.SFTP_CONNECTION_ERROR,
                    filename,
                ]
            )
            logger.error(
                'The file "%s" will be moved to %s in the %s bucket to indicate this failure',
                decoded_file_key,
                failed_full_path,
                BUCKET,
            )

            try:
                move_s3_object(object_key=decoded_file_key, destination=failed_full_path)
            except RuntimeError:
                logger.error(
                    "Unrecoverable error occurred when attempting to move %s to %s",
                    decoded_file_key,
                    failed_full_path,
                    exc_info=True,
                )
            return

        logger.info(
            "Connected successfully to %s. Attempting to upload %s to %s...",
            global_config.sftp_hostname,
            filename,
            destination_folder,
        )

        try:
            with ssh_client.open_sftp() as sftp_client:
                current_dir = ""
                for dir_part in destination_folder.split("/"):
                    if not dir_part:
                        continue
                    current_dir += f"/{dir_part}"
                    try:
                        sftp_client.listdir(current_dir)
                    except IOError:
                        logger.info("%s does not exist, creating it", current_dir)
                        sftp_client.mkdir(current_dir)

                with sftp_client.open(f"{destination_folder}/{filename}", "wb", 32768) as f:
                    s3_client.download_fileobj(Bucket=BUCKET, Key=decoded_file_key, Fileobj=f)  # type: ignore
        except (SSHException, IOError, botocore.exceptions.ClientError):
            logger.error(
                "An unrecoverable exception occurred when attempting to upload %s via SFTP to %s on"
                " %s: ",
                decoded_file_key,
                destination_folder,
                global_config.sftp_hostname,
                exc_info=True,
            )
            failed_full_path = "/".join(
                [
                    partner_config.failed_files_full_path,
                    FileTransferError.SFTP_TRANSFER_ERROR,
                    filename,
                ]
            )
            logger.error(
                'The file "%s" will be moved to %s in the %s bucket to indicate this failure',
                decoded_file_key,
                failed_full_path,
                BUCKET,
            )

            try:
                move_s3_object(object_key=decoded_file_key, destination=failed_full_path)
            except RuntimeError:
                logger.error(
                    "Unrecoverable error occurred when attempting to move %s to %s",
                    decoded_file_key,
                    failed_full_path,
                    exc_info=True,
                )
            return

        logger.info(
            "%s uploaded successfully to %s on %s",
            decoded_file_key,
            destination_folder,
            global_config.sftp_hostname,
        )
        success_full_path = f"{partner_config.sent_files_full_path}/{filename}"
        logger.info(
            'The file "%s" will be moved to %s in the %s bucket to indicate that the file was'
            " successfully uploaded",
            decoded_file_key,
            success_full_path,
            BUCKET,
        )
        try:
            move_s3_object(object_key=decoded_file_key, destination=success_full_path)
        except RuntimeError:
            logger.error(
                "Unrecoverable error occurred when attempting to move %s to %s",
                decoded_file_key,
                success_full_path,
                exc_info=True,
            )
