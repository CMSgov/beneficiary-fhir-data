insert into beneficiaries_history (
	bene_history_id,
	bene_id,
	bene_birth_dt,
	bene_sex_ident_cd,
	bene_crnt_hic_num,
	mbi_num,
	hicn_unhashed,
	mbi_hash,
	efctv_bgn_dt,
	last_updated
)
select
	"beneficiaryHistoryId",
	Cast("beneficiaryId" as bigint),
	"birthDate",
	"sex",
	"hicn",
	"medicareBeneficiaryId",
	"hicnUnhashed",
	"mbiHash",
	"mbiEffectiveDate",
	"lastupdated"
from
	"BeneficiariesHistory"
on conflict on constraint
	beneficiaries_history_pkey
do nothing;