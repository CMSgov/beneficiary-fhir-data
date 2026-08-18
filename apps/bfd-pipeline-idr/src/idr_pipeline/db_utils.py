from .model.base_model import LoadMode
from .settings import SETTINGS


def get_connection_string(load_mode: LoadMode) -> str:
    if load_mode == LoadMode.LOCAL:
        return "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev"

    return (
        f"host={SETTINGS.bfd_db_endpoint} port={SETTINGS.bfd_db_port} "
        f"dbname={SETTINGS.bfd_db_name} user={SETTINGS.bfd_db_username} "
        f"password={SETTINGS.bfd_db_password}"
    )
