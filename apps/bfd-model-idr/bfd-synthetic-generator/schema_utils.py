from collections import defaultdict
from dataclasses import dataclass, field

from constants import CLM_RLT_OCRNC_SGNTR_MBR, PRAUC
from settings import SETTINGS

SYNTHETIC_PREFIX = "SYNTHETIC"
TABLE_PREFIX = "V2_MDCR"
IDR_SCHEMAS = ["cms_vdm_view_mdcr_prd", "cms_edp_view_cvm_prau_prd"]


@dataclass
class TableTarget:
    name: str | None = None
    schema: str | None = None
    database: str | None = None


TABLE_OVERRIDES: dict[str, TableTarget] = {
    PRAUC: TableTarget(name="PRAUC", schema="CMS_EDP_VIEW_CVM_PRAU_PRD"),
    CLM_RLT_OCRNC_SGNTR_MBR: TableTarget(name="V2_CLM_RLT_OCRNC_SGNTR_MBR"),
}


@dataclass
class TableSchema:
    primary_keys: list[str] = field(default_factory=list)
    columns: list[str] = field(default_factory=list)


def get_bfd_connection_string() -> str:
    return (
        f"host='{SETTINGS.bfd_db_endpoint}' "
        f"port='{SETTINGS.bfd_db_port}' "
        f"dbname='{SETTINGS.bfd_db_name}' "
        f"user='{SETTINGS.bfd_db_username}' "
        f"password='{SETTINGS.bfd_db_password}'"
    )


def get_table_name_from_file_name(raw_name: str) -> str:
    override = TABLE_OVERRIDES.get(raw_name)
    if override is not None:
        return override.name or raw_name
    return TABLE_PREFIX + raw_name.removeprefix(SYNTHETIC_PREFIX)


def fetch_schemas() -> dict[str, TableSchema]:
    # Top-level import of psycopg crashes the Snowflake sproc since Snowflake doesn't have
    # Postgres drivers installed. Lazy import psycopg so importing this module inside
    # the Snowflake sproc does not fail. Also, this method is only called during
    # the local CSV flow so there's no need for psycopg in the Snowflake flow
    import psycopg

    query = """
        SELECT
            UPPER(c.table_name) AS table_name,
            UPPER(c.column_name) AS column_name,
            CASE
                WHEN tc.constraint_type = 'PRIMARY KEY' THEN TRUE
                ELSE FALSE
            END AS is_primary_key
        FROM
            information_schema.columns c
        LEFT JOIN
            information_schema.key_column_usage kcu
            ON c.table_schema = kcu.table_schema
            AND c.table_name = kcu.table_name
            AND c.column_name = kcu.column_name
        LEFT JOIN
            information_schema.table_constraints tc
            ON kcu.table_schema = tc.table_schema
            AND kcu.table_name = tc.table_name
            AND kcu.constraint_name = tc.constraint_name
            AND tc.constraint_type = 'PRIMARY KEY'
        WHERE
            c.table_schema = ANY(%s)
        ORDER BY
            c.table_name,
            c.ordinal_position;
    """

    schema_cache: defaultdict[str, TableSchema] = defaultdict(TableSchema)

    with (
        psycopg.connect(get_bfd_connection_string()) as conn,
        conn.execute(query, (IDR_SCHEMAS,)) as cur,
    ):
        for table_name, column_name, is_pk in cur.fetchall():
            if is_pk:
                schema_cache[table_name].primary_keys.append(column_name)
            else:
                schema_cache[table_name].columns.append(column_name)

    return dict(schema_cache)
