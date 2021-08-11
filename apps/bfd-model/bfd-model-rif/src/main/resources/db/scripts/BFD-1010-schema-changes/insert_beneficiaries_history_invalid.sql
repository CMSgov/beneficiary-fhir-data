insert into public.beneficiaries_history_invalid_beneficiaries (
	beneficiary_history_id, 
	bene_id, 
	bene_crnt_hic_num,
	hicn_unhashed,
	mbi_num,
	bene_sex_ident_cd,
	bene_birth_dt )
select
	"beneficiaryHistoryId", 
	Cast("beneficiaryId" as bigint), 
	"hicn", 
	"hicnUnhashed", 
	"medicareBeneficiaryId",
	"sex", 
	"birthDate"
from
	public."BeneficiariesHistoryInvalidBeneficiaries"
on conflict on constraint
	beneficiaries_history_invalid_beneficiaries_pkey
do nothing;