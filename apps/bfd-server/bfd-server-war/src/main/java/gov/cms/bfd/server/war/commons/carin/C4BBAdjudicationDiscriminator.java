package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN CodeSystem for Adjudication slices <a
 * href="http://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBAdjudicationDiscriminator.html">CodeSystem:
 * C4BB Adjudication Discriminator</a>.
 */
public enum C4BBAdjudicationDiscriminator {
  /** Defines the adjudication slice to define allowed units. */
  ALLOWED_UNITS,
  /**
   * Defines the adjudication and item.adjudication slice to indicate whether a claim was
   * adjudicated in or out of network.
   */
  IN_OUT_NETWORK,
  /** Defines the adjudication slice to identify the denial reason. */
  DENIAL_REASON;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {

    return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudicationDiscriminator";
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  public String toCode() {
    switch (this) {
      case ALLOWED_UNITS:
        return "allowedunits";
      case IN_OUT_NETWORK:
        return "inoutnetwork";
      case DENIAL_REASON:
        return "denialreason";
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
      case ALLOWED_UNITS:
        return "allowed units";
      case IN_OUT_NETWORK:
        return "in or Out of Network";
      case DENIAL_REASON:
        return "Denial Reason";
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
      case ALLOWED_UNITS:
        return "defines the adjudication slice to define allowed units";
      case IN_OUT_NETWORK:
        return "defines the adjudication and item.adjudication slice to indicate whether a claim was adjudicatd in or out of network";
      case DENIAL_REASON:
        return "defines the adjudication slice to identify the denial reason";
      default:
        return "?";
    }
  }
}
