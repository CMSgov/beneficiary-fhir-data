-- -----------------------------
-- rebuild all secondary indexes
-- -----------------------------
create index if not exists beneficiaries_hicn_idx
	on beneficiaries (bene_crnt_hic_num);

create index if not exists beneficiaries_history_bene_id_idx
	on beneficiaries_history (bene_id);

create index if not exists beneficiaries_history_hicn_idx
	on beneficiaries_history (bene_crnt_hic_num);

create index if not exists beneficiaries_history_mbi_hash_idx
	on beneficiaries_history (mbi_hash);

create index if not exists beneficiary_monthly_partdcontractnumid_yearmonth_parentbene_idx
	on beneficiary_monthly (year_month, partd_contract_number_id asc, parent_beneficiary asc);

create index if not exists medicare_beneficiaryid_history_bene_id_idx
	on medicare_beneficiaryid_history (bene_id);

create index if not exists carrier_claims_bene_id_idx
	on carrier_claims (bene_id);

create index if not exists dme_claims_bene_id_idx
	on dme_claims (bene_id);

create index if not exists hha_claims_bene_id_idx
	on hha_claims (bene_id);

create index if not exists hospice_claims_bene_id_idx
	on hospice_claims (bene_id);

create index if not exists inpatient_claims_bene_id_idx
	on inpatient_claims (bene_id);

create index if not exists outpatient_claims_bene_id_idx
	on outpatient_claims (bene_id);

create index if not exists partd_events_bene_id_idx
	on partd_events (bene_id);

create index if not exists snf_claims_bene_id_idx
	on snf_claims (bene_id);
