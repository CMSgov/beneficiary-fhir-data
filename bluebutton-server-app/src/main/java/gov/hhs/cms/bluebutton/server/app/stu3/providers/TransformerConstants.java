package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.GroupComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.codesystems.Adjudication;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.dstu3.model.codesystems.ClaimType;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.codebook.model.Variable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * <p>
 * Contains all of the shared constants used to transform CCW JPA entities (e.g.
 * {@link Beneficiary}) into FHIR resources (e.g. {@link Patient}).
 * </p>
 * <h3>Naming Conventions</h3>
 * <p>
 * This is a monster list of constants. To keep it somewhat manageable, the
 * following naming conventions must be used for the constants:
 * </p>
 * <ol>
 * <li>If the constant value is used as a FHIR extension URL (see
 * {@link Extension#setUrl(String)}), the constant must be prefixed with
 * <code>EXTENSION_</code>.</li>
 * <li>If the constant value is used as a FHIR coding system (see
 * {@link Coding#setSystem(String)}):, the constant must be prefixed with
 * <code>CODING_</code>.
 * <ol>
 * <li>If the codeset is part of the Medicare billing standards, the constant
 * must be further prefixed with <code>CMS_</code>.</li>
 * <li>If the codeset is only used with the Chronic Conditions Warehouse, the
 * constant must be further prefixed with <code>CCW_</code>.</li>
 * <li>If the codeset is only used with the Blue Button API, the constant must
 * be further prefixed with <code>BBAPI_</code>.</li>
 * </ol>
 * </li>
 * <li>If the constant is used as a code in a FHIR coding (see
 * {@link Coding#setCode(String)}), the constant must be prefixed with
 * <code>CODED_</code> and the same suffix as the constant for the coding's
 * system constant.</li>
 * <li>If the constant value is used as a FHIR identifier system (see
 * {@link Identifier#setSystem(String)}), the constant must be prefixed with
 * <code>IDENTIFIER_</code>.</li>
 * </ol>
 * <h3>FHIR Extension URLs and Coding System URIs</h3>
 * <p>
 * Every FHIR {@link Extension} must have an {@link Extension#getUrl()} value
 * that applications can use to identify/select the field. Similarly, every FHIR
 * {@link Coding} must have a {@link Coding#getSystem()} URI value that
 * applications can use to identify the codeset.
 * </p>
 * <p>
 * To try and achieve some consistency in our API, the following guidelines must
 * be adhered to when adding new {@link Extension#getUrl()}s and
 * {@link Coding#getSystem()} URIs to our application:
 * </p>
 * <ol>
 * <li>If FHIR has a published URL/URI for it already, that must be used (e.g.
 * {@link #CODING_NPI_US}).</li>
 * <li>All other extension URLs and coding URIs must be set to the URL of a
 * public web page controlled by the Blue Button API team, where links and/or
 * documentation for the item in question can be posted. This is necessary to
 * ensure the API remains appropriately documented and usable in the future.
 * TODO file JIRA ticket to switch all existing URLs/URIs</li>
 * </ol>
 */
final class TransformerConstants {
	/**
	 * <p>
	 * The base URL/URI/system for FHIR output when the domain concept in question
	 * is owned by (or at least documented by) the Blue Button API team. Please note
	 * that the documentation on this site is generated from the following project:
	 * <a href=
	 * "https://github.com/CMSgov/bluebutton-site-static">bluebutton-site-static</a>.
	 * </p>
	 * <p>
	 * This URL will never be used by itself; it will always be suffixed with a more
	 * specific path.
	 * </p>
	 */
	static final String BASE_URL_BBAPI_RESOURCES = "https://bluebutton.cms.gov/resources";

	/**
	 * <p>
	 * The base URL/URI/system for FHIR output related to
	 * {@link CcwCodebookVariable}:
	 * </p>
	 * <ul>
	 * <li>{@link Extension#getUrl()}</li>
	 * <li>{@link Coding#getSystem()}</li>
	 * <li>{@link Identifier#getSystem()}</li>
	 * </ul>
	 * <p>
	 * This URL will never be used by itself; it will always be suffixed with the
	 * (lower-cased) {@link CcwCodebookVariable#getVariable()}'s
	 * {@link Variable#getId()}.
	 * </p>
	 */
	static final String BASE_URL_CCW_VARIABLES = BASE_URL_BBAPI_RESOURCES + "/variables";

	static final String CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS = "CSPINV";

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
	static final String CODED_ADJUDICATION_DISPROPORTIONATE_SHARE_AMOUNT = "Disproportionate Share Amount";

	static final String CODED_ADJUDICATION_INDIRECT_MEDICAL_EDUCATION_AMOUNT = "Indirect Medical Education Amount";

	static final String CODED_ADJUDICATION_PATIENT_PAY = "Patient Pay Amount";

	static final String CODED_ADJUDICATION_PATIENT_RESPONSIBILITY_AMOUNT = "Patient Responsibility Amount";

	static final String CODED_ADJUDICATION_PAYMENT = "Revenue Center Payment Amount";

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

	static final String CODED_BENEFIT_BALANCE_TYPE_BENE_PAYMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/benepmt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_BLOOD_DEDUCTIBLE_LIABILITY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/blddedam.txt";

	public static final String CODED_BENEFIT_BALANCE_TYPE_BLOOD_PINTS_FURNISHED = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bldfrnsh.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_COINSURANCE_LIABILITY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_amt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_DEDUCTIBLE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_amt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_DRUG_OUTLIER_APPROVED_PAYMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/outlrpmt.txt";

	public static final String CODED_BENEFIT_BALANCE_TYPE_NON_UTILIZATION_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nutilday.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_NONCOVERED_CHARGE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncchgamt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PARTB_COINSURANCE_AMOUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_coin.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PARTB_DEDUCTIBLE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_ded.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PASS_THRU_PER_DIEM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/per_diem.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_DISPROPORTIONAL_SHARE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/disp_shr.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_EXCEPTION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_exp.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_FEDRERAL_PORTION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_fsp.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_INDIRECT_MEDICAL_EDU = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ime_amt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_OUTLIER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptloutl.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PPS_OLD_CAPITAL_HOLD_HARMLESS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hldhrmls.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PROFFESIONAL_COMPONENT_CHARGE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pcchgamt.txt";

	public static final String CODED_BENEFIT_BALANCE_TYPE_SYSTEM_UTILIZATION_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/util_day.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_TOTAL_DEDUCTION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tdedamt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_TOTAL_PPS_CAPITAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pps_cptl.txt";

	public static final String CODED_BENEFIT_BALANCE_TYPE_VISIT_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/visitcnt.txt";

	public static final String CODED_BENEFIT_COVERAGE_DATE_EXHAUSTED = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/exhst_dt.txt";

	public static final String CODED_BENEFIT_COVERAGE_DATE_NONCOVERED = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncovfrom.txt";

	public static final String CODED_BENEFIT_COVERAGE_DATE_QUALIFIED = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qlfrfrom.txt";

	public static final String CODED_BENEFIT_COVERAGE_DATE_STAY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carethru.txt";

	static final String CODED_EOB_DISPOSITION = "Debit accepted";

	static final String CODED_MONEY_USD = "USD";

	static final String CODING_BBAPI_BENEFIT_BALANCE_TYPE = "http://bluebutton.cms.hhs.gov/coding#benefitBalanceType";

	static final String CODING_BBAPI_BENEFIT_COVERAGE_DATE = "Benefit Coverage Date";

	private static final String CODING_BETOS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/betos.txt";

	/**
	 * The CMS-custom {@link Coding#getSystem()} value for Medicare
	 * {@link Adjudication}s.
	 */
	static final String CODING_CCW_ADJUDICATION_CATEGORY = "CMS Adjudications";

	private static final String CODING_CCW_ADMISSION_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/type_adm.txt";

	private static final String CODING_CCW_CLAIM_FREQUENCY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/freq_cd.txt";

	static final String CODING_CCW_CLAIM_GROUP_ID = "http://bluebutton.cms.hhs.gov/identifier#claimGroup";

	private static final String CODING_CCW_CLAIM_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_id.txt";

	// FIXME: the following URL is currently just a placeholder for the final one
	// FIXME the constant name shouldn't have CCW in it
	static final String CODING_CCW_CLAIM_TYPE = "https://bluebutton.cms.gov/developer/docs/reference/some-thing";
	
	private static final String CODING_CCW_COINSURANCE_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_day.txt";

	private static final String CODING_CCW_DIAGNOSIS_RELATED_GROUP = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drg_cd.txt";

	private static final String CODING_CCW_HCT_OR_HGB_TEST_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbtp.txt";

	private static final String CODING_CCW_HHA_REFERRAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hha_rfrl.txt";

	private static final String CODING_CCW_LOW_UTILIZATION_PAYMENT_ADJUSTMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lupaind.txt";

	private static final String CODING_CCW_MCO_PAID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mcopdsw.txt";
	
	/*
	 * FIXME: Is NDC count only ever present when line quantity isn't set?
	 * Depending on that, it may be that we should stop using this as an
	 * extension and instead set the code & system on the FHIR quantity field.
	 */
	private static final String CODING_CCW_NDC_UNIT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr_ndc_qty_qlfr_cd.txt";

	private static final String CODING_CCW_NDC_QTY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr_ndc_qty.txt";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pde_id.txt">
	 * CCW Data Dictionary: PDE_ID</a>.
	 */
	public static final String CODING_CCW_PARTD_EVENT_ID = "CCW.PDE_ID";

	private static final String CODING_CCW_PATIENT_DISCHARGE_STATUS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/stus_cd.txt";

	private static final String CODING_CCW_PATIENT_STATUS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptntstus.txt";

	private static final String CODING_CCW_PHYSICIAN_ASSISTANT_ADJUDICATION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/astnt_cd.txt";

	private static final String CODING_CCW_PLACE_OF_SERVICE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt";

	private static final String CODING_CCW_PRESENT_ON_ARRIVAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw1.txt";

	private static final String CODING_CCW_PROCESSING_INDICATOR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcngind.txt";

	private static final String CODING_CCW_PROVIDER_ASSIGNMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/asgmntcd.txt";

	private static final String CODING_CCW_PROVIDER_SPECIALTY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcfaspcl.txt";

	private static final String CODING_CCW_RECORD_ID_CODE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ric_cd.txt";

	private static final String CODING_CCW_RX_BRAND_GENERIC = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/brnd_gnrc_cd.txt";

	// FIXME this URL had a typo -- first 'c' shouldn't have been there
	private static final String CODING_CCW_TYPE_SERVICE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typcsrvcb.txt";

	private static final String CODING_CMS_DISPENSE_STATUS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dspnsng_stus_cd.txt";

	private static final String CODING_CMS_REVENUE_CENTER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr.txt";
	
	private static final String CODING_CMS_REVENUE_CENTER_STATUS_INDICATOR_CODE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/revstind.txt";

	private static final String CODING_CMS_REVENUE_CENTER_IDE_NDC_UPC_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/idendc.txt";

	private static final String CODING_CMS_RX_ADJUSTMENT_DELETION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/adjstmt_dltn_cd.txt";

	private static final String CODING_CMS_RX_CATASTROPHIC_COVERAGE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ctstrphc_cvrg_cd.txt";

	private static final String CODING_CMS_RX_COVERAGE_STATUS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt";

	private static final String CODING_CMS_RX_DISPENSE_AS_WRITTEN = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/daw_prod_slctn_cd.txt";

	private static final String CODING_CMS_RX_NON_STANDARD_FORMAT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nstd_frmt_cd.txt";

	private static final String CODING_CMS_RX_PATIENT_RESIDENCE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptnt_rsdnc_cd.txt";

	private static final String CODING_CMS_RX_PRESCRIPTION_ORIGIN = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_orgn_cd.txt";

	private static final String CODING_CMS_RX_PRICING_EXCEPTION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_excptn_cd.txt";

	private static final String CODING_CMS_RX_SUBMISSION_CLARIFICATION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/submsn_clr_cd.txt";

	private static final String CODING_CMS_SOURCE_ADMISSION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/src_adms.txt";

	// FIXME Switch to HAPI enum for this system, once one is available
	static final String CODING_FHIR_ACT = "http://hl7.org/fhir/v3/ActCode";

	// FIXME Switch to HAPI enum for this system, once one is available
	static final String CODING_FHIR_ACT_INVOICE_GROUP = "http://hl7.org/fhir/ValueSet/v3-ActInvoiceGroupCode";

	static final String CODING_FHIR_DIAGNOSIS_TYPE = "http://hl7.org/fhir/ex-diagnosistype";

	static final String CODING_FHIR_BENEFIT_BALANCE = BenefitCategory.MEDICAL.getSystem();

	static final String CODING_FHIR_CARE_TEAM_ROLE = ClaimCareteamrole.PRIMARY.getSystem();

	static final String CODING_FHIR_CLAIM_TYPE = ClaimType.PROFESSIONAL.getSystem();
	
	/**
	 * A CMS-controlled standard. More info here: <a href=
	 * "https://en.wikipedia.org/wiki/Healthcare_Common_Procedure_Coding_System">
	 * Healthcare Common Procedure Coding System</a>.
	 */
	static final String CODING_HCPCS = "https://www.cms.gov/Medicare/Coding/MedHCPCSGenInfo/index.html";

	static final String CODING_MONEY = "urn:std:iso:4217";
	
	private static final String CODING_NCH_CLAIM_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_type.txt";
	
	static final String CODING_NDC = "https://www.accessdata.fda.gov/scripts/cder/ndc";

	/**
	 * The United States National Provider Identifier, as available at
	 * <a href="http://download.cms.gov/nppes/NPI_Files.html">NPI/NPPES File</a>
	 * .
	 */
	static final String CODING_NPI_US = "http://hl7.org/fhir/sid/us-npi";

	static final String COVERAGE_ISSUER = "Centers for Medicare and Medicaid Services";

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

	private static final String EXTENSION_CMS_HCT_OR_HGB_RESULTS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbrs.txt";

	private static final String EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtdnlcd.txt";

	private static final String EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typesrvc.txt";

	private static final String EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/revdedcd.txt";

	private static final String EXTENSION_CODING_CCW_ESRD_INDICATOR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/esrd_ind.txt";

	private static final String EXTENSION_CODING_CCW_FACILITY_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fac_type.txt";
	
	private static final String EXTENSION_CODING_CCW_FI_NUM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fi_num.txt";

	private static final String EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_sw.txt";
	
	private static final String EXTENSION_CODING_CCW_LINE_SRVC_CNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/srvc_cnt.txt";

	private static final String EXTENSION_CODING_CCW_MEDICARE_ENTITLEMENT_CURRENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/crec.txt";

	private static final String EXTENSION_CODING_CCW_MEDICARE_ENTITLEMENT_ORIGINAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orec.txt";

	private static final String EXTENSION_CODING_CCW_MEDICARE_STATUS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ms_cd.txt";

	private static final String EXTENSION_CODING_CCW_PARTA_TERMINATION_CODE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/a_trm_cd.txt";

	private static final String EXTENSION_CODING_CCW_PARTB_TERMINATION_CODE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/b_trm_cd.txt";

	private static final String EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtindsw.txt";

	private static final String EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nopay_cd.txt";

	private static final String EXTENSION_CODING_CCW_PRICING_LOCALITY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lclty_cd.txt";

	private static final String EXTENSION_CODING_CCW_PRICING_STATE_CD = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_st.txt";
	
	private static final String EXTENSION_CODING_CCW_PROVIDER_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/suplrnum.txt";

	private static final String EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prtcptg.txt";

	private static final String EXTENSION_CODING_CCW_PROVIDER_STATE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvstate.txt";

	private static final String EXTENSION_CODING_CCW_PROVIDER_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prv_type.txt";

	private static final String EXTENSION_CODING_CCW_PROVIDER_ZIP = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provzip.txt";

	private static final String EXTENSION_CODING_CLAIM_QUERY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/query_cd.txt";

	private static final String EXTENSION_CODING_CMS_RX_PHARMACY_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/phrmcy_srvc_type_cd.txt";

	private static final String EXTENSION_CODING_CMS_SUPPLIER_TYPE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sup_type.txt";

	// FIXME: verify this link is correct for this extension coding
	private static final String EXTENSION_CODING_HOSPITALIZATION_PERIOD_COUNT = "https://bluebutton.cms.gov/resources/hospcprd";

	private static final String EXTENSION_CODING_UNIT_IND = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/unit_ind.txt";
	
	private static final String EXTENSION_CODING_MTUS_IND = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mtus_ind.txt";

	private static final String EXTENSION_CODING_PRIMARY_PAYER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpay_cd.txt";

	private static final String EXTENSION_CODING_CARRIER_PRIMARY_PAYER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lprpaycd.txt";

	private static final String EXTENSION_IDENTIFIER_CARRIER_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_num.txt";

	private static final String EXTENSION_IDENTIFIER_CCW_CLIA_LAB_NUM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_line_clia_lab_num.txt";

	private static final String EXTENSION_IDENTIFIER_CARR_LINE_ANSTHSA_UNIT_CNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_line_ansthsa_unit_cnt.txt";

	private static final String EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ccltrnum.txt";

	// FIXME: needs to link to an explanation of what this extension coding is
	private static final String EXTENSION_IDENTIFIER_DME_PROVIDER_BILLING_NUMBER = "https://bluebutton.cms.gov/resources/suplrnum";

	private static final String EXTENSION_IDENTIFIER_PDE_PLAN_BENEFIT_PACKAGE_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_pbp_rec_num.txt";

	private static final String EXTENSION_IDENTIFIER_PDE_PLAN_CONTRACT_ID = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_cntrct_rec_id.txt";

	private static final String EXTENSION_DME_UNIT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dme_unit.txt";
	
	private static final String EXTENSION_MTUS_CNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mtus_cnt.txt";

	private static final String EXTENSION_SCREEN_SAVINGS = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/scrnsvgs.txt";

	// FIXME this constant will go away once its usage sites are fixed
	private static final String FIELD_PDE_DAYS_SUPPLY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/days_suply_num.txt";
	
	private static final String PDE_FILL_NUM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fill_num.txt";

	private static final String IDENTIFIER_CMS_PROVIDER_NUMBER = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provider.txt";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_srvc_rfrnc_num.txt">
	 * CCW Data Dictionary: RX_SRVC_RFRNC_NUM</a>.
	 */
	static final String IDENTIFIER_RX_SERVICE_REFERENCE_NUMBER = "CCW.RX_SRVC_RFRNC_NUM";
}
