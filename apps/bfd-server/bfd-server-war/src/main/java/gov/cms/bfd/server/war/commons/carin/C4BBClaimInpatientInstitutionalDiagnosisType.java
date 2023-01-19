package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Inpatient Institutional Diagnosis types <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-C4BBClaimInpatientInstitutionalDiagnosisType.html">
 * ValueSet: C4BB Claim Inpatient Institutional Diagnosis Type</a>.
 */
public enum C4BBClaimInpatientInstitutionalDiagnosisType {
  /**
   * The single medical diagnosis that is most relevant to the patient's chief complaint or need for
   * treatment.
   */
  PRINCIPAL,
  /** The diagnosis given as the reason why the patient was admitted to the hospital.* */
  ADMITTING,
  /** Required when other conditions coexist or develop subsequently during the treatment. * */
  OTHER,
  /** Required when an external cause of injury is needed to describe the injury. * */
  EXTERNAL_CAUSE;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {
    switch (this) {
      case PRINCIPAL:
      case ADMITTING:
        return "http://terminology.hl7.org/CodeSystem/ex-diagnosistype";
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType";
    }
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  public String toCode() {
    switch (this) {
      case PRINCIPAL:
        return "principal";
      case ADMITTING:
        return "admitting";
      case OTHER:
        return "other";
      case EXTERNAL_CAUSE:
        return "externalcauseofinjury";
      default:
        return "?";
    }
  }

  /**
   * Gets the display string.
   *
   * @return the display string
   */
  public String getDisplay() {
    switch (this) {
      case PRINCIPAL:
        return "Principal Diagnosis";
      case ADMITTING:
        return "Admitting Diagnosis";
      case OTHER:
        return "Other";
      case EXTERNAL_CAUSE:
        return "External Cause of Injury";
      default:
        return "?";
    }
  }

  /**
   * Gets the definition.
   *
   * @return the definition
   */
  public String getDefinition() {
    switch (this) {
      case PRINCIPAL:
        return "The single medical diagnosis that is most relevant to the patient's chief complaint or need for treatment.";
      case ADMITTING:
        return "The diagnosis given as the reason why the patient was admitted to the hospital.";
      case OTHER:
        return "Required when other conditions coexist or develop subsequently during the treatment";
      case EXTERNAL_CAUSE:
        return "Required when an external cause of injury is needed to describe the injury";
      default:
        return "?";
    }
  }
}
