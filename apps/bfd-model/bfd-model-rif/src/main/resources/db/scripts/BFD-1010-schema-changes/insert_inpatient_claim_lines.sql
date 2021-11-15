insert into inpatient_claim_lines (
	parent_claim,
	clm_line_num,
	rev_cntr_ddctbl_coinsrnc_cd,
	rev_cntr_ndc_qty_qlfr_cd,
	rev_cntr_ndc_qty,
	rev_cntr_ncvrd_chrg_amt,
	rev_cntr_rate_amt,
	rev_cntr,
	rev_cntr_tot_chrg_amt,
	rev_cntr_unit_cnt,
	hcpcs_cd,
	rndrng_physn_npi,
	rndrng_physn_upin
)
select
	cast("parentClaim" as bigint),
	"lineNumber",
	"deductibleCoinsuranceCd",
	"nationalDrugCodeQualifierCode",
	"nationalDrugCodeQuantity",
	"nonCoveredChargeAmount",
	"rateAmount",
	"revenueCenter",
	"totalChargeAmount",
	"unitCount",
	"hcpcsCode",
	"revenueCenterRenderingPhysicianNPI",
	"revenueCenterRenderingPhysicianUPIN"
from
	"InpatientClaimLines"
on conflict on constraint
	inpatient_claim_lines_pkey
do nothing;