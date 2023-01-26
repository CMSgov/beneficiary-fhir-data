package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN Code System for Patient Identifier Type <a
 * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBIdentifierType.html">CodeSystem: C4BB
 * Identifier Type</a>.
 */
public enum C4BBIdentifierType {
  /** National Provider Identifier. */
  NPI,
  /** Payer ID. */
  PAYERID,
  /** NAIC Code. */
  NAICCODE,
  /** Patient Account Number. */
  PAT,
  /**
   * Indicates that the patient identifier is a unique member identifier assigned by a payer across
   * all lines of business.
   */
  UM,
  /**
   * Indicates that the claim identifier is that assigned by a payer for a claim received from a
   * provider or subscriber.
   */
  UC;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {
    return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";
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
      case PAYERID:
        return "payerid";
      case NAICCODE:
        return "naiccode";
      case PAT:
        return "pat";
      case UM:
        return "um";
      case UC:
        return "uc";
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
      case PAYERID:
        return "Payer ID";
      case NAICCODE:
        return "NAIC Code";
      case PAT:
        return "Patient Account Number";
      case UM:
        return "Unique Member ID";
      case UC:
        return "Unique Claim ID";
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
      case PAYERID:
        return "Payer ID";
      case NAICCODE:
        return "NAIC Code";
      case PAT:
        return "Patient Account Number";
      case UM:
        return "Indicates that the patient identifier is a unique member identifier assigned by a payer across all lines of business";
      case UC:
        return "Indicates that the claim identifier is that assigned by a payer for a claim received from a provider or subscriber";
      default:
        return "?";
    }
  }
}
