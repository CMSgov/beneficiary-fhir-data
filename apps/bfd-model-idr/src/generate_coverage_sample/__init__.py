import click

from .coverage_generator import SampleGenerator


@click.command
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
def main(source_directory: str, output_directory: str) -> None:
    generator = SampleGenerator(source_directory, output_directory)
    generator.run()


if __name__ == "__main__":
    main()
