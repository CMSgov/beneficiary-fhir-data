from click.testing import CliRunner

from generate_bene_sample import main


def test_generate_bene_sample_for_beneficiary() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            "313081892",
            "--bene-sk 313081892",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
        ],
    )

    assert result.exit_code == 0
