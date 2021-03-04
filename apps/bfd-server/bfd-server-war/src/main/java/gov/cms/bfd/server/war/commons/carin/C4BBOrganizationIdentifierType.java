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
  // Note: This is *not* a valid C4BBOrganizationIdentifierType but PDE uses it
  NCPDP,
  // All codes under http://terminology.hl7.org/CodeSystem/v2-0203
  // Only adding what we explicitly use
  PRN,
  UPIN,
  SL,
  TAX;

  public String getSystem() {
    switch (this) {
        // C4BBIdentifierType
      case NPI:
      case PAYERID:
      case NAICCODE:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";
        // Non-standard
      case NCPDP:
        return "https://bluebutton.cms.gov/resources/codesystem/identifier-type";
        // All others are v2-0203
      default:
        return "http://terminology.hl7.org/CodeSystem/v2-0203";
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
      case NCPDP:
        return "NCPDP";
      case PRN:
        return "PRN";
      case UPIN:
        return "UPIN";
      case SL:
        return "SL";
      case TAX:
        return "TAX";
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
      case NCPDP:
        return "National Council for Prescription Drug Programs";
      case PRN:
        return "Provider number";
      case UPIN:
        return "Medicare/CMS (formerly HCFA)'s Universal Physician Identification numbers";
      case SL:
        return "State license";
      case TAX:
        return "Tax ID number";
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
      case NCPDP:
        return "National Council for Prescription Drug Programs";
      case PRN:
        return "A number that is unique to an individual provider, a provider group or an organization within an Assigning Authority.";
      case UPIN:
        return "An identifier for a provider within the CMS/Medicare program. A globally unique identifier for the provider in the Medicare program.";
      case SL:
        return "State license";
      case TAX:
        return "Tax ID number";
      default:
        return "?";
    }
  }
}
