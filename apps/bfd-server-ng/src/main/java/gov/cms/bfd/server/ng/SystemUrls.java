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

  // Hl7

  /**
   * <a href="https://terminology.hl7.org/1.0.0/CodeSystem-v3-NullFlavor.html">Hl7 Null Flavor.</a>
   */
  public static String HL7_NULL_FLAVOR = "http://terminology.hl7.org/CodeSystem/v3-NullFlavor";

  /** <a href="https://terminology.hl7.org/2.1.0/CodeSystem-v2-0203.html">Hl7 Identifier.</a> */
  public static final String HL7_IDENTIFIER = "http://terminology.hl7.org/CodeSystem/v2-0203";

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
}
