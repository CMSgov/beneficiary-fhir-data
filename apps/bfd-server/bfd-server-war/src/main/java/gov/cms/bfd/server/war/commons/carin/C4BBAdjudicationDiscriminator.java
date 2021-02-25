package gov.cms.bfd.server.war.commons.carin;

public enum C4BBAdjudicationDiscriminator {
  ALLOWED_UNITS,
  IN_OUT_NETWORK,
  DENIAL_REASON;

  public String getSystem() {

    return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudicationDiscriminator";
  }

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
