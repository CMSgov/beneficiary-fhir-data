package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Institutional Care Team Roles <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-C4BBClaimInstitutionalCareTeamRole.html">ValueSet:
 * C4BB Claim Institutional Care Team Role</a>.
 */
public enum C4BBClaimInstitutionalCareTeamRole {
  /** The primary care provider. */
  PRIMARY,
  /** The attending physician. * */
  ATTENDING,
  /** The referring physician. * */
  REFERRING,
  /** The operating physician. * */
  OPERATING,
  /** The other operating physician. * */
  OTHER_OPERATING,
  /** The performing or rendering provider. * */
  PERFORMING;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {
    switch (this) {
      case PRIMARY:
        // This system includes one value from the base care team role
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
      case ATTENDING:
        return "attending";
      case REFERRING:
        return "referring";
      case OPERATING:
        return "operating";
      case OTHER_OPERATING:
        return "otheroperating";
      case PERFORMING:
        return "performing";
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
      case ATTENDING:
        return "Attending";
      case REFERRING:
        return "Referring";
      case OPERATING:
        return "Operating";
      case OTHER_OPERATING:
        return "Other Operating";
      case PERFORMING:
        return "Performing provider";
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
      case ATTENDING:
        return "The attending physician";
      case REFERRING:
        return "The referring physician";
      case OPERATING:
        return "The operating physician";
      case OTHER_OPERATING:
        return "The other operating physician";
      case PERFORMING:
        return "The performing or rendering provider";
      default:
        return "?";
    }
  }
}
