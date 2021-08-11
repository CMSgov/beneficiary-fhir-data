insert into public.beneficiaries_history (
	beneficiary_history_id, 
	bene_id,
	last_updated,
	bene_crnt_hic_num,
	hicn_unhashed,
	mbi_num, 
	mbi_hash, 
	mbi_efctv_bgn_dt, 
	mbi_efctv_end_dt,
	bene_sex_ident_cd, 
	bene_birth_dt) 
select
	"beneficiaryHistoryId", 
	Cast("beneficiaryId" as bigint), 
	"lastupdated",
	"hicn", 
	"hicnUnhashed", 
	"medicareBeneficiaryId", 
	"mbiHash", 
	"mbiEffectiveDate", 
	"mbiObsoleteDate", 
	"sex", 
	"birthDate"
from
	public."BeneficiariesHistory"
on conflict on constraint
	beneficiaries_history_pkey
do nothing;