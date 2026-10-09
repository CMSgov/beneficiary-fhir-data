from pathlib import Path

from click.testing import CliRunner

from generate_eob_sample import main

PROJECT_ROOT = Path(__file__).resolve().parents[3]
SOURCE_DIR = str(PROJECT_ROOT / "bfd-pipeline-idr" / "test_samples1")


def test_generate_eob_sample_base() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "8520736890144",
            "--source-directory",
            SOURCE_DIR,
            "--output-directory",
            "out-test",
        ],
    )
    assert result.exit_code == 0


def test_generate_eob_sample_pharmacy() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "6595861148142",
            "--source-directory",
            SOURCE_DIR,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
