from os import getenv


def idr_private_key() -> str:
    return getenv("IDR_PRIVATE_KEY", "")

def idr_username() -> str:
    return getenv("IDR_USERNAME", "")

def idr_account() -> str:
    return getenv("IDR_ACCOUNT", "")

def idr_warehouse() -> str:
    return getenv("IDR_WAREHOUSE", "")

def idr_database() -> str:
    return getenv("IDR_DATABASE", "")

def idr_schema() -> str:
    return getenv("IDR_SCHEMA", "")

def output_dir() -> str:
    return getenv("EXPORT_FILE_DIR","")

def table_exception_list() -> str:
    return getenv("TABLE_EXCEPTION_LIST","flyway_schema_history,V2_CLM_RLT_OCRNC_SGNTR_MBR")
