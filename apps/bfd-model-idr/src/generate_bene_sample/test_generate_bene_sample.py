from pathlib import Path

from click.testing import CliRunner

from generate_bene_sample import main

PROJECT_ROOT = Path(__file__).resolve().parents[3]
SOURCE_DIR = str(PROJECT_ROOT / "bfd-pipeline-idr" / "test_samples1")


def test_generate_bene_sample_for_beneficiary() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            "47347082",
            "--source-directory",
            SOURCE_DIR,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
