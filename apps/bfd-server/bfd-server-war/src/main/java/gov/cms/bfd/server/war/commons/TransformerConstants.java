package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.CarrierClaimColumn;
import gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
import org.hl7.fhir.r4.model.Organization;

/**
 * Contains all of the shared constants used to transform CCW JPA entities (e.g. {@link
 * Beneficiary}) into FHIR resources (e.g. {@link Patient}).
 *
 * <h2>Naming Conventions</h2>
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
  public static final String BASE_URL_BBAPI_RESOURCES = "https://bluebutton.cms.gov/resources";

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
  public static final String BASE_URL_CCW_VARIABLES = BASE_URL_BBAPI_RESOURCES + "/variables";

  /** Base URL for nubc. */
  public static final String BASE_URL_NUBC = "https://www.nubc.org";

  /** Base URL for nucc. */
  public static final String BASE_URL_NUCC = "http://nucc.org";

  /** System for NUCC taxonomy. */
  public static final String NUCC_TAXONOMY = "/provider-taxonomy";

  /** Full system for NUBC taxonomy. */
  public static final String NUCC_TAXONOMY_SYSTEM = BASE_URL_NUCC + NUCC_TAXONOMY;

  /** System for NUBC admsn. */
  public static final String NUBC_ADMIT_CODE_SYSTEM =
      BASE_URL_NUBC + "/CodeSystem/PriorityTypeOfAdmitOrVisit";

  /** Constant for Carin BB base url. */
  public static final String BASE_URL_CARIN_BB = "http://hl7.org/fhir/us/carin-bb";

  /** Constant for Carin BB Information Category. */
  public static final String CODING_CARIN_BB_INFORMATION_CATEGORY =
      BASE_URL_CARIN_BB + "/CodeSystem/C4BBSupportingInfoType";

  /** Constant for Carin BB admntype code. */
  public static final String CARIN_BB_ADMN_TYPE = "admtype";

  /** Maps a CCW variable for a category to a new system. */
  public static final Map<String, String> CARIN_CATEGORY_SYSTEM_MAP =
      new HashMap<>() {
        {
          put("clm_ip_admsn_type_cd", CODING_CARIN_BB_INFORMATION_CATEGORY);
        }
      };

  /** Maps a CCW variable for a category to a new code. */
  public static final Map<String, String> CARIN_CATEGORY_CODE_MAP =
      new HashMap<>() {
        {
          put("clm_ip_admsn_type_cd", CARIN_BB_ADMN_TYPE);
        }
      };

  /** The constant for the USD code. */
  public static final String CODED_MONEY_USD = "USD";

  /** For custom [Resource].meta.tags entries. */
  public static final String CODING_SYSTEM_BFD_TAGS = BASE_URL_BBAPI_RESOURCES + "/codesystem/tags";

  /** The type value for benefit balance code set. */
  public static final String CODING_BBAPI_BENEFIT_BALANCE_TYPE =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/benefit-balance";

  /** The CMS-custom {@link Coding#getSystem()} value for Medicare {@link Adjudication}s. */
  public static final String CODING_CCW_ADJUDICATION_CATEGORY =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/adjudication";

  /**
   * The CMS-custom {@link Coding#getSystem()} value for Medicare {@link
   * SupportingInformationComponent#getCategory()}s.
   */
  public static final String CODING_BBAPI_INFORMATION_CATEGORY =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/information";

  /**
   * Used as the {@link Identifier#getSystem()} that the RIF <code>CLM_GROUP_ID</code> fields (e.g.
   * {@link CarrierClaimColumn#CLM_GRP_ID}) are mapped to.
   */
  public static final String IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID =
      BASE_URL_BBAPI_RESOURCES + "/identifier/claim-group";

  /**
   * Used as the {@link Coding#getSystem()} for the {@link ExplanationOfBenefit#getType()} entry
   * that each EOB's ClaimType is mapped to.
   */
  public static final String CODING_SYSTEM_BBAPI_EOB_TYPE =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/eob-type";

  /**
   * The constant CODING_CCW_TYPE_SERVICE.
   *
   * <p>FIXME this URL has a typo -- first 'c' shouldn't have been there
   *
   * <p>No longer in use; should this be removed?
   */
  private static final String CODING_CCW_TYPE_SERVICE =
      "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typcsrvcb.txt";

  /** Used as the {@link Coding#getSystem()} for {@link DiagnosisComponent#getType()} entries. */
  public static final String CODING_SYSTEM_BBAPI_DIAGNOSIS_TYPE =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/diagnosis-type";

  /**
   * Used as the {@link Coding#getSystem()} for {@link ItemComponent#getService()} and {@link
   * ItemComponent#getModifier()}. (This is used instead of {@link CcwCodebookVariable#HCPCS_CD} so
   * that we can provide some extra helpful documentation at the URL.)
   */
  public static final String CODING_SYSTEM_HCPCS = BASE_URL_BBAPI_RESOURCES + "/codesystem/hcpcs";

  /** Used as the {@link Coding#getSystem()} for {@link ItemComponent} type entries. */
  public static final String CODING_SYSTEM_CARIN_HCPCS =
      "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets";

  /** Used as the {@link Coding#getSystem()} for CPT. */
  public static final String CODING_SYSTEM_CPT = "http://www.ama-assn.org/go/cpt";

  /**
   * Used as the {@link Coding#getSystem()} for determining the currency of an {@link Identifier}.
   */
  public static final String CODING_SYSTEM_IDENTIFIER_CURRENCY =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/identifier-currency";

  /**
   * The standard {@link Money#getSystem()} for currency. (It looks odd that it has "iso" in there
   * twice, but some web searches seem to confirm that that's correct.)
   */
  public static final String CODING_MONEY = "urn:iso:std:iso:4217";

  /**
   * Used to identify the drugs that were purchased as part of Part D, Carrier, and DME claims. See
   * here for more information on using NDC codes with FHIR: <a
   * href="http://hl7.org/fhir/ndc.html">10 Using NDC and NHRIC Codes with FHIR</a>.
   */
  public static final String CODING_NDC = "http://hl7.org/fhir/sid/ndc";

  /**
   * The United States National Provider Identifier, as available at <a
   * href="http://download.cms.gov/nppes/NPI_Files.html">NPI/NPPES File</a> .
   */
  public static final String CODING_NPI_US = "http://hl7.org/fhir/sid/us-npi";

  /**
   * The standard {@link Coding#getCode()} for {@link Identifier#getType()} entries where the
   * identifier is a NPI.
   */
  public static final String CODED_IDENTIFIER_TYPE_NPI = "NPI";

  /**
   * The standard {@link Coding#getDisplay()} for {@link Identifier#getType()} entries where the
   * identifier is a NPI.
   */
  public static final String CODED_IDENTIFIER_TYPE_NPI_DISPLAY = "National Provider Identifier";

  /** System for encoding UPIN values. */
  public static final String CODING_UPIN =
      "http://hl7.org/fhir/us/carin-bb/ValueSet/C4BBPractitionerIdentifierType";

  /**
   * The standard {@link Coding#getCode()} for {@link Identifier#getType()} entries where the
   * identifier is a UPIN.
   */
  public static final String CODED_IDENTIFIER_TYPE_UPIN = "UPIN";

  /**
   * The standard {@link Coding#getDisplay()} for {@link Identifier#getType()} entries where the
   * identifier is a UPIN.
   */
  public static final String CODED_IDENTIFIER_TYPE_UPIN_DISPLAY =
      "Unique Physician Identification Number";

  /**
   * The Code System, as available at <a
   * href="https://terminology.hl7.org/1.0.0/CodeSystem-v2-0203.html">identifierType</a>.
   */
  public static final String CODING_SYSTEM_HL7_IDENTIFIER_TYPE =
      "http://terminology.hl7.org/CodeSystem/v2-0203";

  /**
   * Used as the {@link Coding#getSystem()} for custom-to-BFD {@link Identifier#getType()} codes.
   */
  public static final String CODING_SYSTEM_IDENTIFIER_TYPE =
      BASE_URL_BBAPI_RESOURCES + "/codesystem/identifier-type";

  /**
   * The standard {@link Coding#getCode()} for {@link Identifier#getType()} entries where the
   * identifier is a PDP.
   */
  public static final String CODED_IDENTIFIER_TYPE_PDP = "NCPDP";

  /**
   * The standard {@link Coding#getDisplay()} for {@link Identifier#getType()} entries where the
   * identifier is a PDP.
   */
  public static final String CODED_IDENTIFIER_TYPE_PDP_DISPLAY =
      "National Council for Prescription Drug Programs";

  /**
   * The standard {@link Coding#getCode()} for {@link Identifier#getType()} entries where the
   * identifier is a DL.
   */
  public static final String CODED_IDENTIFIER_TYPE_DL = "DL";

  /**
   * The standard {@link Coding#getDisplay()} for {@link Identifier#getType()} entries where the
   * identifier is a DL.
   */
  public static final String CODED_IDENTIFIER_TYPE_DL_DISPLAY = "State license number";

  /**
   * The standard {@link Coding#getCode()} for {@link Identifier#getType()} entries where the
   * identifier is a TAX.
   */
  public static final String CODED_IDENTIFIER_TYPE_TAX = "TAX";

  /**
   * The standard {@link Coding#getDisplay()} for {@link Identifier#getType()} entries where the
   * identifier is a TAX.
   */
  public static final String CODED_IDENTIFIER_TYPE_TAX_DISPLAY = "Tax ID number";

  /**
   * The standard {@link Coding#getCode()} for {@link Identifier#getType()} entries where the
   * identifier is an MC.
   */
  public static final String CODED_IDENTIFIER_TYPE_MC = "MC";

  /**
   * The standard {@link Coding#getDisplay()} for {@link Identifier#getType()} entries where the
   * identifier is an MC.
   */
  public static final String CODED_IDENTIFIER_TYPE_MC_DISPLAY = "Patient's Medicare Number";

  /**
   * The {@link Coding#getSystem()} for "The Unified Code for Units of Measure (UCUM)", a
   * standardized coding system for basic units of measure.
   */
  public static final String CODING_SYSTEM_UCUM = "http://unitsofmeasure.org";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} for "pint". */
  public static final String CODING_SYSTEM_UCUM_PINT_CODE = "[pt_us]";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getDisplay()} for "pint". */
  public static final String CODING_SYSTEM_UCUM_PINT_DISPLAY = "pint";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} for "International Unit". */
  public static final String CODING_SYSTEM_UCUM_F2_CODE = "[IU]";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode() constant} for "International Unit". */
  public static final String CODING_SYSTEM_UCUM_F2 = "F2";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getDisplay()} for "International Unit". */
  public static final String CODING_SYSTEM_UCUM_F2_DISPLAY = "International Unit";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} for "Gram". */
  public static final String CODING_SYSTEM_UCUM_GR_CODE = "g";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getDisplay()} for "Gram". */
  public static final String CODING_SYSTEM_UCUM_GR_DISPLAY = "Gram";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} constant for "Gram". */
  public static final String CODING_SYSTEM_UCUM_GR = "GR";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} for "Milliliter". */
  public static final String CODING_SYSTEM_UCUM_ML_CODE = "mL";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getDisplay()} for "Milliliter". */
  public static final String CODING_SYSTEM_UCUM_ML_DISPLAY = "Milliliter";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} constant for "Milliliter". */
  public static final String CODING_SYSTEM_UCUM_ML = "ML";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} for "Milligram". */
  public static final String CODING_SYSTEM_UCUM_ME_CODE = "mg";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getDisplay()} for "Milligram". */
  public static final String CODING_SYSTEM_UCUM_ME_DISPLAY = "Milligram";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} constant for "Milligram". */
  public static final String CODING_SYSTEM_UCUM_ME = "ME";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} for "Unit". */
  public static final String CODING_SYSTEM_UCUM_UN_CODE = "[arb'U]";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getDisplay()} for "Unit". */
  public static final String CODING_SYSTEM_UCUM_UN_DISPLAY = "Unit";

  /** The {@link #CODING_SYSTEM_UCUM} {@link Coding#getCode()} constant for "Unit". */
  public static final String CODING_SYSTEM_UCUM_UN = "UN";

  /**
   * Code System URL for Data Absent <a
   * href="http://hl7.org/fhir/StructureDefinition/data-absent-reason">Extension: Data Absent
   * Reason</a>.
   */
  public static final String CODING_DATA_ABSENT =
      "http://hl7.org/fhir/StructureDefinition/data-absent-reason";

  /** The {@link #CODING_DATA_ABSENT} {@link Coding#getCode()} for "Data Absent Reason". */
  public static final String DATA_ABSENT_REASON_NULL_CODE = "NULL";

  /** The {@link #CODING_DATA_ABSENT} {@link Coding#getDisplay()} for "Data Absent Reason". */
  public static final String DATA_ABSENT_REASON_DISPLAY = "Data Absent Reason";

  /** Used to set the coverage issuer for CMS. */
  public static final String COVERAGE_ISSUER = "Centers for Medicare and Medicaid Services";

  /** Used to set the coverage plan for Medicare. */
  public static final String COVERAGE_PLAN = "Medicare";

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
   * The {@link Identifier#getSystem()} used in {@link Patient} resources to store a one-way
   * cryptographic hash of each Medicare beneficiaries' MBI.
   */
  public static final String CODING_BBAPI_BENE_MBI_HASH =
      BASE_URL_BBAPI_RESOURCES + "/identifier/mbi-hash";

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
  public static final String CODING_BBAPI_BENE_HICN_HASH_OLD =
      "http://bluebutton.cms.hhs.gov/identifier#hicnHash";

  /**
   * The {@link Identifier#getSystem()} used in {@link Patient} resources to store the beneficiaryId
   * (BENE_ID).
   */
  public static final String CODING_BBAPI_BENE_ID = BASE_URL_BBAPI_RESOURCES + "/variables/bene_id";

  /**
   * Fallback value to use when a record does not have a lastUpdated value. These records where
   * loaded before the lastUpdated feature was in place.
   */
  public static final Instant FALLBACK_LAST_UPDATED = Instant.parse("2020-01-01T00:00:00Z");

  /**
   * CARIN Code System for Patient Identifier Type <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBIdentifierType.html">CodeSystem:
   * C4BB Identifier Type</a>.
   */
  public static final String CARIN_IDENTIFIER_SYSTEM =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";

  /**
   * CARIN Code System Display Value for Patient Identifier Type <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBIdentifierType.html">CodeSystem:
   * C4BB Identifier Type</a>.
   */
  public static final String PATIENT_PI_ID_DISPLAY = "Patient internal identifier";

  /**
   * CARIN Code System Display Value for Patient Identifier Type <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBIdentifierType.html">CodeSystem:
   * C4BB Identifier Type</a>.
   */
  public static final String PATIENT_MR_ID_DISPLAY = "Medical record number";

  /**
   * CARIN Code System Display Value for Patient Identifier Type <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBIdentifierType.html">CodeSystem:
   * C4BB Identifier Type</a>.
   */
  public static final String PATIENT_MC_ID_DISPLAY = "Patient's Medicare number";

  /**
   * CARIN Code System Display Value for Member Identifier Type <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBIdentifierType.html">CodeSystem:
   * C4BB Identifier Type</a>.
   */
  public static final String PATIENT_MB_ID_DISPLAY = "Member Number";

  /**
   * US Core Value Set URL for Race Category unknown <a href=
   * "https://www.hl7.org/fhir/us/core/ValueSet-omb-race-category.html">ValueSet Omb Race
   * Category</a>.
   */
  public static final String HL7_RACE_UNKNOWN_CODE = "UNK";

  /**
   * US Core Value Set URL for Race Category unknown <a
   * href="https://www.hl7.org/fhir/us/core/ValueSet-omb-race-category.html">ValueSet Omb Race
   * Category</a>.
   */
  public static final String HL7_RACE_UNKNOWN_DISPLAY = "Unknown";

  /** System for encoding UNK/null values. */
  public static final String CODING_V3_NULL = "http://terminology.hl7.org/CodeSystem/v3-NullFlavor";

  /**
   * US Core Code System URL for Race Category <a
   * href="http://hl7.org/fhir/us/core/STU3.1.1/StructureDefinition-us-core-race.html">CodeSystem:
   * US Core Race</a>.
   */
  public static final String CODING_RACE_US =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race";

  /**
   * CARIN Code System URL for Supporting Info Type <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBSupportingInfoType.html">CodeSystem:
   * C4BB Supporting Info Type</a>.
   */
  public static final String CARIN_SUPPORTING_INFO_TYPE =
      "http://terminology.hl7.org/CodeSystem/claiminformationcategory";

  /**
   * CodeSystem: C4BB Supporting Info Type <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBSupportingInfoType.html">CodeSystem:
   * C4BB Supporting Info Type</a>.
   */
  public static final String C4BB_SUPPORTING_INFO_TYPE =
      "http://hl7.org/fhir/ValueSet/claim-informationcategory";

  /**
   * Code system used for {@link ItemComponent#getRevenue()}.
   *
   * <p>ValueSet: NUBC Revenue Codes <a
   * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-AHANUBCRevenueCodes.html">ValueSet: NUBC
   * Revenue Codes</a>
   */
  public static final String NUBC_REVENUE_CODE_SYSTEM =
      "https://www.nubc.org/CodeSystem/RevenueCodes";

  /**
   * C4BB Institutional Claim SubType Code System <a
   * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBInstitutionalClaimSubType.html">CodeSystem:
   * C4BB Institutional Claim SubType Code System</a>.
   */
  public static final String C4BB_Institutional_Claim_SubType =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBInstitutionalClaimSubType";

  /** Enumerates the options for the currency of an {@link org.hl7.fhir.r4.model.Identifier}. */
  public enum CurrencyIdentifier {
    /** Represents a current identifier. */
    CURRENT,
    /** Represents a historic identifier. */
    HISTORIC;
  }

  /**
   * Code system used for {@link Organization.OrganizationContactComponent#getTelecom()}.
   *
   * <p>ValueSet: C4DIC Contact Type Codes <a
   * href="https://hl7.org/fhir/us/insurance-card/STU1.1/ValueSet-C4DICContactTypeVS.html">ValueSet:
   * C4DIC Contact Type Codes</a>
   */
  public static final String C4DIC_CONTACT_TYPE_CODE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/contactentity-type";

  /**
   * C4DIC Contact Type Value Set URL for code PAYOR <a
   * href="https://hl7.org/fhir/us/insurance-card/STU1.1/ValueSet-C4DICContactTypeVS.html">ValueSet
   * C4DIC Contact Type</a>.
   */
  public static final String C4DIC_CONTACT_TYPE_PAYOR_CODE = "PAYOR";

  /**
   * C4DIC Contact Type Value Set URL for code PAYOR <a
   * href="https://hl7.org/fhir/us/insurance-card/STU1.1/ValueSet-C4DICContactTypeVS.html">ValueSet
   * C4DIC Contact Type</a>.
   */
  public static final String C4DIC_CONTACT_TYPE_PAYOR_DISPLAY = "Payor";

  /** C4DIC Medicare Customer Service Phone Number. */
  public static final String C4DIC_MEDICARE_SERVICE_PHONE_NUMBER =
      "1-800-MEDICARE\n(1-800-633-4227)";

  /** C4DIC Medicare Customer Service TTY Number. */
  public static final String C4DIC_MEDICARE_SERVICE_TTY_NUMBER = "TTY: 1-877-486-2048";

  /** C4DIC Medicare website URL. */
  public static final String C4DIC_MEDICARE_URL = "www.medicare.gov";

  /**
   * Code System URL for C4DIC Color Palette extension <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-ColorPalette-extension">Extension:
   * C4DIC Color Palette</a>.
   */
  public static final String C4DIC_COLOR_PALETTE_EXT_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-ColorPalette-extension";

  /**
   * Code System URL for C4DIC Foreground Color <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-ForegroundColor-extension">Extension:
   * C4DIC Foreground Color</a>.
   */
  public static final String C4DIC_FOREGROUNDCOLOR_EXT_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-ForegroundColor-extension";

  /**
   * Code System for C4DIC color palette extension <a
   * href="http://terminology.hl7.org/CodeSystem/IECColourManagement">Extension: C4DIC color palette
   * extension</a>.
   */
  public static final String C4DIC_COLORS_CODE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/IECColourManagement";

  /**
   * Value for C4DIC Foreground Color <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-ForegroundColor-extension">Extension:
   * C4DIC Foreground Color</a>.
   */
  public static final String C4DIC_FOREGROUNDCOLOR = "#F4FEFF";

  /**
   * Code System URL for C4DIC Background Color <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-BackgroundColor-extension">Extension:
   * C4DIC Background Color</a>.
   */
  public static final String C4DIC_BACKGROUNDCOLOR_EXT_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-BackgroundColor-extension";

  /**
   * Value for C4DIC Background Color <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-BackgroundColor-extension">Extension:
   * C4DIC Background Color</a>.
   */
  public static final String C4DIC_BACKGROUNDCOLOR = "#092E86";

  /**
   * Code System URL for C4DIC Highlight Color <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-HighlightColor-extension">Extension:
   * C4DIC Highlight Color</a>.
   */
  public static final String C4DIC_HIGHLIGHTCOLOR_EXT_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-HighlightColor-extension";

  /**
   * Value for C4DIC Highlight Color <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-HighlightColor-extension">Extension:
   * C4DIC Highlight Color</a>.
   */
  public static final String C4DIC_HIGHLIGHTCOLOR = "#335097";

  /**
   * Code System URL for C4DIC Additional Insurance Card Information Extension URL <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-AdditionalCardInformation-extension">Extension:
   * C4DIC Additional Insurance Card Information </a>.
   */
  public static final String C4DIC_ADD_INFO_EXT_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-AdditionalCardInformation-extension";

  /**
   * Value for C4DIC Additional Insurance Card Information Extension <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-AdditionalCardInformation-extension">
   * C4DIC Additional Insurance Card Information </a>.
   */
  public static final String C4DIC_ADD_INFO =
      "You may be asked to show this card when you get health care services. Only give your personal Medicare "
          + "information to health care providers, or people you trust who work with Medicare on your behalf. "
          + "WARNING: Intentionally misusing this card may be considered fraud and/or other violation of "
          + "federal law and is punishable by law.\n"
          + "\n"
          + "Es posible que le pidan que muestre esta tarjeta cuando reciba servicios de cuidado médico. "
          + "Solamente dé su información personal de Medicare a los proveedores de salud, sus aseguradores o "
          + "personas de su confianza que trabajan con Medicare en su nombre. ¡ADVERTENCIA! El mal uso "
          + "intencional de esta tarjeta puede ser considerado como fraude y/u otra violación de la ley "
          + "federal y es sancionada por la ley.";

  /**
   * Maps a ccw variable to a specific URL. If the variable exists as a key in this map, then the
   * value will be used as the system; otherwise, it will be constructed using the above constants.
   */
  public static final Map<String, String> CCW_SYSTEM_MAP =
      Map.of(
          "clm_ip_admsn_type_cd", NUBC_ADMIT_CODE_SYSTEM,
          "prvdr_npi", CODING_NPI_US);

  /**
   * Code System URL for C4DIC Logo Extension URL <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-Logo-extension">Extension:
   * C4DIC Logo </a>.
   */
  public static final String C4DIC_LOGO_EXT_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-Logo-extension";

  /**
   * URL for C4DIC Logo <a href="https://www.hhs.gov/sites/default/files/logo-white-lg.png">C4DIC
   * Logo </a>.
   */
  public static final String C4DIC_LOGO_URL =
      "https://www.hhs.gov/sites/default/files/logo-white-lg.png";

  /**
   * URL for v3 Confidentiality Code System <a
   * href="http://terminology.hl7.org/CodeSystem/v3-Confidentiality">HL7 v3 Confidentiality Code
   * System</a>.
   */
  public static final String SAMHSA_CONFIDENTIALITY_CODE_SYSTEM_URL =
      "http://terminology.hl7.org/CodeSystem/v3-Confidentiality";

  /**
   * URL for v3 Act Code System <a href="http://terminology.hl7.org/CodeSystem/v3-ActCode">v3 Act
   * Code System</a>.
   */
  public static final String SAMHSA_ACT_CODE_SYSTEM_URL =
      "http://terminology.hl7.org/CodeSystem/v3-ActCode";

  /** US Core Sex extension code for male. */
  public static final String US_CORE_SEX_MALE = "248153007";

  /** US Core Sex extension code for female. */
  public static final String US_CORE_SEX_FEMALE = "248152002";

  /** US Core Sex extension URL. */
  public static final String US_CORE_SEX_URL =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-sex";
}
