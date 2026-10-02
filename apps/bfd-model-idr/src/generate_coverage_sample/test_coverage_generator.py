from click.testing import CliRunner

from generate_coverage_sample import main


def test_generate_bene_sample_for_beneficiary() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
        ],
    )

    assert result.exit_code == 0
