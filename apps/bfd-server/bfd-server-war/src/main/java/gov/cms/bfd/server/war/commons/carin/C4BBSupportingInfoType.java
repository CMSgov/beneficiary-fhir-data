package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN CodeSystem for Supportin INfo <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBSupportingInfoType.html">CodeSystem:
 * C4BB Supporting Info Type Type</a>
 */
public enum C4BBSupportingInfoType {
  TYPE_OF_BILL,
  DISCHARGE_STATUS;

  public String getSystem() {
    return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";
  }

  public String toCode() {
    switch (this) {
      case TYPE_OF_BILL:
        return "typeofbill";
      case DISCHARGE_STATUS:
        return "discharge-status";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case TYPE_OF_BILL:
        return "Type of Bill";
      case DISCHARGE_STATUS:
        return "Discharge Status";
      default:
        return "?";
    }
  }

  public String getDefinition() {
    switch (this) {
      case TYPE_OF_BILL:
        return "UB-04 Type of Bill (FL-04) provides specific information for payer purposes.";
      case DISCHARGE_STATUS:
        return "UB-04 Discharge Status (FL-17) indicates the patient’s status as of the discharge date for a facility stay.";
      default:
        return "?";
    }
  }
}
