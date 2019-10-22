/*
 * Manually specify the Diesel schema for the views we want to support. Allows us to workaround
 * Diesel's column count limitations.
 *
 * Reference: <https://deterministic.space/diesel-view-table-trick.html>.
 */

table! {
    claims_partd (pde_id) {
        pde_id -> Varchar,
        adjstmt_dltn_cd -> Nullable<Bpchar>,
        bene_id -> Varchar,
        brnd_gnrc_cd -> Nullable<Bpchar>,
        ctstrphc_cvrg_cd -> Nullable<Bpchar>,
        clm_grp_id -> Numeric,
        cmpnd_cd -> Int4,
        days_suply_num -> Numeric,
        daw_prod_slctn_cd -> Bpchar,
        dspnsng_stus_cd -> Nullable<Bpchar>,
        drug_cvrg_stus_cd -> Bpchar,
        fill_num -> Numeric,
        rptd_gap_dscnt_num -> Numeric,
        gdc_abv_oopt_amt -> Numeric,
        gdc_blw_oopt_amt -> Numeric,
        lics_amt -> Numeric,
        prod_srvc_id -> Varchar,
        nstd_frmt_cd -> Nullable<Bpchar>,
        othr_troop_amt -> Numeric,
        cvrd_d_plan_pd_amt -> Numeric,
        ncvrd_plan_pd_amt -> Numeric,
        plro_amt -> Numeric,
        ptnt_pay_amt -> Numeric,
        ptnt_rsdnc_cd -> Varchar,
        pd_dt -> Nullable<Date>,
        phrmcy_srvc_type_cd -> Varchar,
        plan_pbp_rec_num -> Varchar,
        plan_cntrct_rec_id -> Varchar,
        prscrbr_id -> Varchar,
        prscrbr_id_qlfyr_cd -> Varchar,
        srvc_dt -> Date,
        rx_orgn_cd -> Nullable<Bpchar>,
        rx_srvc_rfrnc_num -> Numeric,
        prcng_excptn_cd -> Nullable<Bpchar>,
        qty_dspnsd_num -> Numeric,
        srvc_prvdr_id -> Varchar,
        srvc_prvdr_id_qlfyr_cd -> Varchar,
        submsn_clr_cd -> Nullable<Varchar>,
        tot_rx_cst_amt -> Numeric,
        final_action -> Bpchar,
    }
}
