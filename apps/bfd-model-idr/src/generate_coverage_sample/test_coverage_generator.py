from click.testing import CliRunner

from generate_coverage_sample import main


def test_generate_coverage_part_a() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            "47347082",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
            "--parts",
            "A",
        ],
    )

    assert result.exit_code == 0
