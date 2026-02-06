import random
from collections.abc import Callable

import field_constants as f
from claims_static import (
    FISS_CLM_TYPE_CDS,
    MCS_CLM_TYPE_CDS,
    TARGET_RLT_COND_CODES,
    TARGET_SEQUENCE_NUMBERS,
    VMS_CDS,
)
from claims_util import add_meta_timestamps, get_ric_cd_for_clm_type_cd
from generator_util import RowAdapter, gen_basic_id, gen_numeric_id


def _prepare_pac_row(
    init_row: RowAdapter,
    is_pac_predicate: Callable[[], bool],
    exclude_fields_always: set[str],
    exclude_fields_adj: set[str],
):
    exclude_fields = (
        exclude_fields_always
        if is_pac_predicate()
        else exclude_fields_always.union(exclude_fields_adj)
    )
    return RowAdapter({k: v for k, v in init_row.kv.items() if k not in exclude_fields})


def gen_pac_clm(init_clm: RowAdapter):
    # This may look strange, and it should because it's a bit of a hack. The original synthetic
    # claims generation for pac CLMs (and other PAC-related tables) copied an entire bene_sk's set
    # of claim tables related to a single claim and selectively changed columns in each table. We
    # must do the same but in a different way, _and_ we need to support regeneration/update of
    # existing _pac CLMs_ as well as generation of new pac CLMs from adjudicated CLMs. To do both we
    # copy the init_clm and conditionally exclude fields that need changed depending on the type of
    # CLM. That way the generation logic is kept to this function that can be called for each type
    # of CLM, and any new pac-related CLM fields can be added here
    init_clm_type_cd = int(init_clm[f.CLM_TYPE_CD])
    # Always exclude these fields from init_clm. We do this because RowAdapter ignores changes to
    # existing fields, so we could never unset these fields.
    exclude_fields_always = {f.CLM_BLOOD_PT_FRNSH_QTY, f.CLM_NCH_PRMRY_PYR_CD}
    # Exclude these fields if init_clm is adjudicated and we're generating a pac clm from it. We do
    # this so that these fields for an existing adjudicated CLM can be set, or these fields don't
    # exist for pac claims. The combined set of always excluded fields + adjudicated below are used
    # for adjudicated CLMs
    exclude_fields_adj = {
        f.CLM_TYPE_CD,
        f.CLM_UNIQ_ID,
        f.CLM_DT_SGNTR_SK,
        f.GEO_BENE_SK,
        f.CLM_SRC_ID,
        f.META_SRC_SK,
        f.CLM_FINL_ACTN_IND,
        f.CLM_RIC_CD,
        f.CLM_RLT_COND_SGNTR_SK,
    }
    clm = _prepare_pac_row(
        init_row=init_clm,
        is_pac_predicate=lambda: init_clm_type_cd > 1010,
        exclude_fields_always=exclude_fields_always,
        exclude_fields_adj=exclude_fields_adj,
    )

    clm[f.CLM_UNIQ_ID] = gen_basic_id(field=f.CLM_UNIQ_ID, length=13)

    if init_clm_type_cd in (60, 61, 62, 63, 64):
        clm[f.CLM_TYPE_CD] = random.choices(
            [1011, 2011, 1041, 2041], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if init_clm_type_cd == 40:
        clm[f.CLM_TYPE_CD] = random.choices(
            [1013, 2013, 1071, 2071], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if init_clm_type_cd == 10:
        clm[f.CLM_TYPE_CD] = random.choices(
            [1032, 2032, 1033, 2033], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if init_clm_type_cd == 20:
        clm[f.CLM_TYPE_CD] = random.choice([1021, 2021])

    if init_clm_type_cd == 30:
        clm[f.CLM_TYPE_CD] = random.choice([1018, 2018])

    if init_clm_type_cd == 50:
        clm[f.CLM_TYPE_CD] = random.choices(
            [1081, 2081, 1082, 2082], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if init_clm_type_cd in (71, 72):
        clm[f.CLM_TYPE_CD] = random.choice([1700, 2700])

    if init_clm_type_cd in (81, 82):
        clm[f.CLM_TYPE_CD] = random.choice([1800, 2800])

    pac_clm_type_cd = int(clm[f.CLM_TYPE_CD])
    if pac_clm_type_cd < 2000:
        clm[f.CLM_FINL_ACTN_IND] = "N"
    else:
        clm[f.CLM_FINL_ACTN_IND] = "Y"

    clm[f.CLM_DT_SGNTR_SK] = gen_basic_id(field=f.CLM_DT_SGNTR_SK, length=12)
    clm[f.GEO_BENE_SK] = gen_basic_id(field=f.GEO_BENE_SK, length=5)

    if pac_clm_type_cd in FISS_CLM_TYPE_CDS:
        clm[f.CLM_SRC_ID] = 21000  # FISS
        clm[f.META_SRC_SK] = 1003  # FISS
    elif pac_clm_type_cd in MCS_CLM_TYPE_CDS:
        clm[f.CLM_SRC_ID] = 22000  # MCS
        clm[f.META_SRC_SK] = 1001  # MCS
    elif pac_clm_type_cd in VMS_CDS:
        clm[f.CLM_SRC_ID] = 23000  # VMS
        clm[f.META_SRC_SK] = 1002  # VMS

    if pac_clm_type_cd in FISS_CLM_TYPE_CDS:
        clm[f.CLM_RIC_CD] = get_ric_cd_for_clm_type_cd(pac_clm_type_cd)

    clm[f.CLM_RLT_COND_SGNTR_SK] = gen_numeric_id(field=f.CLM_RLT_COND_SGNTR_SK, start=-2)

    return clm


def gen_pac_clm_dt_sgntr(clm: RowAdapter, init_clm_dt_sgntr: RowAdapter):
    clm_dt_sgntr = _prepare_pac_row(
        init_row=init_clm_dt_sgntr,
        # If these match, the initial clm_dt_sgntr is already "pac" in that it's associated with the
        # pac CLM (because this function will be called after the pac CLM is created)
        is_pac_predicate=lambda: clm[f.CLM_DT_SGNTR_SK] == init_clm_dt_sgntr[f.CLM_DT_SGNTR_SK],
        exclude_fields_always={
            f.CLM_MDCR_EXHSTD_DT,
            f.CLM_NCVRD_FROM_DT,
            f.CLM_NCVRD_THRU_DT,
            f.CLM_NCH_WKLY_PROC_DT,
            f.CLM_ACTV_CARE_THRU_DT,
        },
        exclude_fields_adj={f.CLM_DT_SGNTR_SK},
    )
    clm_dt_sgntr[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    return clm_dt_sgntr


def gen_clm_fiss(clm: RowAdapter, init_clm_fiss: RowAdapter | None = None):
    # CLM_FISS and CLM_LCTN_HSTRY are unique in that they don't exist for adjudicated claims, so we
    # don't need to "prepare" a pac RowAdapter, we just create an empty one if none is provided
    clm_fiss = init_clm_fiss or RowAdapter({})
    clm_fiss[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_fiss[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_fiss[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_fiss[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    add_meta_timestamps(clm_fiss, clm)

    return clm_fiss


def gen_clm_lctn_hstry(clm: RowAdapter, init_clm_lctn_hstry: RowAdapter | None = None):
    clm_lctn_hstry = init_clm_lctn_hstry or RowAdapter({})
    clm_lctn_hstry[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_lctn_hstry[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_lctn_hstry[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_lctn_hstry[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_lctn_hstry[f.CLM_LCTN_CD_SQNC_NUM] = "1"
    clm_lctn_hstry[f.CLM_AUDT_TRL_STUS_CD] = random.choice(
        [
            "A",
            "F",
            "I",
            "S",
            "M",
            "P",
            "R",
            "D",
            "T",
            "U",
            "1",
            "2",
            "4",
            "8",
        ]
    )

    add_meta_timestamps(clm_lctn_hstry, clm)

    return clm_lctn_hstry


def gen_pac_clm_line(clm: RowAdapter, init_clm_line: RowAdapter):
    clm_line = _prepare_pac_row(
        init_row=init_clm_line,
        is_pac_predicate=lambda: int(init_clm_line[f.CLM_TYPE_CD]) > 1010,
        exclude_fields_always={
            f.CLM_LINE_PMD_UNIQ_TRKNG_NUM,
            f.CLM_LINE_ANSTHSA_UNIT_CNT,
            f.CLM_RNDRG_PRVDR_PRTCPTG_CD,
        },
        exclude_fields_adj={
            f.CLM_LINE_NUM,
            f.CLM_UNIQ_ID,
            f.GEO_BENE_SK,
            f.CLM_DT_SGNTR_SK,
            f.CLM_TYPE_CD,
        },
    )
    clm_line[f.CLM_LINE_NUM] = init_clm_line[f.CLM_LINE_NUM]
    clm_line[f.CLM_UNIQ_ID] = clm[f.CLM_UNIQ_ID]
    clm_line[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    return clm_line


def gen_pac_clm_line_dcmtn(
    clm: RowAdapter,
    clm_line_num: int,
    tracking_num: str,
    init_clm_line_dcmtn: RowAdapter | None = None,
):
    clm_line_dcmtn = init_clm_line_dcmtn or RowAdapter({})
    clm_line_dcmtn[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line_dcmtn[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line_dcmtn[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]
    clm_line_dcmtn[f.CLM_NUM_SK] = clm[f.CLM_NUM_SK]
    clm_line_dcmtn[f.CLM_LINE_PA_UNIQ_TRKNG_NUM] = tracking_num
    clm_line_dcmtn[f.CLM_LINE_NUM] = clm_line_num

    add_meta_timestamps(clm_line_dcmtn, clm)

    return clm_line_dcmtn


def gen_pac_clm_line_instnl(clm: RowAdapter, init_clm_line_instnl: RowAdapter):
    clm_line_instnl = _prepare_pac_row(
        init_row=init_clm_line_instnl,
        is_pac_predicate=lambda: init_clm_line_instnl[f.GEO_BENE_SK] == clm[f.GEO_BENE_SK],
        exclude_fields_always={f.CLM_ANSI_SGNTR_SK, f.CLM_OTAF_ONE_IND_CD, f.CLM_REV_CNTR_STUS_CD},
        exclude_fields_adj={f.GEO_BENE_SK, f.CLM_DT_SGNTR_SK, f.CLM_TYPE_CD},
    )
    clm_line_instnl[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line_instnl[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line_instnl[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    return clm_line_instnl


def gen_pac_clm_line_prfnl(clm: RowAdapter, init_clm_line_prfnl: RowAdapter):
    clm_line_prfnl = _prepare_pac_row(
        init_row=init_clm_line_prfnl,
        is_pac_predicate=lambda: init_clm_line_prfnl[f.GEO_BENE_SK] == clm[f.GEO_BENE_SK],
        exclude_fields_always={
            f.CLM_MTUS_IND_CD,
            f.CLM_PRCNG_LCLTY_CD,
            f.CLM_PHYSN_ASTNT_CD,
            f.CLM_LINE_PRFNL_MTUS_CNT,
            f.CLM_LINE_CARR_HPSA_SCRCTY_CD,
            f.CLM_PRMRY_PYR_CD,
            f.CLM_FED_TYPE_SRVC_CD,
            f.CLM_PMT_80_100_CD,
            f.CLM_PRCSG_IND_CD,
            f.CLM_PRVDR_SPCLTY_CD,
        },
        exclude_fields_adj={f.GEO_BENE_SK, f.CLM_DT_SGNTR_SK, f.CLM_TYPE_CD},
    )
    clm_line_prfnl[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_line_prfnl[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_line_prfnl[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    return clm_line_prfnl


def gen_pac_clm_val(clm: RowAdapter, init_clm_val: RowAdapter):
    clm_val = _prepare_pac_row(
        init_row=init_clm_val,
        is_pac_predicate=lambda: init_clm_val[f.GEO_BENE_SK] == clm[f.GEO_BENE_SK],
        exclude_fields_always=set(),
        exclude_fields_adj={f.GEO_BENE_SK, f.CLM_DT_SGNTR_SK, f.CLM_TYPE_CD},
    )
    clm_val[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_val[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_val[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    return clm_val


def gen_pac_clm_instnl(clm: RowAdapter, init_clm_instnl: RowAdapter):
    clm_instnl = _prepare_pac_row(
        init_row=init_clm_instnl,
        is_pac_predicate=lambda: init_clm_instnl[f.GEO_BENE_SK] == clm[f.GEO_BENE_SK],
        exclude_fields_always={
            f.CLM_MDCR_IP_BENE_DDCTBL_AMT,
            f.CLM_MDCR_INSTNL_PRMRY_PYR_AMT,
            f.CLM_PPS_IND_CD,
            f.CLM_MDCR_HOSPC_PRD_CNT,
            f.CLM_INSTNL_DRG_OUTLIER_AMT,
            f.CLM_MDCR_HHA_TOT_VISIT_CNT,
            f.CLM_HHA_LUP_IND_CD,
            f.CLM_HHA_RFRL_CD,
            f.CLM_MDCR_INSTNL_BENE_PD_AMT,
        },
        exclude_fields_adj={f.GEO_BENE_SK, f.CLM_DT_SGNTR_SK, f.CLM_TYPE_CD},
    )
    clm_instnl[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_instnl[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_instnl[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    return clm_instnl


def gen_pac_clm_prod(clm: RowAdapter, init_clm_prod: RowAdapter):
    clm_prod = _prepare_pac_row(
        init_row=init_clm_prod,
        is_pac_predicate=lambda: init_clm_prod[f.GEO_BENE_SK] == clm[f.GEO_BENE_SK],
        exclude_fields_always=set(),
        exclude_fields_adj={f.GEO_BENE_SK, f.CLM_DT_SGNTR_SK, f.CLM_TYPE_CD},
    )
    clm_prod[f.GEO_BENE_SK] = clm[f.GEO_BENE_SK]
    clm_prod[f.CLM_DT_SGNTR_SK] = clm[f.CLM_DT_SGNTR_SK]
    clm_prod[f.CLM_TYPE_CD] = clm[f.CLM_TYPE_CD]

    return clm_prod


def gen_pac_clm_rlt_cond_sgntr_mbr(clm: RowAdapter, init_clm_rlt_cond_sgntr_mbr: RowAdapter):
    clm_rlt_cond_sgntr_mbr = _prepare_pac_row(
        init_row=init_clm_rlt_cond_sgntr_mbr,
        is_pac_predicate=lambda: init_clm_rlt_cond_sgntr_mbr[f.CLM_RLT_COND_SGNTR_SK]
        == clm[f.CLM_RLT_COND_SGNTR_SK],
        exclude_fields_always=set(),
        exclude_fields_adj={
            f.CLM_RLT_COND_SGNTR_SK,
            f.CLM_RLT_COND_SGNTR_SQNC_NUM,
            f.CLM_RLT_COND_CD,
            f.CLM_IDR_LD_DT,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        },
    )
    clm_rlt_cond_sgntr_mbr[f.CLM_RLT_COND_SGNTR_SK] = clm[f.CLM_RLT_COND_SGNTR_SK]
    clm_rlt_cond_sgntr_mbr[f.CLM_RLT_COND_SGNTR_SQNC_NUM] = random.choice(TARGET_SEQUENCE_NUMBERS)
    clm_rlt_cond_sgntr_mbr[f.CLM_RLT_COND_CD] = random.choice(TARGET_RLT_COND_CODES)

    add_meta_timestamps(clm_rlt_cond_sgntr_mbr, clm)

    return clm_rlt_cond_sgntr_mbr
