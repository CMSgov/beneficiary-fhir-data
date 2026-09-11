import click

from .generate_eob_sample import SampleGenerator


@click.command
@click.option(
    "--clm-uniq-id",
    type=str,
    required=True,
    help="Pass the claim unique ID.",
)
@click.option(
    "--source-directory",
    type=str,
    required=False,
    help="Pass the source directory containing the claim files.",
    default="out"
)
@click.option(
    "--output-directory",
    type=str,
    required=False,
    help="Pass the output directory for the result file.",
    default="sample-data"
)
def main(clm_uniq_id: str, source_directory: str, output_directory: str) -> None:
    generator = SampleGenerator(source_directory, output_directory)
    generator.run(clm_uniq_id=clm_uniq_id)


if __name__ == "__main__":
    main()
