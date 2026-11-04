import sys
import zipfile
from enum import Enum
from pathlib import Path

import click
from shared_options import common_options
from snowflake.snowpark.types import BooleanType, DataType, IntegerType, StringType, VariantType
from snowflake_session import create_snowflake_session


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
    current_dir = Path(__file__).resolve().parent
    folder_to_zip = current_dir.name
    parent_dir = current_dir.parent
    output_zip_path = parent_dir / f"{folder_to_zip}.zip"

    with zipfile.ZipFile(output_zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        for root, _, files in current_dir.walk(follow_symlinks=True):
            for file in files:
                full_path = root / file
                relative_path = full_path.relative_to(parent_dir)
                zipf.write(full_path, relative_path)
    return str(output_zip_path)


@click.command
@common_options
def main(**kwargs) -> None:
    min_claims = kwargs["min_claims"]
    max_claims = kwargs["max_claims"]
    if min_claims > max_claims:
        raise click.UsageError(
            f"min claims value of {min_claims} is greater than max claims value of {max_claims}"
        )

    session = create_snowflake_session()
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
