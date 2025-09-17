package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The source of the claim. */
@Getter
@AllArgsConstructor
public enum ClaimSourceId {
  /** Medicaid Claims. */
  MEDICAID("N/A", "Medicaid", Optional.empty()),
  /** NCH Claims. */
  NATIONAL_CLAIMS_HISTORY("20000", "NationalClaimsHistory", Optional.of("Adjudicated")),
  /** FISS Claims. */
  FISS("21000", "FISS", Optional.of("PartiallyAdjudicated")),
  /** MCS Claims. */
  MCS("22000", "MCS", Optional.of("PartiallyAdjudicated")),
  /** VIPS Claims. */
  VIPS("23000", "VIPS", Optional.of("PartiallyAdjudicated")),
  /** EDPS Claims. */
  EDPS("24000", "EDPS", Optional.empty()),
  /** Dual claims. */
  ENCOUNTER_MEDICAID_DUALS("25000", "EncounterMedicaidDuals", Optional.empty());

  private final String id;
  private final String source;
  private final Optional<String> adjudicationStatus;

  /**
   * Converts from a database identifier.
   *
   * @param id database id
   * @return Claim source id
   */
  public static ClaimSourceId fromId(String id) {
    return Arrays.stream(values()).filter(c -> c.id.equals(id)).findFirst().get();
  }

  Optional<Coding> toFhirAdjudicationStatus() {
    return adjudicationStatus.map(
        s -> new Coding().setSystem(SystemUrls.BLUE_BUTTON_ADJUDICATION_STATUS).setCode(s));
  }

  Optional<ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome() {
    return switch (this) {
      case NATIONAL_CLAIMS_HISTORY -> Optional.of(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
      default -> Optional.empty();
    };
  }
}
