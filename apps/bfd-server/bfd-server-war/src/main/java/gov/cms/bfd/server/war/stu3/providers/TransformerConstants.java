package gov.cms.bfd.server.war.stu3.providers;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.CarrierClaimColumn;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.GroupComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.codesystems.Adjudication;

/**
 * Contains all of the shared constants used to transform CCW JPA entities (e.g. {@link
 * Beneficiary}) into FHIR resources (e.g. {@link Patient}).
 *
 * <h3>Naming Conventions</h3>
 *
 * <p>This is a monster list of constants. To keep it somewhat manageable, the following naming
 * conventions must be used for the constants:
 *
 * <ol>
 *   <li>If the constant value is used as a FHIR extension URL (see {@link
 *       Extension#setUrl(String)}), the constant must be prefixed with <code>EXTENSION_</code>.
 *   <li>If the constant value is used as a FHIR coding system (see {@link
 *       Coding#setSystem(String)}):, the constant must be prefixed with <code>CODING_</code>.
 *       <ol>
 *         <li>If the codeset is part of the Medicare billing standards, the constant must be
 *             further prefixed with <code>CMS_</code>.
 *         <li>If the codeset is only used with the Chronic Conditions Warehouse, the constant must
 *             be further prefixed with <code>CCW_</code>.
 *         <li>If the codeset is only used with the Blue Button API, the constant must be further
 *             prefixed with <code>BBAPI_</code>.
 *       </ol>
 *   <li>If the constant is used as a code in a FHIR coding (see {@link Coding#setCode(String)}),
 *       the constant must be prefixed with <code>CODED_</code> and the same suffix as the constant
 *       for the coding's system constant.
 *   <li>If the constant value is used as a FHIR identifier system (see {@link
 *       Identifier#setSystem(String)}), the constant must be prefixed with <code>IDENTIFIER_</code>
 *       .
 * </ol>
 *
 * <h3>FHIR Extension URLs and Coding System URIs</h3>
 *
 * <p>Every FHIR {@link Extension} must have an {@link Extension#getUrl()} value that applications
 * can use to identify/select the field. Similarly, every FHIR {@link Coding} must have a {@link
 * Coding#getSystem()} URI value that applications can use to identify the codeset.
 *
 * <p>To try and achieve some consistency in our API, the following guidelines must be adhered to
 * when adding new {@link Extension#getUrl()}s and {@link Coding#getSystem()} URIs to our
 * application:
 *
 * <ol>
 *   <li>If FHIR has a published URL/URI for it already, that must be used (e.g. {@link
 *       #CODING_NPI_US}).
 *   <li>All other extension URLs and coding URIs must be set to the URL of a public web page
 *       controlled by the Blue Button API team, where links and/or documentation for the item in
 *       question can be posted. This is necessary to ensure the API remains appropriately
 *       documented and usable in the future. TODO file JIRA ticket to switch all existing URLs/URIs
 * </ol>
 */
public final class TransformerConstants {
  /**
   * The base URL/URI/system for FHIR output when the domain concept in question is owned by (or at
   * least documented by) the Blue Button API team. Please note that the documentation on this site
   * is generated from the following project: <a href=
   * "https://github.com/CMSgov/bluebutton-site-static">bluebutton-site-static</a>.
   *
   * <p>This URL will never be used by itself; it will always be suffixed with a more specific path.
   */
  static final String BASE_URL_BBAPI_RESOURCES = "https://bluebutton.cms.gov/resources";

  /**
   * The base URL/URI/system for FHIR output related to {@link CcwCodebookVariable}:
   *
   * <ul>
   *   <li>{@link Extension#getUrl()}
   *   <li>{@link Coding#getSystem()}
   *   <li>{@link Identifier#getSystem()}
   * </ul>
   *
   * <p>This URL will never be used by itself; it will always be suffixed with the (lower-cased)
   * {@link CcwCodebookVariable#getVariable()}'s {@link Variable#getId()}.
   */
  static final String BASE_URL_CCW_VARIABLES = BASE_URL_BBAPI_RESOURCES + "/variables";

  static final String CODED_MONEY_USD = "USD";

  static final String CODING_BBAPI_BENEFIT_BALANCE_TYPE =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/benefit-balance";

  /** The CMS-custom {@link Coding#getSystem()} value for Medicare {@link Adjudication}s. */
  static final String CODING_CCW_ADJUDICATION_CATEGORY =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/adjudication";

  /**
   * The CMS-custom {@link Coding#getSystem()} value for Medicare {@link
   * SupportingInformationComponent#getCategory()}s.
   */
  static final String CODING_BBAPI_INFORMATION_CATEGORY =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/information";

  /**
   * Used as the {@link Identifier#getSystem()} that the RIF <code>CLM_GROUP_ID</code> fields (e.g.
   * {@link CarrierClaimColumn#CLM_GRP_ID}) are mapped to.
   */
  static final String IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID =
      BASE_URL_BBAPI_RESOURCES + "/identifier/claim-group";

  /**
   * Used as the {@link Coding#getSystem()} for the {@link ExplanationOfBenefit#getType()} entry
   * that each EOB's ClaimType is mapped to.
   */
  static final String CODING_SYSTEM_BBAPI_EOB_TYPE =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/eob-type";

  // FIXME this URL has a typo -- first 'c' shouldn't have been there
  private static final String CODING_CCW_TYPE_SERVICE =
      "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typcsrvcb.txt";

  /** Used as the {@link Coding#getSystem()} for {@link DiagnosisComponent#getType()} entries. */
  static final String CODING_SYSTEM_BBAPI_DIAGNOSIS_TYPE =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/diagnosis-type";

  /**
   * Used as the {@link Coding#getSystem()} for {@link ItemComponent#getService()} and {@link
   * ItemComponent#getModifier()}. (This is used instead of {@link CcwCodebookVariable#HCPCS_CD} so
   * that we can provide some extra helpful documentation at the URL.)
   */
  static final String CODING_SYSTEM_HCPCS = BASE_URL_BBAPI_RESOURCES + "/codesystem/hcpcs";

  /**
   * Used as the {@link Coding#getSystem()} for determining the currency of an {@link Identifier}.
   */
  static final String CODING_SYSTEM_IDENTIFIER_CURRENCY =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/identifier-currency";

  /**
   * The standard {@link Money#getSystem()} for currency. (It looks odd that it has "iso" in there
   * twice, but some web searches seem to confirm that that's correct.)
   */
  static final String CODING_MONEY = "urn:iso:std:iso:4217";

  /**
   * Used to identify the drugs that were purchased as part of Part D, Carrier, and DME claims. See
   * here for more information on using NDC codes with FHIR: <a
   * href="http://hl7.org/fhir/ndc.html">10 Using NDC and NHRIC Codes with FHIR</a>.
   */
  static final String CODING_NDC = "http://hl7.org/fhir/sid/ndc";

  /**
   * The United States National Provider Identifier, as available at <a
   * href="http://download.cms.gov/nppes/NPI_Files.html">NPI/NPPES File</a> .
   */
  static final String CODING_NPI_US = "http://hl7.org/fhir/sid/us-npi";

  /**
   * The {@link Coding#getSystem()} for "The Unified Code for Units of Mesaure (UCUM)", a
   * standardized coding system for basic units of measure.
   */
  static final String CODING_SYSTEM_UCUM = "http://unitsofmeasure.org";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} for "pint". */
  static final String CODING_SYSTEM_UCUM_PINT_CODE = "[pt_us]";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getDisplay()} for "pint". */
  static final String CODING_SYSTEM_UCUM_PINT_DISPLAY = "pint";

  static final String COVERAGE_ISSUER = "Centers for Medicare and Medicaid Services";

  static final String COVERAGE_PLAN = "Medicare";

  /** The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()} value for Part A. */
  public static final String COVERAGE_PLAN_PART_A = "Part A";

  /** The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()} value for Part B. */
  public static final String COVERAGE_PLAN_PART_B = "Part B";

  /** The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()} value for Part C. */
  public static final String COVERAGE_PLAN_PART_C = "Part C";

  /** The {@link Coverage#getGrouping()} {@link GroupComponent#getSubPlan()} value for Part D. */
  public static final String COVERAGE_PLAN_PART_D = "Part D";

  /**
   * The {@link Identifier#getSystem()} used in {@link Patient} resources to store a one-way
   * cryptographic hash of each Medicare beneficiaries' HICN. Note that, with the SSNRI initiative,
   * CMS is planning to move away from HICNs. However, HICNs are still the primary/only Medicare
   * identifier for now.
   */
  public static final String CODING_BBAPI_BENE_HICN_HASH =
      BASE_URL_BBAPI_RESOURCES + "/identifier/hicn-hash";

  /**
   * The {@link Identifier#getSystem()} used in {@link Patient} resources to store the unhashed
   * version of each Medicare beneficiaries' HICN.
   */
  public static final String CODING_BBAPI_BENE_HICN_UNHASHED =
      "http://hl7.org/fhir/sid/us-medicare";

  /**
   * The {@link Identifier#getSystem()} used in {@link Patient} resources to store the unhashed
   * version of each Medicare beneficiaries' medicare beneficiary id.
   */
  public static final String CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED =
      "http://hl7.org/fhir/sid/us-mbi";

  /**
   * The {@link #CODING_BBAPI_BENE_HICN_HASH} used in earlier versions of the API, which is still
   * supported by {@link PatientResourceProvider} for backwards compatibility reasons.
   */
  static final String CODING_BBAPI_BENE_HICN_HASH_OLD =
      "http://bluebutton.cms.hhs.gov/identifier#hicnHash";
}
