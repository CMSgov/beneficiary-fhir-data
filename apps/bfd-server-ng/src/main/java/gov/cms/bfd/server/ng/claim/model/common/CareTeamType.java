package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.claim.model.common.ProviderFhirHelper;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

/** Represents the enum Care Team Type. */
@AllArgsConstructor
@Getter
public enum CareTeamType {
  /** Attending care team type. */
  ATTENDING("attending", "Attending"),
  /** Operating care team type. */
  OPERATING("operating", "Operating"),
  /** Rendering care team type. */
  RENDERING("rendering", "Rendering provider"),
  /** Other operating care team type. */
  OTHER("otheroperating", "Other Operating"),
  /** Prescribing care team type. */
  PRESCRIBING("prescribing", "Prescribing provider"),
  /** Referring care team type. */
  REFERRING("referring", "Referring"),
  /** Service care team type. */
  SERVICE("service", "Service provider"),
  /** Billing care team type. */
  BILLING("billing", "Billing provider"),
  /** Supervisor care team type. */
  SUPERVISOR("supervisor", "Supervisor provider");

  private final String roleCode;
  private final String roleDisplay;

  /**
   * toFhir(), but returns a record with a practitioner and CareTeamComponent.
   * @param sequenceGenerator sequence generator
   * @param value value for practitioner
   * @param name name for practitioner
   * @param pinNumber PIN for practitioner identifier
   * @return a CareTeamComponent + Practitioner record
   */
  public CareTeamComponents toFhir(
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

  /**
   * Create a record from the careteam component.
   * @param practitioner associated practitioner
   * @param careTeam associated careteam component
   */
  public record CareTeamComponents(
      Practitioner practitioner, ExplanationOfBenefit.CareTeamComponent careTeam) {}
}
