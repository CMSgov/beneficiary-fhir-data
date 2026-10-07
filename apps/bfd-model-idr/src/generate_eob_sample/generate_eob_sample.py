import json
import sys
from pathlib import Path
from typing import Any

from idr_model.claims_static import (
    ADJUDICATED_PROFESSIONAL_CARRIER_CLAIM_TYPES,
    ADJUDICATED_PROFESSIONAL_CLAIM_TYPES_DME,
    INSTITUTIONAL_CLAIM_TYPES,
    MCS_CLM_TYPE_CDS,
    PHARMACY_CLM_TYPE_CDS,
    VMS_CDS,
)

from .clm_line import ClmLineBuilder
from .param import Param
from .util import (
    extract_col_str,
    read_multi_line_file,
    read_single_line_file,
)

CLM_COL = [
    "CLM_FINL_ACTN_IND",
    "BENE_SK",
    "CLM_TYPE_CD",
    "CLM_UNIQ_ID",
    "CLM_CNTL_NUM",
    "CLM_FROM_DT",
    "CLM_THRU_DT",
    "CLM_EFCTV_DT",
    "CLM_SRC_ID",
    "META_SRC_SK",
    "PRVDR_BLG_PRVDR_NPI_NUM",
    "CLM_ORIG_CNTL_NUM",
    "CLM_SRVC_PRVDR_GNRC_ID_NUM",
    "CLM_PD_DT",
    "CLM_PRSBNG_PRVDR_GNRC_ID_NUMPRVDR_PRSBNG_ID_QLFYR_CD",
    "CLM_BENE_PMT_AMT",
    "CLM_OTHR_TP_PD_AMT",
    "PRVDR_SRVC_ID_QLFYR_CD",
    "CLM_MDCR_DDCTBL_AMT",
    "CLM_PMT_AMT",
    "CLM_PRVDR_PMT_AMT",
    "CLM_SBMT_CHRG_AMT",
    "CLM_MDCR_COINSRNC_AMT",
    "CLM_NCVRD_CHRG_AMT",
    "CLM_BLOOD_LBLTY_AMT",
    "CLM_BLG_PRVDR_OSCAR_NUM",
    "CLM_RFRG_PRVDR_PIN_NUM",
    "CLM_ALOWD_CHRG_AMT",
    "CLM_BENE_PMT_COINSRNC_AMT",
    "PRVDR_RNDRNG_PRVDR_NPI_NUM",
    "CLM_BILL_FAC_TYPE_CD",
    "CLM_BILL_CLSFCTN_CD",
    "CLM_BILL_FREQ_CD",
    "CLM_BLOOD_PT_FRNSH_QTY",
    "CLM_NCH_PRMRY_PYR_CD",
    "CLM_QUERY_CD",
    "CLM_IDR_LD_DT",
    "CLM_CNTRCTR_NUM",
    "CLM_ADJSTMT_TYPE_CD",
    "CLM_DISP_CD",
    "CLM_PRVDR_RMNG_DUE_AMT",
    "CLM_BLG_PRVDR_ZIP5_CD",
    "GEO_BLG_SSA_STATE_CD",
    "CLM_BLG_PRVDR_TAX_NUM",
    "PRVDR_ATNDG_PRVDR_NPI_NUM",
    "PRVDR_OPRTG_PRVDR_NPI_NUM",
    "PRVDR_OTHR_PRVDR_NPI_NUM",
    "CLM_BLOOD_CHRG_AMT",
    "CLM_BLOOD_NCVRD_CHRG_AMT",
    "CLM_COB_PTNT_RESP_AMT",
    "CLM_PRVDR_INTRST_PD_AMT",
    "CLM_PRVDR_OTAF_AMT",
    "CLM_BNFT_ENHNCMT_1_CD",
    "CLM_BNFT_ENHNCMT_2_CD",
    "CLM_BNFT_ENHNCMT_3_CD",
    "CLM_BNFT_ENHNCMT_4_CD",
    "CLM_BNFT_ENHNCMT_5_CD",
    "CLM_ACO_CARE_MGMT_HCBS_SW",
    "CLM_TOT_CNTRCTL_AMT",
    "CLM_ATNDG_FED_PRVDR_SPCLTY_CD",
    "CLM_RLT_COND_SGNTR_SK",
    "CLM_IDR_LD_DT",
    "CLM_SBMT_FRMT_CD",
    "CLM_SBMTR_CNTRCT_NUM",
    "CLM_SBMTR_CNTRCT_PBP_NUM",
    "CLM_DT_SGNTR_SK",
    "CLM_PD_STUS_CD",
    "CLM_RIC_CD",
    "CLM_BENE_PD_AMT",
]

PROV_COL = ["PRVDR_LAST_NAME"]

DIAG_COL = [
    "CLM_VAL_SQNC_NUM",
    "CLM_DGNS_CD",
    "CLM_DGNS_PRCDR_ICD_IND",
    "CLM_PROD_TYPE_CD",
    "CLM_POA_IND",
]

PROF_COMP_COL = [
    "CLM_PRVDR_ACNT_RCVBL_OFST_AMT",
    "CLM_MDCR_PRFNL_PRMRY_PYR_AMT",
    "CLM_AUDT_TRL_STUS_CD",
    "CLM_CARR_PMT_DNL_CD",
    "CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW",
    "CLM_CLNCL_TRIL_NUM",
]

SIG_LINE_COL = [
    "CLM_SUBMSN_DT",
    "CLM_NCH_WKLY_PROC_DT",
    "CLM_CMS_PROC_DT",
    "CLM_ACTV_CARE_FROM_DT",
    "CLM_DSCHRG_DT",
    "CLM_MDCR_EXHSTD_DT",
    "CLM_NCVRD_FROM_DT",
    "CLM_NCVRD_THRU_DT",
    "CLM_ACTV_CARE_THRU_DT",
    "CLM_QLFY_STAY_FROM_DT",
    "CLM_QLFY_STAY_THRU_DT",
    "CLM_DT_SGNTR_SK",
]

DCSTN_COL = [
    "CLM_NRLN_RIC_CD",
]

LCTN_COL = ["CLM_AUDT_TRL_STUS_CD"]

CTN_PBP_COL = ["CNTRCT_PBP_NAME"]

INST_COL = [
    "CLM_MDCR_HHA_TOT_VISIT_CNT",
    "CLM_MDCR_HOSPC_PRD_CNT",
    "CLM_MDCR_INSTNL_PRMRY_PYR_AMT",
    "CLM_MDCR_IP_LRD_USE_CNT",
    "CLM_INSTNL_PER_DIEM_AMT",
    "CLM_INSTNL_CVRD_DAY_CNT",
    "CLM_HIPPS_UNCOMPD_CARE_AMT",
    "CLM_MDCR_IP_PPS_DSPRPRTNT_AMT",
    "CLM_MDCR_IP_PPS_DRG_WT_NUM",
    "CLM_INSTNL_MDCR_COINS_DAY_CNT",
    "CLM_INSTNL_NCVRD_DAY_CNT",
    "CLM_MDCR_IP_PPS_EXCPTN_AMT",
    "CLM_MDCR_IP_PPS_CPTL_FSP_AMT",
    "CLM_MDCR_IP_PPS_CPTL_IME_AMT",
    "CLM_MDCR_IP_PPS_OUTLIER_AMT",
    "CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT",
    "CLM_MDCR_IP_PPS_CPTL_TOT_AMT",
    "CLM_INSTNL_DRG_OUTLIER_AMT",
    "CLM_INSTNL_PRFNL_AMT",
    "CLM_FINL_STDZD_PYMT_AMT",
    "CLM_HAC_RDCTN_PYMT_AMT",
    "CLM_HIPPS_MODEL_BNDLD_PMT_AMT",
    "CLM_HIPPS_READMSN_RDCTN_AMT",
    "CLM_HIPPS_VBP_AMT",
    "CLM_MDCR_IP_1ST_YR_RATE_AMT",
    "CLM_MDCR_IP_SCND_YR_RATE_AMT",
    "CLM_PPS_MD_WVR_STDZD_VAL_AMT",
    "CLM_SITE_NTRL_CST_BSD_PYMT_AMT",
    "CLM_SITE_NTRL_IP_PPS_PYMT_AMT",
    "CLM_SS_OUTLIER_STD_PYMT_AMT",
    "DGNS_DRG_CD",
    "CLM_ADMSN_TYPE_CD",
    "BENE_PTNT_STUS_CD",
    "CLM_MDCR_INSTNL_MCO_PD_SW",
    "CLM_ADMSN_SRC_CD",
    "CLM_PPS_IND_CD",
    "CLM_HHA_LUP_IND_CD",
    "CLM_HHA_RFRL_CD",
    "DGNS_DRG_OUTLIER_CD",
    "CLM_MDCR_NPMT_RSN_CD",
    "CLM_FI_ACTN_CD",
]

CLM_VAL_COL = [
    "CLM_VAL_CD",
    "CLM_VAL_AMT",
]

PROC_COL = [
    "CLM_VAL_SQNC_NUM",
    "CLM_PRCDR_PRFRM_DT",
    "CLM_PRCDR_CD",
    "CLM_DGNS_PRCDR_ICD_IND",
]

RX_LINE_COL = [
    "CLM_LINE_GRS_ABOVE_THRSHLD_AMT",
    "CLM_LINE_GRS_BLW_THRSHLD_AMT",
    "CLM_LINE_LIS_AMT",
    "CLM_LINE_TROOP_TOT_AMT",
    "CLM_LINE_PLRO_AMT",
    "CLM_RPTD_MFTR_DSCNT_AMT",
    "CLM_LINE_INGRDNT_CST_AMT",
    "CLM_LINE_SRVC_CST_AMT",
    "CLM_LINE_SLS_TAX_AMT",
    "CLM_LINE_VCCN_ADMIN_FEE_AMT",
    "CLM_PRCNG_EXCPTN_CD",
    "CLM_CMS_CALCD_MFTR_DSCNT_AMT",
    "CLM_LINE_GRS_CVRD_CST_TOT_AMT",
    "CLM_LINE_REBT_PASSTHRU_POS_AMT",
    "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT",
    "CLM_LINE_RPTD_GAP_DSCNT_AMTCLM_LINE_AUTHRZD_FILL_NUM",
    "CLM_PHRMCY_SRVC_TYPE_CD",
    "CLM_LINE_RX_ORGN_CD",
    "CLM_BRND_GNRC_CD",
    "CLM_PTNT_RSDNC_CD",
    "CLM_LTC_DSPNSNG_MTHD_CD",
    "CLM_CMPND_CD",
    "CLM_LINE_DAYS_SUPLY_QTY",
    "CLM_LINE_RX_FILL_NUM",
    "CLM_DAW_PROD_SLCTN_CD",
    "CLM_DRUG_CVRG_STUS_CD",
    "CLM_CTSTRPHC_CVRG_IND_CD",
    "CLM_DSPNSNG_STUS_CD",
]


class Result:
    def __init__(self, result_json: dict[str, Any], output_file: str):
        self.result_json = result_json
        self.output_file = output_file

    result_json: dict[str, Any]
    output_file: str


class SampleGenerator:
    def __init__(self, source_directory: str, output_directory: str):
        self.source_directory = source_directory
        self.output_directory = output_directory

    def run(self, clm_uniq_id: str) -> None:
        """Generate EOB sample JSON from SYNTHETIC_EOB.csv based on claim unique ID."""
        print(f"Generating EOB sample for claim unique ID: {clm_uniq_id}")
        print(f"Source Directory: {self.source_directory}")
        if not Path(self.source_directory).exists():
            print("Source directory not found. Run the generator or this will not go well.")
            sys.exit(1)

        claim_row = self.read_clm(clm_uniq_id)
        claim_type_raw = claim_row.get("CLM_TYPE_CD")

        if not claim_type_raw:
            print("Claim type not found. Exiting.")
            sys.exit(1)

        claim_type = int(claim_type_raw)

        if claim_type in PHARMACY_CLM_TYPE_CDS:
            result = self.create_base(
                clm_uniq_id, claim_type, claim_row, "EOB-Pharmacy-Sample.json"
            )
            self.add_rx_line(claim_row=claim_row, result=result)
            result.result_json["resourceType"] = "ExplanationOfBenefit-Pharmacy"
        elif claim_type in ADJUDICATED_PROFESSIONAL_CARRIER_CLAIM_TYPES:
            result = self.create_base(clm_uniq_id, claim_type, claim_row, "EOB-Carrier-Sample.json")
        elif claim_type in ADJUDICATED_PROFESSIONAL_CLAIM_TYPES_DME:
            result = self.create_base(clm_uniq_id, claim_type, claim_row, "EOB-DME-Sample.json")
        elif claim_type in MCS_CLM_TYPE_CDS:
            result = self.create_base(
                clm_uniq_id, claim_type, claim_row, "EOB-Carrier-MCS-Sample.json"
            )
            self.add_mcs_lines(claim_row=claim_row, result=result)
            self.add_fiss_lines(claim_row=claim_row, result=result)
        # this a default fallback for institutional claim types at the moment
        # as this work continues we may add more specific handling
        # for different institutional claim types
        # also fall back for known claim types that we do not have a special sample for
        elif claim_type in INSTITUTIONAL_CLAIM_TYPES or claim_type in VMS_CDS:
            result = self.create_base(clm_uniq_id, claim_type, claim_row, "EOB-Base-Sample.json")
            self.add_instl(claim_row=claim_row, result=result)
            self.add_proc_lines(claim_row=claim_row, result=result)
            self.add_clm_values(claim_row=claim_row, result=result)
        else:
            print("Unknown Type")
            sys.exit(1)

        output_path = Path(result.output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with Path(result.output_file).open(mode="w", encoding="utf-8") as f:
            json.dump(result.result_json, f, indent=2)

        print(f"Successfully generated sample JSON: {result.output_file}")

    def create_base(
        self,
        clm_uniq_id: str,
        clm_type_cd: int,
        claim_row: dict[str, str],
        destination_file_name: str,
    ) -> Result:

        prod_lines = self.read_prod_lines(claim_row)

        diagnoses_lines = [
            {
                "ROW_NUM": f"{prod_lines.index(prod_line) + 1}",
                **{col: extract_col_str(prod_line, col) for col in DIAG_COL},
            }
            for prod_line in (prod_lines or [])
        ]

        prfnl = self.read_prfnl(clm_row=claim_row)
        lctn_hist = self.read_lctn_hist(clm_row=claim_row)
        dcmtn = self.read_dcmtn(clm_row=claim_row)
        sig_line = self.read_sig_line(str(claim_row.get("CLM_DT_SGNTR_SK", "")).strip())
        provider_npi = str(claim_row.get("PRVDR_PRSCRBNG_PRVDR_NPI_NUM", "")).strip()

        prov_row = self.read_provider(provider_npi)
        clm_sbmtr_cntrct_num = str(claim_row.get("CLM_SBMTR_CNTRCT_NUM", "")).strip()
        clm_sbmtr_cntrct_pbp_num = str(claim_row.get("CLM_SBMTR_CNTRCT_PBP_NUM", "")).strip()

        ctr_pbp_row = self.read_pbp(clm_sbmtr_cntrct_num, clm_sbmtr_cntrct_pbp_num)

        output_json = {
            "resourceType": "ExplanationOfBenefitBase",
            "id": str(clm_uniq_id.replace("-", "")).strip(),
            "lastUpdated": extract_col_str(claim_row, "IDR_UPDT_TS"),
            "CLM_TYPE_CD": clm_type_cd,
            **{col: extract_col_str(claim_row, col) for col in CLM_COL},
            **{col: extract_col_str(prov_row, col) for col in PROV_COL},
            "PRVDR_PRSCRBNG_PRVDR_NPI_NUM": provider_npi,
            **{col: extract_col_str(ctr_pbp_row, col) for col in CTN_PBP_COL},
            "diagnoses": diagnoses_lines,
            "supportingInfoComponents": [],
            "lineItemComponents": ClmLineBuilder(
                self.source_directory, claim_row
            ).build_line_items(),
            "profComponents": {
                **{col: extract_col_str(prfnl, col) for col in PROF_COMP_COL},
            },
            **{col: extract_col_str(dcmtn, col) for col in DCSTN_COL},
            **{col: extract_col_str(sig_line, col) for col in SIG_LINE_COL},
            **{col: extract_col_str(lctn_hist, col) for col in LCTN_COL},
            # these next two are in CLM_RLT_OCRNC_SGNTR_MBR_POC.csv which is read an manipulated
            #  by augment elements we will eventually deprecate that program into here
            # for now we will hard code for the sample
            "CLM_OCRNC_SGNTR_SK": "118815",
            "CLM_RLT_OCRNC_SGNTR_SK": "119817",
        }

        return Result(
            result_json=output_json, output_file=f"{self.output_directory}/{destination_file_name}"
        )

    def add_rx_line(self, claim_row: dict[str, str], result: Result) -> None:
        rx_line = self.read_rx_line(claim_row)
        result.result_json["lineItemComponents"][0].update(
            {
                "isCompound": str(extract_col_str(rx_line, "CLM_CMPND_CD") == "2").lower(),
                **{col: extract_col_str(rx_line, col) for col in RX_LINE_COL},
            }
        )

    def add_instl(self, claim_row: dict[str, str], result: Result) -> None:
        instnl = self.read_instnl(clm_row=claim_row)
        result.result_json["institutionalComponents"] = {
            col: extract_col_str(instnl, col) for col in INST_COL
        }

    def add_clm_values(self, claim_row: dict[str, str], result: Result) -> None:
        claim_values = [
            {col: extract_col_str(val_line, col) for col in CLM_VAL_COL}
            for val_line in self.read_clm_val(clm_line=claim_row)
        ]
        result.result_json["claimValues"] = claim_values

    def add_proc_lines(self, claim_row: dict[str, str], result: Result) -> None:
        procedure_lines = [
            {col: extract_col_str(prod_line, col) for col in PROC_COL}
            for prod_line in (self.read_prod_lines(claim_row) or [])
        ]
        result.result_json["procedures"] = procedure_lines

    def read_clm(self, clm_uniq_id: str) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory, "SYNTHETIC_CLM", True, [Param("CLM_UNIQ_ID", clm_uniq_id)]
        )

    def read_provider(self, provider_npi: str) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_PRVDR_HSTRY",
            False,
            [Param("PRVDR_NPI_NUM", provider_npi)],
        )

    def read_sig_line(self, clm_dt_sgntr_sk: str) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_DT_SGNTR",
            False,
            [Param("CLM_DT_SGNTR_SK", clm_dt_sgntr_sk)],
        )

    def read_pbp(self, clm_sbmtr_cntrct_num: str, clm_sbmtr_cntrct_pbp_num: str) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_CNTRCT_PBP_NUM",
            False,
            [
                Param("CNTRCT_NUM", clm_sbmtr_cntrct_num),
                Param("CNTRCT_PBP_NUM", clm_sbmtr_cntrct_pbp_num),
            ],
        )

    def read_instnl(self, clm_row: dict[str, str]) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_INSTNL",
            False,
            [
                Param("GEO_BENE_SK", clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_row["CLM_NUM_SK"]),
            ],
        )

    def read_prfnl(self, clm_row: dict[str, str]) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_PRFNL",
            False,
            [
                Param("GEO_BENE_SK", clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_row["CLM_NUM_SK"]),
            ],
        )

    def read_lctn_hist(self, clm_row: dict[str, str]) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_LCTN_HSTRY",
            False,
            [
                Param("GEO_BENE_SK", clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_row["CLM_NUM_SK"]),
            ],
        )

    def read_dcmtn(self, clm_row: dict[str, str]) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_DCMTN",
            False,
            [
                Param("GEO_BENE_SK", clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_row["CLM_NUM_SK"]),
            ],
        )

    def read_rx_line(self, clm_line: dict[str, str]) -> dict[str, str]:
        return read_single_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_LINE_RX",
            True,
            [
                Param("GEO_BENE_SK", clm_line["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_line["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_line["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_line["CLM_NUM_SK"]),
            ],
        )

    def read_prod_lines(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        return read_multi_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_PROD",
            False,
            [
                Param("GEO_BENE_SK", clm_line["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_line["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_line["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_line["CLM_NUM_SK"]),
            ],
        )

    def read_instnl_lines(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        return read_multi_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_LINE_INSTNL",
            False,
            [
                Param("GEO_BENE_SK", clm_line["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_line["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_line["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_line["CLM_NUM_SK"]),
            ],
        )

    def read_prfnl_lines(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        return read_multi_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_LINE_PRFNL",
            False,
            [
                Param("GEO_BENE_SK", clm_line["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_line["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_line["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_line["CLM_NUM_SK"]),
            ],
        )

    def read_mcs_lines(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        return read_multi_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_LINE_MCS",
            False,
            [
                Param("GEO_BENE_SK", clm_line["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_line["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_line["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_line["CLM_NUM_SK"]),
            ],
        )

    def add_mcs_lines(self, claim_row: dict[str, str], result: Result) -> None:
        mcs_lines = self.read_mcs_lines(clm_line=claim_row)
        for line_item in result.result_json["lineItemComponents"] or []:
            mcs_found = [
                mcs_line
                for mcs_line in mcs_lines
                if extract_col_str(mcs_line, "CLM_LINE_NUM") == line_item["CLM_LINE_NUM"]
            ]
            if mcs_found:
                line_item["CLM_LINE_PRFRMG_PRVDR_LCLTY_CD"] = extract_col_str(
                    mcs_found[0], "CLM_LINE_PRFRMG_PRVDR_LCLTY_CD"
                )
                line_item["CLM_LINE_HCT_LVL_NUM"] = extract_col_str(
                    mcs_found[0], "CLM_LINE_HCT_LVL_NUM"
                )
                line_item["CLM_LINE_HGB_LVL_NUM"] = extract_col_str(
                    mcs_found[0], "CLM_LINE_HGB_LVL_NUM"
                )
                line_item["CLM_LINE_RBNDLG_CRTFCTN_NUM"] = extract_col_str(
                    mcs_found[0], "CLM_LINE_RBNDLG_CRTFCTN_NUM"
                )

    def read_fiss_lines(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        return read_multi_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_LINE_FISS",
            False,
            [
                Param("GEO_BENE_SK", clm_line["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_line["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_line["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_line["CLM_NUM_SK"]),
            ],
        )

    def add_fiss_lines(self, claim_row: dict[str, str], result: Result) -> None:
        fiss_lines = self.read_fiss_lines(clm_line=claim_row)
        for line_item in result.result_json["lineItemComponents"] or []:
            fiss_found = [
                fiss_line
                for fiss_line in fiss_lines
                if extract_col_str(fiss_line, "CLM_LINE_NUM") == line_item["CLM_LINE_NUM"]
            ]
            if fiss_found:
                line_item["CLM_LINE_MSP_COINSRNC_AMT"] = extract_col_str(
                    fiss_found[0], "CLM_LINE_MSP_COINSRNC_AMT"
                )

    def read_clm_val(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        return read_multi_line_file(
            self.source_directory,
            "SYNTHETIC_CLM_VAL",
            False,
            [
                Param("GEO_BENE_SK", clm_line["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", clm_line["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", clm_line["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", clm_line["CLM_NUM_SK"]),
            ],
        )
