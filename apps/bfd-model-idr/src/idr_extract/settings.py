from os import getenv


def output_dir() -> str:
    return getenv("EXPORT_FILE_DIR","")

def table_exception_list() -> str:
    return getenv("TABLE_EXCEPTION_LIST","flyway_schema_history")
