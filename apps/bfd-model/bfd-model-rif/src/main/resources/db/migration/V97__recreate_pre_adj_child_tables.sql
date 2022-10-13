-- Rebuild the schema to match new RDA specifications and truncate tables to end soft launch

drop table rda.fiss_audit_trails;
drop table rda.fiss_diagnosis_codes;
drop table rda.fiss_payers;
drop table rda.fiss_proc_codes;
drop table rda.mcs_adjustments;
drop table rda.mcs_audits;
drop table rda.mcs_details;
drop table rda.mcs_diagnosis_codes;
drop table rda.mcs_locations;
truncate table rda.fiss_claims;
truncate table rda.mcs_claims;
truncate table rda.message_errors;
truncate table rda.rda_api_progress;
-- Not truncating the metadata or MBI tables
alter table rda.claim_message_meta_data ${logic.alter-rename-column} received_date ${logic.rename-to} last_updated;
alter index rda.claim_message_meta_data_received_date_idx rename to claim_message_meta_data_last_updated_idx;

/*
 * fiss_audit_trails
 */
CREATE TABLE rda.fiss_audit_trails (
   dcn varchar(23) NOT NULL,
   rda_position smallint NOT NULL,
   badt_status varchar(1),
   badt_loc varchar(5),
   badt_oper_id varchar(9),
   badt_reas varchar(5),
   badt_curr_date date,
   CONSTRAINT fiss_audit_trails_key PRIMARY KEY (dcn, rda_position),
   CONSTRAINT fiss_audit_trails_parent FOREIGN KEY (dcn) REFERENCES rda.fiss_claims(dcn)
);

/*
 * fiss_diagnosis_codes
 */
CREATE TABLE rda.fiss_diagnosis_codes (
  dcn varchar(23) NOT NULL,
  rda_position smallint NOT NULL,
  diag_cd2 varchar(7) NOT NULL,
  diag_poa_ind varchar(1),
  bit_flags varchar(4),
  CONSTRAINT fiss_diagnosis_codes_key PRIMARY KEY (dcn, rda_position),
  CONSTRAINT fiss_diagnosis_codes_parent FOREIGN KEY (dcn) REFERENCES rda.fiss_claims(dcn)
);

/*
 * fiss_payers
 */
CREATE TABLE rda.fiss_payers (
     dcn varchar(23) NOT NULL,
     rda_position smallint NOT NULL,
     payer_type varchar(20),
     payers_id varchar(1),
     payers_name varchar(32),
     rel_ind varchar(1),
     assign_ind varchar(1),
     provider_number varchar(13),
     adj_dcn_icn varchar(23),
     prior_pmt decimal(11,2),
     est_amt_due decimal(11,2),
     bene_rel varchar(2),
     bene_last_name varchar(15),
     bene_first_name varchar(10),
     bene_mid_init varchar(1),
     bene_ssn_hic varchar(19),
     insured_rel varchar(2),
     insured_name varchar(25),
     insured_ssn_hic varchar(19),
     insured_group_name varchar(17),
     insured_group_nbr varchar(20),
     bene_dob date,
     bene_sex varchar(1),
     treat_auth_cd varchar(18),
     insured_sex varchar(1),
     insured_rel_x12 varchar(2),
     insured_dob date,
     insured_dob_text varchar(9),
     CONSTRAINT fiss_payers_key PRIMARY KEY (dcn, rda_position),
     CONSTRAINT fiss_payers_parent FOREIGN KEY (dcn) REFERENCES rda.fiss_claims(dcn)
);

/*
 * fiss_proc_codes
 */
CREATE TABLE rda.fiss_proc_codes (
     dcn varchar(23) NOT NULL,
     rda_position smallint NOT NULL,
     proc_code varchar(10) NOT NULL,
     proc_flag varchar(4),
     proc_date date,
     CONSTRAINT fiss_proc_codes_key PRIMARY KEY (dcn, rda_position),
     CONSTRAINT fiss_proc_codes_parent FOREIGN KEY (dcn) REFERENCES rda.fiss_claims(dcn)
);

/*
 * mcs_adjustments
 */
CREATE TABLE rda.mcs_adjustments (
     idr_clm_hd_icn varchar(15) NOT NULL,
     rda_position smallint NOT NULL,
     idr_adj_date date,
     idr_xref_icn varchar(15),
     idr_adj_clerk varchar(4),
     idr_init_ccn varchar(15),
     idr_adj_chk_wrt_dt date,
     idr_adj_b_eomb_amt decimal(7,2),
     idr_adj_p_eomb_amt decimal(7,2),
     CONSTRAINT mcs_adjustments_key PRIMARY KEY (idr_clm_hd_icn, rda_position),
     CONSTRAINT mcs_adjustments_parent FOREIGN KEY (idr_clm_hd_icn) REFERENCES rda.mcs_claims(idr_clm_hd_icn)
);

/*
 * mcs_audits
 */
CREATE TABLE rda.mcs_audits (
    idr_clm_hd_icn varchar(15) NOT NULL,
    rda_position smallint NOT NULL,
    idr_j_audit_num int,
    idr_j_audit_ind varchar(1),
    idr_j_audit_disp varchar(1),
    CONSTRAINT mcs_audits_key PRIMARY KEY (idr_clm_hd_icn, rda_position),
    CONSTRAINT mcs_audits_parent FOREIGN KEY (idr_clm_hd_icn) REFERENCES rda.mcs_claims(idr_clm_hd_icn)
);

/*
 * mcs_details
 */
CREATE TABLE rda.mcs_details (
     idr_clm_hd_icn varchar(15) NOT NULL,
     idr_dtl_number smallint NOT NULL,
     idr_dtl_status varchar(1),
     idr_dtl_from_date date,
     idr_dtl_to_date date,
     idr_proc_code varchar(5),
     idr_mod_one varchar(2),
     idr_mod_two varchar(2),
     idr_mod_three varchar(2),
     idr_mod_four varchar(2),
     idr_dtl_diag_icd_type varchar(1),
     idr_dtl_primary_diag_code varchar(7),
     idr_k_pos_lname_org varchar(60),
     idr_k_pos_fname varchar(35),
     idr_k_pos_mname varchar(25),
     idr_k_pos_addr1 varchar(55),
     idr_k_pos_addr2_1st varchar(30),
     idr_k_pos_addr2_2nd varchar(25),
     idr_k_pos_city varchar(30),
     idr_k_pos_state varchar(2),
     idr_k_pos_zip varchar(15),
     idr_tos varchar(1),
     idr_two_digit_pos varchar(2),
     idr_dtl_rend_type varchar(2),
     idr_dtl_rend_spec varchar(2),
     idr_dtl_rend_npi varchar(10),
     idr_dtl_rend_prov varchar(10),
     idr_k_dtl_fac_prov_npi varchar(10),
     idr_dtl_amb_pickup_addres1 varchar(25),
     idr_dtl_amb_pickup_addres2 varchar(20),
     idr_dtl_amb_pickup_city varchar(20),
     idr_dtl_amb_pickup_state varchar(2),
     idr_dtl_amb_pickup_zipcode varchar(9),
     idr_dtl_amb_dropoff_name varchar(24),
     idr_dtl_amb_dropoff_addr_l1 varchar(25),
     idr_dtl_amb_dropoff_addr_l2 varchar(20),
     idr_dtl_amb_dropoff_city varchar(20),
     idr_dtl_amb_dropoff_state varchar(2),
     idr_dtl_amb_dropoff_zipcode varchar(9),
     CONSTRAINT mcs_details_key PRIMARY KEY (idr_clm_hd_icn, idr_dtl_number),
     CONSTRAINT mcs_details_parent FOREIGN KEY (idr_clm_hd_icn) REFERENCES rda.mcs_claims(idr_clm_hd_icn)
);

/*
 * mcs_diagnosis_codes
 */
CREATE TABLE rda.mcs_diagnosis_codes (
     idr_clm_hd_icn varchar(15) NOT NULL,
     rda_position smallint NOT NULL,
     idr_diag_icd_type varchar(1),
     idr_diag_code varchar(7) NOT NULL,
     CONSTRAINT mcs_diagnosis_codes_key PRIMARY KEY (idr_clm_hd_icn, rda_position),
     CONSTRAINT mcs_diagnosis_codes_parent FOREIGN KEY (idr_clm_hd_icn) REFERENCES rda.mcs_claims(idr_clm_hd_icn)
);

/*
 * mcs_locations
 */
CREATE TABLE rda.mcs_locations (
   idr_clm_hd_icn varchar(15) NOT NULL,
   rda_position smallint NOT NULL,
   idr_loc_clerk varchar(4),
   idr_loc_code varchar(3),
   idr_loc_date date,
   idr_loc_actv_code varchar(1),
   CONSTRAINT mcs_locations_key PRIMARY KEY (idr_clm_hd_icn, rda_position),
   CONSTRAINT mcs_locations_parent FOREIGN KEY (idr_clm_hd_icn) REFERENCES rda.mcs_claims(idr_clm_hd_icn)
);