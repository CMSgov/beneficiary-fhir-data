import os
import sys
import zipfile
from enum import Enum

import click
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from shared_options import common_options
from snowflake.snowpark import Session
from snowflake.snowpark.types import BooleanType, DataType, IntegerType, StringType, VariantType


def _require_env(name: str) -> str:
    val = os.environ.get(name)
    if not val:
        print(
            f"Missing required env variable {name}. "
            + "Source load-idr-credentials.sh with BFD_ENV set before running."
        )
        sys.exit(1)
    return val


def _get_snowpark_and_sql_types(param: click.Parameter) -> tuple[DataType, str]:
    param_type = param.type

    if isinstance(param_type, click.types.BoolParamType):
        return BooleanType(), "BOOLEAN"
    if isinstance(param_type, click.types.IntParamType):
        return IntegerType(), "NUMBER"
    if isinstance(param_type, (click.types.StringParamType, click.Choice)):
        return StringType(), "VARCHAR"
    return StringType(), "VARCHAR"


def _zip_current_directory() -> str:
    current_dir = os.path.dirname(os.path.abspath(__file__))
    folder_to_zip = os.path.basename(current_dir)
    parent_dir = os.path.dirname(current_dir)
    output_zip_path = os.path.join(parent_dir, f"{folder_to_zip}.zip")

    with zipfile.ZipFile(output_zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        for root, _, files in os.walk(current_dir, followlinks=True):
            for file in files:
                full_path = os.path.join(root, file)
                relative_path = os.path.relpath(full_path, parent_dir)
                zipf.write(full_path, relative_path)
    return output_zip_path


def _create_snowflake_session() -> Session:
    private_key = serialization.load_pem_private_key(
        _require_env("IDR_PRIVATE_KEY").encode(),
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
            "account": _require_env("IDR_ACCOUNT"),
            "user": _require_env("IDR_USERNAME"),
            "private_key": private_key_bytes,
            "warehouse": _require_env("IDR_WAREHOUSE"),
            "database": _require_env("IDR_DATABASE"),
            "schema": "CMS_VDM_VIEW_MDCR_PRD",
        }
    ).create()


@click.command
@common_options
def main(**kwargs) -> None:
    min_claims = kwargs.get("min_claims")
    max_claims = kwargs.get("max_claims")
    if min_claims > max_claims:
        raise click.UsageError(
            f"min claims value of {min_claims} is greater than max claims value of {max_claims}"
        )

    session = _create_snowflake_session()
    db_name = session.get_current_database().replace('"', "")
    schema_name = session.get_current_schema().replace('"', "")

    zip_file = _zip_current_directory()
    session.sql(f"create or replace stage {db_name}.{schema_name}.generator_files_stage").collect()
    print("Uploading synthetic generator code...")
    put_result = session.file.put(
        zip_file, f"@{db_name}.{schema_name}.generator_files_stage", auto_compress=False
    )
    print(f"Upload Status: {put_result[0].status}")

    input_types = []
    sql_input_types = []
    ordered_inputs = []
    for param in main.params:
        snowpark_type, sql_string = _get_snowpark_and_sql_types(param)
        input_types.append(snowpark_type)
        sql_input_types.append(sql_string)
        val = kwargs[param.name]
        if isinstance(val, Enum):
            ordered_inputs.append(val.value.upper())
        else:
            ordered_inputs.append(val)

    print("Creating/Updating stored procedure...")
    generator_sproc = session.sproc.register_from_file(
        file_path=f"@{db_name}.{schema_name}.generator_files_stage/bfd-synthetic-generator.zip",
        func_name="generator_sproc_handler.generate_synthetic_data",
        return_type=VariantType(),
        name="generate_synthetic_data",
        is_permanent=True,
        stage_location=f"@{db_name}.{schema_name}.generator_files_stage",
        replace=True,
        packages=[
            "snowflake-snowpark-python",
            "click",
            "tqdm",
            "pydantic",
            "faker",
            "pandas",
        ],
        input_types=input_types,
    )

    # Set LOG_LEVEL so Snowflake can hook our logging into an event table
    session.sql(f"""
    ALTER PROCEDURE generate_synthetic_data({", ".join(sql_input_types)}) SET LOG_LEVEL = 'INFO';
    """).collect()
    event_table = f"{db_name}.CMS_VDM_VIEW_MDCR_PRD.procedure_event_table"
    session.sql(f"CREATE EVENT TABLE IF NOT EXISTS {event_table}").collect()
    session.sql(f"ALTER DATABASE {db_name} SET EVENT_TABLE = {event_table}").collect()
    # Delete old logs from event table or else they'll grow and will be permanently retained
    session.sql(
        f"DELETE FROM {event_table} WHERE TIMESTAMP < DATEADD('day', -90, CURRENT_TIMESTAMP())"
    ).collect()

    # Check if an instance of the synthetic data generator stored procedure is already running
    # before executing
    active_sproc = session.sql("""
        SELECT COUNT(*) as ACTIVE_COUNT
        FROM table(information_schema.query_history())
        WHERE execution_status = 'RUNNING'
            AND query_text ILIKE 'CALL generate_synthetic_data%'
    """).collect()

    if active_sproc[0]["ACTIVE_COUNT"] > 0:
        print(
            "ABORTED: The synthetic data generator stored procedure is already running. "
            "Please wait for the current run to complete before launching another. "
            "Verify completion by reviewing the logs in the event table."
        )
        sys.exit(1)

    print("Running stored procedure to generate synthetic data...")
    generator_sproc(*ordered_inputs)
    print("Finished!")


if __name__ == "__main__":
    main()
