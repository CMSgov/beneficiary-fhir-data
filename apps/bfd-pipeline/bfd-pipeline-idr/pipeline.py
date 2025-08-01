import datetime
import logging
import os
import sys

import extractor
import loader
from extractor import Extractor, PostgresExtractor, SnowflakeExtractor
from loader import PostgresLoader, get_connection_string
from model import (
    IdrBeneficiary,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryHistory,
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
    LoadProgress,
    T,
)

console_handler = logging.StreamHandler()
formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
console_handler.setFormatter(formatter)
logging.basicConfig(level=logging.INFO, handlers=[console_handler])

logger = logging.getLogger(__name__)


def main() -> None:
    batch_size = int(os.environ.get("IDR_BATCH_SIZE", "100_000"))
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode == "local":
        pg_local = "host=localhost dbname=idr user=bfd password=InsecureLocalDev"
        run_pipeline(
            PostgresExtractor(
                connection_string=pg_local,
                batch_size=batch_size,
            ),
            pg_local,
        )
    elif mode == "synthetic":
        run_pipeline(
            PostgresExtractor(
                connection_string=get_connection_string(),
                batch_size=batch_size,
            ),
            get_connection_string(),
        )
    else:
        run_pipeline(
            SnowflakeExtractor(
                batch_size=batch_size,
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
    batch_start = datetime.datetime.now()
    logger.info("progress for %s - %s", cls.table(), progress.last_ts if progress else "none")
    data_iter = data_extractor.extract_idr_data(
        cls,
        progress,
    )

    loader = PostgresLoader(connection_string)
    loader.load(data_iter, cls, batch_start, progress)
    return loader


def load_all(data_extractor: Extractor, connection_string: str, *cls: type[T]) -> None:
    for c in cls:
        extract_and_load(c, data_extractor, connection_string)


def run_pipeline(data_extractor: Extractor, connection_string: str) -> None:
    logger.info("load start")

    load_all(
        data_extractor,
        connection_string,
        IdrBeneficiaryHistory,
        IdrBeneficiaryMbiId,
    )

    bene_loader = extract_and_load(
        IdrBeneficiary,
        data_extractor,
        connection_string,
    )
    bene_loader.run_sql("SELECT idr.refresh_overshare_mbis()")

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
    )

    logger.info("done")
    extractor.print_timers()
    loader.print_timers()


if __name__ == "__main__":
    main()
