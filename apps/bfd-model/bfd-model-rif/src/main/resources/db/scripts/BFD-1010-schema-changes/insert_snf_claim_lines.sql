insert into cmac.snf_claim_lines (
	parent_claim,
	clm_line_num,
	rev_cntr_ddctbl_coinsrnc_cd,
	hcpcs_cd,
	rev_cntr_ndc_qty_qlfr_cd,
	rev_cntr_ndc_qty,
	rev_cntr_ncvrd_chrg_amt,
	rev_cntr_rate_amt,
	rev_cntr,
	rndrng_physn_npi,
	rndrng_physn_upin,
	rev_cntr_tot_chrg_amt,
	rev_cntr_unit_cnt
)
select
	cast("parentClaim" as bigint),
	"lineNumber",
	"deductibleCoinsuranceCd",
	"hcpcsCode",
	"nationalDrugCodeQualifierCode",
	"nationalDrugCodeQuantity",
	"nonCoveredChargeAmount",
	"rateAmount",
	"revenueCenter",
	"revenueCenterRenderingPhysicianNPI",
	"revenueCenterRenderingPhysicianUPIN",
	"totalChargeAmount",
	"unitCount"
from public."SNFClaimLines";