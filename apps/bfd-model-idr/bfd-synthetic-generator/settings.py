from functools import cached_property
from os import getenv


class _Settings:
    # Database credentials/settings

    @cached_property
    def bfd_db_port(self) -> str:
        return getenv("BFD_LOCAL_DB_PORT", "5432")

    @cached_property
    def bfd_db_name(self) -> str:
        return getenv("BFD_LOCAL_DB_NAME", "fhirdb")

    @cached_property
    def bfd_db_endpoint(self) -> str:
        return getenv("BFD_LOCAL_DB_ENDPOINT", "")

    @cached_property
    def bfd_db_username(self) -> str:
        return getenv("BFD_LOCAL_DB_USERNAME", "")

    @cached_property
    def bfd_db_password(self) -> str:
        return getenv("BFD_LOCAL_DB_PASSWORD", "")


SETTINGS: _Settings = _Settings()
