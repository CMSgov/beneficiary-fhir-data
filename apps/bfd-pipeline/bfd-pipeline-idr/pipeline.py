import logging
import sys
from loader import PostgresLoader, get_connection_string
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
    IdrClaimAnsiSignature,
    IdrClaimDateSignature,
    IdrClaimInstitutional,
    IdrClaimLine,
    IdrClaimLineInstitutional,
    IdrClaimProcedure,
    IdrClaimValue,
    IdrContractPbpNumber,
    IdrElectionPeriodUsage,
)
from extractor import Extractor, PostgresExtractor, SnowflakeExtractor
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
        pg_connection = get_connection_string()
        run_pipeline(
            PostgresExtractor(
                connection_string=pg_connection,
                batch_size=100_000,
            ),
            pg_connection,
        )
    else:
        pg_connection = get_connection_string()
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


def load_all(data_extractor: Extractor, connection_string: str, *cls: type[T]):
    for c in cls:
        extract_and_load(c, data_extractor, connection_string)


def run_pipeline(data_extractor: Extractor, connection_string: str):
    logger.info("load start")

    load_all(
        data_extractor,
        connection_string,
        IdrBeneficiaryHistory,
        IdrBeneficiaryMbiId,
        IdrBeneficiary,
    )

    bene_loader = extract_and_load(
        IdrBeneficiary,
        data_extractor,
        connection_string,
    )
    bene_loader.refresh_materialized_view("idr.overshare_mbis")

    load_all(
        data_extractor,
        connection_string,
        IdrBeneficiaryStatus,
        IdrBeneficiaryThirdParty,
        IdrBeneficiaryEntitlement,
        IdrBeneficiaryEntitlementReason,
        IdrContractPbpNumber,
        IdrElectionPeriodUsage,
        IdrClaim,
        IdrClaimInstitutional,
        IdrClaimDateSignature,
        IdrClaimValue,
        IdrClaimLine,
        IdrClaimLineInstitutional,
        IdrClaimAnsiSignature,
        IdrClaimProcedure,
    )

    logger.info("done")
    extractor.print_timers()
    loader.print_timers()


if __name__ == "__main__":
    main()
