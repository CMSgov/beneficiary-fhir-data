from pathlib import Path

import snowflake.connector
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from idr_pipeline.settings import SETTINGS
from snowflake.connector import SnowflakeConnection

from .settings import (
    output_dir,
    table_exception_list,
)


class SnowflakeTable:
    def __init__(self, schema_name: str, table_name: str) -> None:
        self.schema_name = schema_name
        self.table_name = table_name


class SnowflakeExecutor:

    def __init__(self) -> None:
        self.conn = SnowflakeExecutor.connect()

    @staticmethod
    def connect() -> SnowflakeConnection:
        private_key = serialization.load_pem_private_key(
            SETTINGS.idr_private_key.encode(),
            password=None,
            backend=default_backend(),
        )
        private_key_bytes = private_key.private_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
        return snowflake.connector.connect(  # type: ignore
            user=SETTINGS.idr_username,
            private_key=private_key_bytes,
            account=SETTINGS.idr_account,
            warehouse=SETTINGS.idr_warehouse,
            database=SETTINGS.idr_database,
            schema=SETTINGS.idr_schema,
        )

    def prep(self) -> None:
        self.conn.execute("CREATE OR REPLACE STAGE export_stage")

    def export(self, file_name:str, sql: str) -> None:
        # Create directory if it does not exists
        dir_path = Path(output_dir())
        dir_path.mkdir(parents=True, exist_ok=True)
        # Run export from snowflake
        cursor = self.conn.cursor()
        # Create Stage
        cursor.execute("CREATE OR REPLACE TEMPORARY STAGE tmp_export_stage")
        # GetData
        cursor.execute("""
            COPY INTO @tmp_export_stage/{FILE_NAME}
            FROM (
                {QUERY}
            )
            FILE_FORMAT = (
                TYPE = 'CSV' 
                FIELD_DELIMITER = ',' 
                COMPRESSION = NONE 
                TIMESTAMP_FORMAT = 'YYYY-MM-DD"T"HH24:MI:SS.FF6'
                NULL_IF = ('')
                FIELD_OPTIONALLY_ENCLOSED_BY = '"'
            ) 
            HEADER = TRUE 
            SINGLE = TRUE 
            OVERWRITE = TRUE 
            MAX_FILE_SIZE = 1073741824;
        """
        .replace("{FILE_NAME}",file_name)
        .replace("{QUERY}",sql))

        try:
            get_command = f"GET @tmp_export_stage/{file_name} file://{output_dir()}"
            cursor.execute(get_command)
        except snowflake.connector.errors.OperationalError:
            print(f"No records found for file {file_name} in Snowflake.")

    def get_tables(self) -> list[SnowflakeTable]:
        exception_list = table_exception_list().split(",")
        el_placeholder = ", ".join(["%s"] * len(expection_list))
        cur = self.conn.cursor()
        cur.execute(f"""
            SELECT table_schema, table_name
            FROM information_schema.tables
            WHERE table_type = 'BASE TABLE'
              AND table_name NOT IN ({el_placeholder})
            ORDER BY table_name
        """,exception_list)
        return [SnowflakeTable(row[0],row[1]) for row in cur.fetchall()]



