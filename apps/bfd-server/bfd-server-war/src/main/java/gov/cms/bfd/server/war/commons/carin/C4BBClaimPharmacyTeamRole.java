package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Pharmacy Care Team Roles <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-C4BBClaimPharmacyTeamRole.html">ValueSet:
 * C4BB Claim Pharmacy CareTeam Roles</a>
 */
public enum C4BBClaimPharmacyTeamRole {
  PRIMARY,
  PRESCRIBING;

  public String getSystem() {
    switch (this) {
      case PRIMARY:
        return "http://terminology.hl7.org/CodeSystem/claimcareteamrole";
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole";
    }
  }

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
