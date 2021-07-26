insert into public.beneficiaries_history (
	beneficiary_history_id, 
	bene_id, 
	bene_crnt_hicn,
	hicn_unhashed,
	mbi_num, 
	mbi_hash, 
	mbi_efctv_bgn_dt, 
	mbi_efctv_end_dt,
	bene_sex_ident_cd, 
	bene_birth_dt, 
	last_updated) 
select
	"beneficiaryHistoryId", 
	Cast("beneficiaryId" as bigint), 
	"hicn", 
	"hicnUnhashed", 
	"medicareBeneficiaryId", 
	"mbiHash", 
	"mbiEffectiveDate", 
	"mbiObsoleteDate", 
	"sex", 
	"birthDate", 
	"lastupdated" 
from   public."BeneficiariesHistory"; 