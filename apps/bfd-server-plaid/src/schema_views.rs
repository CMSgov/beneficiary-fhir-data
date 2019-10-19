/*
 * Manually specify the Diesel schema for the views we want to support. Allows us to workaround
 * Diesel's column count limitations.
 *
 * Reference: <https://deterministic.space/diesel-view-table-trick.html>.
 */

table! {
    claims_partd (PDE_ID) {
        PDE_ID -> Varchar,
        ADJSTMT_DLTN_CD -> Nullable<Bpchar>,
        BENE_ID -> Varchar,
        BRND_GNRC_CD -> Nullable<Bpchar>,
        CTSTRPHC_CVRG_CD -> Nullable<Bpchar>,
        CLM_GRP_ID -> Numeric,
        CMPND_CD -> Int4,
        DAYS_SUPLY_NUM -> Numeric,
        DAW_PROD_SLCTN_CD -> Bpchar,
        DSPNSNG_STUS_CD -> Nullable<Bpchar>,
        DRUG_CVRG_STUS_CD -> Bpchar,
        FILL_NUM -> Numeric,
        RPTD_GAP_DSCNT_NUM -> Numeric,
        GDC_ABV_OOPT_AMT -> Numeric,
        GDC_BLW_OOPT_AMT -> Numeric,
        LICS_AMT -> Numeric,
        PROD_SRVC_ID -> Varchar,
        NSTD_FRMT_CD -> Nullable<Bpchar>,
        OTHR_TROOP_AMT -> Numeric,
        CVRD_D_PLAN_PD_AMT -> Numeric,
        NCVRD_PLAN_PD_AMT -> Numeric,
        PLRO_AMT -> Numeric,
        PTNT_PAY_AMT -> Numeric,
        PTNT_RSDNC_CD -> Varchar,
        PD_DT -> Nullable<Date>,
        PHRMCY_SRVC_TYPE_CD -> Varchar,
        PLAN_PBP_REC_NUM -> Varchar,
        PLAN_CNTRCT_REC_ID -> Varchar,
        PRSCRBR_ID -> Varchar,
        PRSCRBR_ID_QLFYR_CD -> Varchar,
        SRVC_DT -> Date,
        RX_ORGN_CD -> Nullable<Bpchar>,
        RX_SRVC_RFRNC_NUM -> Numeric,
        PRCNG_EXCPTN_CD -> Nullable<Bpchar>,
        QTY_DSPNSD_NUM -> Numeric,
        SRVC_PRVDR_ID -> Varchar,
        SRVC_PRVDR_ID_QLFYR_CD -> Varchar,
        SUBMSN_CLR_CD -> Nullable<Varchar>,
        TOT_RX_CST_AMT -> Numeric,
        FINAL_ACTION -> Bpchar,
    }
}
