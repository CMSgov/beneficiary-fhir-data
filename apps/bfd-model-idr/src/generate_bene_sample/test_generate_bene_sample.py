from click.testing import CliRunner

from generate_bene_sample import main


def test_generate_bene_sample_for_beneficiary() -> None:
    result = CliRunner().invoke(main, ["--bene-sk", "1234"])

    assert result.exit_code == 0
