import json
from pathlib import Path

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
    output_file = Path("../../out-test/Coverage-FFS-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == "632187879"
    assert result_data["coveragePart"] == "A"


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
    output_file = Path("../../out-test/Coverage-FFS-Sample-PartB.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == "800678894"
    assert result_data["coveragePart"] == "B"


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
    output_file = Path("../../out-test/Coverage-PartC-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == "441149422"
    assert result_data["coveragePart"] == "C"


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
    output_file = Path("../../out-test/Coverage-PartD-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == "547437476"
    assert result_data["coveragePart"] == "D"


def test_generate_coverage_part_dual() -> None:
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
            "DUAL",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("../../out-test/Coverage-Dual-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["XREF_EFCTV_BENE_SK"] == "800678894"
    assert result_data["coveragePart"] == "DUAL"
