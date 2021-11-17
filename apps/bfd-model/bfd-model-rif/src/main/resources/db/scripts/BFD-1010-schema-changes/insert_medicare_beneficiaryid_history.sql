-- setup for parallel processing
SET max_parallel_workers = 6;
SET max_parallel_workers_per_gather = 6;
SET parallel_leader_participation = off;
SET parallel_tuple_cost = 0;
SET parallel_setup_cost = 0;
SET min_parallel_table_scan_size = 0;

insert into medicare_beneficiaryid_history (
	bene_mbi_id,
	bene_id,
	last_updated,
	bene_clm_acnt_num,
	bene_ident_cd,
	bene_crnt_rec_ind_id,
	mbi_sqnc_num,
	mbi_num,
	mbi_efctv_bgn_dt,
	mbi_efctv_end_dt,
	mbi_bgn_rsn_cd,
	mbi_end_rsn_cd,
	mbi_card_rqst_dt,
	creat_user_id,
	creat_ts,
	updt_user_id,
	updt_ts
)
select
	"medicareBeneficiaryIdKey",
	cast("beneficiaryId" as bigint),
	"lastupdated",
	"claimAccountNumber",
	"beneficiaryIdCode",
	"mbiCrntRecIndId",
	"mbiSequenceNumber",
	"medicareBeneficiaryId",
	"mbiEffectiveDate",
	"mbiEndDate",
	"mbiEffectiveReasonCode",
	"mbiEndReasonCode",
	"mbiCardRequestDate",
	"mbiAddUser",
	"mbiAddDate",
	"mbiUpdateUser",
	"mbiUpdateDate"
from
	"MedicareBeneficiaryIdHistory";