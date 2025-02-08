import psycopg
from model import IdrBeneficiary
from source.db_executor import Fetcher, PostgresFetcher, copy_data


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


def main():
    run(PostgresFetcher(100_000))


def run(fetcher: Fetcher):
    conn = psycopg.connect(
        "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev"
    )
    iter = fetcher.fetch(IdrBeneficiary, fetch_query, {})
    copy_data(
        conn,
        "idr.beneficiary",
        "beneficiary_temp",
        "bene_sk",
        ["bene_id"],
        ["bene_sk", "bene_mbi_id", "bene_1st_name", "bene_last_name"],
        iter,
    )


if __name__ == "__main__":
    main()
