import os
import sys

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from snowflake.snowpark import Session


def get_env_var(name: str) -> str:
    val = os.environ.get(name)
    if not val:
        print(
            f"Missing required env variable {name}. "
            + "Source load-idr-credentials.sh with BFD_ENV set before running."
        )
        sys.exit(1)
    return val


def create_snowflake_session() -> Session:
    private_key = serialization.load_pem_private_key(
        get_env_var("IDR_PRIVATE_KEY").encode(),
        password=None,
        backend=default_backend(),
    )
    private_key_bytes = private_key.private_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )
    return Session.builder.configs(
        {
            "account": get_env_var("IDR_ACCOUNT"),
            "user": get_env_var("IDR_USERNAME"),
            "private_key": private_key_bytes,
            "warehouse": get_env_var("IDR_WAREHOUSE"),
            "database": get_env_var("IDR_DATABASE"),
            "schema": "CMS_VDM_VIEW_MDCR_PRD",
        }
    ).create()
