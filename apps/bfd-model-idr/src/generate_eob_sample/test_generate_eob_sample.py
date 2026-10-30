import json
from pathlib import Path

from click.testing import CliRunner

from generate_eob_sample import main
from idr_model.claim_type_codes import (
    ADJUDICATED_PROFESSIONAL_CARRIER_NCH,
    ADJUDICATED_PROFESSIONAL_DME_NCH,
    INSTITUTIONAL_CLAIM_TYPES,
    MCS_SS,
    PHARMACY_CLM_TYPE_CDS,
    VMS_SS,
    INSTITUTIONAL_HHA_NCH,
    INSTITUTIONAL_SNF_NCH,
    INSTITUTIONAL_OUTPATIENT_NCH,
    INSTITUTIONAL_HOSPICE_NCH,
    INSTITUTIONAL_INPATIENT_NCH,
)

PROJECT_ROOT = Path(__file__).resolve().parents[3]
SOURCE_DIR_SAMPLES1 = str(PROJECT_ROOT / "bfd-pipeline-idr" / "test_samples1")
SOURCE_DIR_SAMPLES2 = PROJECT_ROOT / "bfd-pipeline-idr" / "test_samples2"


def test_generate_eob_sample_base() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "8520736890144",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )
    assert result.exit_code == 0
    output_file = Path("out-test/eob-inpatient-nch.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "8520736890144"
    assert int(result_data["CLM_TYPE_CD"]) in INSTITUTIONAL_INPATIENT_NCH


def test_generate_eob_sample_institutional_outpatient() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "0113370100080",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/eob-outpatient-nch.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "0113370100080"
    assert int(result_data["CLM_TYPE_CD"]) in INSTITUTIONAL_OUTPATIENT_NCH


def test_generate_eob_sample_institutional_hospice() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "7026678960101",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/eob-hospice-nch.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "7026678960101"
    assert int(result_data["CLM_TYPE_CD"]) in INSTITUTIONAL_HOSPICE_NCH


def test_generate_eob_sample_institutional_snf() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "-7226585371632",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/eob-snf-nch.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "-7226585371632"
    assert int(result_data["CLM_TYPE_CD"]) in INSTITUTIONAL_SNF_NCH


def test_generate_eob_sample_institutional_hha() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "2512230584690",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/eob-hha-nch.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "2512230584690"
    assert int(result_data["CLM_TYPE_CD"]) in INSTITUTIONAL_HHA_NCH


def test_generate_eob_sample_pharmacy() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "6595861148142",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/eob-pharmacy.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "6595861148142"
    assert int(result_data["CLM_TYPE_CD"]) in PHARMACY_CLM_TYPE_CDS


def test_generate_eob_sample_carrier() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "4045037817088",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0
    output_file = Path("out-test/eob-carrier-nch.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "4045037817088"
    assert int(result_data["CLM_TYPE_CD"]) in ADJUDICATED_PROFESSIONAL_CARRIER_NCH


def test_generate_eob_sample_carrier_dme() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "9298559719445",
            "--source-directory",
            SOURCE_DIR_SAMPLES1,
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0

    output_file = Path("out-test/eob-dme-nch.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "9298559719445"
    assert int(result_data["CLM_TYPE_CD"]) in ADJUDICATED_PROFESSIONAL_DME_NCH


# this test is pointed at test_sample2 because test sample 1 does not have mcs claims
def test_generate_eob_sample_carrier_mcs() -> None:
    result = CliRunner().invoke(
        main,
        [
            "--clm-uniq-id",
            "3351266481403",
            "--source-directory",
            str(SOURCE_DIR_SAMPLES2 / "professional"),
            "--output-directory",
            "out-test",
        ],
    )

    assert result.exit_code == 0

    output_file = Path("out-test/eob-mcs-multi-carrier-ss.json")
    assert output_file.exists(), f"Expected output file not found: {output_file}"

    with output_file.open("r", encoding="utf-8") as f:
        result_data = json.load(f)

    assert isinstance(result_data, dict)
    assert result_data["CLM_UNIQ_ID"] == "3351266481403"
    assert int(result_data["CLM_TYPE_CD"]) in MCS_SS
