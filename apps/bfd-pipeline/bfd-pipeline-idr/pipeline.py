import logging
from loader import PostgresLoader
from model import IdrBeneficiary, LoadProgress
from extractor import Extractor, PostgresExtractor

logger = logging.getLogger(__name__)

fetch_query = """
SELECT
    bene.bene_sk,
    bene.bene_xref_efctv_sk, 
    bene.bene_mbi_id,
    bene.bene_1st_name,
    bene.bene_midl_name,
    bene.bene_last_name,
    bene.bene_brth_dt,
    bene.bene_sex_cd,
    bene.bene_race_cd,
    bene.geo_usps_state_cd,
    bene.geo_zip5_cd,
    bene.geo_zip4_cd,
    bene.geo_zip_plc_name,
    bene.bene_line_1_adr,
    bene.bene_line_2_adr,
    bene.bene_line_3_adr,
    bene.bene_line_4_adr,
    bene.bene_line_5_adr,
    bene.bene_line_6_adr,
    bene.cntct_lang_cd,
    bene.idr_trans_efctv_ts,
    bene.idr_trans_obslt_ts
FROM
    cms_vdm_view_mdcr_prd.v2_mdcr_bene bene
{WHERE_CLAUSE}
ORDER BY idr_trans_efctv_ts, bene_sk
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
    progress = PostgresExtractor(connection_string, 1).extract_single(
        LoadProgress,
        "SELECT table_name, last_id, last_timestamp FROM idr.load_progress WHERE table_name = 'idr.beneficiary'",
        {},
    )
    logger.info("fetching IDR data")
    loader = PostgresLoader(
        connection_string=connection_string,
        table="idr.beneficiary",
        temp_table="beneficiary_temp",
        primary_key="bene_sk",
        exclude_cols=[],
        insert_cols=[
            "bene_sk",
            "bene_xref_efctv_sk",
            "bene_mbi_id",
            "bene_1st_name",
            "bene_midl_name",
            "bene_last_name",
            "bene_brth_dt",
            "bene_sex_cd",
            "bene_race_cd",
            "geo_usps_state_cd",
            "geo_zip5_cd",
            "geo_zip4_cd",
            "geo_zip_plc_name",
            "bene_line_1_adr",
            "bene_line_2_adr",
            "bene_line_3_adr",
            "bene_line_4_adr",
            "bene_line_5_adr",
            "bene_line_6_adr",
            "cntct_lang_cd",
            "idr_trans_efctv_ts",
            "idr_trans_obslt_ts",
        ],
    )
    logger.info("loading data")

    if progress is not None:
        iter = extractor.extract_many(
            IdrBeneficiary,
            fetch_query.replace(
                "{WHERE_CLAUSE}",
                "WHERE (idr_trans_efctv_ts = %(timestamp)s AND bene_sk > %(bene_sk)s) OR idr_trans_efctv_ts > %(timestamp)s",
            ),
            {"timestamp": progress.last_timestamp, "bene_sk": progress.last_id},
        )
        loader.load(iter)
    else:
        iter = extractor.extract_many(
            IdrBeneficiary, fetch_query.replace("{WHERE_CLAUSE}", ""), {}
        )
        loader.load(iter)
    logger.info("done")


if __name__ == "__main__":
    main()
