from ctypes import c_int32

from model.base_model import LoadMode
from settings import bfd_db_endpoint, bfd_db_name, bfd_db_password, bfd_db_port, bfd_db_username


def get_connection_string(load_mode: LoadMode) -> str:
    if load_mode == LoadMode.LOCAL:
        return "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev"

    return f"host={bfd_db_endpoint()} port={bfd_db_port()} dbname={bfd_db_name()} \
        user={bfd_db_username()} password={bfd_db_password()}"


def to_pg_integer(val: int) -> int:
    """Convert any integer into a Postgres 32-bit integer."""
    if -(2**31) <= val < 2**31:
        return val
    return c_int32(val).value
