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

  /**
   * URL for the CARIN Blue Button (C4BB) Coverage Profile, version 2.1.0. <a
   * href="http://hl7.org/fhir/us/carin-bb/STU2.1/StructureDefinition-C4BB-Coverage.html">C4BB
   * Coverage 2.1.0</a>
   */
  // add version numbers
  public static final String PROFILE_C4BB_COVERAGE_2_1_0 =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Coverage|2.1.0";

  /**
   * URL for the US Core Coverage Profile, version 6.1.0. <a
   * href="http://hl7.org/fhir/us/core/STU6.1/StructureDefinition-us-core-coverage.html">US Core
   * Coverage 6.1.0</a>
   */
  public static final String PROFILE_US_CORE_COVERAGE_6_1_0 =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-coverage|6.1.0";

  /**
   * URL for the CARIN Blue Button (C4BB) Organization Profile, version 2.1.0. Used for the
   * contained CMS Organization. <a
   * href="http://hl7.org/fhir/us/carin-bb/STU2.1/StructureDefinition-C4BB-Organization.html">C4BB
   * Organization 2.1.0</a>
   */
  public static final String PROFILE_C4BB_ORGANIZATION_2_1_0 =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization|2.1.0";

  /**
   * System URL for the HL7 Subscriber Relationship code system. Used for {@code
   * Coverage.relationship.coding.system}. <a
   * href="http://terminology.hl7.org/CodeSystem/subscriber-relationship">Subscriber
   * Relationship</a>
   */
  public static final String SYS_SUBSCRIBER_RELATIONSHIP =
      "http://terminology.hl7.org/CodeSystem/subscriber-relationship";

  /**
   * System URL for the NAHDO Standard Payer Type/Product/Plan (SOPT) code system. Used for {@code
   * Coverage.type.coding.system}. <a href="https://nahdo.org/sopt">NAHDO SOPT</a>
   */
  public static final String SYS_SOPT = "https://nahdo.org/sopt";

  /**
   * System URL for the HL7 Coverage Class code system. Used for {@code
   * Coverage.class.type.coding.system}. <a
   * href="http://terminology.hl7.org/CodeSystem/coverage-class">Coverage Class</a>
   */
  public static final String SYS_COVERAGE_CLASS =
      "http://terminology.hl7.org/CodeSystem/coverage-class";

  /**
   * System URL for the HL7 Contact Entity Type code system. Used for {@code
   * Organization.contact.purpose.coding.system}. <a
   * href="http://terminology.hl7.org/CodeSystem/contactentity-type">Contact Entity Type</a>
   */
  public static final String SYS_CONTACT_ENTITY_TYPE =
      "http://terminology.hl7.org/CodeSystem/contactentity-type";

  /**
   * Extension URL for BFD Medicare Status Code. Source: V2_MDCR_BENE_MDCR_STUS.BENE_MDCR_STUS_CD <a
   * href="https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-STUS-CD">BENE-MDCR-STUS-CD
   * Extension</a>
   */
  public static final String EXT_BENE_MDCR_STUS_CD_URL =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-STUS-CD";

  /**
   * Extension URL for BFD Entitlement Buy-In Indicator Code. Source: V2_MDCR_BENE_TP.BENE_BUYIN_CD
   * <a href="https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-BUYIN-CD">BENE-BUYIN-CD
   * Extension</a>
   */
  public static final String EXT_BENE_BUYIN_CD_URL =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-BUYIN-CD";

  /**
   * Extension URL for BFD Medicare Entitlement Status Code. Source:
   * BENE_MDCR_ENTLMT.BENE_MDCR_ENTLMT_STUS_CD (from v2_mdcr_bene_mdcr_entlmt) <a
   * href="https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-ENTLMT-STUS-CD">BENE-MDCR-ENTLMT-STUS-CD
   * Extension</a>
   */
  public static final String EXT_BENE_MDCR_ENTLMT_STUS_CD_URL =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-ENTLMT-STUS-CD";

  /**
   * Extension URL for BFD Medicare Enrollment Reason Code. Source:
   * V2_BENE_MDCR_ENTLMT.BENE_MDCR_ENRLMT_RSN_CD (from v2_mdcr_bene_mdcr_entlmt) <a
   * href="https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-ENRLMT-RSN-CD">BENE-ENRLMT-RSN-CD
   * Extension</a>
   */
  public static final String EXT_BENE_ENRLMT_RSN_CD_URL =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-ENRLMT-RSN-CD";

  /**
   * Extension URL for BFD Medicare Current Entitlement Reason Code. Source:
   * V2_BENE_MDCR_ENTLMT_RSN.BENE_MDCR_ENTLMT_RSN_CD (from v2_mdcr_bene_mdcr_entlmt_rsn) <a
   * href="https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-ENTLMT-RSN-CD">BENE-MDCR-ENTLMT-RSN-CD
   * Extension</a>
   */
  public static final String EXT_BENE_MDCR_ENTLMT_RSN_CD_URL =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-ENTLMT-RSN-CD";

  /**
   * Extension URL for BFD ESRD (End-Stage Renal Disease) Status Indicator. Derived from
   * BENE_MDCR_STUS_CD. <a
   * href="https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-ESRD-STUS-ID">BENE-ESRD-STUS-ID
   * Extension</a>
   */
  public static final String EXT_BENE_ESRD_STUS_ID_URL =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-ESRD-STUS-ID";

  /**
   * Extension URL for BFD Disability Status Indicator. Derived from BENE_MDCR_STUS_CD. <a
   * href="https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-DSBLD-STUS-ID">BENE-DSBLD-STUS-ID
   * Extension</a>
   */
  public static final String EXT_BENE_DSBLD_STUS_ID_URL =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-DSBLD-STUS-ID";

  /**
   * System URL for the BFD/BlueButton Medicare Status Code system. Used for the coding in the
   * BENE-MDCR-STUS-CD extension. <a
   * href="https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-STUS-CD">BENE-MDCR-STUS-CD
   * CodeSystem</a>
   */
  public static final String SYS_BENE_MDCR_STUS_CD =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-STUS-CD";

  /**
   * System URL for the BFD/BlueButton Beneficiary Buy-In Code system. Used for the coding in the
   * BENE-BUYIN-CD extension. <a
   * href="https://bluebutton.cms.gov/fhir/CodeSystem/BENE-BUYIN-CD">BENE-BUYIN-CD CodeSystem</a>
   */
  public static final String SYS_BENE_BUYIN_CD =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-BUYIN-CD";

  /**
   * System URL for the BFD/BlueButton Medicare Entitlement Status Code system. Used for the coding
   * in the BENE-MDCR-ENTLMT-STUS-CD extension. <a
   * href="https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-ENTLMT-STUS-CD">BENE-MDCR-ENTLMT-STUS-CD
   * CodeSystem</a>
   */
  public static final String SYS_BENE_MDCR_ENTLMT_STUS_CD =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-ENTLMT-STUS-CD";

  /**
   * System URL for the BFD/BlueButton Beneficiary Enrollment Reason Code system. Used for the
   * coding in the BENE-ENRLMT-RSN-CD extension. <a
   * href="https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ENRLMT-RSN-CD">BENE-ENRLMT-RSN-CD
   * CodeSystem</a>
   */
  public static final String SYS_BENE_ENRLMT_RSN_CD =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ENRLMT-RSN-CD";

  /**
   * System URL for the BFD/BlueButton Medicare Current Entitlement Reason Code system. Used for the
   * coding in the BENE-MDCR-ENTLMT-RSN-CD extension. <a
   * href="https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-ENTLMT-RSN-CD">BENE-MDCR-ENTLMT-RSN-CD
   * CodeSystem</a>
   */
  public static final String SYS_BENE_MDCR_ENTLMT_RSN_CD =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-ENTLMT-RSN-CD";

  /**
   * System URL for the BFD/BlueButton ESRD (End-Stage Renal Disease) Status Indicator system. Used
   * for the coding in the BENE-ESRD-STUS-ID extension. <a
   * href="https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ESRD-STUS-ID">BENE-ESRD-STUS-ID
   * CodeSystem</a>
   */
  public static final String SYS_BENE_ESRD_STUS_ID =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ESRD-STUS-ID";

  /**
   * System URL for the BFD/BlueButton Disability Status Indicator system. Used for the coding in
   * the BENE-DSBLD-STUS-ID extension. <a
   * href="https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DSBLD-STUS-ID">BENE-DSBLD-STUS-ID
   * CodeSystem</a>
   */
  public static final String SYS_BENE_DSBLD_STUS_ID =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DSBLD-STUS-ID";

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
