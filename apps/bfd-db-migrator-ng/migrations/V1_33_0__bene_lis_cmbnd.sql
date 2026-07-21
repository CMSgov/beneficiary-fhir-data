CREATE TABLE idr.beneficiary_low_income_subsidy_cmbnd(
   bene_sk BIGINT NOT NULL,
   bene_cmbnd_deemd_efctv_dt DATE NOT NULL,
   bene_cmbnd_deemd_trmntn_dt DATE NOT NULL,
   bene_cmbnd_deemd_copmt_lvl_id VARCHAR(1) NOT NULL,
   bene_cmbnd_deemd_ind VARCHAR(1) NOT NULL,
   bene_cmbnd_deemd_prm_pct VARCHAR(3),
   idr_ltst_trans_flg VARCHAR(1) NOT NULL,
   idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
   idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
   idr_insrt_ts TIMESTAMPTZ NOT NULL,
   idr_updt_ts TIMESTAMPTZ NOT NULL,
   bfd_created_ts TIMESTAMPTZ NOT NULL,
   bfd_updated_ts TIMESTAMPTZ NOT NULL,
   PRIMARY KEY(bene_sk, bene_cmbnd_deemd_efctv_dt, idr_trans_obslt_ts)
);
