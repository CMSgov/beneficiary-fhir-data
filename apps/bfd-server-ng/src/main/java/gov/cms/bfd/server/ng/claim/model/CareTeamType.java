package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SequenceGenerator;
import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Practitioner;

@AllArgsConstructor
public enum CareTeamType {
  ATTENDING("attending", "Attending"),
  OPERATING("operating", "Operating"),
  RENDERING("rendering", "Rendering provider"),
  OTHER("otheroperating", "Other Operating");

  private final String roleCode;
  private final String roleDisplay;

  CareTeamComponents toFhir(
      SequenceGenerator sequenceGenerator, ExplanationOfBenefit eob, String value) {
    var practitioner = new Practitioner();
    var sequence = sequenceGenerator.next();
    practitioner.setId("careteam-provider-" + sequence);
    practitioner.setMeta(
        new Meta()
            .addProfile(SystemUrls.PROFILE_CARIN_BB_PRACTITIONER_2_1_0)
            .addProfile(SystemUrls.PROFILE_US_CORE_PRACTITIONER_6_1_0));
    practitioner.addIdentifier(
        new Identifier()
            .setType(
                new CodeableConcept(
                    new Coding().setSystem(SystemUrls.HL7_IDENTIFIER).setCode("NPI")))
            .setSystem(SystemUrls.NPI)
            .setValue(value));
    // practitioner.addName(new HumanName().setFamily(family));

    var component =
        new ExplanationOfBenefit.CareTeamComponent()
            .setSequence(sequence)
            .setRole(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
                        .setCode(roleCode)
                        .setDisplay(roleDisplay)))
            .setProvider(practitioner.castToReference(eob));
    return new CareTeamComponents(practitioner, component);
  }

  public record CareTeamComponents(
      Practitioner practitioner, ExplanationOfBenefit.CareTeamComponent careTeam) {}
}
