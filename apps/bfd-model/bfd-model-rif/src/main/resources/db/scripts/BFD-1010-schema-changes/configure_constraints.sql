-- -----------------------------
-- drop all secondary indexes
-- -----------------------------
drop index if exists beneficiaries_hicn_idx;
drop index if exists beneficiaries_history_bene_id_idx;
drop index if exists beneficiaries_history_hicn_idx;
drop index if exists beneficiaries_history_mbi_hash_idx;
drop index if exists beneficiary_monthly_partdcontractnumid_yearmonth_parentbene_idx;
drop index if exists medicare_beneficiaryid_history_bene_id_idx;
drop index if exists carrier_claims_bene_id_idx;
drop index if exists dme_claims_bene_id_idx;
drop index if exists hha_claims_bene_id_idx;
drop index if exists hospice_claims_bene_id_idx;
drop index if exists inpatient_claims_bene_id_idx;
drop index if exists outpatient_claims_bene_id_idx;
drop index if exists partd_events_bene_id_idx;
drop index if exists snf_claims_bene_id_idx;

-- ---------------------
-- beneficiaries
-- ---------------------
alter table beneficiaries
drop constraint if exists beneficiaries_pkey;

alter table beneficiaries
add constraint beneficiaries_pkey primary key (bene_id) deferrable initially deferred;

-- ---------------------
-- beneficiaries_history
-- ---------------------
alter table beneficiaries_history
drop constraint if exists beneficiaries_history_pkey;

alter table beneficiaries_history
drop constraint if exists beneficiaries_history_pkey;

alter table beneficiaries_history
drop constraint if exists beneficiaries_history_bene_id_to_beneficiaries;

alter table beneficiaries_history
add constraint beneficiaries_history_pkey primary key (bene_history_id) deferrable initially deferred;

-- -------------------------------------------
-- beneficiaries_history_invalid_beneficiaries
-- -------------------------------------------
alter table beneficiaries_history_invalid_beneficiaries
drop constraint if exists beneficiaries_history_invalid_beneficiaries_pkey;

alter table beneficiaries_history_invalid_beneficiaries
add constraint beneficiaries_history_invalid_beneficiaries_pkey primary key (bene_history_id) deferrable initially deferred;

-- ---------------------
-- beneficiary_monthly
-- ---------------------
alter table beneficiary_monthly
drop constraint if exists beneficiary_monthly_pkey;

alter table beneficiary_monthly
drop constraint if exists beneficiary_monthly_parent_beneficiary_to_beneficiary;

alter table beneficiary_monthly
add constraint beneficiary_monthly_pkey primary key (year_month, parent_beneficiary) deferrable initially deferred;

-- ------------------------------
-- medicare_beneficiaryid_history
-- ------------------------------
alter table medicare_beneficiaryid_history
drop constraint if exists medicare_beneficiaryid_history_pkey;

alter table medicare_beneficiaryid_history
drop constraint if exists medicare_beneficiaryid_history_bene_id_to_beneficiaries;

alter table medicare_beneficiaryid_history
add constraint medicare_beneficiaryid_history_pkey primary key (bene_mbi_id) deferrable initially deferred;

-- -----------------------------------------------------
-- medicare_beneficiaryid_history_invalid_beneficiaries
-- -----------------------------------------------------
alter table medicare_beneficiaryid_history_invalid_beneficiaries
drop constraint if exists medicare_beneficiaryid_history_invalid_beneficiaries_pkey;

alter table medicare_beneficiaryid_history_invalid_beneficiaries
add constraint medicare_beneficiaryid_history_invalid_beneficiaries_pkey primary key (bene_mbi_id) deferrable initially deferred;

-- ----------------------------
-- carrier_claims
-- ----------------------------
alter table carrier_claims
drop constraint if exists carrier_claims_pkey;

alter table carrier_claims
drop constraint if exists carrier_claims_bene_id_to_beneficiary;

alter table carrier_claims
add constraint carrier_claims_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- carrier_claim_lines
-- ----------------------------
alter table carrier_claim_lines
drop constraint if exists carrier_claim_lines_pkey;

alter table carrier_claim_lines
drop constraint if exists carrier_claim_lines_parent_claim_to_carrier_claims;

alter table carrier_claim_lines
add constraint carrier_claim_lines_pkey primary key (parent_claim, clm_line_num) deferrable initially deferred;

-- ----------------------------
-- dme_claims
-- ----------------------------
alter table dme_claims
drop constraint if exists dme_claims_pkey;

alter table dme_claims
drop constraint if exists dme_claims_bene_id_to_beneficiary;

alter table dme_claims
add constraint dme_claims_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- dme_claim_lines
-- ----------------------------
alter table dme_claim_lines
drop constraint if exists dme_claim_lines_pkey;

alter table dme_claim_lines
drop constraint if exists dme_claim_lines_parent_claim_to_dme_claims;

alter table dme_claim_lines
add constraint dme_claim_lines_pkey primary key (parent_claim, clm_line_num) deferrable initially deferred;

-- ----------------------------
-- hha_claims
-- ----------------------------
alter table hha_claims
drop constraint if exists hha_claims_pkey;

alter table hha_claims
drop constraint if exists hha_claims_bene_id_to_beneficiary;

alter table hha_claims
add constraint hha_claims_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- hha_claim_lines
-- ----------------------------
alter table hha_claim_lines
drop constraint if exists hha_claim_lines_pkey;

alter table hha_claim_lines
drop constraint if exists hha_claim_lines_parent_claim_to_dme_claims;

alter table hha_claim_lines
add constraint hha_claim_lines_pkey primary key (parent_claim, clm_line_num) deferrable initially deferred;

-- ----------------------------
-- hospice_claims
-- ----------------------------
alter table hospice_claims
drop constraint if exists hospice_claims_pkey;

alter table hospice_claims
drop constraint if exists hospice_claims_bene_id_to_beneficiary;

alter table hospice_claims
add constraint hospice_claims_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- hospice_claim_lines
-- ----------------------------
alter table hospice_claim_lines
drop constraint if exists hospice_claim_lines_pkey;

alter table hospice_claim_lines
drop constraint if exists hospice_claim_lines_parent_claim_to_dme_claims;

alter table hospice_claim_lines
add constraint hospice_claim_lines_pkey primary key (parent_claim, clm_line_num) deferrable initially deferred;

-- ----------------------------
-- inpatient_claims
-- ----------------------------
alter table inpatient_claims
drop constraint if exists inpatient_claims_pkey;

alter table inpatient_claims
drop constraint if exists inpatient_claims_bene_id_to_beneficiary;

alter table inpatient_claims
add constraint inpatient_claims_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- inpatient_claim_lines
-- ----------------------------
alter table inpatient_claim_lines
drop constraint if exists inpatient_claim_lines_pkey;

alter table inpatient_claim_lines
drop constraint if exists inpatient_claim_lines_parent_claim_to_dme_claims;

alter table inpatient_claim_lines
add constraint inpatient_claim_lines_pkey primary key (parent_claim, clm_line_num) deferrable initially deferred;

-- ----------------------------
-- outpatient_claims
-- ----------------------------
alter table outpatient_claims
drop constraint if exists outpatient_claims_pkey;

alter table outpatient_claims
drop constraint if exists outpatient_claims_bene_id_to_beneficiary;

alter table outpatient_claims
add constraint outpatient_claims_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- outpatient_claim_lines
-- ----------------------------
alter table outpatient_claim_lines
drop constraint if exists outpatient_claim_lines_pkey;

alter table outpatient_claim_lines
drop constraint if exists outpatient_claim_lines_parent_claim_to_dme_claims;

alter table outpatient_claim_lines
add constraint outpatient_claim_lines_pkey primary key (parent_claim, clm_line_num) deferrable initially deferred;

-- ----------------------------
-- partd_events
-- ----------------------------
alter table partd_events
drop constraint if exists partd_events_pkey;

alter table partd_events
drop constraint if exists partd_events_bene_id_to_beneficiaries;

alter table partd_events
add constraint partd_events_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- snf_claims
-- ----------------------------
alter table snf_claims
drop constraint if exists snf_claims_pkey;

alter table snf_claims
drop constraint if exists snf_claims_bene_id_to_beneficiary;

alter table snf_claims
add constraint snf_claims_pkey primary key (clm_id) deferrable initially deferred;

-- ----------------------------
-- snf_claim_lines
-- ----------------------------
alter table snf_claim_lines
drop constraint if exists snf_claim_lines_pkey;

alter table snf_claim_lines
drop constraint if exists snf_claim_lines_parent_claim_to_snf_claims;

alter table snf_claim_lines
add constraint snf_claim_lines_pkey primary key (parent_claim, clm_line_num) deferrable initially deferred;