package gov.cms.bfd.server.war.commons;

/**
 * Enumerates the value codeset indicating the class of coverage of the {@link
 * gov.cms.bfd.model.rif.Beneficiary}.
 */
public enum CoverageClass {
  GROUP,
  SUBGROUP,
  PLAN,
  SUBPLAN,
  CLASS,
  SUBCLASS,
  SEQUENCE,
  RXBIN,
  RXPCN,
  RXID,
  RXGROUP;

  public String getSystem() {
    return "http://terminology.hl7.org/CodeSystem/coverage-class";
  }

  public String toCode() {
    switch (this) {
      case GROUP:
        return "group";
      case SUBGROUP:
        return "subgroup";
      case PLAN:
        return "plan";
      case SUBPLAN:
        return "subplan";
      case CLASS:
        return "class";
      case SUBCLASS:
        return "subclass";
      case SEQUENCE:
        return "sequence";
      case RXBIN:
        return "rxbin";
      case RXPCN:
        return "rxpcn";
      case RXID:
        return "rxid";
      case RXGROUP:
        return "rxgroup";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case GROUP:
        return "Group";
      case SUBGROUP:
        return "SubGroup";
      case PLAN:
        return "Plan";
      case SUBPLAN:
        return "SubPlan";
      case CLASS:
        return "Class";
      case SUBCLASS:
        return "SubClass";
      case SEQUENCE:
        return "Sequence";
      case RXBIN:
        return "RX BIN";
      case RXPCN:
        return "RX PCN";
      case RXID:
        return "RX Id";
      case RXGROUP:
        return "RX Group";
      default:
        return "?";
    }
  }
}
