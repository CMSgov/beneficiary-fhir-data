package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Pharmacy Care Team Roles <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-C4BBClaimPharmacyTeamRole.html">ValueSet:
 * C4BB Claim Pharmacy CareTeam Roles</a>.
 */
public enum C4BBClaimPharmacyTeamRole {
  /** The primary care provider. */
  PRIMARY,
  /** The prescribing provider. */
  PRESCRIBING;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {
    switch (this) {
      case PRIMARY:
        return "http://terminology.hl7.org/CodeSystem/claimcareteamrole";
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole";
    }
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  public String toCode() {
    switch (this) {
      case PRIMARY:
        return "primary";
      case PRESCRIBING:
        return "prescribing";
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
      case PRIMARY:
        return "Primary provider";
      case PRESCRIBING:
        return "Prescribing provider";
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
      case PRIMARY:
        return "The primary care provider.";
      case PRESCRIBING:
        return "The prescribing provider";
      default:
        return "?";
    }
  }
}
