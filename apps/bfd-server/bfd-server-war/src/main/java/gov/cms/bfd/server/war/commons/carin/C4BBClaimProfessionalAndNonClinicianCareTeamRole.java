package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Professional And Non Clinician Care Team Roles <a
 * href="http://hl7.org/fhir/us/carin-bb/STU1/ValueSet-C4BBClaimProfessionalAndNonClinicianCareTeamRole.html">ValueSet:
 * C4BB Claim Professional And Non Clinician Care Team Role</a>
 */
public enum C4BBClaimProfessionalAndNonClinicianCareTeamRole {
  PRIMARY,
  SUPERVISOR,
  PERFORMING,
  PURCHASED_SERVICE,
  REFERRING,
  OTHER;

  public String getSystem() {
    switch (this) {
      case PRIMARY:
      case SUPERVISOR:
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
      case SUPERVISOR:
        return "supervisor";
      case PERFORMING:
        return "performing";
      case PURCHASED_SERVICE:
        return "purchasedservice";
      case REFERRING:
        return "referring";
      case OTHER:
        return "other";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case PRIMARY:
        return "Primary provider";
      case SUPERVISOR:
        return "Supervising Provider";
      case PERFORMING:
        return "Performing provider";
      case PURCHASED_SERVICE:
        return "Purchased Service";
      case REFERRING:
        return "Referring";
      case OTHER:
        return "Other";
      default:
        return "?";
    }
  }

  public String getDefinition() {
    switch (this) {
      case PRIMARY:
        return "The primary care provider.";
      case SUPERVISOR:
        return "Supervising care provider.";
      case PERFORMING:
        return "The performing or rendering provider";
      case PURCHASED_SERVICE:
        return "A purchased service occurs when one provider purchases a service from another provider and then provides it to the patient, e.g. a diagnostic exam";
      case REFERRING:
        return "The referring physician";
      case OTHER:
        return "Other role on the care team";
      default:
        return "?";
    }
  }
}
