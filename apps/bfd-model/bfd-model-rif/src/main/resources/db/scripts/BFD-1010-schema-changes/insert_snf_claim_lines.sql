insert into snf_claim_lines (
	parent_claim,
	clm_line_num,
	hcpcs_cd,
	rev_cntr,
	rev_cntr_ndc_qty_qlfr_cd,
	rev_cntr_ndc_qty,
	rev_cntr_ncvrd_chrg_amt,
	rev_cntr_rate_amt,
	rev_cntr_tot_chrg_amt,
	rev_cntr_ddctbl_coinsrnc_cd,
	rev_cntr_unit_cnt,
	rndrng_physn_npi,
	rndrng_physn_upin
)
select
	cast("parentClaim" as bigint),
	"lineNumber",
	"hcpcsCode",
	"revenueCenter",
	"nationalDrugCodeQualifierCode",
	"nationalDrugCodeQuantity",
	"nonCoveredChargeAmount",
	"rateAmount",
	"totalChargeAmount",
	"deductibleCoinsuranceCd",
	"unitCount",
	"revenueCenterRenderingPhysicianNPI",
	"revenueCenterRenderingPhysicianUPIN"
from
	"SNFClaimLines"
on conflict on constraint
	snf_claim_lines_pkey
do nothing;