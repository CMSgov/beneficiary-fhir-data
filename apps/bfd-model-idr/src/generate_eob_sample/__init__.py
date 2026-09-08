import click

from .generate_eob_sample import run


@click.command
@click.option(
    "--clm-uniq-id",
    type=str,
    required=True,
    help="Pass the claim unique ID.",
)
def main(clm_uniq_id: str):
    run(clm_uniq_id)


if __name__ == "__main__":
    main()
