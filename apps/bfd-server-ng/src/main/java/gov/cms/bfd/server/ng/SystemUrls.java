package gov.cms.bfd.server.ng;

/** URLs used for FHIR systems. */
public class SystemUrls {
  // Profiles

  /**
   * <a href="https://hl7.org/fhir/us/carin-bb/STU2.1/StructureDefinition-C4BB-Patient.html">C4BB
   * Patient 2.1.0.</a>
   */
  public static final String PROFILE_C4BB_PATIENT_2_1_0 =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient|2.1.0";

  /**
   * <a href="https://hl7.org/fhir/us/core/STU6.1/StructureDefinition-us-core-patient.html">US Core
   * Patient 6.1.0.</a>
   */
  public static final String PROFILE_US_CORE_PATIENT_6_1_0 =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient|6.1.0";

  public static final String PROFILE_CARIN_BB_ORGANIZATION_2_1_0 =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization|2.1.0";

  public static final String PROFILE_US_CORE_ORGANIZATION_6_1_0 =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization|6.1.0";

  // Hl7

  /**
   * <a href="https://terminology.hl7.org/1.0.0/CodeSystem-v3-NullFlavor.html">Hl7 Null Flavor.</a>
   */
  public static String HL7_NULL_FLAVOR = "http://terminology.hl7.org/CodeSystem/v3-NullFlavor";

  /** <a href="https://terminology.hl7.org/2.1.0/CodeSystem-v2-0203.html">Hl7 Identifier.</a> */
  public static final String HL7_IDENTIFIER = "http://terminology.hl7.org/CodeSystem/v2-0203";

  public static final String HL7_CLAIM_TYPE = "http://terminology.hl7.org/CodeSystem/claim-type";

  public static final String HL7_DIAGNOSIS_TYPE =
      "http://terminology.hl7.org/CodeSystem/ex-diagnosistype";

  // US Core
  /**
   * <a href="https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-ethnicity.html">US Core
   * Ethnicity.</a>
   */
  public static final String US_CORE_ETHNICITY =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity";

  /**
   * <a href="https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-race.html">US Core
   * Race.</a>
   */
  public static final String US_CORE_RACE =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race";

  /**
   * <a href="https://hl7.org/fhir/us/core/STU7/StructureDefinition-us-core-sex.html">US Core
   * Sex.</a>
   */
  public static final String US_CORE_SEX =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-sex";

  // Carin Blue Button

  public static final String CARIN_CLAIM_SUBTYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBInstitutionalClaimSubType";

  public static final String CARIN_STRUCTURE_DEFINITION_PHARMACY =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Pharmacy|2.1.0";

  public static final String CARIN_STRUCTURE_DEFINITION_INSTITUTIONAL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Inpatient-Institutional|2.1.0";

  public static final String CARIN_STRUCTURE_DEFINITION_PROFESSIONAL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Professional-NonClinician|2.1.0";

  public static final String CARIN_CODE_SYSTEM_IDENTIFIER_TYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";

  public static final String CARIN_CODE_SYSTEM_CLAIM_PROCEDURE_TYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimProcedureType";

  public static final String CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType";

  // CMS Blue Button

  public static final String BLUE_BUTTON_CLAIM_TYPE_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-TYPE-CD";

  public static final String BLUE_BUTTON_ADJUDICATION_STATUS =
      "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication-Status";

  public static final String BLUE_BUTTON_CLAIM_CONTROL_NUMBER =
      "https://bluebutton.cms.gov/identifiers/CLM-CNTL-NUM";

  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_QUERY_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-QUERY-CD";

  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_QUERY_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-QUERY-CD";

  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_CONTRACTOR_NUMBER =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CNTRCTR-NUM";

  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_CONTRACTOR_NUMBER =
      "https://bluebutton.cms.gov/CodeSystem/CLM-CNTRCTR-NUM";

  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_RECORD_TYPE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-NRLN-RIC-CD";

  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_RECORD_TYPE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NRLN-RIC-CD";

  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_DISPOSITION_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-DISP-CD";

  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_DISPOSITION_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-DISP-CD";

  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_PROCESS_DATE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CMS-PROC-DT";

  // Other

  /**
   * <a href="https://terminology.hl7.org/3.1.0/CodeSystem-CDCREC.html">CDC Race and Ethnicity.</a>
   */
  public static final String CDC_RACE_ETHNICITY = "urn:oid:2.16.840.1.113883.6.238";

  /**
   * <a href="https://build.fhir.org/ig/HL7/US-Core/ValueSet-omb-race-category.html">OMB Race
   * categories.</a>
   */
  public static final String OMB_CATEGORY = "ombCategory";

  /**
   * <a href="https://terminology.hl7.org/6.2.0/CodeSystem-v3-ietf3066.html">IETF Language
   * Identification.</a>
   */
  public static final String IETF_LANGUAGE = "urn:ietf:bcp:47";

  /** <a href="https://terminology.hl7.org/6.2.0/NamingSystem-cmsMBI.html">CMS MBI.</a> */
  public static final String CMS_MBI = "http://hl7.org/fhir/sid/us-mbi";

  public static final String NPI = "http://hl7.org/fhir/sid/us-npi";

  public static final String CMS_CERTIFICATION_NUMBERS =
      "http://terminology.hl7.org/NamingSystem/CCN";

  public static final String CMS_ICD_9 = "http://www.cms.gov/Medicare/Coding/ICD9";
  public static final String CMS_ICD_10 = "http://www.cms.gov/Medicare/Coding/ICD10";
  public static final String ICD_9_CM = "http://hl7.org/fhir/sid/icd-9-cm";
  public static final String ICD_10_CM = "http://hl7.org/fhir/sid/icd-10-cm";
}
