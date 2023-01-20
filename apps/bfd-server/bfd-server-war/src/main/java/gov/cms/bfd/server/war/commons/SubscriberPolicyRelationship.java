package gov.cms.bfd.server.war.commons;

/** Enumerates the value code set indicating the race category of the Patient. */
public enum SubscriberPolicyRelationship {
  /** Represents the Beneficiary is a child of the Subscriber. */
  CHILD,
  /** Represents the Beneficiary is a parent of the Subscriber. */
  PARENT,
  /** Represents the Beneficiary is a spouse or equivalent of the Subscriber. */
  SPOUSE,
  /** Represents the Beneficiary is a common law spouse or equivalent of the Subscriber. */
  COMMON,
  /** Represents the Beneficiary has some other relationship the Subscriber. */
  OTHER,
  /** Represents the Beneficiary is the Subscriber. */
  SELF,
  /** Represents the Beneficiary is covered under insurance of the subscriber due to an injury. */
  INJURED;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {
    return "http://terminology.hl7.org/CodeSystem/subscriber-relationship";
  }

  /**
   * Gets the code.
   *
   * @return the string
   */
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

  /**
   * Gets the display.
   *
   * @return the display
   */
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
