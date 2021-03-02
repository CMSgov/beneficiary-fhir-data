package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Care Team Roles <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-C4BBClaimInstitutionalCareTeamRole.html">ValueSet:
 * C4BB Claim Institutional Care Team Role<a>
 */
public enum C4BBClaimInstitutionalCareTeamRole {
  PRIMARY,
  ATTENDING,
  REFERRING,
  OPERATING,
  OTHER_OPERATING,
  PERFORMING;

  public String getSystem() {
    switch (this) {
      case PRIMARY:
        // This system includes one value from the base care team role
        return "http://terminology.hl7.org/CodeSystem/claimcareteamrole";
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole";
    }
  }

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
