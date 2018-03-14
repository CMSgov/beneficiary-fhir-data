package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.GroupComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.codesystems.Adjudication;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;

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

	// FIXME also used as a benefit balance
	static final String CODED_ADJUDICATION_ALLOWED_CHARGE = "Allowed Charge Amount";

	// FIXME also used as a benefit balance
	static final String CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT = "Payment Amount to Beneficiary";

	// FIXME only used as a benefit balance
	static final String CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE = "NCH Beneficiary Part B Deductible Amount";

	// FIXME only used as a benefit balance
	static final String CODED_ADJUDICATION_DISPROPORTIONATE_SHARE_AMOUNT = "Disproportionate Share Amount";

	// FIXME only used as a benefit balance
	static final String CODED_ADJUDICATION_INDIRECT_MEDICAL_EDUCATION_AMOUNT = "Indirect Medical Education Amount";

	// FIXME also used as a benefit balance
	static final String CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT = "Primary Payer Paid Amount";

	// FIXME also used as a benefit balance
	static final String CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT = "Provider Payment Amount";

	// FIXME also used as a benefit balance
	static final String CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT = "Submitted Charge Amount";

	static final String CODED_BENEFIT_BALANCE_TYPE_BENE_PAYMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/benepmt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_BLOOD_DEDUCTIBLE_LIABILITY = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/blddedam.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_DRUG_OUTLIER_APPROVED_PAYMENT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/outlrpmt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PARTB_COINSURANCE_AMOUNT = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_coin.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PARTB_DEDUCTIBLE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptb_ded.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PASS_THRU_PER_DIEM = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/per_diem.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_PROFFESIONAL_COMPONENT_CHARGE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pcchgamt.txt";

	static final String CODED_BENEFIT_BALANCE_TYPE_TOTAL_PPS_CAPITAL = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pps_cptl.txt";

	public static final String CODED_BENEFIT_COVERAGE_DATE_QUALIFIED = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qlfrfrom.txt";

	static final String CODED_MONEY_USD = "USD";

	static final String CODING_BBAPI_BENEFIT_BALANCE_TYPE = BASE_URL_BBAPI_RESOURCES + "/codesystem/benefit-balance";

	static final String CODING_BBAPI_BENEFIT_COVERAGE_DATE = "Benefit Coverage Date";

	/**
	 * The CMS-custom {@link Coding#getSystem()} value for Medicare
	 * {@link Adjudication}s.
	 */
	static final String CODING_CCW_ADJUDICATION_CATEGORY = BASE_URL_BBAPI_RESOURCES + "/codesystem/adjudication";

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

	/**
	 * Used as the {@link Coding#getSystem()} for {@link ItemComponent#getService()}
	 * and {@link ItemComponent#getModifier()}. (This is used instead of
	 * {@link CcwCodebookVariable#HCPCS_CD} so that we can provide some extra
	 * helpful documentation at the URL.)
	 */
	static final String CODING_SYSTEM_HCPCS = BASE_URL_BBAPI_RESOURCES + "/codesystem/hcpcs";

	static final String CODING_MONEY = "urn:std:iso:4217";
	
	/**
	 * Used to identify the drugs that were purchased as part of Part D, Carrier,
	 * and DME claims. See here for more information on using NDC codes with FHIR:
	 * <a href="http://hl7.org/fhir/ndc.html">10 Using NDC and NHRIC Codes with
	 * FHIR</a>.
	 */
	static final String CODING_NDC = "http://hl7.org/fhir/sid/ndc";

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
}
