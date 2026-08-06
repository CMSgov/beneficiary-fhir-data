CREATE UNIQUE INDEX new_pk_idx ON idr.beneficiary_ma_part_d_enrollment (
    bene_sk,
    bene_enrlmt_bgn_dt,
    bene_enrlmt_pgm_type_cd,
    idr_trans_obslt_ts);
ALTER TABLE idr.beneficiary_ma_part_d_enrollment DROP CONSTRAINT beneficiary_ma_part_d_enrollment_pkey;
ALTER INDEX idr.new_pk_idx RENAME TO beneficiary_ma_part_d_enrollment_pkey;
ALTER TABLE idr.beneficiary_ma_part_d_enrollment
ADD PRIMARY KEY USING INDEX beneficiary_ma_part_d_enrollment_pkey;

CREATE UNIQUE INDEX new_pk_idx ON idr.beneficiary_ma_part_d_enrollment_rx (bene_sk,bene_cntrct_num,bene_pbp_num,bene_enrlmt_bgn_dt,bene_enrlmt_pdp_rx_info_bgn_dt,idr_trans_obslt_ts);
ALTER TABLE idr.beneficiary_ma_part_d_enrollment_rx DROP CONSTRAINT beneficiary_ma_part_d_enrollment_rx_pkey;
ALTER INDEX idr.new_pk_idx RENAME TO beneficiary_ma_part_d_enrollment_rx_pkey;
ALTER TABLE idr.beneficiary_ma_part_d_enrollment_rx
ADD PRIMARY KEY USING INDEX beneficiary_ma_part_d_enrollment_rx_pkey;
