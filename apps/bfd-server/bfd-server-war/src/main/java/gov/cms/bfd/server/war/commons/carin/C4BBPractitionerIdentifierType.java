package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN Value set for Practitioner Identifiers <a
 * href="http://hl7.org/fhir/us/carin-bb/STU1/ValueSet-C4BBPractitionerIdentifierType.html">ValueSet:
 * C4BB Practitioner Identifier Type<a>
 */
public enum C4BBPractitionerIdentifierType {
  NPI,
  UPIN;

  public String getSystem() {
    switch (this) {
      case NPI:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType";
      default:
        return "http://terminology.hl7.org/CodeSystem/v2-0203";
    }
  }

  public String toCode() {
    switch (this) {
      case NPI:
        return "npi";
      case UPIN:
        return "UPIN";
      default:
        return "?";
    }
  }

  public String getDisplay() {
    switch (this) {
      case NPI:
        return "National Provider Identifier";
      case UPIN:
        return "Medicare/CMS (formerly HCFA)'s Universal Physician Identification numbers";
      default:
        return "?";
    }
  }

  public String getDefinition() {
    switch (this) {
      case NPI:
        return "National Provider Identifier";
      case UPIN:
        return "An identifier for a provider within the CMS/Medicare program. A globally unique identifier for the provider in the Medicare program.";
      default:
        return "?";
    }
  }
}
