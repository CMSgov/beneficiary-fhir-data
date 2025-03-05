package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN Value Set for Organization Identifier Type <a
 * href="http://hl7.org/fhir/us/carin-bb/STU1/ValueSet-C4BBOrganizationIdentifierType.html">ValueSet:
 * C4BB Organization Identifier Type</a>.
 */
public enum C4BBOrganizationIdentifierType {
  /** See {@link C4BBIdentifierType#NPI}. */
  NPI,
  /** See {@link C4BBIdentifierType#PAYERID}. */
  PAYERID,
  /** See {@link C4BBIdentifierType#NAICCODE}. */
  NAICCODE,
  // Note: This is *not* a valid C4BBOrganizationIdentifierType but PDE uses it
  /** National Council for Prescription Drug Programs. */
  NCPDP,
  // All codes under http://terminology.hl7.org/CodeSystem/v2-0203
  // Only adding what we explicitly use
  /**
   * A number that is unique to an individual provider, a provider group or an organization within
   * an Assigning Authority.
   */
  PRN,
  /**
   * An identifier for a provider within the CMS/Medicare program. A globally unique identifier for
   * the provider in the Medicare program.
   */
  UPIN,
  /** State license. */
  SL,
  /** Tax ID number. */
  TAX;

  /**
   * Gets the system.
   *
   * @return the system
   */
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

  /**
   * Gets the code.
   *
   * @return the code
   */
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

  /**
   * Gets the display string.
   *
   * @return the display string
   */
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

  /**
   * Gets the definition.
   *
   * @return the definition
   */
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
