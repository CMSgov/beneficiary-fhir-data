from .param import Param
from .util import (
    extract_col_str,
    find_field_in_line_by_num,
    find_linked_field_in_tgt_by_line_num_sk,
    read_multi_line_file,
)

CLM_LINE_COL = [
    "CLM_LINE_NUM",
    "CLM_LINE_ALOWD_CHRG_AMT",
    "CLM_LINE_ANSTHSA_UNIT_CNT",
    "CLM_LINE_BENE_PD_AMT",
    "CLM_LINE_BENE_PMT_AMT",
    "CLM_LINE_BLOOD_DDCTBL_AMT",
    "CLM_LINE_CVRD_PD_AMT",
    "CLM_LINE_DGNS_CD",
    "CLM_LINE_FROM_DT",
    "CLM_LINE_HCPCS_CD",
    "CLM_LINE_MDCR_COINSRNC_AMT",
    "CLM_LINE_MDCR_DDCTBL_AMT",
    "CLM_LINE_NCVRD_CHRG_AMT",
    "CLM_LINE_NDC_CD",
    "CLM_LINE_NDC_QTY",
    "CLM_LINE_PMD_UNIQ_TRKNG_NUM",
    "CLM_LINE_PRVDR_PMT_AMT",
    "CLM_LINE_REV_CTR_CD",
    "CLM_LINE_RX_NUM",
    "CLM_LINE_SBMT_CHRG_AMT",
    "CLM_LINE_SRVC_UNIT_QTY",
    "CLM_LINE_THRU_DT",
    "CLM_POS_CD",
    "CLM_RNDRG_FED_PRVDR_SPCLTY_CD",
    "CLM_RNDRG_PRVDR_PRTCPTG_CD",
    "CLM_RNDRG_PRVDR_NPI_NUM",
    "CLM_RNDRG_PRVDR_TAX_NUM",
    "CLM_RNDRG_PRVDR_TYPE_CD",
    "GEO_RNDRG_SSA_STATE_CD",
    "HCPCS_1_MDFR_CD",
    "HCPCS_2_MDFR_CD",
    "HCPCS_3_MDFR_CD",
    "HCPCS_4_MDFR_CD",
    "HCPCS_5_MDFR_CD",
    "PRVDR_RNDRNG_PRVDR_NPI_NUM",
    "CLM_REV_CNTR_TDAPA_AMT",
    "CLM_LINE_INSTNL_REV_CTR_DT",
    "CLM_LINE_OTHR_TP_PD_AMT",
    "CLM_LINE_NCVRD_PD_AMT",
]

PRFNL_LINE_COL = [
    "CLM_SUPLR_TYPE_CD",
    "CLM_BENE_PRMRY_PYR_PD_AMT",
    "CLM_FED_TYPE_SRVC_CD",
    "CLM_LINE_CARR_CLNCL_CHRG_AMT",
    "CLM_LINE_CARR_CLNCL_LAB_NUM",
    "CLM_LINE_CARR_HPSA_SCRCTY_CD",
    "CLM_LINE_CARR_PSYCH_OT_LMT_AMT",
    "CLM_LINE_HCT_HGB_RSLT_NUM",
    "CLM_LINE_HCT_HGB_TYPE_CD",
    "CLM_LINE_NDC_QTY_QLFYR_CD",
    "CLM_LINE_PRFNL_DME_PRICE_AMT",
    "CLM_LINE_DMERC_SCRN_SVGS_AMT",
    "CLM_LINE_PRFNL_INTRST_AMT",
    "CLM_LINE_PRFNL_MTUS_CNT",
    "CLM_MDCR_PRMRY_PYR_ALOWD_AMT",
    "CLM_PHYSN_ASTNT_CD",
    "CLM_PMT_80_100_CD",
    "CLM_PRCNG_LCLTY_CD",
    "CLM_PRCSG_IND_CD",
    "CLM_PRMRY_PYR_CD",
    "CLM_PRVDR_SPCLTY_CD",
    "CLM_SRVC_DDCTBL_SW",
]

INST_LINE_COL = [
    "CLM_DDCTBL_COINSRNC_CD",
    "CLM_LINE_INSTNL_ADJSTD_AMT",
    "CLM_LINE_INSTNL_MSP1_PD_AMT",
    "CLM_LINE_INSTNL_MSP2_PD_AMT",
    "CLM_LINE_INSTNL_RATE_AMT",
    "CLM_LINE_INSTNL_RDCD_AMT",
    "CLM_MTUS_IND_CD",
    "CLM_REV_APC_HIPPS_CD",
    "CLM_LINE_ADD_ON_PYMT_AMT",
    "CLM_LINE_NON_EHR_RDCTN_AMT",
    "CLM_LINE_NDC_QTY_QLFYR_CD",
    "CLM_REV_CNTR_STUS_CD",
    "CLM_REV_PMT_MTHD_CD",
    "CLM_REV_PACKG_IND_CD",
    "CLM_OTAF_ONE_IND_CD",
    "CLM_REV_DSCNT_IND_CD",
    "CLM_ANSI_SGNTR_SK",
]

ANSI_SGNTR_COL = [
    "CLM_1_REV_CNTR_ANSI_GRP_CD",
    "CLM_1_REV_CNTR_ANSI_RSN_CD"
]

DCMTN_LINE_COL = [
    "CLM_LINE_PA_UNIQ_TRKNG_NUM",
]


class ClmLineBuilder:
    def __init__(self, source_directory: str, clm_row: dict[str, str]):
        self.source_directory = source_directory
        self.clm_row = clm_row

    def build_line_items(self) -> list[dict[str, str | None]]:
        clm_lines = self.read_line()
        instnl_lines = self.read_instnl_lines()
        prfnl_lines = self.read_prfnl_lines()
        dcmtn_lines = self.read_dcmtn_lines()
        ansi_sgntr_sk = "CLM_ANSI_SGNTR_SK"
        return [
            {
                # extract <-- CLM_LINE_COLs from line
                **{col: extract_col_str(clm_line, col) for col in CLM_LINE_COL},
                # extract <-- professional line cols linked by line_num
                **{
                    col: find_field_in_line_by_num(prfnl_lines, clm_line, col)
                    for col in PRFNL_LINE_COL
                },
                # extract <-- insitutional line cols linked by line_num
                **{
                    col: find_field_in_line_by_num(instnl_lines, clm_line, col)
                    for col in INST_LINE_COL
                },
                # extract <-- dcmtn line cols linked by line_num
                **{
                    col: find_field_in_line_by_num(dcmtn_lines, clm_line, col)
                    for col in DCMTN_LINE_COL
                },
                # extract <-- ansi_sgntr cols linked by line_num (clm_line -> instl_line)
                # & sk (instl_line.ansi_sgntr_sk --> ansi_sgntr line)
                **{
                    col: find_linked_field_in_tgt_by_line_num_sk(src_lines=instnl_lines,
                    clm_line=clm_line, source_directory=self.source_directory,
                    sk_field=ansi_sgntr_sk, extracted_field=col)
                    for col in ANSI_SGNTR_COL
                },
            }
            for clm_line in clm_lines
        ]

    def read_line(self) -> list[dict[str, str]]:
        return read_multi_line_file(
            source_directory=self.source_directory,
            file_name="SYNTHETIC_CLM_LINE",
            not_found_fail=True,
            params=[
                Param("GEO_BENE_SK", self.clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", self.clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", self.clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", self.clm_row["CLM_NUM_SK"]),
            ],
        )

    def read_instnl_lines(self) -> list[dict[str, str]]:
        return read_multi_line_file(
            source_directory=self.source_directory,
            file_name="SYNTHETIC_CLM_LINE_INSTNL",
            not_found_fail=False,
            params=[
                Param("GEO_BENE_SK", self.clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", self.clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", self.clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", self.clm_row["CLM_NUM_SK"]),
            ],
        )

    def read_prfnl_lines(self) -> list[dict[str, str]]:
        return read_multi_line_file(
            source_directory=self.source_directory,
            file_name="SYNTHETIC_CLM_LINE_PRFNL",
            not_found_fail=False,
            params=[
                Param("GEO_BENE_SK", self.clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", self.clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", self.clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", self.clm_row["CLM_NUM_SK"]),
            ],
        )

    def read_dcmtn_lines(self) -> list[dict[str, str]]:
        return read_multi_line_file(
            source_directory=self.source_directory,
            file_name="SYNTHETIC_CLM_LINE_DCMTN",
            not_found_fail=False,
            params=[
                Param("GEO_BENE_SK", self.clm_row["GEO_BENE_SK"]),
                Param("CLM_DT_SGNTR_SK", self.clm_row["CLM_DT_SGNTR_SK"]),
                Param("CLM_TYPE_CD", self.clm_row["CLM_TYPE_CD"]),
                Param("CLM_NUM_SK", self.clm_row["CLM_NUM_SK"]),
            ],
        )
