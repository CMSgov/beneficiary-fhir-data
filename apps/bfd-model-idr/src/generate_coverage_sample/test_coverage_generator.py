import json
from pathlib import Path

from click.testing import CliRunner

from generate_coverage_sample import main

PROJECT_ROOT = Path(__file__).resolve().parents[3]
SOURCE_DIR_SAMPLES1 = str(PROJECT_ROOT / "bfd-pipeline-idr" / "test_samples1")


def test_generate_coverage_part_a() -> None:
    sample_bene_sk = "632187879"
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            sample_bene_sk,
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
            "--parts",
            "A",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/coverage-ffs-part-A.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == sample_bene_sk
    assert result_data["coveragePart"] == "A"


def test_generate_coverage_part_b() -> None:
    sample_bene_sk = "800678894"
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            sample_bene_sk,
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
            "--parts",
            "B",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/coverage-ffs-part-B.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == sample_bene_sk
    assert result_data["coveragePart"] == "B"


def test_generate_coverage_part_c() -> None:
    sample_bene_sk = "441149422"
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            sample_bene_sk,
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
            "--parts",
            "C",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/coverage-part-C.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == sample_bene_sk
    assert result_data["coveragePart"] == "C"


def test_generate_coverage_part_d() -> None:
    sample_bene_sk = "547437476"
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            sample_bene_sk,
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
            "--parts",
            "D",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/coverage-part-D.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == sample_bene_sk
    assert result_data["coveragePart"] == "D"


def test_generate_coverage_part_dual() -> None:
    sample_bene_sk = "800678894"
    result = CliRunner().invoke(
        main,
        [
            "--bene-sk",
            sample_bene_sk,
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
            "--parts",
            "DUAL",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/coverage-dual.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == sample_bene_sk
    assert result_data["coveragePart"] == "DUAL"
