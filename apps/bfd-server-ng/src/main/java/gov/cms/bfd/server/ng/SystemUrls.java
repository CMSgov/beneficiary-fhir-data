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
  /** URL. */
  public static final String PROFILE_CARIN_BB_ORGANIZATION_2_1_0 =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization|2.1.0";

  /** URL. */
  public static final String PROFILE_US_CORE_ORGANIZATION_6_1_0 =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization|6.1.0";

  /** URL. */
  public static final String PROFILE_CARIN_BB_PRACTITIONER_2_1_0 =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Practitioner|2.1.0";

  /** URL. */
  public static final String PROFILE_US_CORE_PRACTITIONER_6_1_0 =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner|6.1.0";

  /** The URL for your adjudication status code system. */
  public static final String SYS_ADJUDICATION_STATUS =
      "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication-Status";

  // Hl7 Code Systems

  /**
   * <a href="https://terminology.hl7.org/1.0.0/CodeSystem-v3-NullFlavor.html">Hl7 Null Flavor.</a>
   */
  public static String HL7_NULL_FLAVOR = "http://terminology.hl7.org/CodeSystem/v3-NullFlavor";

  /** <a href="https://terminology.hl7.org/2.1.0/CodeSystem-v2-0203.html">Hl7 Identifier.</a> */
  public static final String HL7_IDENTIFIER = "http://terminology.hl7.org/CodeSystem/v2-0203";

  /** URL. */
  public static final String HL7_CLAIM_TYPE = "http://terminology.hl7.org/CodeSystem/claim-type";

  /** URL. */
  public static final String HL7_DIAGNOSIS_TYPE =
      "http://terminology.hl7.org/CodeSystem/ex-diagnosistype";

  /** URL. */
  public static final String HL7_CLAIM_INFORMATION =
      "http://terminology.hl7.org/CodeSystem/claiminformationcategory";

  /** URL. */
  public static final String HL7_DATA_ABSENT =
      "http://terminology.hl7.org/CodeSystem/data-absent-reason";

  /** URL. */
  public static final String HL7_ADJUDICATION =
      "http://terminology.hl7.org/CodeSystem/adjudication";

  /** URL. */
  public static final String HL7_BENEFIT_CATEGORY =
      "http://terminology.hl7.org/CodeSystem/ex-benefitcategory";

  /** URL. */
  public static final String HL7_ADJUDICATION_CATEGORY =
      "http://terminology.hl7.org/CodeSystem/adjudication";

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

  /** URL. */
  public static final String CARIN_CLAIM_SUBTYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBInstitutionalClaimSubType";

  /** URL. */
  public static final String CARIN_STRUCTURE_DEFINITION_PHARMACY =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Pharmacy|2.1.0";

  /** URL. */
  public static final String CARIN_STRUCTURE_DEFINITION_INPATIENT_INSTITUTIONAL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Inpatient-Institutional|2.1.0";

  /** URL. */
  public static final String CARIN_STRUCTURE_DEFINITION_OUTPATIENT_INSTITUTIONAL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Outpatient-Institutional|2.1.0";

  /** URL. */
  public static final String CARIN_STRUCTURE_DEFINITION_PROFESSIONAL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Professional-NonClinician|2.1.0";

  /** URL. */
  public static final String CARIN_CODE_SYSTEM_IDENTIFIER_TYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";

  /** URL. */
  public static final String CARIN_CODE_SYSTEM_CLAIM_PROCEDURE_TYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimProcedureType";

  /** URL. */
  public static final String POA_CODING =
      "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/HospitalAcqCond/Coding";

  /** URL. */
  public static final String CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType";

  /** URL. */
  public static final String CARIN_CODE_SYSTEM_SUPPORTING_INFO_TYPE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";

  /** URL. */
  public static final String CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudicationDiscriminator";

  /** URL. */
  public static String CARIN_CODE_SYSTEM_ADJUDICATION =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication";

  /** URL. */
  public static String CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole";

  /** URL. */
  public static String CARIN_CODE_SYSTEM_PAYER_ADJUDICATION_STATUS =
      "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBPayerAdjudicationStatus";

  // CMS Blue Button
  /** URL. */
  public static final String BLUE_BUTTON_CLAIM_TYPE_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-TYPE-CD";

  /** URL. */
  public static final String BLUE_BUTTON_ADJUDICATION_STATUS =
      "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication-Status";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION =
      "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication";

  /** URL. */
  public static final String BLUE_BUTTON_CLAIM_CONTROL_NUMBER =
      "https://bluebutton.cms.gov/identifiers/CLM-CNTL-NUM";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_QUERY_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-QUERY-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_QUERY_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-QUERY-CD";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_CONTRACTOR_NUMBER =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CNTRCTR-NUM";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_CONTRACTOR_NUMBER =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-CNTRCTR-NUM";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_RECORD_TYPE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-NRLN-RIC-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_RECORD_TYPE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NRLN-RIC-CD";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_DISPOSITION_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-DISP-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_DISPOSITION_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-DISP-CD";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_PROCESS_DATE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CMS-PROC-DT";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_ADMISSION_SOURCE_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-ADMSN-SRC-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_PATIENT_STATUS_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-PTNT-STUS-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_ADMISSION_TYPE_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-ADMSN-TYPE-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_SUPPORTING_INFORMATION =
      "https://bluebutton.cms.gov/fhir/CodeSystem/Supporting-Information";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_PRIMARY_PAYOR_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NCH-PRMRY-PYR-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_MCO_PAID_SWITCH =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-MDCR-INSTNL-MCO-PD-SW";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_FACILITY_TYPE_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-BILL-FAC-TYPE-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_BILL_CLASSIFICATION_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-BILL-CLSFCTN-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_BILL_FREQUENCY_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-BILL-FREQ-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_REVENUE_CENTER_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-REV-CNTR-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_CLAIM_DEDUCTIBLE_COINSURANCE_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-DDCTBL-COINSRNC-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION_SYSTEM =
      "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_PPS_DRG_WEIGHT_NUMBER =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-MDCR-IP-PPS-DRG-WT-NUM";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_NONPAYMENT_REASON_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-MDCR-NPMT-RSN-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_NONPAYMENT_REASON_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-MDCR-NPMT-RSN-CD";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_FINAL_ACTION_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-FI-ACTN-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_FINAL_ACTION_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-FI-ACTN-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_BENEFIT_BALANCE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/Benefit-Balance";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_DUAL_STATUS_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DUAL-STUS-CD";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_DUAL_STATUS_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-DUAL-STUS-CD";

  /** URL. */
  public static final String BLUE_BUTTON_CODE_SYSTEM_DUAL_TYPE_CODE =
      "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DUAL-TYPE-CD";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_DUAL_TYPE_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-DUAL-TYPE-CD";

  /** URL. */
  public static final String BLUE_BUTTON_STRUCTURE_DEFINITION_MEDICAID_STATE_CODE =
      "https://bluebutton.cms.gov/fhir/StructureDefinition/MEDICAID-STATE-CD";

  // CMS
  /** URL. */
  public static final String CMS_ICD_9 = "http://www.cms.gov/Medicare/Coding/ICD9";

  /** URL. */
  public static final String CMS_ICD_10 = "http://www.cms.gov/Medicare/Coding/ICD10";

  /** URL. */
  public static final String CMS_MS_DRG =
      "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software";

  /** URL. */
  public static final String CMS_HCPCS = "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets";

  /** URL. */
  public static final String CMS_HIPPS =
      "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/ProspMedicareFeeSvcPmtGen/HIPPSCodes";

  // NUBC

  /** URL. */
  public static final String NUBC_PATIENT_DISCHARGE_STATUS =
      "https://www.nubc.org/CodeSystem/PatDischargeStatus";

  /** URL. */
  public static final String NUBC_POINT_OF_ORIGIN = "https://www.nubc.org/CodeSystem/PointOfOrigin";

  /** URL. */
  public static final String NUBC_TYPE_OF_ADMIT =
      "https://www.nubc.org/CodeSystem/PriorityTypeOfAdmitOrVisit";

  /** URL. */
  public static final String NUBC_TYPE_OF_BILL = "https://www.nubc.org/CodeSystem/TypeOfBill";

  /** URL. */
  public static final String NUBC_REVENUE_CODES = "https://www.nubc.org/CodeSystem/RevenueCodes";

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

  /** URL. */
  public static final String NPI = "http://hl7.org/fhir/sid/us-npi";

  /** URL. */
  public static final String NDC = "http://hl7.org/fhir/sid/ndc";

  /** URL. */
  public static final String CMS_CERTIFICATION_NUMBERS =
      "http://terminology.hl7.org/NamingSystem/CCN";

  /** URL. */
  public static final String ICD_9_CM = "http://hl7.org/fhir/sid/icd-9-cm";

  /** URL. */
  public static final String ICD_10_CM = "http://hl7.org/fhir/sid/icd-10-cm";

  /** URL. */
  public static final String UNITS_OF_MEASURE = "http://unitsofmeasure.org";

  /** URL. */
  public static final String AMA_CPT = "http://www.ama-assn.org/go/cpt";

  /** URL. */
  public static final String X12_CLAIM_ADJUSTMENT_REASON_CODES =
      "https://x12.org/codes/claim-adjustment-reason-codes";

  /** URL. */
  public static final String USPS = "https://www.usps.com";
}
