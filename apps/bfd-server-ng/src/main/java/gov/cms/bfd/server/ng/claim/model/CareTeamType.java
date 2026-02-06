package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

@AllArgsConstructor
@Getter
enum CareTeamType {
  ATTENDING("attending", "Attending"),
  OPERATING("operating", "Operating"),
  RENDERING("rendering", "Rendering provider"),
  OTHER("otheroperating", "Other Operating"),
  PRESCRIBING("prescribing", "Prescribing provider"),
  REFERRING("referring", "Referring provider"),
  SERVICE("service", "Service provider"),
  BILLING("billing", "Billing provider");

  private final String roleCode;
  private final String roleDisplay;

  CareTeamComponents toFhir(
      SequenceGenerator sequenceGenerator,
      String value,
      HumanName name,
      Optional<String> pinNumber) {
    var sequence = sequenceGenerator.next();
    var id =
        (roleCode.equals(PRESCRIBING.roleCode)
                ? "careteam-prescriber-practitioner-"
                : "careteam-provider-")
            + sequence;
    var practitioner = ProviderFhirHelper.createPractitioner(id, value, name);

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
