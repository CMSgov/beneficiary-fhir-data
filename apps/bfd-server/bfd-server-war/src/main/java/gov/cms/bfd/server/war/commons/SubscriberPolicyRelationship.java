package gov.cms.bfd.server.war.commons;

/** Enumerates the value codeset indicating the race category of the {@link Patient}. */
public enum SubscriberPolicyRelationship {
  CHILD,
  PARENT,
  SPOUSE,
  COMMON,
  OTHER,
  SELF,
  INJURED;

  public String getSystem() {
    return "http://terminology.hl7.org/CodeSystem/subscriber-relationship";
  }

  public String toCode() {
    switch (this) {
      case CHILD:
        return "child";
      case PARENT:
        return "parent";
      case SPOUSE:
        return "spouse";
      case COMMON:
        return "common";
      case OTHER:
        return "other";
      case SELF:
        return "self";
      case INJURED:
        return "injured";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case CHILD:
        return "Child";
      case PARENT:
        return "Parent";
      case SPOUSE:
        return "Spouse";
      case COMMON:
        return "Common Law Spouse";
      case OTHER:
        return "Other";
      case SELF:
        return "Self";
      case INJURED:
        return "Injured Party";
      default:
        return "?";
    }
  }
}
