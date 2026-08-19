import shutil
import subprocess
import tempfile
from pathlib import Path

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization

from .settings import (
    idr_account,
    idr_database,
    idr_private_key,
    idr_schema,
    idr_username,
    idr_warehouse,
    output_dir,
)


def run_flyway() -> None:

    with tempfile.NamedTemporaryFile(
            delete=False, 
            suffix=".p8", 
            dir=output_dir(), 
            mode="w"
        ) as temp_key_file:
        temp_key_file.write(idr_private_key())
        temp_key_path = Path(temp_key_file.name)

    try:
        jdbc_url = (
            f"jdbc:snowflake://{idr_account()}.snowflakecomputing.com/?"
            f"warehouse={idr_warehouse()}&db={idr_database()}&schema={idr_schema()}&"
            f"private_key_file={temp_key_path.as_posix()}&JDBC_QUERY_RESULT_FORMAT=JSON"
        )


        print("Executing Flyway migration through Maven...")

        mvn = shutil.which("mvn") or "mvn"
        subprocess.run(
            f"{mvn} flyway:migrate "
            f'-Dflyway.url="{jdbc_url}" '
            f"-Dflyway.user={idr_username()} "
            "-Duser.timezone=UTC",
            cwd=Path(__file__).parent.parent.parent.joinpath("../bfd-db-migrator-synthetic"),
            shell=True,
            capture_output=True,
            check=True,
            text=True,
        )
        
        print("Migration completed successfully.")

    except subprocess.CalledProcessError as e:
        print(f"Maven Flyway migration failed with exit code: {e.returncode}")
        raise e
        
    finally:
        if Path(temp_key_path).exists():
           Path(temp_key_path).unlink()
