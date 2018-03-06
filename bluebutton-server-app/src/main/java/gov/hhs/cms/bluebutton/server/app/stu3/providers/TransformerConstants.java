package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.GroupComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
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
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimColumn;

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

	static final String CODED_ADJUDICATION_1ST_ANSI_CD = "Revenue Center 1st ANSI Code";

	static final String CODED_ADJUDICATION_1ST_MSP_AMOUNT = "Revenue Center 1st Medicare Secondary Payer (MSP) Paid Amount";

	static final String CODED_ADJUDICATION_2ND_ANSI_CD = "Revenue Center 2nd ANSI Code";

	static final String CODED_ADJUDICATION_2ND_MSP_AMOUNT = "Revenue Center 2nd Medicare Secondary Payer (MSP) Paid Amount";

	static final String CODED_ADJUDICATION_3RD_ANSI_CD = "Revenue Center 3rd ANSI Code";

	static final String CODED_ADJUDICATION_4TH_ANSI_CD = "Revenue Center 4th ANSI Code";

	static final String CODED_ADJUDICATION_ALLOWED_CHARGE = "Allowed Charge Amount";

	static final String CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT = "Payment Amount to Beneficiary";

	static final String CODED_ADJUDICATION_BLOOD_DEDUCTIBLE = "Blood Deductible Amount";

	static final String CODED_ADJUDICATION_CASH_DEDUCTIBLE = "Cash Deductible Amount";

	static final String CODED_ADJUDICATION_DEDUCTIBLE = "Beneficiary Deductible Amount";

	static final String CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT = "Medicare Coverage Gap Discount Amount";

	static final String CODED_ADJUDICATION_GDCA = "Gross Drug Cost Above Out-of-Pocket Threshold (GDCA)";

	static final String CODED_ADJUDICATION_GDCB = "Gross Drug Cost Below Out-of-Pocket Threshold (GDCB)";

	static final String CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT = "Coinsurance Amount";

	static final String CODED_ADJUDICATION_LINE_PRIMARY_PAYER_ALLOWED_CHARGE = "Primary Payer Allowed Charge Amount";

	static final String CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT = "Purchase Price Amount";

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lics_amt.txt">
	 * CCW Data Dictionary: LICS_AMT</a>.
	 */
	static final String CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT = "Part D Low Income Subsidy (LICS) Amount";

	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE = "NCH Beneficiary Part B Deductible Amount";

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

	static final String CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT = "Primary Payer Paid Amount";

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

	static final String CODED_ADJUDICATION_WAGE_ADJ_COINSURANCE_AMOUNT = "Wage Adj Coinsurance Amount";

	static final String CODED_BENEFIT_BALANCE_TYPE_BENE_PAYMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/benepmt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_BLOOD_DEDUCTIBLE_LIABILITY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/blddedam.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_DRUG_OUTLIER_APPROVED_PAYMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/outlrpmt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PARTB_COINSURANCE_AMOUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_coin.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PARTB_DEDUCTIBLE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_ded.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PASS_THRU_PER_DIEM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/per_diem.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PROFFESIONAL_COMPONENT_CHARGE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pcchgamt.txt";

	public static final String CODED_BENEFIT_BALANCE_TYPE_SYSTEM_UTILIZATION_DAY_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/util_day.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_TOTAL_PPS_CAPITAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pps_cptl.txt";

	public static final String CODED_BENEFIT_BALANCE_TYPE_VISIT_COUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/visitcnt.txt";

	public static final String CODED_BENEFIT_COVERAGE_DATE_QUALIFIED = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qlfrfrom.txt";

	static final String CODED_MONEY_USD = "USD";

	static final String CODING_BBAPI_BENEFIT_BALANCE_TYPE = "http://bluebutton.cms.hhs.gov/coding#benefitBalanceType";

	static final String CODING_BBAPI_BENEFIT_COVERAGE_DATE = "Benefit Coverage Date";

	/**
	 * The CMS-custom {@link Coding#getSystem()} value for Medicare
	 * {@link Adjudication}s.
	 */
	static final String CODING_CCW_ADJUDICATION_CATEGORY = "CMS Adjudications";

	/**
	 * Used as the {@link Identifier#getSystem()} that the RIF
	 * <code>CLM_GROUP_ID</code> fields (e.g. {@link CarrierClaimColumn#CLM_GRP_ID})
	 * are mapped to.
	 */
	static final String IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID = BASE_URL_BBAPI_RESOURCES + "/identifier/claim-group";

	/**
	 * Used as the {@link Coding#getSystem()} for the
	 * {@link ExplanationOfBenefit#getType()} entry that each EOB's ClaimType is
	 * mapped to.
	 */
	static final String CODING_SYSTEM_BBAPI_EOB_TYPE = BASE_URL_BBAPI_RESOURCES + "/codesystem/eob-type";

	// FIXME this URL has a typo -- first 'c' shouldn't have been there
	private static final String CODING_CCW_TYPE_SERVICE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typcsrvcb.txt";

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

	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_srvc_rfrnc_num.txt">
	 * CCW Data Dictionary: RX_SRVC_RFRNC_NUM</a>.
	 */
	static final String IDENTIFIER_RX_SERVICE_REFERENCE_NUMBER = "CCW.RX_SRVC_RFRNC_NUM";
}
