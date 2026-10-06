from click.testing import CliRunner

from generate_coverage_sample import main


def test_generate_coverage_part_a() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            "632187879",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
            "--parts",
            "A",
        ],
    )

    assert result.exit_code == 0


def test_generate_coverage_part_b() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            "800678894",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
            "--parts",
            "B",
        ],
    )

    assert result.exit_code == 0


def test_generate_coverage_part_c() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            "441149422",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
            "--parts",
            "C",
        ],
    )

    assert result.exit_code == 0


def test_generate_coverage_part_d() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            "547437476",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
            "--parts",
            "D",
        ],
    )

    assert result.exit_code == 0
