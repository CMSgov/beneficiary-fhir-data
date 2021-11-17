insert into hospice_claim_lines (
	parent_claim,
	clm_line_num,
	rev_cntr,
	rev_cntr_pmt_amt_amt,
	rev_cntr_dt,
	rev_cntr_tot_chrg_amt,
	rev_cntr_unit_cnt,
	rev_cntr_bene_pmt_amt,
	rev_cntr_ddctbl_coinsrnc_cd,
	rev_cntr_ndc_qty_qlfr_cd,
	rev_cntr_ndc_qty,
	rev_cntr_ncvrd_chrg_amt,
	rev_cntr_prvdr_pmt_amt,
	rev_cntr_rate_amt,
	hcpcs_cd,
	hcpcs_1st_mdfr_cd,
	hcpcs_2nd_mdfr_cd,
	rndrng_physn_npi,
	rndrng_physn_upin
)
select
	cast("parentClaim" as bigint),
	"lineNumber",
	"revenueCenterCode",
	"paymentAmount",
	"revenueCenterDate",
	"totalChargeAmount",
	"unitCount",
	"benficiaryPaymentAmount",
	"deductibleCoinsuranceCd",
	"nationalDrugCodeQualifierCode",
	"nationalDrugCodeQuantity",
	"nonCoveredChargeAmount",
	"providerPaymentAmount",
	"rateAmount",
	"hcpcsCode",
	"hcpcsInitialModifierCode",
	"hcpcsSecondModifierCode",
	"revenueCenterRenderingPhysicianNPI",
	"revenueCenterRenderingPhysicianUPIN"
from
	"HospiceClaimLines"
on conflict on constraint
	hospice_claim_lines_pkey
do nothing;