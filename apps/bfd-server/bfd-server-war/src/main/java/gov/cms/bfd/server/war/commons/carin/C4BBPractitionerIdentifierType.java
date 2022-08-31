package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Practitioner Identifiers <a
 * href="http://hl7.org/fhir/us/carin-bb/STU1/ValueSet-C4BBPractitionerIdentifierType.html">ValueSet:
 * C4BB Practitioner Identifier Type</a>.
 */
public enum C4BBPractitionerIdentifierType {
  /** National Provider Identifier. */
  NPI,
  /**
   * An identifier for a provider within the CMS/Medicare program. A globally unique identifier for
   * the provider in the Medicare program.
   */
  UPIN,
  /** Identifier that uniquely identifies a geographic location in the US. */
  PIN,
  /** Tax ID number. */
  TAX;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {
    switch (this) {
      case NPI:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";
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
        return "npi";
      case UPIN:
        return "UPIN";
      case PIN:
        return "PIN";
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
        return "National Provider Identifier";
      case UPIN:
        return "Medicare/CMS (formerly HCFA)'s Universal Physician Identification numbers";
      case PIN:
        return "Premises Identifier Number (US Official)";
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
        return "National Provider Identifier";
      case UPIN:
        return "An identifier for a provider within the CMS/Medicare program. A globally unique identifier for the provider in the Medicare program.";
      case PIN:
        return "Identifier that uniquely identifies a geographic location in the US.";
      case TAX:
        return "Tax ID number";

      default:
        return "?";
    }
  }
}
