import click

from .generate_prior_auth_sample import run


@click.command
@click.option(
    "--utn",
    type=str,
    required=True,
    help="Pass the UTN.",
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
def main(utn: str, source_directory: str, output_directory: str) -> None:
    """Generate a prior auth sample JSON from SYNTHETIC_PRAUC.csv based on UTN."""
    run(utn=utn, source_directory=source_directory, output_directory=output_directory)


if __name__ == "__main__":
    main()
