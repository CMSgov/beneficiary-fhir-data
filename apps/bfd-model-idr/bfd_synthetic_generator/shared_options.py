import click
from constants import GeneratePacDataMode
from load_synthetic_output import BeneSkMode

_BATCH_SIZE = 50_000


def common_options(function):
    function = click.option(
        "--force-ztm",
        type=bool,
        help=(
            'Allow "zero-to-many" rows (e.g. BENE_ENTLMT, c/d data, etc.) for an existing patient to '
            "be generated. This will introduce new rows for patients that previously had "
            "none. Useful if not all tables for a patient have been generated yet."
        ),
    )(function)

    function = click.option(
        "--patients",
        type=int,
        default=0,
        show_default=True,
        help="Number of NEW patients to generate. Does not affect patients regenerated when truncate "
        "flag is set to False",
    )(function)

    function = click.option(
        "--claims",
        type=bool,
        default=True,
        show_default=True,
        help="Automatically generate claims after patient generation using the generated BENE_HSTRY",
    )(function)

    function = click.option(
        "--min-claims",
        envvar="MIN_CLAIMS",
        type=int,
        default=5,
        show_default=True,
        help="Minimum number of claims to generate per person",
    )(function)

    function = click.option(
        "--max-claims",
        envvar="MAX_CLAIMS",
        type=int,
        default=5,
        show_default=True,
        help="Maximum number of claims to generate per person",
    )(function)

    function = click.option(
        "--samhsa",
        envvar="ENABLE_SAMHSA",
        type=bool,
        default=True,
        show_default=True,
        help="Enables generation of SAMHSA-related data",
    )(function)

    function = click.option(
        "--pac-gen",
        envvar="PAC_GEN",
        type=click.Choice(GeneratePacDataMode, case_sensitive=False),
        default=GeneratePacDataMode.IF_NONE,
        show_default=True,
        help=(
            "Generate new partially-adjudicated claims data based on choice. 'no' will never "
            "generate pac data, 'if_none' will generate if the input claims data has no pac CLMs, "
            "and 'always' will force the generation always"
        ),
    )(function)

    function = click.option(
        "--bene-sk-mode",
        envvar="BENE_SK_MODE",
        type=click.Choice(BeneSkMode, case_sensitive=False),
        default=BeneSkMode.BOTH,
        show_default=True,
        help=(
            "Sets the mode for which tables from which distinct BENE_SKs are read when generating "
            "claims. 'bene_hstry' indicates that BENE_SKs are only loaded from BENE_HSTRY, 'clm' "
            "indicates loading from only from CLM. 'both' indicates loading from both"
        ),
    )(function)

    function = click.option(
        "--truncate",
        type=bool,
        default=False,
        show_default=True,
        help="Truncate tables before reloading. Default is false",
    )(function)

    function = click.option(
        "--batch-size",
        type=int,
        default=_BATCH_SIZE,
        show_default=True,
        help="Batch size of claims to process",
    )(function)

    return function
