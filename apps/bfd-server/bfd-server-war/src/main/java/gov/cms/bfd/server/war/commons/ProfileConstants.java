package gov.cms.bfd.server.war.commons;

/** A collection of constants used for various profile urls. */
public class ProfileConstants {
  /**
   * C4BB Resource Profile: C4BB ExplanationOfBenefit Inpatient Institutional<a
   * href="https://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-C4BB-ExplanationOfBenefit-Inpatient-Institutional.html">
   * C4BBExplanationOfBenefitInpatientInstitutional</a>.
   */
  public static final String C4BB_EOB_INPATIENT_PROFILE_URL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Inpatient-Institutional";

  /**
   * C4BB Resource Profile: C4BB ExplanationOfBenefit Outpatient Institutional<a
   * href="https://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-C4BB-ExplanationOfBenefit-Outpatient-Institutional.html">
   * C4BBExplanationOfBenefitOutpatientInstitutional</a>.
   */
  public static final String C4BB_EOB_OUTPATIENT_PROFILE_URL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Outpatient-Institutional";

  /**
   * C4BB Resource Profile: C4BB ExplanationOfBenefit Pharmacy <a
   * href="https://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-C4BB-ExplanationOfBenefit-Pharmacy.html">
   * C4BBExplanationOfBenefitPharmacy</a>.
   */
  public static final String C4BB_EOB_PHARMACY_PROFILE_URL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Pharmacy";

  /**
   * C4BB Resource Profile: C4BB ExplanationOfBenefit Professional NonClinician <a
   * href="https://build.fhir.org/ig/HL7/carin-bb/StructureDefinition-C4BB-ExplanationOfBenefit-Professional-NonClinician.html">
   * C4BBExplanationOfBenefitProfessionalNonClinician</a>.
   */
  public static final String C4BB_EOB_NONCLINICIAN_PROFILE_URL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit-Professional-NonClinician";

  /**
   * C4BB Resource Profile: C4BB Coverage <a
   * href="http://hl7.org/fhir/us/carin-bb/STU1/StructureDefinition-C4BB-Coverage.html">
   * C4BBCoverage</a>.
   */
  public static final String C4BB_COVERAGE_URL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Coverage";

  /**
   * C4DIC Resource Profile: C4DIC Coverage <a
   * href="https://hl7.org/fhir/us/insurance-card/STU1.1/StructureDefinition-C4DIC-Coverage.html">
   * C4DICCoverage</a>.
   */
  public static final String C4DIC_COVERAGE_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-Coverage";

  /**
   * C4BB Resource Profile: C4BB Organization <a
   * href="http://hl7.org/fhir/us/carin-bb/STU1/StructureDefinition-C4BB-Organization.html">
   * C4BBOrganization</a>.
   */
  public static final String C4BB_ORGANIZATION_URL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization";

  /**
   * C4BB Resource Profile: C4DIC Organization <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition-C4DIC-Organization.html">
   * C4BBOrganization</a>.
   */
  public static final String C4DIC_ORGANIZATION_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-Organization";

  /**
   * C4BB Resource Profile: C4BB Patient <a
   * href="http://hl7.org/fhir/us/carin-bb/STU1/StructureDefinition-C4BB-Patient.html">
   * C4BBPatient</a>.
   */
  public static final String C4BB_PATIENT_URL =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient";

  /**
   * C4DIC Resource Profile: C4DIC Patient <a
   * href="https://hl7.org/fhir/us/insurance-card/STU1.1/StructureDefinition-C4DIC-Patient.html">
   * C4DICPatient</a>.
   */
  public static final String C4DIC_PATIENT_URL =
      "http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-Patient";

  /** C4BB profile version suffix. This is omitted for backwards-compatibility reasons. */
  public static final String C4BB_VERSION_SUFFIX = "";

  /** C4DIC profile version suffix. This is the C4DIC version we currently support. */
  public static final String C4DIC_VERSION_SUFFIX = "|1.1";
}
