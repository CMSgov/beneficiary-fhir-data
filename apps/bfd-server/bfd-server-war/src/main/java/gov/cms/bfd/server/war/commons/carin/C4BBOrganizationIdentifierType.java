package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN Value Set for Organization Identifier Type <a
 * href="http://hl7.org/fhir/us/carin-bb/STU1/ValueSet-C4BBOrganizationIdentifierType.html">ValueSet:
 * C4BB Organization Identifier Type</a>
 */
public enum C4BBOrganizationIdentifierType {
  NPI,
  PAYERID,
  NAICCODE,
  // All codes under http://terminology.hl7.org/CodeSystem/v2-0203
  // Only adding what we explicitly use
  PRN;

  public String getSystem() {
    switch (this) {
      case PRN:
        return "http://terminology.hl7.org/CodeSystem/v2-0203";
        // All others are C4BBIdentifierType
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";
    }
  }

  public String toCode() {
    switch (this) {
      case NPI:
        return C4BBIdentifierType.NPI.toCode();
      case PAYERID:
        return C4BBIdentifierType.PAYERID.toCode();
      case NAICCODE:
        return C4BBIdentifierType.NAICCODE.toCode();
      case PRN:
        return "PRN";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case NPI:
        return C4BBIdentifierType.NPI.getDisplay();
      case PAYERID:
        return C4BBIdentifierType.PAYERID.getDisplay();
      case NAICCODE:
        return C4BBIdentifierType.NAICCODE.getDisplay();
      case PRN:
        return "Provider number";
      default:
        return "?";
    }
  }

  public String getDefinition() {
    switch (this) {
      case NPI:
        return C4BBIdentifierType.NPI.getDefinition();
      case PAYERID:
        return C4BBIdentifierType.PAYERID.getDefinition();
      case NAICCODE:
        return C4BBIdentifierType.NAICCODE.getDefinition();
      case PRN:
        return "A number that is unique to an individual provider, a provider group or an organization within an Assigning Authority.";
      default:
        return "?";
    }
  }
}
