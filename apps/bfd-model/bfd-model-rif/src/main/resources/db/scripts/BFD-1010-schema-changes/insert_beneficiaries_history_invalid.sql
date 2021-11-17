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