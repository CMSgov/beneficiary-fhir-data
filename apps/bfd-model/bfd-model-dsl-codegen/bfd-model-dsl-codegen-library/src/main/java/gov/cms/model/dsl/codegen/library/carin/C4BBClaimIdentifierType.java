package gov.cms.model.dsl.codegen.library.carin;

/**
 * CARIN ValueSet for Claim Identifiers<a
 * href="http://hl7.org/fhir/us/carin-bb/ValueSet/C4BBClaimIdentifierType">ValueSet: C4BB Claim
 * Identifier Type</a>.
 */
public enum C4BBClaimIdentifierType {
  /**
   * Indicates that the claim identifier is that assigned by a payer for a claim received from a
   * provider or subscriber. *
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
      case UC:
        return "Indicates that the claim identifier is that assigned by a payer for a claim received from a provider or subscriber";
      default:
        return "?";
    }
  }
}
