import sys
from datetime import date, datetime

import click

from .coverage_generator import MedicarePart, SampleGenerator

parts_values_str = f"{','.join(str(part.value) for part in MedicarePart)}"


@click.command
@click.option(
    "--bene-sk",
    type=str,
    required=True,
    help="Pass the bene unique ID.",
)
@click.option(
    "--parts",
    type=str,
    required=True,
    help=f"Parts request comma delimited without spaces. Exceptable values: {parts_values_str}",
)
@click.option(
    "--current-date",
    type=str,
    required=False,
    help="Current Date for the samples. Format: YYYY-MM-DD",
    default=date.today,
)
@click.option(
    "--source-directory",
    type=str,
    required=False,
    help="Pass the source directory containing the bene files.",
    default="out",
)
@click.option(
    "--output-directory",
    type=str,
    required=False,
    help="Pass the output directory for the result file.",
    default="sample-data",
)
def main(
    bene_sk: str, parts: str, current_date: str, source_directory: str, output_directory: str
) -> None:
    try:
        medicare_parts = [MedicarePart[part.strip().upper()] for part in parts.split(",")]
    except KeyError:
        print(f"Error Invalid parts: {parts}")
        sys.exit(1)

    generator = SampleGenerator(
        bene_sk,
        source_directory,
        output_directory,
        medicare_parts,
        datetime.strptime(current_date, "%Y-%m-%d").date(),
    )
    generator.run()


if __name__ == "__main__":
    main()
