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
    IdrBeneficiaryMbiId,
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrClaim,
    IdrClaimDateSignature,
    IdrClaimInstitutional,
    IdrClaimLine,
    IdrClaimLineInstitutional,
    IdrClaimValue,
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
    pg_connection = f"host={os.environ["BFD_DB_ENDPOINT"]} dbname=idr user={os.environ["BFD_DB_USERNAME"]} password={os.environ["BFD_DB_PASSWORD"]}"
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
                connection_string=pg_connection,
                batch_size=100_000,
            ),
            pg_connection,
        )
    else:
        run_pipeline(
            SnowflakeExtractor(
                batch_size=100_000,
            ),
            pg_connection,
        )


def extract_and_load(
    cls: type[T],
    data_extractor: Extractor,
    connection_string: str,
):
    data_iter = data_extractor.extract_idr_data(
        cls,
        connection_string,
    )

    loader = PostgresLoader(connection_string)
    loader.load(data_iter, cls)
    return loader


def run_pipeline(data_extractor: Extractor, connection_string: str):
    logger.info("load start")

    extract_and_load(
        IdrBeneficiaryHistory,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrBeneficiaryMbiId,
        data_extractor,
        connection_string,
    )

    bene_loader = extract_and_load(
        IdrBeneficiary,
        data_extractor,
        connection_string,
    )

    bene_loader.refresh_materialized_view("idr.overshare_mbis")

    extract_and_load(
        IdrBeneficiaryStatus,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrBeneficiaryThirdParty,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrBeneficiaryEntitlement,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrBeneficiaryEntitlementReason,
        data_extractor,
        connection_string,
    )

    # number of records in this table is relatively small (~300,000) and we don't have created/updated timestamps
    # so we can just sync all of the non-obsolete records each time
    pbp_fetch_query = data_extractor.get_query(IdrContractPbpNumber)
    pbp_iter = data_extractor.extract_many(IdrContractPbpNumber, pbp_fetch_query, {})
    pbp_loader = PostgresLoader(connection_string)
    pbp_loader.load(pbp_iter, IdrContractPbpNumber)

    extract_and_load(
        IdrElectionPeriodUsage,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrClaim,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrClaimInstitutional,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrClaimDateSignature,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrClaimValue,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrClaimLine,
        data_extractor,
        connection_string,
    )

    extract_and_load(
        IdrClaimLineInstitutional,
        data_extractor,
        connection_string,
    )

    logger.info("done")
    extractor.print_timers()
    loader.print_timers()


if __name__ == "__main__":
    main()
