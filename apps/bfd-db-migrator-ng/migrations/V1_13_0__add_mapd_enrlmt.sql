ALTER TABLE idr.contract_pbp_number
ADD COLUMN cntrct_pbp_sgmt_num VARCHAR(3) NOT NULL;

CREATE TABLE idr.beneficiary_ma_part_d_enrollment(
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_pbp_num VARCHAR(3) NOT NULL,
    bene_enrlmt_bgn_dt DATE NOT NULL,
    bene_enrlmt_end_dt DATE,
    bene_cntrct_num VARCHAR(5) NOT NULL,
    bene_cvrg_type_cd VARCHAR(2) NOT NULL,
    bene_enrlmt_pgm_type_cd VARCHAR(4) NOT NULL,
    bene_enrlmt_emplr_sbsdy_sw VARCHAR(1),
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_enrlmt_bgn_dt, bene_enrlmt_pgm_type_cd)
);

CREATE TABLE idr.beneficiary_ma_part_d_enrollment_rx(
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5) NOT NULL,
    bene_pbp_num VARCHAR(3) NOT NULL,
    bene_enrlmt_bgn_dt DATE NOT NULL,
    bene_pdp_enrlmt_mmbr_id_num VARCHAR(20) NOT NULL,
    bene_pdp_enrlmt_grp_num VARCHAR(15) NOT NULL,
    bene_pdp_enrlmt_prcsr_num VARCHAR(10) NOT NULL,
    bene_pdp_enrlmt_bank_id_num VARCHAR(6),
    bene_enrlmt_pdp_rx_info_bgn_dt DATE NOT NULL,
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_cntrct_num, bene_pbp_num, bene_enrlmt_bgn_dt, bene_enrlmt_pdp_rx_info_bgn_dt)
);

CREATE TABLE idr.beneficiary_low_income_subsidy(
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_lis_copmt_lvl_cd VARCHAR(1) NOT NULL,
    bene_lis_ptd_prm_pct VARCHAR(3),
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt)
);

DROP TABLE idr.beneficiary_election_period_usage;

CREATE VIEW idr.beneficiary_part_c_and_d_enrollment AS
SELECT
    e.bene_sk,
    e.bene_enrlmt_pgm_type_cd,
    e.bene_enrlmt_bgn_dt,
    e.bene_enrlmt_end_dt,
    e.bene_cntrct_num,
    e.bene_pbp_num,
    e.bene_cvrg_type_cd,
    e.bene_enrlmt_emplr_sbsdy_sw,
    e.bfd_updated_ts AS enr_bfd_updated_ts,
    COALESCE(rx.bene_enrlmt_pdp_rx_info_bgn_dt, DATE '9999-12-31') AS bene_enrlmt_pdp_rx_info_bgn_dt,
    rx.bene_pdp_enrlmt_mmbr_id_num,
    rx.bene_pdp_enrlmt_grp_num,
    rx.bene_pdp_enrlmt_prcsr_num,
    rx.bene_pdp_enrlmt_bank_id_num,
    rx.bfd_updated_ts AS enr_rx_bfd_updated_ts
FROM idr.beneficiary_ma_part_d_enrollment e
LEFT JOIN idr.beneficiary_ma_part_d_enrollment_rx rx
    ON e.bene_sk = rx.bene_sk
    AND e.bene_enrlmt_bgn_dt = rx.bene_enrlmt_bgn_dt
    AND e.bene_cntrct_num = rx.bene_cntrct_num
    AND e.bene_pbp_num = rx.bene_pbp_num
    AND e.bene_enrlmt_pgm_type_cd in ('2', '3');
