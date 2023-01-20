package gov.cms.bfd.server.war.commons;

/**
 * Enumerates the value codeset indicating the race category of the {@link
 * org.hl7.fhir.r4.model.Patient}.
 */
public enum RaceCategory {
  /** Represents WHITE race category. */
  WHITE,
  /** Represents BLACK_OR_AFRICAN_AMERICAN race category. */
  BLACK_OR_AFRICAN_AMERICAN,
  /** Represents ASIAN race category. */
  ASIAN,
  /** Represents AMERICAN_INDIAN_OR_ALASKA_NATIVE race category. */
  AMERICAN_INDIAN_OR_ALASKA_NATIVE,
  /** Represents NATIVE_HAWAIIAN_OR_OTHER_PACIFIC_ISLANDER race category. */
  NATIVE_HAWAIIAN_OR_OTHER_PACIFIC_ISLANDER,
  /** Represents UNKNOWN race category. */
  UNKNOWN,
  /** Represents ASKED_NOT_FOUND race category. */
  ASKED_NOT_FOUND;

  /**
   * Gets the system for this {@link RaceCategory}.
   *
   * @return the system
   */
  public String getSystem() {
    switch (this) {
      case UNKNOWN:
      case ASKED_NOT_FOUND:
        return "http://terminology.hl7.org/CodeSystem/v3-NullFlavor";
      default:
        return "urn:oid:2.16.840.1.113883.6.238";
    }
  }

  /**
   * Gets the code for this {@link RaceCategory}.
   *
   * @return the code
   */
  public String toCode() {
    switch (this) {
      case WHITE:
        return "2106-3";
      case BLACK_OR_AFRICAN_AMERICAN:
        return "2054-5";
      case UNKNOWN:
        return "UNK";
      case AMERICAN_INDIAN_OR_ALASKA_NATIVE:
        return "1002-5";
      case ASIAN:
        return "2028-9";
      case NATIVE_HAWAIIAN_OR_OTHER_PACIFIC_ISLANDER:
        return "2076-8";
      case ASKED_NOT_FOUND:
        return "ASKU";
      default:
        return "?";
    }
  }

  /**
   * Gets the display for this {@link RaceCategory}.
   *
   * @return the display
   */
  public String getDisplay() {
    switch (this) {
      case WHITE:
        return "White";
      case BLACK_OR_AFRICAN_AMERICAN:
        return "Black or African American";
      case UNKNOWN:
        return "Unknown";
      case AMERICAN_INDIAN_OR_ALASKA_NATIVE:
        return "American Indian or Alaska Native";
      case ASIAN:
        return "Asian";
      case NATIVE_HAWAIIAN_OR_OTHER_PACIFIC_ISLANDER:
        return "Native Hawaiian or Other Pacific Islander";
      case ASKED_NOT_FOUND:
        return "Asked but no answer";
      default:
        return "?";
    }
  }
}
