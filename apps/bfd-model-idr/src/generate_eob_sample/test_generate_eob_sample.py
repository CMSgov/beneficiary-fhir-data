import json
from pathlib import Path

from click.testing import CliRunner

from generate_eob_sample import main
from idr_model.claims_static import (
    ADJUDICATED_PROFESSIONAL_CARRIER_CLAIM_TYPES,
    ADJUDICATED_PROFESSIONAL_CLAIM_TYPES_DME,
    INSTITUTIONAL_CLAIM_TYPES,
    MCS_CLM_TYPE_CDS,
    PHARMACY_CLM_TYPE_CDS,
)


def test_generate_eob_sample_base() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "8520736890144",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("../../out-test/EOB-Base-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "8520736890144"
    assert result_data["CLM_TYPE_CD"] in INSTITUTIONAL_CLAIM_TYPES


def test_generate_eob_sample_pharmacy() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "6595861148142",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("../../out-test/EOB-Pharmacy-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "6595861148142"
    assert result_data["CLM_TYPE_CD"] in PHARMACY_CLM_TYPE_CDS


def test_generate_eob_sample_carrier() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "4045037817088",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("../../out-test/EOB-Carrier-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "4045037817088"
    assert result_data["CLM_TYPE_CD"] in ADJUDICATED_PROFESSIONAL_CARRIER_CLAIM_TYPES


def test_generate_eob_sample_carrier_dme() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "9298559719445",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples1",
            "--output-directory",
            "../../out-test",
        ],
    )

    assert result.exit_code == 0

    output_file = Path("../../out-test/EOB-DME-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "9298559719445"
    assert result_data["CLM_TYPE_CD"] in ADJUDICATED_PROFESSIONAL_CLAIM_TYPES_DME

# this test is pointed at test_sample2 because test sample 1 does not have mcs claims
def test_generate_eob_sample_carrier_mcs() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "3351266481403",
            "--source-directory",
            "../../../bfd-pipeline-idr/test_samples2/professional",
            "--output-directory",
            "../../out-test",
        ],
    )

    assert result.exit_code == 0

    output_file = Path("../../out-test/EOB-Carrier-MCS-Sample.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "3351266481403"
    assert result_data["CLM_TYPE_CD"] in MCS_CLM_TYPE_CDS
