import subprocess
import sys
import urllib.request
from pathlib import Path

import click


@click.command
@click.option(
    "--validator-cli-version",
    type=str,
    required=False,
    help="The validator cli version.",
    default="6.7.10",
)
def main(validator_cli_version: str) -> None:
    try:
        git_root = subprocess.check_output(
            ["git", "rev-parse", "--show-toplevel"], text=True, stderr=subprocess.STDOUT
        ).strip()
    except subprocess.CalledProcessError as e:
        print(f"Error: Failed to find git repository root.\n{e.output}", file=sys.stderr)
        sys.exit(1)

    jar_path = get_validator_jar_path()

    if (jar_path.exists()):
        print("validator_cli.jar exists. Exiting.")
        sys.exit(0)

    url = f"https://github.com/hapifhir/org.hl7.fhir.core/releases/download/{validator_cli_version}/validator_cli.jar"
    try:
        urllib.request.urlretrieve(url, jar_path.resolve())
    except Exception as e:
        print(f"Error downloading the jar file: {e}", file=sys.stderr)
        sys.exit(1)

def get_validator_jar_path() -> Path:
    try:
        git_root = subprocess.check_output(
            ["git", "rev-parse", "--show-toplevel"], text=True, stderr=subprocess.STDOUT
        ).strip()
    except subprocess.CalledProcessError as e:
        print(f"Error: Failed to find git repository root.\n{e.output}", file=sys.stderr)
        sys.exit(1)

    return Path(f"{git_root}/apps/bfd-model-idr/validator_cli.jar")


if __name__ == "__main__":
    main()
