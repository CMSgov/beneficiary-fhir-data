import json
from base64 import b64decode
from dataclasses import dataclass
from typing import TYPE_CHECKING, Annotated

import botocore.exceptions
from pydantic import BaseModel, BeforeValidator, Field, TypeAdapter

if TYPE_CHECKING:
    from mypy_boto3_ssm.client import SSMClient
else:
    Topic = object
    SSMClient = object


class HostKey(BaseModel):
    key_type: str
    key_bytes: Annotated[bytes, BeforeValidator(b64decode)] = Field(validation_alias="key_base64")


@dataclass(frozen=True, eq=True)
class GlobalSsmConfig:
    sftp_connect_timeout: int
    sftp_hostname: str
    sftp_host_pub_keys: list[HostKey]
    sftp_username: str
    sftp_user_priv_key: str
    enrolled_partners: list[str]
    home_dirs_to_partner: dict[str, str]

    @classmethod
    def from_ssm(cls, ssm_client: SSMClient, env: str) -> "GlobalSsmConfig":
        sftp_connect_timeout = int(
            get_ssm_parameter(
                ssm_client=ssm_client,
                path=f"/bfd/{env}/eft/sensitive/outbound/sftp/timeout",
                with_decrypt=True,
            )
        )
        sftp_hostname = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{env}/eft/sensitive/outbound/sftp/host",
            with_decrypt=True,
        )
        sftp_host_pub_keys = TypeAdapter(list[HostKey]).validate_json(
            get_ssm_parameter(
                ssm_client=ssm_client,
                path=f"/bfd/{env}/eft/sensitive/outbound/sftp/trusted_host_keys_json",
                with_decrypt=True,
            ),
            by_alias=True,
        )
        sftp_username = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{env}/eft/sensitive/outbound/sftp/username",
            with_decrypt=True,
        )
        sftp_user_priv_key = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{env}/eft/sensitive/outbound/sftp/user_priv_key",
            with_decrypt=True,
        )
        enrolled_partners: list[str] = json.loads(
            get_ssm_parameter(
                ssm_client=ssm_client,
                path=f"/bfd/{env}/eft/sensitive/outbound/partners_list_json",
                with_decrypt=True,
            )
        )
        home_dirs_to_partner = {
            get_ssm_parameter(
                ssm_client=ssm_client,
                path=f"/{partner}/{env}/eft/sensitive/bucket_home_dir",
                with_decrypt=True,
            ): partner
            for partner in enrolled_partners
        }

        return GlobalSsmConfig(
            sftp_connect_timeout=sftp_connect_timeout,
            sftp_hostname=sftp_hostname,
            sftp_host_pub_keys=sftp_host_pub_keys,
            sftp_username=sftp_username,
            sftp_user_priv_key=sftp_user_priv_key,
            enrolled_partners=enrolled_partners,
            home_dirs_to_partner=home_dirs_to_partner,
        )


@dataclass(frozen=True, eq=True)
class RecognizedFile:
    type: str
    filename_pattern: str
    staging_folder: str
    input_folder: str


@dataclass(frozen=True, eq=True)
class PartnerSsmConfig:
    partner: str
    bucket_home_dir: str
    pending_files_dir: str
    bucket_home_path: str
    pending_files_full_path: str
    recognized_files: list[RecognizedFile]

    @classmethod
    def from_ssm(cls, ssm_client: SSMClient, partner: str, env: str) -> "PartnerSsmConfig":
        bucket_home_dir = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/{partner}/{env}/eft/sensitive/bucket_home_dir",
            with_decrypt=True,
        )
        pending_files_dir = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/{partner}/{env}/eft/sensitive/outbound/pending_dir",
            with_decrypt=True,
        )
        bucket_root_dir = get_ssm_parameter(
            ssm_client=ssm_client,
            path=f"/bfd/{env}/eft/sensitive/inbound/sftp_server/eft_user_home_dir",
            with_decrypt=True,
        )
        bucket_home_path = f"{bucket_root_dir}/{bucket_home_dir}"
        pending_files_full_path = f"{bucket_home_path}/{pending_files_dir}"
        recognized_files = [
            RecognizedFile(**file_dict)
            for file_dict in json.loads(
                get_ssm_parameter(
                    ssm_client=ssm_client,
                    path=(f"/{partner}/{env}/eft/sensitive/outbound/recognized_files_json"),
                    with_decrypt=True,
                )
            )
        ]

        return PartnerSsmConfig(
            partner=partner,
            bucket_home_dir=bucket_home_dir,
            pending_files_dir=pending_files_dir,
            bucket_home_path=bucket_home_path,
            pending_files_full_path=pending_files_full_path,
            recognized_files=recognized_files,
        )


def get_ssm_parameter(ssm_client: SSMClient, path: str, with_decrypt: bool = False) -> str:
    try:
        response = ssm_client.get_parameter(Name=path, WithDecryption=with_decrypt)

        return response["Parameter"]["Value"]  # type: ignore
    except (KeyError, botocore.exceptions.ClientError) as exc:
        raise ValueError(f'SSM parameter "{path}" not found or empty') from exc
