ALTER TABLE idr.claim_item_institutional_ss ALTER COLUMN clm_rlt_cond_cd TYPE VARCHAR(60);
ALTER TABLE idr.claim_item_institutional_nch ALTER COLUMN clm_rlt_cond_cd TYPE VARCHAR(60);

ALTER TABLE idr.claim_item_institutional_nch
-- columns derived from v2_mdcr_clm_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_ncvrd_from_dt date,
ADD COLUMN bfd_clm_ncvrd_thru_dt date,
ADD COLUMN bfd_clm_qlfy_stay_from_dt date,
ADD COLUMN bfd_clm_qlfy_stay_thru_dt date,
-- columns derived from v2_clm_rlt_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_mdcr_exhstd_dt date,
ADD COLUMN bfd_clm_actv_care_thru_dt date;

ALTER TABLE idr.claim_item_institutional_ss
-- columns derived from v2_mdcr_clm_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_ncvrd_from_dt date,
ADD COLUMN bfd_clm_ncvrd_thru_dt date,
ADD COLUMN bfd_clm_qlfy_stay_from_dt date,
ADD COLUMN bfd_clm_qlfy_stay_thru_dt date,
-- columns derived from v2_clm_rlt_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_mdcr_exhstd_dt date,
ADD COLUMN bfd_clm_actv_care_thru_dt date,
-- columns derived from v2_mdcr_clm_line_fiss_bnft_svg
ADD COLUMN clm_bnft_svg_ansi_grp_1_cd character varying(2),
ADD COLUMN clm_bnft_svg_ansi_grp_2_cd character varying(2),
ADD COLUMN clm_bnft_svg_ansi_grp_3_cd character varying(2),
ADD COLUMN clm_bnft_svg_ansi_grp_4_cd character varying(2),
ADD COLUMN clm_bnft_svg_ansi_rsn_1_cd character varying(3),
ADD COLUMN clm_bnft_svg_ansi_rsn_2_cd character varying(3),
ADD COLUMN clm_bnft_svg_ansi_rsn_3_cd character varying(3),
ADD COLUMN clm_bnft_svg_ansi_rsn_4_cd character varying(3),
DROP COLUMN clm_bnft_svg_ansi_grp_cd,
DROP COLUMN clm_bnft_svg_ansi_rsn_cd;

ALTER TABLE idr.claim_item_professional_nch
-- columns derived from v2_mdcr_clm_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_ncvrd_from_dt date,
ADD COLUMN bfd_clm_ncvrd_thru_dt date,
ADD COLUMN bfd_clm_qlfy_stay_from_dt date,
ADD COLUMN bfd_clm_qlfy_stay_thru_dt date,
-- columns derived from v2_clm_rlt_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_mdcr_exhstd_dt date,
ADD COLUMN bfd_clm_actv_care_thru_dt date;

ALTER TABLE idr.claim_item_professional_ss
-- columns derived from v2_mdcr_clm_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_ncvrd_from_dt date,
ADD COLUMN bfd_clm_ncvrd_thru_dt date,
ADD COLUMN bfd_clm_qlfy_stay_from_dt date,
ADD COLUMN bfd_clm_qlfy_stay_thru_dt date,
-- columns derived from v2_clm_rlt_ocrnc_sgntr_mbr
ADD COLUMN bfd_clm_mdcr_exhstd_dt date,
ADD COLUMN bfd_clm_actv_care_thru_dt date;
