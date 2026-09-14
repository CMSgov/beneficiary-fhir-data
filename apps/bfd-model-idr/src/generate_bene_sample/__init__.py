import click

from .bene_generator import SampleGenerator


@click.command
@click.option(
    "--bene-sk",
    type=str,
    required=True,
    help="Pass the bene unique ID.",
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
def main(bene_sk: str, source_directory: str, output_directory: str) -> None:
    generator = SampleGenerator(source_directory, output_directory)
    generator.run(bene_sk=bene_sk)


if __name__ == "__main__":
    main()
