import logging
from loader import PostgresLoader
from model import IdrBeneficiary
from extractor import Extractor, PostgresExtractor

logger = logging.getLogger(__name__)

fetch_query = """
SELECT
    bene.bene_sk,
    bene.bene_mbi_id,
    bene.bene_1st_name,
    bene.bene_last_name
FROM
    cms_vdm_view_mdcr_prd.v2_mdcr_bene bene
WHERE
    bene.idr_trans_obslt_ts > '9999-12-30' 
    AND bene.bene_xref_efctv_sk = bene.bene_sk
"""


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
    logger.info("fetching IDR data")
    iter = extractor.extract(IdrBeneficiary, fetch_query, {})

    logger.info("loading data")
    PostgresLoader(
        connection_string=connection_string,
        table="idr.beneficiary",
        temp_table="beneficiary_temp",
        primary_key="bene_sk",
        exclude_cols=["bene_id"],
        insert_cols=["bene_sk", "bene_mbi_id", "bene_1st_name", "bene_last_name"],
    ).load(iter)
    logger.info("done")


if __name__ == "__main__":
    main()
