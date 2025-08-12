import logging
import sys

import extractor
import loader
from extractor import Extractor, PostgresExtractor, SnowflakeExtractor
from loader import PostgresLoader, get_connection_string
from model import (
    IdrBeneficiary,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryMbiId,
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrBeneficiaryXref,
    IdrClaim,
    IdrClaimAnsiSignature,
    IdrClaimDateSignature,
    IdrClaimInstitutional,
    IdrClaimLine,
    IdrClaimLineInstitutional,
    IdrClaimProcedure,
    IdrClaimValue,
    IdrClaimProfessional,
    IdrClaimLineProfessional,
    LoadProgress,
    T,
)

logger = logging.getLogger(__name__)


def init_logger() -> None:
    logger.setLevel(logging.INFO)
    console_handler = logging.StreamHandler()
    formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)


def main() -> None:
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


def get_progress(connection_string: str, table_name: str) -> LoadProgress | None:
    return PostgresExtractor(connection_string, batch_size=1).extract_single(
        LoadProgress,
        LoadProgress.fetch_query(False),
        {LoadProgress.query_placeholder(): table_name},
    )


def extract_and_load(
    cls: type[T],
    data_extractor: Extractor,
    connection_string: str,
) -> PostgresLoader:
    logger.info("loading %s", cls.table())
    progress = get_progress(connection_string, cls.table())
    data_iter = data_extractor.extract_idr_data(
        cls,
        progress,
    )

    loader = PostgresLoader(connection_string)
    loader.load(data_iter, cls, progress)
    return loader


def load_all(data_extractor: Extractor, connection_string: str, *cls: type[T]) -> None:
    for c in cls:
        extract_and_load(c, data_extractor, connection_string)


def run_pipeline(data_extractor: Extractor, connection_string: str) -> None:
    logger.info("load start")

    load_all(
        data_extractor,
        connection_string,
        IdrBeneficiaryMbiId,
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
        IdrBeneficiaryXref,
        # Ignore for now, we'll likely source these elsewhere when we load contract data
        # IdrContractPbpNumber,
        # IdrElectionPeriodUsage,
        IdrClaim,
        IdrClaimInstitutional,
        IdrClaimDateSignature,
        IdrClaimValue,
        IdrClaimLine,
        IdrClaimLineInstitutional,
        IdrClaimAnsiSignature,
        IdrClaimProcedure,
        IdrClaimProfessional,
        IdrClaimLineProfessional
    )

    logger.info("done")
    extractor.print_timers()
    loader.print_timers()


if __name__ == "__main__":
    main()
