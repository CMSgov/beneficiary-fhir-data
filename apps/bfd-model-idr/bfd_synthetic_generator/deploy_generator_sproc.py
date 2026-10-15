import os
import sys
import zipfile
from typing import Any

import click
from claims_generator import GeneratePacDataMode
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from load_synthetic_output import BeneSkMode
from snowflake.snowpark import Session
from snowflake.snowpark.types import BooleanType, DataType, IntegerType, StringType, VariantType

_BATCH_SIZE = 50_000

_INPUT_TYPE_MAP: dict[Any, tuple[DataType, str]] = {
    click.BOOL: (BooleanType(), "BOOLEAN"),
    click.INT: (IntegerType(), "NUMBER"),
    click.STRING: (StringType(), "VARCHAR"),
    click.Choice: (StringType(), "VARCHAR"),
}


def _require_env(name: str) -> str:
    val = os.environ.get(name)
    if not val:
        print(
            f"Missing required env variable {name}. "
            + "Source load-synthetic-credentials.sh with BFD_ENV set before running."
        )
        sys.exit(1)
    return val


def _get_snowpark_and_sql_types(param: click.Parameter) -> tuple[DataType, str]:
    param_type = param.type
    if param.type in _INPUT_TYPE_MAP:
        return _INPUT_TYPE_MAP[param_type]

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
@click.option(
    "--force_ztm",
    type=bool,
    help=(
        'Allow "zero-to-many" rows (e.g. BENE_ENTLMT, c/d data, etc.) for an existing patient to '
        "be generated. This will introduce new rows for patients that previously had "
        "none. Useful if not all tables for a patient have been generated yet."
    ),
)
@click.option(
    "--patients",
    type=int,
    default=0,
    show_default=True,
    help="Number of NEW patients to generate. Does not affect patients regenerated when truncate "
    "flag is set to False",
)
@click.option(
    "--claims",
    type=bool,
    default=True,
    show_default=True,
    help="Automatically generate claims after patient generation using the generated BENE_HSTRY",
)
@click.option(
    "--min-claims",
    envvar="MIN_CLAIMS",
    type=int,
    default=5,
    show_default=True,
    help="Minimum number of claims to generate per person",
)
@click.option(
    "--max-claims",
    envvar="MAX_CLAIMS",
    type=int,
    default=5,
    show_default=True,
    help="Maximum number of claims to generate per person",
)
@click.option(
    "--enable-samhsa/--disable-samhsa",
    envvar="ENABLE_SAMHSA",
    type=bool,
    default=True,
    show_default=True,
    help="Enables generation of SAMHSA-related data",
)
@click.option(
    "--pac-gen",
    envvar="PAC_GEN",
    type=click.Choice(GeneratePacDataMode, case_sensitive=False),
    default=GeneratePacDataMode.IF_NONE,
    show_default=True,
    help=(
        "Generate new partially-adjudicated claims data based on choice. 'no' will never generate "
        "pac data, 'if_none' will generate if the input claims data has no pac CLMs, and "
        "'always' will force the generation always"
    ),
)
@click.option(
    "--bene-sk-mode",
    envvar="BENE_SK_MODE",
    type=click.Choice(BeneSkMode, case_sensitive=False),
    default=BeneSkMode.BOTH,
    show_default=True,
    help=(
        "Sets the mode for which tables from which distinct BENE_SKs are read when generating "
        "claims. 'bene_hstry' indicates that BENE_SKs are only loaded from BENE_HSTRY, 'clm' "
        "indicates loading from only from CLM. 'both' indicates loading from both"
    ),
)
@click.option(
    "--truncate",
    type=bool,
    default=False,
    show_default=True,
    help="Truncate tables before reloading. Default is false",
)
@click.option(
    "--batch_size",
    type=int,
    default=_BATCH_SIZE,
    show_default=True,
    help="Batch size of benes and claims to process",
)
@click.pass_context
def main(
    ctx: click.Context,
    force_ztm: bool,
    patients: int,
    claims: bool,
    min_claims: int,
    max_claims: int,
    enable_samhsa: bool,
    pac_gen: GeneratePacDataMode,
    bene_sk_mode: BeneSkMode,
    truncate: bool = False,
    batch_size: int = _BATCH_SIZE,
) -> None:
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
    for param in main.params:
        snowpark_type, sql_string = _get_snowpark_and_sql_types(param)
        input_types.append(snowpark_type)
        sql_input_types.append(sql_string)

    print("Creating/Updating stored procedure...")
    generator_sproc = session.sproc.register_from_file(
        file_path=f"@{db_name}.{schema_name}.generator_files_stage/bfd_synthetic_generator.zip",
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
        f"DELETE FROM {event_table} WHERE TIMESTAMP < DATEADD('week', -2, CURRENT_TIMESTAMP())"
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
    generator_sproc(
        force_ztm,
        pac_gen.value,
        bene_sk_mode.value,
        patients,
        claims,
        min_claims,
        max_claims,
        enable_samhsa,
        truncate,
        batch_size,
    )
    print("Finished!")


if __name__ == "__main__":
    main()
