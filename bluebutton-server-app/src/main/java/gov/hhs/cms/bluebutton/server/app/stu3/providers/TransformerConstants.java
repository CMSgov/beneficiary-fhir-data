package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.GroupComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.codesystems.Adjudication;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * Contains all of the shared constants used to transform CCW JPA entities (e.g.
 * {@link Beneficiary}) into FHIR resources (e.g. {@link Patient}).
 */
final class TransformerConstants {
	static final String BENEFIT_BALANCE_TYPE = "http://bluebutton.cms.hhs.gov/coding#benefitBalanceType";

	static final String BENEFIT_COVERAGE_DATE = "Benefit Coverage Date";

	static final String CARE_TEAM_ROLE_ASSISTING = "Assist";

	static final String CARE_TEAM_ROLE_OTHER = "Other";

	static final String CARE_TEAM_ROLE_PRIMARY = "Primary";

	static final String CLAIM_CLINICAL_TRIAL_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt";

	static final String CLAIM_HOSPICE_START_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hspcstrt.txt";

	static final String CODED_ADJUDICATION_1ST_ANSI_CD = "Revenue Center 1st ANSI Code";

	static final String CODED_ADJUDICATION_1ST_MSP_AMOUNT = "Revenue Center 1st Medicare Secondary Payer (MSP) Paid Amount";

	static final String CODED_ADJUDICATION_2ND_ANSI_CD = "Revenue Center 2nd ANSI Code";

	static final String CODED_ADJUDICATION_2ND_MSP_AMOUNT = "Revenue Center 2nd Medicare Secondary Payer (MSP) Paid Amount";

	static final String CODED_ADJUDICATION_3RD_ANSI_CD = "Revenue Center 3rd ANSI Code";

	static final String CODED_ADJUDICATION_4TH_ANSI_CD = "Revenue Center 4th ANSI Code";

	static final String CODED_ADJUDICATION_ALLOWED_CHARGE = "Allowed Charge Amount";

	static final String CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT = "Payment Amount to Beneficiary";

	static final String CODED_ADJUDICATION_BENEFICIARY_PRIMARY_PAYER_PAID = "Beneficiary Primary Payer Paid Amount";

	static final String CODED_ADJUDICATION_BLOOD_DEDUCTIBLE = "Blood Deductible Amount";

	static final String CODED_ADJUDICATION_CASH_DEDUCTIBLE = "Cash Deductible Amount";

	static final String CODED_ADJUDICATION_DEDUCTIBLE = "Beneficiary Deductible Amount";

	static final String CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT = "Medicare Coverage Gap Discount Amount";

	static final String CODED_ADJUDICATION_GDCA = "Gross Drug Cost Above Out-of-Pocket Threshold (GDCA)";

	static final String CODED_ADJUDICATION_GDCB = "Gross Drug Cost Below Out-of-Pocket Threshold (GDCB)";

	static final String CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT = "Coinsurance Amount";

	static final String CODED_ADJUDICATION_LINE_PRIMARY_PAYER_ALLOWED_CHARGE = "Primary Payer Allowed Charge Amount";

	static final String CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR = "Line Processing Indicator Code";

	static final String CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT = "Purchase Price Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lics_amt.txt">
	 * CCW Data Dictionary: LICS_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT = "Part D Low Income Subsidy (LICS) Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT = "NCH Beneficiary Blood Deductible Liability Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_INPATIENT_DEDUCTIBLE = "NCH Beneficiary Inpatient Deductible Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_A_COINSURANCE_LIABILITY = "NCH Beneficiary Part A Coinsurance Liability Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_COINSURANCE = "NCH Beneficiary Part B Coinsurance Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE = "NCH Beneficiary Part B Deductible Amount";

	static final String CODED_ADJUDICATION_NCH_PRIMARY_PAYER_CLAIM_PAID_AMOUNT = "NCH Primary Payer Claim Paid Amount";

	static final String CODED_ADJUDICATION_NONCOVERED_CHARGE = "Noncovered Charge";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/othr_troop_amt.txt">
	 * CCW Data Dictionary: OTHR_TROOP_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_OTHER_TROOP_AMOUNT = "Other True Out-of-Pocket (TrOOP) Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt">
	 * CCW Data Dictionary: DRUG_CVRG_STUS_CD</a>.
	 */
	static final String CODED_ADJUDICATION_PART_D_COVERED = "Part D Covered";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt">
	 * CCW Data Dictionary: DRUG_CVRG_STUS_CD</a>.
	 */
	static final String CODED_ADJUDICATION_PART_D_NONCOVERED_OTC = "Part D Over-the-counter drugs";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt">
	 * CCW Data Dictionary: DRUG_CVRG_STUS_CD</a>.
	 */
	static final String CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT = "Part D Supplemental drugs (reported by plans that provide Enhanced Alternative coverage)";

	static final String CODED_ADJUDICATION_PASS_THROUGH_PER_DIEM_AMOUNT = "Claim Pass Thru Per Diem Amount";

	static final String CODED_ADJUDICATION_PASS_THRU_PER_DIEM_AMOUNT = "Line Allowed Charge Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plro_amt.txt">
	 * CCW Data Dictionary: PLRO_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT = "Reduction in patient liability due to payments by other payers (PLRO) Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptnt_pay_amt.txt">
	 * CCW Data Dictionary: PTNT_PAY_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_PATIENT_PAY = "Patient Pay Amount";

	static final String CODED_ADJUDICATION_PATIENT_RESPONSIBILITY_AMOUNT = "Patient Responsibility Amount";

	static final String CODED_ADJUDICATION_PAYMENT = "NCH Payment Amount";

	static final String CODED_ADJUDICATION_PAYMENT_B = "Provider Payment Amount";

	static final String CODED_ADJUDICATION_PHYSICIAN_ASSISTANT = "Carrier Line Reduced Payment Physician Assistant Code";

	static final String CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT = "Primary Payer Paid Amount";

	static final String CODED_ADJUDICATION_PROFESSIONAL_COMP_CHARGE = "Professional Component Charge Amount";

	static final String CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT = "Provider Payment Amount";

	static final String CODED_ADJUDICATION_RATE_AMOUNT = "Revenue Center Rate Amount";

	static final String CODED_ADJUDICATION_REDUCED_COINSURANCE_AMOUNT = "Reduced Coinsurance Amount";

	static final String CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT = "Submitted Charge Amount";

	static final String CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT = "Line Total Charge Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tot_rx_cst_amt.txt">
	 * CCW Data Dictionary: TOT_RX_CST_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_TOTAL_COST = "Total Prescription Cost";

	static final String CODED_ADJUDICATION_TOTAL_DEDUCTION_AMOUNT = "Total Deduction Amount";

	static final String CODED_ADJUDICATION_WAGE_ADJ_COINSURANCE_AMOUNT = "Wage Adj Coinsurance Amount";

	static final String CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS = "CSPINV";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prscrbr_id.txt">
	 * CCW Data Dictionary: PRSCRBR_ID</a>.
	 */
	static final String CODED_PRESCRIBER_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prscrbr_id.txt";

	static final String CODING_BENEFIT_BALANCE_URL = "http://build.fhir.org/explanationofbenefit-definitions.html#ExplanationOfBenefit.benefitBalance.category";

	static final String CODING_BENEFIT_DEDUCTIBLE_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_amt.txt";

	static final String CODING_CLAIM_OUTPAT_BEN__PAYMENT_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/benepmt.txt";

	static final String CODING_CLAIM_OUTPAT_PROVIDER_PAYMENT_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvdrpmt.txt";

	static final String CODING_CLAIM_PASS_THRU_PER_DIEM_AMT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/per_diem.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/disp_shr.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_exp.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_fsp.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ime_amt.txt";

	static final String CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptloutl.txt";

	static final String CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hldhrmls.txt";

	static final String CODING_CLAIM_TOTAL_PPS_CAPITAL_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pps_cptl.txt";

	static final String CODING_NCH_BEN_PART_B_COINSUR_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_coin.txt";

	static final String CODING_NCH_BEN_PART_B_DED_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_ded.txt";

	static final String CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/blddedam.txt";

	static final String CODING_NCH_BENEFIT_COIN_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_amt.txt";

	static final String CODING_NCH_DRUG_OUTLIER_APPROVED_PAYMENT_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/outlrpmt.txt";

	static final String CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncchgamt.txt";

	static final String CODING_NCH_INPATIENT_TOTAL_AMT_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tdedamt.txt";

	static final String CODING_NCH_PRIMARY_PAYER_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpayamt.txt";

	static final String CODING_NCH_PROFFESIONAL_CHARGE_URL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pcchgamt.txt";

	static final String CODING_REVENUE_CENTER_RENDER_PHY_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rndrng_physn_npi.txt";

	/**
	 * The CMS-custom {@link Coding#getSystem()} value for Medicare
	 * {@link Adjudication}s.
	 */
	static final String CODING_SYSTEM_ADJUDICATION_CMS = "CMS Adjudications";

	static final String CODING_SYSTEM_ADJUDICATION_FHIR = "http://hl7.org/fhir/adjudication";

	public static final String CODING_SYSTEM_ADMISSION_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/admsn_dt.txt";

	public static final String CODING_SYSTEM_ADMISSION_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/type_adm.txt";

	public static final String CODING_SYSTEM_BENEFICIARY_DISCHARGE_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dschrgdt.txt";

	public static final String CODING_SYSTEM_BENEFITS_EXHAUSTED_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/exhst_dt.txt";

	static final String CODING_SYSTEM_BETOS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/betos.txt";

	public static final String CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bldfrnsh.txt";

	static final String CODING_SYSTEM_CARE_TEAM_ROLE = "http://build.fhir.org/valueset-claim-careteamrole.html";

	static final String CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/at_npi.txt";

	/**
	 * The {@link Identifier#getSystem()} used in {@link Patient} resources to
	 * store a one-way cryptographic hash of each Medicare beneficiaries' HICN.
	 * Note that, with the SSNRI initiative, CMS is planning to move away from
	 * HICNs. However, HICNs are still the primary/only Medicare identifier for
	 * now.
	 */
	public static final String CODING_SYSTEM_CCW_BENE_HICN_HASH = "http://bluebutton.cms.hhs.gov/identifier#hicnHash";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_id.txt">
	 * CCW Data Dictionary: BENE_ID</a>.
	 */
	public static final String CODING_SYSTEM_CCW_BENE_ID = "CCW.BENE_ID";

	static final String CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ms_cd.txt";

	static final String CODING_SYSTEM_CCW_BENE_PTA_TRMNTN_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/a_trm_cd.txt";

	static final String CODING_SYSTEM_CCW_BENE_PTB_TRMNTN_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/b_trm_cd.txt";

	static final String CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_num.txt";

	static final String CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION = "Debit accepted";

	static final String CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ccltrnum.txt";

	static final String CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtdnlcd.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prtcptg.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcfaspcl.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvstate.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prv_type.txt";

	static final String CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provzip.txt";

	public static final String CODING_SYSTEM_CCW_CLAIM_GRP_ID = "http://bluebutton.cms.hhs.gov/identifier#claimGroup";

	public static final String CODING_SYSTEM_CCW_CLAIM_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_id.txt";

	static final String CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typesrvc.txt";

	public static final String CODING_SYSTEM_CCW_CLAIM_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_type.txt";

	static final String CODING_SYSTEM_CCW_DEDUCTIBLE_INDICATOR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_sw.txt";

	static final String CODING_SYSTEM_CCW_ESRD_INDICATOR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/esrd_ind.txt";

	static final String CODING_SYSTEM_CCW_FACILITY_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fac_type.txt";

	static final String CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nopay_cd.txt";

	static final String CODING_SYSTEM_CCW_INP_POA_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw1.txt";

	static final String CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/crec.txt";

	static final String CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orec.txt";

	static final String CODING_SYSTEM_CCW_OPERATING_PHYSICIAN_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/op_npi.txt";

	static final String CODING_SYSTEM_CCW_OTHER_PHYSICIAN_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ot_npi.txt";

	static final String CODING_SYSTEM_CCW_PAYMENT_80_100_INDICATOR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtindsw.txt";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pde_id.txt">
	 * CCW Data Dictionary: PDE_ID</a>.
	 */
	public static final String CODING_SYSTEM_CCW_PDE_ID = "CCW.PDE_ID";

	static final String CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/phrmcy_srvc_type_cd.txt";

	static final String CODING_SYSTEM_CCW_PRICING_LOCALITY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lclty_cd.txt";

	static final String CODING_SYSTEM_CCW_PROCESSING_INDICATOR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcngind.txt";

	static final String CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/asgmntcd.txt";

	static final String CODING_SYSTEM_CCW_RECORD_ID_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ric_cd.txt";

	static final String CODING_SYSTEM_CLIA_LAB_NUM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_line_clia_lab_num.txt";

	static final String CODING_SYSTEM_CMS_CLAIM_TYPES = "http://bluebutton.cms.hhs.gov/coding#claimType";

	static final String CODING_SYSTEM_CMS_HCT_OR_HGB_TEST_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbtp.txt";

	static final String CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_sw.txt";

	static final String CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtindsw.txt";

	static final String CODING_SYSTEM_CMS_LINE_PROCESSING_INDICATOR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcngind.txt";

	public static final String CODING_SYSTEM_COINSURANCE_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_day.txt";

	public static final String CODING_SYSTEM_COVERED_CARE_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carethru.txt";

	public static final String CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/revdedcd.txt";

	public static final String CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drg_cd.txt";

	static final String CODING_SYSTEM_FHIR_ACT = "http://hl7.org/fhir/v3/ActCode";

	static final String CODING_SYSTEM_FHIR_CLAIM_TYPE = "http://build.fhir.org/valueset-claim-type.html";

	static final String CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt";

	static final String CODING_SYSTEM_FHIR_EOB_ITEM_TYPE = "http://hl7.org/fhir/ValueSet/v3-ActInvoiceGroupCode";

	static final String CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typcsrvcb.txt";

	public static final String CODING_SYSTEM_FREQUENCY_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/freq_cd.txt";

	/**
	 * A CMS-controlled standard. More info here: <a href=
	 * "https://en.wikipedia.org/wiki/Healthcare_Common_Procedure_Coding_System">
	 * Healthcare Common Procedure Coding System</a>.
	 */
	static final String CODING_SYSTEM_HCPCS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_cd.txt";

	static final String CODING_SYSTEM_HCPCS_YR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_yr.txt";

	public static final String CODING_SYSTEM_HCT_HGB_TEST_RESULTS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbrs.txt";

	public static final String CODING_SYSTEM_HCT_HGB_TEST_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbtp.txt";

	public static final String CODING_SYSTEM_HHA_CARE_START_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hhstrtdt.txt";

	public static final String CODING_SYSTEM_HHA_LUPA_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lupaind.txt";

	public static final String CODING_SYSTEM_HHA_REFERRAL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hha_rfrl.txt";

	public static final String CODING_SYSTEM_HHA_VISIT_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/visitcnt.txt";

	static final String CODING_SYSTEM_ICD9_DIAG = "http://hl7.org/fhir/sid/icd-9-cm/diagnosis";

	public static final String CODING_SYSTEM_MCO_PAID_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mcopdsw.txt";

	static final String CODING_SYSTEM_MONEY = "urn:std:iso:4217";

	static final String CODING_SYSTEM_MONEY_US = "USD";

	public static final String CODING_SYSTEM_MTUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mtus_ind.txt";

	public static final String CODING_SYSTEM_MTUS_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mtus_cnt.txt";

	static final String CODING_SYSTEM_NDC = "https://www.accessdata.fda.gov/scripts/cder/ndc";

	static final String CODING_SYSTEM_NDC_QLFR_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr_ndc_qty_qlfr_cd.txt";

	public static final String CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nutilday.txt";

	public static final String CODING_SYSTEM_NONCOVERED_STAY_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncovfrom.txt";

	/**
	 * The United States National Provider Identifier, as available at
	 * <a href="http://download.cms.gov/nppes/NPI_Files.html">NPI/NPPES File</a>
	 * .
	 */
	static final String CODING_SYSTEM_NPI_US = "http://hl7.org/fhir/sid/us-npi";

	public static final String CODING_SYSTEM_PATIENT_DISCHARGE_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/stus_cd.txt";

	public static final String CODING_SYSTEM_PATIENT_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptntstus.txt";

	static final String CODING_SYSTEM_PDE_DAYS_SUPPLY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/days_suply_num.txt";

	static final String CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_pbp_rec_num.txt";

	static final String CODING_SYSTEM_PDE_PLAN_CONTRACT_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_cntrct_rec_id.txt";

	static final String CODING_SYSTEM_PHYSICIAN_ASSISTANT_ADJUDICATION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/astnt_cd.txt";

	public static final String CODING_SYSTEM_PRICING_STATE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_st.txt";

	public static final String CODING_SYSTEM_PRIMARY_PAYER_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpay_cd.txt";

	public static final String CODING_SYSTEM_PROVIDER_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provider.txt";

	public static final String CODING_SYSTEM_QUALIFIED_STAY_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qlfrfrom.txt";

	public static final String CODING_SYSTEM_QUERY_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/query_cd.txt";

	static final String CODING_SYSTEM_RACE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/race.txt";

	static final String CODING_SYSTEM_REVENUE_CENTER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr.txt";

	static final String CODING_SYSTEM_RX_ADJUSTMENT_DEL_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/adjstmt_dltn_cd.txt";

	static final String CODING_SYSTEM_RX_BRAND_GENERIC_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/brnd_gnrc_cd.txt";

	static final String CODING_SYSTEM_RX_CATASTROPHIC_COV_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ctstrphc_cvrg_cd.txt";

	static final String CODING_SYSTEM_RX_COVERAGE_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt";

	static final String CODING_SYSTEM_RX_DAW_PRODUCT_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/daw_prod_slctn_cd.txt";

	static final String CODING_SYSTEM_RX_DISPENSE_STATUS_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dspnsng_stus_cd.txt";

	static final String CODING_SYSTEM_RX_NON_STD_FORMAT_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nstd_frmt_cd.txt";

	static final String CODING_SYSTEM_RX_PATIENT_RESIDENCE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptnt_rsdnc_cd.txt";

	static final String CODING_SYSTEM_RX_PHARMACY_SVC_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/phrmcy_srvc_type_cd.txt";

	static final String CODING_SYSTEM_RX_PRESCRIPTION_ORIGIN_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_orgn_cd.txt";

	static final String CODING_SYSTEM_RX_PRICING_EXCEPTION_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_excptn_cd.txt";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_srvc_rfrnc_num.txt">
	 * CCW Data Dictionary: RX_SRVC_RFRNC_NUM</a>.
	 */
	static final String CODING_SYSTEM_RX_SRVC_RFRNC_NUM = "CCW.RX_SRVC_RFRNC_NUM";

	static final String CODING_SYSTEM_RX_SUBMISSION_CLARIFICATION_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/submsn_clr_cd.txt";

	public static final String CODING_SYSTEM_SCREEN_SAVINGS_AMT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/scrnsvgs.txt";

	public static final String CODING_SYSTEM_SOURCE_ADMISSION_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/src_adms.txt";

	public static final String CODING_SYSTEM_SUPPLIER_TYPE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sup_type.txt";

	public static final String CODING_SYSTEM_UTILIZATION_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/util_day.txt";

	static final String COVERAGE_PLAN = "Medicare";

	/**
	 * The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()}
	 * value for Part A.
	 */
	public static final String COVERAGE_PLAN_PART_A = "Part A";

	/**
	 * The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()}
	 * value for Part B.
	 */
	public static final String COVERAGE_PLAN_PART_B = "Part B";

	/**
	 * The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()}
	 * value for Part D.
	 */
	public static final String COVERAGE_PLAN_PART_D = "Part D";

	static final String EXTENSION_CMS_ADMITTING_DIAGNOSIS = "http://bluebutton.cms.hhs.gov/extensions#admittingDiagnosis";

	static final String EXTENSION_CMS_ATTENDING_PHYSICIAN = "http://bluebutton.cms.hhs.gov/extensions#attendingPhysician";

	static final String EXTENSION_CMS_CLAIM_TYPE = "http://bluebutton.cms.hhs.gov/extensions#claimType";

	static final String EXTENSION_CMS_DIAGNOSIS_GROUP = "http://bluebutton.cms.hhs.gov/extensions#diagnosisRelatedGroupCode";

	static final String EXTENSION_CMS_DIAGNOSIS_LINK_ID = "http://bluebutton.cms.hhs.gov/extensions#diagnosisLinkId";

	static final String EXTENSION_CMS_HCT_OR_HGB_RESULTS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbrs.txt";

	static final String EXTENSION_CMS_OPERATING_PHYSICIAN = "http://bluebutton.cms.hhs.gov/extensions#operatingPhysician";

	static final String EXTENSION_CMS_OTHER_PHYSICIAN = "http://bluebutton.cms.hhs.gov/extensions#otherPhysician";

	static final String EXTENSION_US_CORE_RACE = "http://hl7.org/fhir/StructureDefinition/us-core-race";

	static final String HCPCS_INITIAL_MODIFIER_CODE1 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd1.txt";

	static final String HCPCS_INITIAL_MODIFIER_CODE2 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd2.txt";

	static final String HCPCS_INITIAL_MODIFIER_CODE3 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd3.txt";

	static final String HCPCS_INITIAL_MODIFIER_CODE4 = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd4.txt";

	static final String LINE_1ST_EXPNS_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/expnsdt1.txt";

	static final String LINE_LAST_EXPNS_DATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/expnsdt2.txt";

	static final String LINE_PLACE_OF_SERVICE_CODE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt";

	static final String ORGANIZATION_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orgnpinm.txt";

	static final String PROVIDER_NPI = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sup_npi.txt";

	static final String PROVIDER_STATE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvstate.txt";

	static final BigDecimal ZERO = new BigDecimal(0);
}
