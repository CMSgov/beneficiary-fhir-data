import random
import string
from datetime import date
from typing import Any

import field_constants as f
from claims_static import (
    ADJUDICATED_PROFESSIONAL_CLAIM_TYPES,
    AVAIL_CLM_RLT_COND_SK,
    AVAIL_CONTRACT_NUMS,
    AVAIL_OSCAR_CODES_INSTITUTIONAL,
    AVAIL_PBP_NUMS,
    AVAILABLE_NDC,
    CLM_POA_IND_CHOICES,
    FISS_CLM_TYPE_CDS,
    HCPCS_MODS,
    INSTITUTIONAL_CLAIM_TYPES,
    NOW,
    PHARMACY_CLM_TYPE_CDS,
    TARGET_RLT_COND_CODES,
    TARGET_SEQUENCE_NUMBERS,
    TYPE_1_NPIS,
    TYPE_2_NPIS,
    get_drg_dgns_codes,
    get_hcpcs_proc_codes,
    get_icd_10_dgns_codes,
    get_icd_10_prcdr_codes,
)
from claims_util import add_meta_timestamps, get_ric_cd_for_clm_type_cd
from generator_util import (
    GeneratorUtil,
    RowAdapter,
    gen_basic_id,
    gen_multipart_id,
    gen_numeric_id,
    gen_thru_dt,
    probability,
    random_date,
)


def gen_clm(
    gen_utils: GeneratorUtil,
    bene_sk: str,
    init_clm: RowAdapter | None = None,
    min_date: str = "2018-01-01",
    max_date: str = str(NOW),
):
    clm = init_clm or RowAdapter({})
    clm[f.CLM_DT_SGNTR_SK] = gen_basic_id(field=f.CLM_DT_SGNTR_SK, length=12)
    clm[f.CLM_UNIQ_ID] = gen_basic_id(field=f.CLM_UNIQ_ID, length=13)
    clm[f.CLM_RLT_COND_SGNTR_SK] = gen_numeric_id(field=f.CLM_RLT_COND_SGNTR_SK, start=-2)

    clm_type_cd = (
        int(clm[f.CLM_TYPE_CD])
        if clm.kv.get(f.CLM_TYPE_CD)
        else random.choice([1, 2, 3, 4, 10, 20, 30, 40, 50, 60, 71, 72, 81, 82])
    )
    clm[f.CLM_TYPE_CD] = clm_type_cd
    clm_src_id = 20000
    clm[f.CLM_SRC_ID] = clm_src_id
    clm[f.META_SRC_SK] = 7
    clm[f.CLM_FROM_DT] = random_date(min_date, max_date)
    clm[f.CLM_THRU_DT] = gen_thru_dt(clm[f.CLM_FROM_DT])

    # NON-PDE
    clm[f.CLM_CNTL_NUM] = gen_multipart_id(
        field=f.CLM_CNTL_NUM, parts=[(string.digits, 14), (string.ascii_uppercase, 3)]
    )
    # PDE -> diff Claim control number process.
    if clm_type_cd in PHARMACY_CLM_TYPE_CDS:
        clm[f.CLM_ORIG_CNTL_NUM] = gen_multipart_id(
            field=f.CLM_ORIG_CNTL_NUM, parts=[(string.digits, 14), (string.ascii_uppercase, 3)]
        )
        clm[f.CLM_RLT_COND_SGNTR_SK] = "-1"
        clm[f.META_SRC_SK] = 1

    if clm_type_cd in (20, 30, 40, 60, 61, 62, 63, 71, 72):
        clm[f.CLM_BLOOD_PT_FRNSH_QTY] = random.randint(0, 20)

    clm[f.CLM_NUM_SK] = gen_numeric_id(field=f.CLM_NUM_SK)
    clm[f.CLM_EFCTV_DT] = str(date.today())
    clm[f.CLM_IDR_LD_DT] = random_date(clm[f.CLM_FROM_DT], max_date)
    clm[f.CLM_OBSLT_DT] = "9999-12-31"
    clm[f.GEO_BENE_SK] = gen_numeric_id(field=f.GEO_BENE_SK)
    clm[f.BENE_SK] = bene_sk
    clm[f.CLM_DISP_CD] = random.choice(gen_utils.code_systems[f.CLM_DISP_CD])
    clm[f.CLM_ADJSTMT_TYPE_CD] = random.choice(gen_utils.code_systems[f.CLM_ADJSTMT_TYPE_CD])

    if clm_type_cd not in PHARMACY_CLM_TYPE_CDS:
        clm[f.CLM_QUERY_CD] = random.choice(gen_utils.code_systems[f.CLM_QUERY_CD])
    else:
        clm[f.CLM_SRVC_PRVDR_GNRC_ID_NUM] = random.choice(TYPE_2_NPIS)
        clm[f.PRVDR_SRVC_PRVDR_NPI_NUM] = clm[f.CLM_SRVC_PRVDR_GNRC_ID_NUM]
        clm[f.CLM_PD_DT] = random_date(clm[f.CLM_FROM_DT], clm[f.CLM_THRU_DT])
        clm[f.PRVDR_PRSCRBNG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm[f.CLM_SBMT_CHRG_AMT] = round(random.uniform(1, 1000000), 2)
        clm[f.CLM_SBMT_FRMT_CD] = random.choice(gen_utils.code_systems[f.CLM_SBMT_FRMT_CD])
        clm[f.CLM_SBMTR_CNTRCT_NUM] = random.choice(AVAIL_CONTRACT_NUMS)
        clm[f.CLM_SBMTR_CNTRCT_PBP_NUM] = random.choice(AVAIL_PBP_NUMS)
        clm[f.CLM_BENE_PMT_AMT] = round(random.uniform(0, 1000), 2)
        clm[f.CLM_OTHR_TP_PD_AMT] = round(random.uniform(0, 1000), 2)

    if clm_type_cd in INSTITUTIONAL_CLAIM_TYPES:
        tob_code = random.choice(gen_utils.code_systems[f.CLM_BILL_FREQ_CD])
        clm[f.CLM_BILL_FAC_TYPE_CD] = tob_code[0]
        clm[f.CLM_BILL_CLSFCTN_CD] = tob_code[1]
        clm[f.CLM_BILL_FREQ_CD] = tob_code[2]

    clm[f.CLM_CNTRCTR_NUM] = random.choice(gen_utils.code_systems[f.CLM_CNTRCTR_NUM])
    clm[f.CLM_NCH_PRMRY_PYR_CD] = random.choice(gen_utils.code_systems[f.CLM_NCH_PRMRY_PYR_CD])

    clm[f.CLM_FINL_ACTN_IND] = "Y"
    if clm[f.CLM_TYPE_CD] == 3 or clm[f.CLM_ADJSTMT_TYPE_CD] == "1":
        clm[f.CLM_FINL_ACTN_IND] = "N"

    clm_ltst_clm_ind = "N"
    if clm_type_cd in (1, 2, 3, 4, 10, 20, 30, 40, 50, 60, 61, 62, 63, 71, 72, 81, 82):
        clm_ltst_clm_ind = random.choice(["Y", "N"])
    clm[f.CLM_LTST_CLM_IND] = clm_ltst_clm_ind

    if (clm_type_cd < 65 and clm_type_cd >= 10) or clm_type_cd in FISS_CLM_TYPE_CDS:
        clm[f.PRVDR_BLG_PRVDR_NPI_NUM] = random.choice(TYPE_2_NPIS)
        clm[f.CLM_ATNDG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm[f.PRVDR_ATNDG_PRVDR_NPI_NUM] = clm[f.CLM_ATNDG_PRVDR_NPI_NUM]
        clm[f.CLM_OPRTG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm[f.PRVDR_OPRTG_PRVDR_NPI_NUM] = clm[f.CLM_OPRTG_PRVDR_NPI_NUM]
        clm[f.CLM_OTHR_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm[f.PRVDR_OTHR_PRVDR_NPI_NUM] = clm[f.CLM_OTHR_PRVDR_NPI_NUM]
        clm[f.CLM_RNDRG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm[f.PRVDR_RNDRNG_PRVDR_NPI_NUM] = clm[f.CLM_RNDRG_PRVDR_NPI_NUM]
        clm[f.CLM_BLG_PRVDR_OSCAR_NUM] = random.choice(AVAIL_OSCAR_CODES_INSTITUTIONAL)
        clm[f.CLM_MDCR_COINSRNC_AMT] = round(random.uniform(0, 25), 2)
        clm[f.CLM_BLG_PRVDR_ZIP5_CD] = random.choice(["75205", "77550", "77005"])
        clm[f.CLM_RLT_COND_SGNTR_SK] = random.choice(AVAIL_CLM_RLT_COND_SK)

    if clm_type_cd == 40 or (clm_type_cd > 70 and clm_type_cd <= 82):
        clm[f.PRVDR_RFRG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
    if clm_type_cd > 70 and clm_type_cd <= 82:
        clm[f.CLM_BLG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm[f.CLM_RLT_COND_SGNTR_SK] = "0"
        if random.choice([0, 1]):
            clm[f.CLM_BLG_PRVDR_NPI_NUM] = random.choice(TYPE_2_NPIS)
            clm[f.PRVDR_BLG_PRVDR_NPI_NUM] = clm[f.CLM_BLG_PRVDR_NPI_NUM]

    # generate claim header financial elements here
    clm[f.CLM_SBMT_CHRG_AMT] = round(random.uniform(1, 1000000), 2)
    if clm_type_cd == 71 or clm_type_cd == 72:
        clm[f.CLM_RFRG_PRVDR_PIN_NUM] = random.choice(
            [
                9181272397,
                9181272391,
                918127239123,
            ]
        )
    if clm_type_cd > 70 and clm_type_cd <= 82:
        clm[f.CLM_ALOWD_CHRG_AMT] = round(random.uniform(1, 1000000), 2)
        clm[f.CLM_BENE_PD_AMT] = round(random.uniform(1, 1000000), 2)
        clm[f.CLM_BENE_PMT_AMT] = round(random.uniform(1, 1000000), 2)
        clm[f.CLM_PRVDR_PMT_AMT] = round(random.uniform(1, 1000000), 2)
    clm[f.CLM_PMT_AMT] = round(random.uniform(1, float(clm[f.CLM_SBMT_CHRG_AMT])), 2)
    clm[f.CLM_MDCR_DDCTBL_AMT] = round(random.uniform(1, 1676), 2)
    clm[f.CLM_NCVRD_CHRG_AMT] = round(
        float(clm[f.CLM_SBMT_CHRG_AMT]) - float(clm[f.CLM_PMT_AMT]), 2
    )
    clm[f.CLM_BLOOD_LBLTY_AMT] = round(random.uniform(0, 25), 2)

    if clm_type_cd in (40, 71, 72, 81, 82):
        # be sure to check that DME claims meet the above.
        clm[f.CLM_PRVDR_PMT_AMT] = round(random.uniform(0, 25), 2)

    if clm_type_cd in (71, 72, 81, 82):
        clm[f.CLM_BENE_INTRST_PD_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        clm[f.CLM_BENE_PMT_COINSRNC_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        clm[f.CLM_BLOOD_CHRG_AMT] = round(random.uniform(0, 500), 2)
        clm[f.CLM_BLOOD_NCVRD_CHRG_AMT] = round(random.uniform(0, 500), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        clm[f.CLM_COB_PTNT_RESP_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (81, 82):
        clm[f.CLM_OTHR_TP_PD_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64, 71, 72, 81, 82):
        clm[f.CLM_PRVDR_INTRST_PD_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        clm[f.CLM_PRVDR_OTAF_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (40, 50, 60, 61, 62, 63, 64, 81, 82):
        clm[f.CLM_PRVDR_RMNG_DUE_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        clm[f.CLM_TOT_CNTRCTL_AMT] = round(random.uniform(0, 10000), 2)

    add_meta_timestamps(clm, clm)

    return clm


def gen_clm_rlt_cond_sgntr_mbr(
    clm: RowAdapter, init_clm_rlt_cond_sgntr_mbr: RowAdapter | None = None
):
    clm_rlt_cond_sgntr_mbr = init_clm_rlt_cond_sgntr_mbr or RowAdapter({})
    clm_rlt_cond_sgntr_mbr[f.CLM_RLT_COND_SGNTR_SK] = (
        clm[f.CLM_RLT_COND_SGNTR_SK]
        if int(clm[f.CLM_RLT_COND_SGNTR_SK]) < -1
        else gen_numeric_id(field=f.CLM_RLT_COND_SGNTR_SK, start=-2)
    )
    clm_rlt_cond_sgntr_mbr[f.CLM_RLT_COND_SGNTR_SQNC_NUM] = random.choice(TARGET_SEQUENCE_NUMBERS)
    clm_rlt_cond_sgntr_mbr[f.CLM_RLT_COND_CD] = random.choice(TARGET_RLT_COND_CODES)

    # HACK: Of all claims tables that are derived from CLM, this particular table is the only one
    # where rows can be orphaned from their root CLM row as some CLM rows may set their
    # CLM_RLT_COND_SGNTR_SK to an invalid value. There are no other fields from which a foreign-key
    # relationship can be derived, so we must store the CLM_UNIQ_ID of the parent CLM for each row
    # of this table, otherwise we would have to special-case the logic for generating this table.
    # CLM_UNIQ_ID will be ignored by the pipeline when loading, so this is OK
    clm_rlt_cond_sgntr_mbr[f.CLM_UNIQ_ID] = clm[f.CLM_UNIQ_ID]

    add_meta_timestamps(clm_rlt_cond_sgntr_mbr, clm)

    return clm_rlt_cond_sgntr_mbr


def gen_pharm_clm_line(clm: RowAdapter, init_clm_line: RowAdapter | None = None):
    clm_line = init_clm_line or RowAdapter({})
    clm_line[f.CLM_UNIQ_ID] = clm[f.CLM_UNIQ_ID]
    clm_line[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_line[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_line[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line[f.CLM_LINE_CVRD_PD_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line[f.CLM_LINE_NCVRD_PD_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line[f.CLM_LINE_NCVRD_CHRG_AMT] = round(random.uniform(0, 1500), 2)
    clm_line[f.CLM_LINE_NDC_CD] = random.choice(AVAILABLE_NDC)
    clm_line[f.CLM_LINE_SRVC_UNIT_QTY] = random.randint(1, 10)
    clm_line[f.CLM_LINE_FROM_DT] = clm[f.CLM_FROM_DT]
    clm_line[f.CLM_LINE_THRU_DT] = clm[f.CLM_THRU_DT]
    clm_line[f.CLM_LINE_NDC_QTY] = random.randint(1, 10)
    clm_line[f.CLM_LINE_NDC_QTY_QLFYR_CD] = "ML"
    clm_line[f.CLM_LINE_BENE_PD_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line[f.CLM_LINE_PRVDR_PMT_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line[f.CLM_LINE_SBMT_CHRG_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_BENE_PMT_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_BLOOD_DDCTBL_AMT] = round(random.uniform(0, 15), 2)
    clm_line[f.CLM_LINE_MDCR_DDCTBL_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_NUM] = "1"
    clm_line[f.CLM_FROM_DT] = clm[f.CLM_FROM_DT]
    clm_line[f.CLM_LINE_RX_NUM] = round(random.uniform(0, 100000), 2)
    clm_line[f.CLM_LINE_GRS_CVRD_CST_TOT_AMT] = round(random.uniform(0, 1000), 2)
    clm_line[f.CLM_LINE_OTHR_TP_PD_AMT] = round(random.uniform(0, 1000), 2)

    return clm_line


def gen_pharm_clm_line_rx(
    gen_utils: GeneratorUtil, clm: RowAdapter, init_clm_line_rx: RowAdapter | None = None
):
    clm_line_rx = init_clm_line_rx or RowAdapter({})
    clm_line_rx[f.CLM_UNIQ_ID] = clm[f.CLM_UNIQ_ID]
    clm_line_rx[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line_rx[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_line_rx[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_line_rx[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line_rx[f.CLM_LINE_NUM] = "1"
    clm_line_rx[f.CLM_FROM_DT] = clm[f.CLM_FROM_DT]
    clm_line_rx[f.CLM_DSPNSNG_STUS_CD] = random.choice(["P", "C"])
    clm_line_rx[f.CLM_LINE_RX_ORGN_CD] = random.choice(
        gen_utils.code_systems[f.CLM_LINE_RX_ORGN_CD]
    )
    clm_line_rx[f.CLM_BRND_GNRC_CD] = random.choice(gen_utils.code_systems[f.CLM_BRND_GNRC_CD])
    clm_line_rx[f.CLM_PTNT_RSDNC_CD] = random.choice(gen_utils.code_systems[f.CLM_PTNT_RSDNC_CD])
    clm_line_rx[f.CLM_PHRMCY_SRVC_TYPE_CD] = random.choice(
        gen_utils.code_systems[f.CLM_PHRMCY_SRVC_TYPE_CD]
    )
    clm_line_rx[f.CLM_LINE_AUTHRZD_FILL_NUM] = (
        "0"  # for whatever reason, this is always zero in the IDR
    )
    clm_line_rx[f.CLM_LTC_DSPNSNG_MTHD_CD] = random.choice(
        gen_utils.code_systems[f.CLM_LTC_DSPNSNG_MTHD_CD]
    )
    clm_line_rx[f.CLM_CMPND_CD] = random.choice(gen_utils.code_systems[f.CLM_CMPND_CD])
    clm_line_rx[f.CLM_LINE_DAYS_SUPLY_QTY] = random.randint(1, 10)
    clm_line_rx[f.CLM_LINE_RX_FILL_NUM] = random.randint(1, 10)
    clm_line_rx[f.CLM_DAW_PROD_SLCTN_CD] = random.choice([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])
    clm_line_rx[f.CLM_DRUG_CVRG_STUS_CD] = random.choice(
        gen_utils.code_systems[f.CLM_DRUG_CVRG_STUS_CD]
    )
    clm_line_rx[f.CLM_CTSTRPHC_CVRG_IND_CD] = random.choice(
        gen_utils.code_systems[f.CLM_CTSTRPHC_CVRG_IND_CD]
    )
    clm_line_rx[f.CLM_LINE_GRS_ABOVE_THRSHLD_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_GRS_BLW_THRSHLD_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_LIS_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_TROOP_TOT_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_PLRO_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_RPTD_MFTR_DSCNT_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_INGRDNT_CST_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_VCCN_ADMIN_FEE_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_SRVC_CST_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_LINE_SLS_TAX_AMT] = round(random.uniform(1, 1000000), 2)
    clm_line_rx[f.CLM_PRCNG_EXCPTN_CD] = random.choice(["", "O", "M"])
    clm_line_rx[f.CLM_CMS_CALCD_MFTR_DSCNT_AMT] = round(random.uniform(0, 1000), 2)
    clm_line_rx[f.CLM_LINE_REBT_PASSTHRU_POS_AMT] = round(random.uniform(0, 1000), 2)
    clm_line_rx[f.CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT] = round(random.uniform(0, 1000), 2)
    clm_line_rx[f.CLM_LINE_RPTD_GAP_DSCNT_AMT] = round(random.uniform(1, 1000000), 2)

    add_meta_timestamps(clm_line_rx, clm)

    return clm_line_rx


def gen_clm_dcmtn(clm: RowAdapter, init_clm_dcmtn: RowAdapter | None = None):
    clm_dcmtn = init_clm_dcmtn or RowAdapter({})
    clm_dcmtn[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_dcmtn[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_dcmtn[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_dcmtn[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    # CLM_RIC_CDs are generally tied to the claim type code.
    ric_cd = get_ric_cd_for_clm_type_cd(clm[f.CLM_TYPE_CD])
    if ric_cd:
        clm_dcmtn[f.CLM_NRLN_RIC_CD] = ric_cd

    add_meta_timestamps(clm_dcmtn, clm)

    return clm_dcmtn


def gen_dsprtnt_clm_val(clm: RowAdapter, init_clm_val: RowAdapter | None = None):
    # CLM_OPRTNL_DSPRTNT_AMT
    # Note, this is a table we'll use sparsely, it appears. I've replaced the 5 key unique
    # identifier with CLM_UNIQ_ID.
    clm_val = init_clm_val or RowAdapter({})
    clm_val[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_val[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_val[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_val[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_val[f.CLM_VAL_CD] = 18
    clm_val[f.CLM_VAL_AMT] = round(random.uniform(1, 15000), 2)
    clm_val[f.CLM_VAL_SQNC_NUM] = 14

    add_meta_timestamps(clm_val, clm)

    return clm_val


def gen_ime_clm_val(clm: RowAdapter, init_clm_val: RowAdapter | None = None):
    clm_val = init_clm_val or RowAdapter({})
    clm_val[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_val[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_val[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_val[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_val[f.CLM_VAL_CD] = 19
    clm_val[f.CLM_VAL_AMT] = round(random.uniform(1, 15000), 2)
    clm_val[f.CLM_VAL_SQNC_NUM] = 3

    add_meta_timestamps(clm_val, clm)

    return clm_val


def gen_proc_clm_prod(
    clm: RowAdapter, clm_val_sqnc_num: int, init_clm_prod: RowAdapter | None = None
):
    clm_prod = init_clm_prod or RowAdapter({})
    clm_prod[f.CLM_PROD_TYPE_CD] = "S"
    clm_prod[f.CLM_PRCDR_CD] = random.choice(get_icd_10_prcdr_codes())
    clm_prod[f.CLM_DGNS_PRCDR_ICD_IND] = "0"
    clm_prod[f.CLM_PRCDR_PRFRM_DT] = random_date(clm[f.CLM_FROM_DT], clm[f.CLM_THRU_DT])
    clm_prod[f.CLM_VAL_SQNC_NUM] = clm_val_sqnc_num
    clm_prod[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_prod[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_prod[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_prod[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    add_meta_timestamps(clm_prod, clm)

    return clm_prod


def gen_diag_clm_prod_list(clm: RowAdapter, init_diagnoses: list[RowAdapter] | None = None):
    if init_diagnoses is None:
        init_diagnoses = []

    def match_diag(subdict: dict[str, Any]):
        # Find the row with matching columns so that we can run regeneration on diagnosis rows
        return next(
            (x for x in init_diagnoses if subdict.items() <= x.kv.items()), None
        ) or RowAdapter({})

    diagnoses: list[RowAdapter] = []
    clm_type_cd = int(clm[f.CLM_TYPE_CD])
    num_add_diags = 0
    if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        # inpatient uses concepts of principal, admitting, other, external

        # "*_static" dicts are used to match upon existing diagnosis rows so that their "*_data"
        # columns can be updated during regeneration; otherwise, everytime claims data is
        # regenerated these rows would have different column values
        principal_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "P"}
        principal_data = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | principal_static
        principal = match_diag(principal_static)
        principal.extend(principal_data)

        first_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "D"}
        first_data = {
            f.CLM_DGNS_CD: principal[f.CLM_DGNS_CD],
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: random.choice(CLM_POA_IND_CHOICES),
        } | first_static
        first = match_diag(first_static)
        first.extend(first_data)

        admitting_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "A"}
        admitting_data = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | admitting_static
        admitting = match_diag(admitting_static)
        admitting.extend(admitting_data)

        external_1_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "E"}
        external_1_data = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "0",  # ALWAYS for ICD-10 codes. not always for icd-9.
        } | external_1_static
        external_1 = match_diag(external_1_static)
        external_1.extend(external_1_data)

        first_external_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "1"}
        first_external_data = {
            f.CLM_DGNS_CD: external_1[f.CLM_DGNS_CD],
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | first_external_static
        first_external = match_diag(first_external_static)
        first_external.extend(first_external_data)

        diagnoses.append(principal)
        diagnoses.append(first)
        diagnoses.append(admitting)
        diagnoses.append(external_1)
        diagnoses.append(first_external)
        num_add_diags = (
            len(init_diagnoses) - 5 if len(init_diagnoses) > 5 else random.randint(2, 15)
        )
    elif clm_type_cd == 40:
        # outpatient uses principal, other, external cause of injury, patient reason for visit
        principal_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "P"}
        principal_data = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | principal_static
        principal = match_diag(principal_static)
        principal.extend(principal_data)

        first_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "D"}
        first_data = {
            f.CLM_DGNS_CD: principal[f.CLM_DGNS_CD],
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | first_static
        first = match_diag(first_static)
        first.extend(first_data)

        rfv_diag_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "R"}
        rfv_diag_data = {
            f.CLM_DGNS_CD: principal[f.CLM_DGNS_CD],
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | rfv_diag_static
        rfv_diag = match_diag(rfv_diag_static)
        rfv_diag.extend(rfv_diag_data)

        diagnoses.append(principal)
        diagnoses.append(first)
        diagnoses.append(rfv_diag)
        num_add_diags = (
            len(init_diagnoses) - 3 if len(init_diagnoses) > 3 else random.randint(2, 15)
        )
    elif clm_type_cd in ADJUDICATED_PROFESSIONAL_CLAIM_TYPES:
        # professional claims use principal diagnosis and other diagnoses
        principal_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "P"}
        principal_data = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | principal_static
        principal = match_diag(principal_static)
        principal.extend(principal_data)

        first_static = {f.CLM_VAL_SQNC_NUM: "1", f.CLM_PROD_TYPE_CD: "D"}
        first_data = {
            f.CLM_DGNS_CD: principal[f.CLM_DGNS_CD],
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_POA_IND: "~",
        } | first_static
        first = match_diag(first_static)
        first.extend(first_data)

        diagnoses.append(principal)
        diagnoses.append(first)
        num_add_diags = (
            len(init_diagnoses) - 2 if len(init_diagnoses) > 2 else random.randint(2, 8)
        )  # Professional claims typically have fewer diagnoses

    for diagnosis_sqnc in range(2, num_add_diags + 2):
        diagnosis_static = {f.CLM_VAL_SQNC_NUM: str(diagnosis_sqnc), f.CLM_PROD_TYPE_CD: "D"}
        diagnosis_data: dict[str, Any] = {}
        if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
            diagnosis_data = {
                f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
                f.CLM_DGNS_PRCDR_ICD_IND: "0",
                f.CLM_POA_IND: random.choice(CLM_POA_IND_CHOICES),
            }
        elif clm_type_cd == 40:
            diagnosis_data = {
                f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
                f.CLM_DGNS_PRCDR_ICD_IND: "0",
            }
        elif clm_type_cd in ADJUDICATED_PROFESSIONAL_CLAIM_TYPES:
            diagnosis_data = {
                f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
                f.CLM_DGNS_PRCDR_ICD_IND: "0",
                f.CLM_POA_IND: "~",
            }

        diagnosis = match_diag(diagnosis_static)
        diagnosis.extend(diagnosis_data | diagnosis_static)
        diagnoses.append(diagnosis)

    for diagnosis in diagnoses:
        diagnosis[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
        diagnosis[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
        diagnosis[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
        diagnosis[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
        add_meta_timestamps(diagnosis, clm)

    return diagnoses


def gen_clm_instnl(
    gen_utils: GeneratorUtil, clm: RowAdapter, init_clm_instnl: RowAdapter | None = None
):
    clm_type_cd = int(clm[f.CLM_TYPE_CD])
    clm_instnl = init_clm_instnl or RowAdapter({})

    clm_instnl[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_instnl[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_instnl[f.CLM_TYPE_CD] = clm_type_cd
    clm_instnl[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    if clm_type_cd == 40:
        clm_instnl[f.CLM_OP_SRVC_TYPE_CD] = random.choice(
            gen_utils.code_systems[f.CLM_OP_SRVC_TYPE_CD]
        )
    clm_instnl[f.CLM_FI_ACTN_CD] = random.choice(gen_utils.code_systems[f.CLM_FI_ACTN_CD])
    clm_instnl[f.CLM_ADMSN_TYPE_CD] = random.choice(gen_utils.code_systems[f.CLM_ADMSN_TYPE_CD])
    clm_instnl[f.BENE_PTNT_STUS_CD] = random.choice(gen_utils.code_systems[f.BENE_PTNT_STUS_CD])
    clm_instnl[f.CLM_MDCR_INSTNL_MCO_PD_SW] = random.choice(
        gen_utils.code_systems[f.CLM_MDCR_INSTNL_MCO_PD_SW]
    )
    clm_instnl[f.CLM_ADMSN_SRC_CD] = random.choice(gen_utils.code_systems[f.CLM_ADMSN_SRC_CD])
    clm_instnl[f.DGNS_DRG_CD] = random.choice(get_drg_dgns_codes())
    clm_instnl[f.DGNS_DRG_OUTLIER_CD] = random.choice(gen_utils.code_systems[f.DGNS_DRG_OUTLIER_CD])
    clm_instnl[f.CLM_INSTNL_CVRD_DAY_CNT] = random.randint(0, 10)
    clm_instnl[f.CLM_MDCR_IP_LRD_USE_CNT] = random.randint(0, 10)
    clm_instnl[f.CLM_INSTNL_PER_DIEM_AMT] = round(random.uniform(0, 350), 2)
    clm_instnl[f.CLM_HIPPS_UNCOMPD_CARE_AMT] = round(random.uniform(0, 350), 2)
    clm_instnl[f.CLM_MDCR_INSTNL_PRMRY_PYR_AMT] = round(random.uniform(0, 3500), 2)
    clm_instnl[f.CLM_INSTNL_DRG_OUTLIER_AMT] = round(random.uniform(0, 3500), 2)
    clm_instnl[f.CLM_MDCR_IP_PPS_DSPRPRTNT_AMT] = round(random.uniform(0, 3500), 2)
    clm_instnl[f.CLM_INSTNL_MDCR_COINS_DAY_CNT] = random.randint(0, 5)
    clm_instnl[f.CLM_INSTNL_NCVRD_DAY_CNT] = random.randint(0, 5)
    clm_instnl[f.CLM_MDCR_IP_PPS_DRG_WT_NUM] = round(random.uniform(0.5, 1.5), 2)
    clm_instnl[f.CLM_MDCR_IP_PPS_EXCPTN_AMT] = round(random.uniform(0, 25), 2)
    clm_instnl[f.CLM_MDCR_IP_PPS_CPTL_FSP_AMT] = round(random.uniform(0, 25), 2)
    clm_instnl[f.CLM_MDCR_IP_PPS_CPTL_IME_AMT] = round(random.uniform(0, 25), 2)
    clm_instnl[f.CLM_MDCR_IP_PPS_OUTLIER_AMT] = round(random.uniform(0, 25), 2)
    clm_instnl[f.CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT] = round(random.uniform(0, 25), 2)
    clm_instnl[f.CLM_MDCR_IP_PPS_CPTL_TOT_AMT] = round(random.uniform(0, 25), 2)
    clm_instnl[f.CLM_MDCR_IP_BENE_DDCTBL_AMT] = round(random.uniform(0, 25), 2)
    clm_instnl[f.CLM_PPS_IND_CD] = random.choice(["", "2"])

    if clm_type_cd in (10, 20):
        clm_instnl[f.CLM_FINL_STDZD_PYMT_AMT] = round(random.uniform(0, 10000), 2)
    if clm_type_cd == 20:
        clm_instnl[f.CLM_HAC_RDCTN_PYMT_AMT] = round(random.uniform(0, 5000), 2)
        clm_instnl[f.CLM_HIPPS_MODEL_BNDLD_PMT_AMT] = round(random.uniform(0, 10000), 2)
        clm_instnl[f.CLM_SITE_NTRL_CST_BSD_PYMT_AMT] = round(random.uniform(0, 10000), 2)
        clm_instnl[f.CLM_SITE_NTRL_IP_PPS_PYMT_AMT] = round(random.uniform(0, 10000), 2)
        clm_instnl[f.CLM_SS_OUTLIER_STD_PYMT_AMT] = round(random.uniform(0, 10000), 2)
    if clm_type_cd in (20, 30, 60, 61, 62, 63, 64):
        clm_instnl[f.CLM_HIPPS_READMSN_RDCTN_AMT] = round(random.uniform(0, 5000), 2)
        clm_instnl[f.CLM_HIPPS_VBP_AMT] = round(random.uniform(0, 5000), 2)
        clm_instnl[f.CLM_INSTNL_LOW_VOL_PMT_AMT] = round(random.uniform(0, 10000), 2)
        clm_instnl[f.CLM_MDCR_IP_1ST_YR_RATE_AMT] = round(random.uniform(0, 10000), 2)
        clm_instnl[f.CLM_MDCR_IP_SCND_YR_RATE_AMT] = round(random.uniform(0, 10000), 2)
        clm_instnl[f.CLM_PPS_MD_WVR_STDZD_VAL_AMT] = round(random.uniform(0, 10000), 2)
    if clm_type_cd in (40, 61, 64, 62, 20, 63, 30, 60):
        clm_instnl[f.CLM_INSTNL_PRFNL_AMT] = round(random.uniform(0, 10000), 2)

    add_meta_timestamps(clm_instnl, clm)

    if clm_type_cd == 10:
        if random.choice([0, 1]):
            clm_instnl[f.CLM_HHA_LUP_IND_CD] = "L"
        clm_instnl[f.CLM_HHA_RFRL_CD] = random.choice(gen_utils.code_systems[f.CLM_HHA_RFRL_CD])
        clm_instnl[f.CLM_MDCR_HHA_TOT_VISIT_CNT] = round(random.uniform(0, 25), 2)

    if clm_type_cd == 40:
        clm_instnl[f.CLM_MDCR_INSTNL_BENE_PD_AMT] = round(random.uniform(0, 25), 2)

    if clm_type_cd == 50:
        clm_instnl[f.CLM_MDCR_HOSPC_PRD_CNT] = random.choice(["1", "2", "3"])
    # We'll throw in a non-payment code on occasion
    if random.choice([0, 10]) > 1:
        clm_instnl[f.CLM_MDCR_NPMT_RSN_CD] = random.choice(
            gen_utils.code_systems[f.CLM_MDCR_NPMT_RSN_CD]
        )

    return clm_instnl


def gen_clm_prfnl(
    gen_utils: GeneratorUtil, clm: RowAdapter, init_clm_prfnl: RowAdapter | None = None
):
    clm_prfnl = init_clm_prfnl or RowAdapter({})

    clm_prfnl[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_prfnl[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_prfnl[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_prfnl[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_prfnl[f.CLM_CARR_PMT_DNL_CD] = random.choice(gen_utils.code_systems[f.CLM_CARR_PMT_DNL_CD])
    clm_prfnl[f.CLM_MDCR_PRFNL_PRMRY_PYR_AMT] = round(random.uniform(10, 1000), 2)
    clm_prfnl[f.CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW] = random.choice(
        gen_utils.code_systems[f.CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW]
    )
    clm_prfnl[f.CLM_CLNCL_TRIL_NUM] = str(random.randint(0, 10000))
    if clm[f.CLM_TYPE_CD] in (71, 72, 81, 82):
        clm_prfnl[f.CLM_PRVDR_ACNT_RCVBL_OFST_AMT] = round(random.uniform(0, 1000), 2)

    add_meta_timestamps(clm_prfnl, clm)

    return clm_prfnl


def gen_clm_line(
    gen_utils: GeneratorUtil,
    clm: RowAdapter,
    clm_line_num: int,
    diagnoses: list[RowAdapter],
    init_clm_line: RowAdapter | None = None,
):
    clm_type_cd = int(clm[f.CLM_TYPE_CD])
    clm_line = init_clm_line or RowAdapter({})
    clm_line[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line[f.CLM_TYPE_CD] = clm_type_cd
    clm_line[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_line[f.CLM_FROM_DT] = clm[f.CLM_FROM_DT]
    clm_line[f.CLM_LINE_FROM_DT] = clm[f.CLM_FROM_DT]
    clm_line[f.CLM_LINE_THRU_DT] = clm[f.CLM_THRU_DT]
    if probability(0.10):
        clm_line[f.CLM_LINE_PMD_UNIQ_TRKNG_NUM] = gen_basic_id(
            field=f.CLM_LINE_PMD_UNIQ_TRKNG_NUM,
            length=13,  # varchar(14) so 13 + 1 for '-' prefix
            allowed_chars=string.ascii_uppercase + string.digits,
        )

    if clm_type_cd >= 71 and clm_type_cd <= 82:
        clm_line[f.CLM_RNDRG_PRVDR_TYPE_CD] = random.choice(
            gen_utils.code_systems[f.CLM_PRVDR_TYPE_CD]
        )

        # these don't have much variance in our synthetic data, but they are not strictly
        # the same in actual data!
        clm_line[f.CLM_LINE_MDCR_COINSRNC_AMT] = round(random.uniform(0, 5), 2)

        # pick a random diagnosis.
        clm_line[f.CLM_LINE_DGNS_CD] = random.choice(diagnoses)[f.CLM_DGNS_CD]
        clm_line[f.CLM_POS_CD] = random.choice(gen_utils.code_systems[f.CLM_POS_CD])
        clm_line[f.CLM_RNDRG_PRVDR_PRTCPTG_CD] = random.choice(
            gen_utils.code_systems[f.CLM_RNDRG_PRVDR_PRTCPTG_CD]
        )

    if clm_type_cd >= 71 and clm_type_cd <= 72:
        clm_line[f.CLM_RNDRG_PRVDR_TAX_NUM] = random.choice(["1928347912", "912834729"])
        clm_line[f.CLM_RNDRG_PRVDR_PIN_NUM] = random.choice(["29364819", "19238747"])
        clm_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm_line[f.CLM_RNDRG_PRVDR_NPI_NUM] = clm_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM]
        if random.choice([0, 10]) == 7:
            clm_line[f.CLM_LINE_ANSTHSA_UNIT_CNT] = random.uniform(0, 10)
        if random.choice([0, 15]) == 7:
            clm_line[f.CLM_LINE_RX_NUM] = random.choice(["1234", "423482347"])

    if clm_type_cd == 81 or clm_type_cd == 82:
        clm_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        clm_line[f.CLM_RNDRNG_PRVDR_NPI_NUM] = clm_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM]

    clm_line[f.CLM_LINE_HCPCS_CD] = random.choice(get_hcpcs_proc_codes())
    num_mods = random.randint(0, 5)
    if num_mods:
        clm_line[f.HCPCS_1_MDFR_CD] = random.choice(HCPCS_MODS)
    if num_mods > 1:
        clm_line[f.HCPCS_2_MDFR_CD] = random.choice(HCPCS_MODS)
    if num_mods > 2:
        clm_line[f.HCPCS_3_MDFR_CD] = random.choice(HCPCS_MODS)
    if num_mods > 3:
        clm_line[f.HCPCS_4_MDFR_CD] = random.choice(HCPCS_MODS)
    if num_mods > 4:
        clm_line[f.HCPCS_5_MDFR_CD] = random.choice(HCPCS_MODS)
    if random.choice([0, 1]):
        clm_line[f.CLM_LINE_NDC_CD] = random.choice(AVAILABLE_NDC)
        clm_line[f.CLM_LINE_NDC_QTY] = round(random.uniform(1, 1000), 2)
        clm_line[f.CLM_LINE_NDC_QTY_QLFYR_CD] = "ML"
    clm_line[f.CLM_LINE_SRVC_UNIT_QTY] = round(random.uniform(0, 5), 2)
    if clm_type_cd in INSTITUTIONAL_CLAIM_TYPES:
        clm_line[f.CLM_LINE_REV_CTR_CD] = random.choice(gen_utils.code_systems[f.CLM_REV_CNTR_CD])
    clm_line[f.CLM_LINE_BENE_PMT_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_BENE_PD_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_ALOWD_CHRG_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_SBMT_CHRG_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_CVRD_PD_AMT] = round(random.uniform(0, 5), 2)
    clm_line[f.CLM_LINE_BLOOD_DDCTBL_AMT] = round(random.uniform(0, 15), 2)
    clm_line[f.CLM_LINE_MDCR_DDCTBL_AMT] = round(random.uniform(0, 5), 2)

    clm_line[f.CLM_LINE_PRVDR_PMT_AMT] = round(random.uniform(0, 1500), 2)
    clm_line[f.CLM_LINE_NCVRD_CHRG_AMT] = round(random.uniform(0, 1500), 2)

    clm_line[f.CLM_LINE_FINL_ACTN_IND] = random.choice(["Y", "N"])
    clm_line[f.CLM_LINE_LTST_CLM_IND] = random.choice(["Y", "N"])
    if clm_type_cd in (20, 30, 40, 50, 60, 61, 62, 63, 64, 71, 72, 81, 82):
        clm_line[f.CLM_LINE_OTAF_AMT] = round(random.uniform(0, 1000), 2)

    add_meta_timestamps(clm_line, clm)

    clm_line[f.CLM_UNIQ_ID] = clm[f.CLM_UNIQ_ID]
    clm_line[f.CLM_LINE_NUM] = clm_line_num

    return clm_line


def gen_clm_line_instnl(
    gen_utils: GeneratorUtil,
    clm: RowAdapter,
    clm_line_num: int,
    init_clm_line_instnl: RowAdapter | None = None,
):
    clm_type_cd = int(clm[f.CLM_TYPE_CD])
    clm_line_instnl = init_clm_line_instnl or RowAdapter({})
    clm_line_instnl[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line_instnl[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line_instnl[f.CLM_TYPE_CD] = clm_type_cd
    clm_line_instnl[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]

    clm_line_instnl[f.CLM_LINE_INSTNL_ADJSTD_AMT] = round(random.uniform(0, 1500), 2)
    clm_line_instnl[f.CLM_LINE_INSTNL_RDCD_AMT] = round(random.uniform(0, 1500), 2)
    clm_line_instnl[f.CLM_DDCTBL_COINSRNC_CD] = random.choice(
        gen_utils.code_systems[f.CLM_DDCTBL_COINSRNC_CD]
    )
    clm_line_instnl[f.CLM_LINE_INSTNL_RATE_AMT] = round(random.uniform(0, 15), 2)
    clm_line_instnl[f.CLM_LINE_INSTNL_MSP1_PD_AMT] = round(random.uniform(0, 15), 2)
    clm_line_instnl[f.CLM_LINE_INSTNL_MSP2_PD_AMT] = round(random.uniform(0, 2), 2)
    clm_line_instnl[f.CLM_LINE_INSTNL_REV_CTR_DT] = clm[f.CLM_FROM_DT]

    # In contrast to v2 DD this appears to populated in many.
    clm_line_instnl[f.CLM_REV_DSCNT_IND_CD] = random.choice(
        gen_utils.code_systems[f.CLM_REV_DSCNT_IND_CD]
    )
    clm_line_instnl[f.CLM_OTAF_ONE_IND_CD] = random.choice(
        gen_utils.code_systems[f.CLM_OTAF_IND_CD]
    )
    clm_line_instnl[f.CLM_REV_PACKG_IND_CD] = random.choice(
        gen_utils.code_systems[f.CLM_REV_PACKG_IND_CD]
    )
    clm_line_instnl[f.CLM_REV_PMT_MTHD_CD] = random.choice(
        gen_utils.code_systems[f.CLM_REV_PMT_MTHD_CD]
    )
    clm_line_instnl[f.CLM_REV_CNTR_STUS_CD] = random.choice(
        gen_utils.code_systems[f.CLM_REV_CNTR_STUS_CD]
    )
    clm_line_instnl[f.CLM_ANSI_SGNTR_SK] = random.choice(
        [
            "-8585",
            "-1",
            "-4365",
            "-1508",
            "-5555",
            "-9204",
            "-6857",
            "-5816",
            "-11978",
        ]
    )
    clm_line_instnl[f.CLM_LINE_ADD_ON_PYMT_AMT] = round(random.uniform(0, 10000), 2)
    clm_line_instnl[f.CLM_LINE_NON_EHR_RDCTN_AMT] = round(random.uniform(0, 500), 2)
    clm_line_instnl[f.CLM_REV_CNTR_TDAPA_AMT] = round(random.uniform(0, 10000), 2)

    clm_line_instnl[f.CLM_LINE_NUM] = clm_line_num

    add_meta_timestamps(clm_line_instnl, clm)

    return clm_line_instnl


def gen_clm_line_prfnl(
    gen_utils: GeneratorUtil,
    clm: RowAdapter,
    clm_line_num: int,
    init_clm_line_prfnl: RowAdapter | None = None,
):
    clm_type_cd = int(clm[f.CLM_TYPE_CD])
    clm_line_prfnl = init_clm_line_prfnl or RowAdapter({})
    clm_line_prfnl[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line_prfnl[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line_prfnl[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_line_prfnl[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]

    clm_line_prfnl[f.CLM_BENE_PRMRY_PYR_PD_AMT] = round(random.uniform(0, 10000), 2)
    clm_line_prfnl[f.CLM_SRVC_DDCTBL_SW] = random.choice(
        gen_utils.code_systems[f.CLM_SRVC_DDCTBL_SW]
    )
    clm_line_prfnl[f.CLM_PRCSG_IND_CD] = random.choice(gen_utils.code_systems[f.CLM_PRCSG_IND_CD])
    clm_line_prfnl[f.CLM_PMT_80_100_CD] = random.choice(gen_utils.code_systems[f.CLM_PMT_80_100_CD])

    clm_line_prfnl[f.CLM_MTUS_IND_CD] = random.choice(gen_utils.code_systems[f.CLM_MTUS_IND_CD])
    clm_line_prfnl[f.CLM_LINE_PRFNL_MTUS_CNT] = random.randint(0, 10)
    # claim_line_prfnl[fc.CLM_PRCNG_LCLTY_CD] =
    # random.choice(generator.code_systems[fc.CLM_PRCNG_LCLTY_CD])
    # not yet available from the IDR
    clm_line_prfnl[f.CLM_PHYSN_ASTNT_CD] = random.choice(
        gen_utils.code_systems[f.CLM_PHYSN_ASTNT_CD]
    )

    clm_line_prfnl[f.CLM_LINE_CARR_CLNCL_CHRG_AMT] = round(random.uniform(0, 10000), 2)
    clm_line_prfnl[f.CLM_LINE_CARR_PSYCH_OT_LMT_AMT] = round(random.uniform(0, 10000), 2)
    clm_line_prfnl[f.CLM_LINE_PRFNL_INTRST_AMT] = round(random.uniform(0, 10000), 2)
    clm_line_prfnl[f.CLM_MDCR_PRMRY_PYR_ALOWD_AMT] = round(random.uniform(0, 10000), 2)

    if random.randint(0, 10) == 6:
        clm_line_prfnl[f.CLM_LINE_HCT_HGB_TYPE_CD] = random.choice(["R1", "R2"])
        clm_line_prfnl[f.CLM_LINE_CARR_CLNCL_LAB_NUM] = random.choice(
            [
                "11D1111111",
                "22D2222222",
            ]
        )

    if clm_type_cd == 81 or clm_type_cd == 82:
        clm_line_prfnl[f.CLM_LINE_DMERC_SCRN_SVGS_AMT] = round(random.uniform(0, 10000), 2)
        clm_line_prfnl[f.CLM_SUPLR_TYPE_CD] = random.choice(
            gen_utils.code_systems[f.CLM_SUPLR_TYPE_CD]
        )
        clm_line_prfnl[f.CLM_LINE_PRFNL_DME_PRICE_AMT] = round(random.uniform(0, 10000), 2)

    clm_line_prfnl[f.CLM_LINE_NUM] = clm_line_num

    add_meta_timestamps(clm_line_prfnl, clm)

    return clm_line_prfnl
