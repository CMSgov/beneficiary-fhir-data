import logging
import os
import sys
import time
from datetime import datetime

from snowflake.connector.network import ReauthenticationRequest, RetryRequest

import extractor
import loader
from extractor import Extractor, PostgresExtractor, SnowflakeExtractor
from loader import PostgresLoader, get_connection_string
from model import (
    IdrBeneficiary,
    IdrBeneficiaryDualEligibility,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryMbiId,
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrClaim,
    IdrClaimAnsiSignature,
    IdrClaimDateSignature,
    IdrClaimInstitutional,
    IdrClaimItem,
    IdrClaimLineInstitutional,
    IdrClaimLineProfessional,
    IdrClaimProfessional,
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


def get_progress(
    connection_string: str, table_name: str, start_time: datetime
) -> LoadProgress | None:
    return PostgresExtractor(connection_string, batch_size=1).extract_single(
        LoadProgress,
        LoadProgress.fetch_query(False, start_time),
        {LoadProgress.query_placeholder(): table_name},
    )


def extract_and_load(
    cls: type[T],
    data_extractor: Extractor,
    connection_string: str,
) -> tuple[PostgresLoader, bool]:
    logger.info("loading %s", cls.table())
    batch_start = datetime.now()
    progress = get_progress(connection_string, cls.table(), batch_start)

    logger.info(
        "progress for %s - last_ts: %s batch_start_ts: %s batch_complete_ts: %s",
        cls.table(),
        progress.last_ts if progress else "none",
        progress.batch_start_ts if progress else "none",
        progress.batch_complete_ts if progress else "none",
    )
    max_attempts = 5
    loader = PostgresLoader(connection_string)
    for attempt in range(max_attempts):
        try:
            data_iter = data_extractor.extract_idr_data(cls, progress, batch_start)
            data_loaded = loader.load(data_iter, cls, batch_start, progress)
            return (loader, data_loaded)
        # Snowflake will throw a reauth error if the pipeline has been running for several hours
        # but it seems to be wrapped in a ProgrammingError.
        # Unclear the best way to handle this, it will require a bit more trial and error
        except (ReauthenticationRequest, RetryRequest) as ex:
            logger.warning("received transient error, retrying...", exc_info=ex)
            data_extractor.reconnect()
            if attempt == max_attempts - 1:
                logger.error("max attempts exceeded")
                raise ex
            time.sleep(1)
    return (loader, False)


def load_all(data_extractor: Extractor, connection_string: str, *cls: type[T]) -> None:
    for c in cls:
        extract_and_load(c, data_extractor, connection_string)


def parse_bool(var: str) -> bool:
    # bool(str) interprets anything non-empty as true so we gotta do it manually
    return var.lower() == "true" or var == "1"


def run_pipeline(data_extractor: Extractor, connection_string: str) -> None:
    logger.info("load start")

    # temporary flags to load a subset of data for testing
    load_benes = parse_bool(os.environ.get("IDR_LOAD_BENES", "true"))
    load_claims = parse_bool(os.environ.get("IDR_LOAD_CLAIMS", "true"))

    if load_benes:
        load_all(
            data_extractor,
            connection_string,
            IdrBeneficiaryMbiId,
        )

        (bene_loader, data_loaded) = extract_and_load(
            IdrBeneficiary,
            data_extractor,
            connection_string,
        )
        if data_loaded:
            bene_loader.run_sql("SELECT idr.refresh_overshare_mbis()")

        load_all(
            data_extractor,
            connection_string,
            IdrBeneficiaryStatus,
            IdrBeneficiaryThirdParty,
            IdrBeneficiaryEntitlement,
            IdrBeneficiaryEntitlementReason,
            IdrBeneficiaryDualEligibility,
            # Ignore for now, we'll likely source these elsewhere when we load contract data
            # IdrContractPbpNumber,
            # IdrElectionPeriodUsage,
        )

    if load_claims:
        load_all(
            data_extractor,
            connection_string,
            IdrClaim,
            IdrClaimInstitutional,
            IdrClaimDateSignature,
            IdrClaimItem,
            IdrClaimLineInstitutional,
            IdrClaimAnsiSignature,
            IdrClaimProfessional,
            IdrClaimLineProfessional,
        )

    logger.info("done")
    extractor.print_timers()
    loader.print_timers()


if __name__ == "__main__":
    main()
