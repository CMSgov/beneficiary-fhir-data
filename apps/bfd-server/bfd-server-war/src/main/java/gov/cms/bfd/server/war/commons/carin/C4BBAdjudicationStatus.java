package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for <a
 * href="http://hl7.org/fhir/us/carin-bb/STU1.1/CodeSystem-C4BBPayerAdjudicationStatus.html">ValueSet:
 * C4BB Payer Adjudication Status</a>.
 */
public enum C4BBAdjudicationStatus {
  /**
   * Indicates the claim or claim line was paid in network. This does not indicate the contracting
   * status of the provider.
   */
  IN_NETWORK,
  /**
   * Indicates the claim or claim line was paid out of network. This does not indicate the
   * contracting status of the provider.
   */
  OUT_OF_NETWORK,
  /** Indicates other network status or when a network does not apply. */
  OTHER,
  /** Indicates the provider was contracted for the service. */
  CONTRACTED,
  /** Indicates the provider was not contracted for the service. */
  NON_CONTRACTED,
  /** Indicates if the claim was approved for payment. */
  PAID,
  /** Indicates if the claim was denied. */
  DENIED,
  /** Indicates that some line items on the claim were denied. */
  PARTIALLY_PAID;

  /**
   * Gets the system for this status.
   *
   * @return the C4BB system url
   */
  public String getSystem() {
    return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBPayerAdjudicationStatus";
  }

  /**
   * Gets the code for this status.
   *
   * @return the code representation of this status
   */
  public String toCode() {
    switch (this) {
      case IN_NETWORK:
        return "innetwork";
      case OUT_OF_NETWORK:
        return "outofnetwork";
      case OTHER:
        return "other";
      case CONTRACTED:
        return "contracted";
      case NON_CONTRACTED:
        return "noncontracted";
      case PAID:
        return "paid";
      case DENIED:
        return "denied";
      case PARTIALLY_PAID:
        return "partiallypaid";
      default:
        return "?";
    }
  }

  /**
   * Gets the display.
   *
   * @return the display
   */
  public String getDisplay() {
    switch (this) {
      case IN_NETWORK:
        return "In Network";
      case OUT_OF_NETWORK:
        return "Out Of Network";
      case OTHER:
        return "Other";
      case CONTRACTED:
        return "Contracted";
      case NON_CONTRACTED:
        return "Non-Contracted";
      case PAID:
        return "Paid";
      case DENIED:
        return "Denied";
      case PARTIALLY_PAID:
        return "Partially Paid\t";
      default:
        return "?";
    }
  }

  /**
   * Gets the definition of this status.
   *
   * @return the definition
   */
  public String getDefinition() {
    switch (this) {
      case IN_NETWORK:
        return "Indicates the claim or claim line was paid in network. This does not indicate the contracting status of the provider.";
      case OUT_OF_NETWORK:
        return "Indicates the claim or claim line was paid out of network. This does not indicate the contracting status of the provider.";
      case OTHER:
        return "Indicates other network status or when a network does not apply.";
      case CONTRACTED:
        return "Indicates the provider was contracted for the service.";
      case NON_CONTRACTED:
        return "Indicates the provider was not contracted for the service.";
      case PAID:
        return "Indicates if the claim was approved for payment.";
      case DENIED:
        return "Indicates if the claim was denied.";
      case PARTIALLY_PAID:
        return "Indicates that some line items on the claim were denied.";
      default:
        return "?";
    }
  }
}
