import logging
from loader import PostgresLoader
from model import IdrBeneficiary, IdrBeneficiaryHistory, LoadProgress
from extractor import Extractor, PostgresExtractor, fetch_bene_data

logger = logging.getLogger(__name__)


def init_logger():
    logger.setLevel(logging.INFO)
    console_handler = logging.StreamHandler()
    formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)


def main():
    init_logger()

    run_pipeline(
        PostgresExtractor(
            "host=localhost dbname=idr user=bfd password=InsecureLocalDev", 100_000
        ),
        "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev",
    )


def run_pipeline(extractor: Extractor, connection_string: str):
    logger.info("load start")

    history_fetch_query = """
    SELECT
        {COLUMNS}
    FROM
        cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry
    {WHERE_CLAUSE}
    ORDER BY idr_trans_efctv_ts, bene_sk
    """
    history_iter = fetch_bene_data(
        extractor,
        IdrBeneficiaryHistory,
        connection_string,
        "idr.beneficiary_history",
        history_fetch_query,
    )

    history_loader = PostgresLoader(
        connection_string=connection_string,
        table="idr.beneficiary_history",
        temp_table="beneficiary_history_temp",
        unique_key=["bene_sk", "idr_trans_efctv_ts"],
        sort_key="bene_sk",
        exclude_keys=["bene_xref_efctv_sk_computed"],
    )
    history_loader.load(history_iter, IdrBeneficiaryHistory)

    bene_fetch_query = """
    SELECT
        {COLUMNS}
    FROM
        cms_vdm_view_mdcr_prd.v2_mdcr_bene
    {WHERE_CLAUSE}
    ORDER BY idr_trans_efctv_ts, bene_sk
    """
    bene_iter = fetch_bene_data(
        extractor,
        IdrBeneficiary,
        connection_string,
        "idr.beneficiary",
        bene_fetch_query,
    )

    bene_loader = PostgresLoader(
        connection_string=connection_string,
        table="idr.beneficiary",
        temp_table="beneficiary_temp",
        unique_key=["bene_sk"],
        sort_key="bene_sk",
        exclude_keys=["bene_xref_efctv_sk_computed"],
    )
    bene_loader.load(bene_iter, IdrBeneficiary)
    bene_loader.refresh_materialized_view("idr.overshare_mbis")

    logger.info("done")


if __name__ == "__main__":
    main()
