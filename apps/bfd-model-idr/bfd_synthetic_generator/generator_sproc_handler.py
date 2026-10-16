import logging
import os
import sys
import time

from snowflake.snowpark import Session

# Needed to find generator files inside Snowflake's environment
current_file_dir = os.path.dirname(os.path.abspath(__file__))
package_root_dir = os.path.dirname(current_file_dir)

for path in (current_file_dir, package_root_dir):
    if path not in sys.path:
        sys.path.append(path)

from claims_generator import GeneratePacDataMode, generate  # noqa: E402
from load_synthetic_output import BeneSkMode, SnowflakeWriter  # noqa: E402
from patient_generator import load_inputs  # noqa: E402

logger = logging.getLogger(__name__)

_BATCH_SIZE = 50_000


def generate_synthetic_data(
    session: Session,
    force_ztm: bool,
    pac_gen: str,
    bene_sk_mode: str,
    patients: int = 0,
    claims: bool = True,
    min_claims: int = 5,
    max_claims: int = 5,
    enable_samhsa: bool = True,
    truncate: bool = False,
    batch_size: int = _BATCH_SIZE,
) -> None:
    perf_start = time.perf_counter()
    db = session.get_current_database().replace('"', "")
    schema = session.get_current_schema().replace('"', "")
    writer = SnowflakeWriter(session, db, schema)

    # generate patient data
    load_inputs(
        patients=patients,
        force_ztm=force_ztm,
        batch_size=batch_size,
        truncate=truncate,
        writer=writer,
    )

    pac_gen = pac_gen.split(".")[-1].upper() if pac_gen else "IF_NONE"
    bene_sk_mode = bene_sk_mode.split(".")[-1].upper() if bene_sk_mode else "BOTH"

    # generate claim data
    if claims:
        generate(
            min_claims=min_claims,
            max_claims=max_claims,
            enable_samhsa=enable_samhsa,
            pac_gen=GeneratePacDataMode[pac_gen],
            bene_sk_mode=BeneSkMode[bene_sk_mode],
            writer=writer,
            truncate=truncate,
            batch_size=batch_size,
        )

    duration = time.perf_counter() - perf_start
    logger.info(f"{duration:.6f} seconds to run the procedure")
