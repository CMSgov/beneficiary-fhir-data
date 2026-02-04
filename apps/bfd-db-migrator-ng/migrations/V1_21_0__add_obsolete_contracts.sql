DROP INDEX contract_pbp_number_cntrct_pbp_num_cntrct_num_idx;

ALTER TABLE idr.contract_pbp_number
ADD COLUMN cntrct_pbp_sk_obslt_dt TIMESTAMPTZ;

ALTER TABLE idr.contract_pbp_number
ADD COLUMN bfd_contract_version_rank INT;

ALTER TABLE idr.contract_pbp_number
ADD UNIQUE (cntrct_num, cntrct_pbp_num, bfd_contract_version_rank);

-- Changing to join on cntrct_pbp_sk
DROP VIEW idr.beneficiary_part_c_and_d_enrollment;
CREATE VIEW idr.beneficiary_part_c_and_d_enrollment AS
SELECT
    e.bene_sk,
    e.bene_enrlmt_pgm_type_cd,
    e.bene_enrlmt_bgn_dt,
    e.bene_enrlmt_end_dt,
    e.bene_cntrct_num,
    e.bene_pbp_num,
    e.cntrct_pbp_sk,
    e.bene_cvrg_type_cd,
    e.bene_enrlmt_emplr_sbsdy_sw,
    COALESCE(rx.bene_enrlmt_pdp_rx_info_bgn_dt, DATE '9999-12-31') AS bene_enrlmt_pdp_rx_info_bgn_dt,
    rx.bene_pdp_enrlmt_mmbr_id_num,
    rx.bene_pdp_enrlmt_grp_num,
    rx.bene_pdp_enrlmt_prcsr_num,
    rx.bene_pdp_enrlmt_bank_id_num
FROM idr.beneficiary_ma_part_d_enrollment e
LEFT JOIN idr.beneficiary_ma_part_d_enrollment_rx rx
    ON e.bene_sk = rx.bene_sk
    AND e.bene_enrlmt_bgn_dt = rx.bene_enrlmt_bgn_dt
    AND e.cntrct_pbp_sk = rx.cntrct_pbp_sk
    AND e.bene_enrlmt_pgm_type_cd in ('2', '3');
