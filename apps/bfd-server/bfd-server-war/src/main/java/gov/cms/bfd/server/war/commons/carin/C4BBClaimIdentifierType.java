package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Claim Identifiers<a
 * href="http://hl7.org/fhir/us/carin-bb/ValueSet/C4BBClaimIdentifierType">ValueSet: C4BB Claim
 * Identifier Type</a>
 */
public enum C4BBClaimIdentifierType {
  UC;

  public String getSystem() {
    return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";
  }

  public String toCode() {
    switch (this) {
      case UC:
        return "uc";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case UC:
        return "Unique Claim ID";
      default:
        return "?";
    }
  }

  public String getDefinition() {
    switch (this) {
      case UC:
        return "Indicates that the claim identifier is that assigned by a payer for a claim received from a provider or subscriber";
      default:
        return "?";
    }
  }
}
