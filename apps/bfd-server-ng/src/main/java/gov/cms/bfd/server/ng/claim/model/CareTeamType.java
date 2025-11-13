package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;

@AllArgsConstructor
enum CareTeamType {
  ATTENDING("attending", "Attending"),
  OPERATING("operating", "Operating"),
  RENDERING("rendering", "Rendering provider"),
  OTHER("otheroperating", "Other Operating"),
  PRESCRIBING("prescribing", "Prescribing"),
  REFERRING("referring", "Referring provider");

  private final String roleCode;
  private final String roleDisplay;

  CareTeamComponents toFhir(
      SequenceGenerator sequenceGenerator,
      String value,
      Optional<String> familyName,
      Optional<String> pinNumber) {
    var practitioner = new Practitioner();
    var sequence = sequenceGenerator.next();
    if (roleCode.equals(PRESCRIBING.roleCode)) {
      practitioner.setId("careteam-prescriber-practitioner-" + sequence);
    } else {
      practitioner.setId("careteam-provider-" + sequence);
    }
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
    // todo: modify based on BFD-4286
    familyName.ifPresent(n -> practitioner.addName(new HumanName().setFamily(n)));

    pinNumber.ifPresent(
        p ->
            practitioner.addIdentifier(
                new Identifier().setSystem(SystemUrls.BLUE_BUTTON_PIN_NUM).setValue(p)));

    var component =
        new ExplanationOfBenefit.CareTeamComponent()
            .setSequence(sequence)
            .setRole(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
                        .setCode(roleCode)
                        .setDisplay(roleDisplay)))
            .setProvider(new Reference(practitioner));
    return new CareTeamComponents(practitioner, component);
  }

  public record CareTeamComponents(
      Practitioner practitioner, ExplanationOfBenefit.CareTeamComponent careTeam) {}
}
