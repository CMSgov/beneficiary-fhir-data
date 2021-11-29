-- -----------------------------
-- rebuild all secondary indexes
-- -----------------------------
create index if not exists beneficiaries_benecrnthicnum_idx
	on beneficiaries (bene_crnt_hic_num);

create index if not exists beneficiaries_history_beneid_idx
	on beneficiaries_history (bene_id);

create index if not exists beneficiaries_history_benecrnthicnum_idx
	on beneficiaries_history (bene_crnt_hic_num);

create index if not exists beneficiaries_history_mbihash_idx
	on beneficiaries_history (mbi_hash);

create index if not exists beneficiary_monthly_yearmonth_partdcontract_parentbene_idx
	on beneficiary_monthly (year_month, partd_contract_number_id asc, parent_beneficiary asc);

create index if not exists medicare_beneficiaryid_history_beneid_idx
	on medicare_beneficiaryid_history (bene_id);

create index if not exists carrier_claims_beneid_idx
	on carrier_claims (bene_id);

create index if not exists dme_claims_beneid_idx
	on dme_claims (bene_id);

create index if not exists hha_claims_beneid_idx
	on hha_claims (bene_id);

create index if not exists hospice_claims_beneid_idx
	on hospice_claims (bene_id);

create index if not exists inpatient_claims_beneid_idx
	on inpatient_claims (bene_id);

create index if not exists outpatient_claims_beneid_idx
	on outpatient_claims (bene_id);

create index if not exists partd_events_beneid_idx
	on partd_events (bene_id);

create index if not exists snf_claims_beneid_idx
	on snf_claims (bene_id);
