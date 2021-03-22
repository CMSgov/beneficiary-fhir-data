package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Professional and Non Clinician Diagnosis <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-C4BBClaimProfessionalAndNonClinicianDiagnosisType.html">
 * ValueSet: C4BB Claim Professional And Non Clinician Diagnosis Type</a>
 */
public enum C4BBClaimProfessionalAndNonClinicianDiagnosisType {
  PRINCIPAL,
  SECONDARY;

  public String getSystem() {
    switch (this) {
      case PRINCIPAL:
        return "http://terminology.hl7.org/CodeSystem/ex-diagnosistype";
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType";
    }
  }

  public String toCode() {
    switch (this) {
      case PRINCIPAL:
        return "principal";
      case SECONDARY:
        return "secondary";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case PRINCIPAL:
        return "principal";
      case SECONDARY:
        return "secondary";
      default:
        return "?";
    }
  }

  public String getDefinition() {
    switch (this) {
      case PRINCIPAL:
        return "The single medical diagnosis that is most relevant to the patient's chief complaint or need for treatment.";
      case SECONDARY:
        return "Required when necessary to report additional diagnoses on professional and non-clinician claims";
      default:
        return "?";
    }
  }
}
