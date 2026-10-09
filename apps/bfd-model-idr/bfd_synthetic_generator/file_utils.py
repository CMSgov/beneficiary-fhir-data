from pathlib import Path
from zipfile import Path as ZipPath


# Detect whether the code is running out of a normal directory (local development)
# or inside a zipped environment (Snowflake stored procedure)
def _find_root() -> Path | ZipPath:
    here = Path(__file__).resolve().parent
    for parent in here.parents:
        if parent.suffix == ".zip" and parent.is_file():
            inner = here.relative_to(parent).as_posix()
            return ZipPath(parent, "" if inner == "." else f"{inner}/")
    return here


ROOT = _find_root()
