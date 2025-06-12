import logging
import sys
import os
from loader import PostgresLoader
import loader
from model import (
    T,
    IdrBeneficiary,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryHistory,
    IdrBeneficiaryMbi,
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrContractPbpNumber,
    IdrElectionPeriodUsage,
)
from extractor import Extractor, PostgresExtractor, SnowflakeExtractor, print_timers
import extractor

logger = logging.getLogger(__name__)


def init_logger():
    logger.setLevel(logging.INFO)
    console_handler = logging.StreamHandler()
    formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)


def main():
    init_logger()
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode == "local":
        pg_local = "host=localhost dbname=idr user=bfd password=InsecureLocalDev"
        run_pipeline(
            PostgresExtractor(
                connection_string=pg_local,
                batch_size=100_000,
            ),
            pg_local,
        )
    elif mode == "synthetic":
        run_pipeline(
            PostgresExtractor(
                connection_string=get_connection_string(),
                batch_size=100_000,
            ),
            get_connection_string(),
        )
    else:
        run_pipeline(
            SnowflakeExtractor(
                batch_size=100_000,
            ),
            get_connection_string(),
        )

def get_connection_string():
    return f"host={os.environ["BFD_DB_ENDPOINT"]} dbname=idr user={os.environ["BFD_DB_USERNAME"]} password={os.environ["BFD_DB_PASSWORD"]}"

def extract_and_load(
    cls: type[T],
    data_extractor: Extractor,
    fetch_query: str,
    table_to_load: str,
    unique_key: list[str],
    exclude_keys: list[str],
    connection_string: str,
):
    data_iter = data_extractor.extract_idr_data(
        cls,
        connection_string=connection_string,
        fetch_query=fetch_query,
        table=table_to_load,
    )

    loader = PostgresLoader(
        connection_string=connection_string,
        table=table_to_load,
        unique_key=unique_key,
        exclude_keys=exclude_keys,
    )
    loader.load(data_iter, cls)
    return loader


def run_pipeline(data_extractor: Extractor, connection_string: str):
    logger.info("load start")

    extract_and_load(
        IdrBeneficiaryHistory,
        data_extractor,
        fetch_query="""
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry
            {WHERE_CLAUSE}
            {ORDER_BY}
        """,
        table_to_load="idr.beneficiary_history",
        unique_key=["bene_sk", "idr_trans_efctv_ts"],
        exclude_keys=["bene_xref_efctv_sk_computed"],
        connection_string=connection_string,
    )

    extract_and_load(
        IdrBeneficiaryMbi,
        data_extractor,
        fetch_query="""
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mbi_id
            {WHERE_CLAUSE}
            {ORDER_BY}
        """,
        table_to_load="idr.beneficiary_mbi_id",
        unique_key=["bene_mbi_id", "idr_trans_efctv_ts"],
        exclude_keys=[],
        connection_string=connection_string,
    )

    bene_loader = extract_and_load(
        IdrBeneficiary,
        data_extractor,
        fetch_query="""
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene
            {WHERE_CLAUSE}
            {ORDER_BY}
        """,
        table_to_load="idr.beneficiary",
        unique_key=["bene_sk"],
        exclude_keys=["bene_xref_efctv_sk_computed"],
        connection_string=connection_string,
    )

    bene_loader.refresh_materialized_view("idr.overshare_mbis")

    extract_and_load(
        IdrBeneficiaryStatus,
        data_extractor,
        fetch_query="""
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_stus
            {WHERE_CLAUSE}
            {ORDER_BY}
        """,
        table_to_load="idr.beneficiary_status",
        unique_key=[
            "bene_sk",
            "mdcr_stus_bgn_dt",
            "mdcr_stus_end_dt",
            "idr_trans_efctv_ts",
        ],
        exclude_keys=[],
        connection_string=connection_string,
    )

    extract_and_load(
        IdrBeneficiaryThirdParty,
        data_extractor,
        fetch_query="""
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_tp
            {WHERE_CLAUSE}
            {ORDER_BY}
        """,
        table_to_load="idr.beneficiary_third_party",
        unique_key=[
            "bene_sk",
            "bene_rng_bgn_dt",
            "bene_rng_end_dt",
            "bene_tp_type_cd",
            "idr_trans_efctv_ts",
        ],
        exclude_keys=[],
        connection_string=connection_string,
    )

    extract_and_load(
        IdrBeneficiaryEntitlement,
        data_extractor,
        fetch_query="""
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt
            {WHERE_CLAUSE}
            {ORDER_BY}
        """,
        table_to_load="idr.beneficiary_entitlement",
        unique_key=[
            "bene_sk",
            "bene_rng_bgn_dt",
            "bene_rng_end_dt",
            "bene_mdcr_entlmt_type_cd",
            "idr_trans_efctv_ts",
        ],
        exclude_keys=[],
        connection_string=connection_string,
    )

    extract_and_load(
        IdrBeneficiaryEntitlementReason,
        data_extractor,
        fetch_query="""
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt_rsn
            {WHERE_CLAUSE}
            {ORDER_BY}
        """,
        table_to_load="idr.beneficiary_entitlement_reason",
        unique_key=[
            "bene_sk",
            "bene_rng_bgn_dt",
            "bene_rng_end_dt",
            "idr_trans_efctv_ts",
        ],
        exclude_keys=[],
        connection_string=connection_string,
    )

    # number of records in this table is relatively small (~300,000) and we don't have created/updated timestamps
    # so we can just sync all of the non-obsolete records each time
    pbp_fetch_query = data_extractor.get_query(
        IdrContractPbpNumber,
        """
        SELECT {COLUMNS}
        FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num
        WHERE cntrct_pbp_sk_obslt_dt >= '9999-12-31'
        """,
    )
    pbp_iter = data_extractor.extract_many(IdrContractPbpNumber, pbp_fetch_query, {})
    pbp_loader = PostgresLoader(
        connection_string=connection_string,
        table="idr.contract_pbp_number",
        unique_key=["cntrct_pbp_sk"],
        exclude_keys=[],
    )
    pbp_loader.load(pbp_iter, IdrContractPbpNumber)

    extract_and_load(
        IdrElectionPeriodUsage,
        data_extractor,
        # equivalent to "select distinct on", but Snowflake has different syntax for that so it's unfortunately not portable
        fetch_query="""
            WITH dupes as (
                SELECT {COLUMNS}, ROW_NUMBER() OVER (PARTITION BY bene_sk, cntrct_pbp_sk, bene_enrlmt_efctv_dt 
                {ORDER_BY} DESC) as row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_elctn_prd_usg
                {WHERE_CLAUSE}
                {ORDER_BY}
            )
            SELECT {COLUMNS} FROM dupes WHERE row_order = 1
            """,
        table_to_load="idr.beneficiary_election_period_usage",
        unique_key=["bene_sk", "cntrct_pbp_sk", "bene_enrlmt_efctv_dt"],
        exclude_keys=[],
        connection_string=connection_string,
    )

    logger.info("done")
    extractor.print_timers()
    loader.print_timers()


if __name__ == "__main__":
    main()
