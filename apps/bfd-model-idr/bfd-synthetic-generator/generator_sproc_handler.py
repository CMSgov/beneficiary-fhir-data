import logging
import sys
import time
from pathlib import Path

from snowflake.snowpark import Session

current_file_dir = Path(__file__).resolve().parent
package_root_dir = current_file_dir.parent

for path_obj in (current_file_dir, package_root_dir):
    string_path = str(path_obj)
    if string_path not in sys.path:
        sys.path.append(string_path)

from claims_generator import GeneratePacDataMode, generate  # noqa: E402
from load_synthetic_output import BeneSkMode, SnowflakeWriter, parse_overwrites  # noqa: E402
from patient_generator import load_inputs  # noqa: E402

logger = logging.getLogger(__name__)


def generate_synthetic_data(
    session: Session,
    overwrite: str,
    batch_size: int,
    truncate: bool,
    bene_sk_mode: str,
    pac_gen: str,
    samhsa: bool,
    max_claims: int,
    min_claims: int,
    claims: bool,
    patients: int,
    force_ztm: bool,
) -> None:
    perf_start = time.perf_counter()
    db = session.get_current_database().replace('"', "")
    schema = session.get_current_schema().replace('"', "")
    writer = SnowflakeWriter(session, db, schema, parse_overwrites(overwrite))

    # generate patient data
    load_inputs(
        patients=patients,
        force_ztm=force_ztm,
        batch_size=batch_size,
        truncate=truncate,
        writer=writer,
    )

    # generate claim data
    if claims:
        generate(
            min_claims=min_claims,
            max_claims=max_claims,
            samhsa=samhsa,
            pac_gen=GeneratePacDataMode[pac_gen],
            bene_sk_mode=BeneSkMode[bene_sk_mode],
            writer=writer,
            truncate=truncate,
            batch_size=batch_size,
        )

    duration = time.perf_counter() - perf_start
    logger.info("%.3f seconds to run the procedure", duration)
