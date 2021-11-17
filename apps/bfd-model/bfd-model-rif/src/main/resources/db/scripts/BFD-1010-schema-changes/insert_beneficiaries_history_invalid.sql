-- setup for parallel processing
SET max_parallel_workers = 6;
SET max_parallel_workers_per_gather = 6;
SET parallel_leader_participation = off;
SET parallel_tuple_cost = 0;
SET parallel_setup_cost = 0;
SET min_parallel_table_scan_size = 0;

insert into beneficiaries_history_invalid_beneficiaries (
	bene_history_id, 
	bene_id,
	bene_birth_dt,
	bene_sex_ident_cd,
	bene_crnt_hic_num,
	mbi_num,
	hicn_unhashed
)
select
	"beneficiaryHistoryId", 
	Cast("beneficiaryId" as bigint),
	"birthDate",
	"sex",
	"hicn",
	"medicareBeneficiaryId",
	"hicnUnhashed"
from
	"BeneficiariesHistoryInvalidBeneficiaries";