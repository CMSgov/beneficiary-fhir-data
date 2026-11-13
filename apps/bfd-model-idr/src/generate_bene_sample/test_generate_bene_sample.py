import json
from pathlib import Path

from click.testing import CliRunner

from generate_bene_sample import main

PROJECT_ROOT = Path(__file__).resolve().parents[3]
SOURCE_DIR_SAMPLES1 = str(PROJECT_ROOT / "bfd-pipeline-idr" / "test_samples1")


def test_generate_bene_sample_for_beneficiary() -> None:
    sample_bene_sk = "47347082"
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            sample_bene_sk,
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/beneficiary.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["BENE_SK"] == sample_bene_sk
    assert result_data["BENE_XREF_EFCTV_SK"] == sample_bene_sk
    assert sample_bene_sk in result_data["ALL_BENE_SKs"]
