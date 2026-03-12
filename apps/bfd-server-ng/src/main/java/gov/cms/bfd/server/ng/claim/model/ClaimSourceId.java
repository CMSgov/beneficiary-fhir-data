package gov.cms.bfd.server.ng.claim.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The source of the claim. */
@Getter
@AllArgsConstructor
public enum ClaimSourceId {
  /** Medicaid Claims. */
  MEDICAID("N/A", "Medicaid"),
  /** NCH Claims. */
  NATIONAL_CLAIMS_HISTORY("20000", "NCH"),
  /** FISS Claims. */
  FISS("21000", "FISS"),
  /** MCS Claims. */
  MCS("22000", "MCS"),
  /** VMS Claims. */
  VMS("23000", "VMS"),
  /** EDPS Claims. */
  EDPS("24000", "EDPS"),
  /** Dual claims. */
  ENCOUNTER_MEDICAID_DUALS("25000", "EncounterMedicaidDuals");

  private final String id;
  private final String source;

  /**
   * Converts from a database identifier.
   *
   * @param id database id
   * @return Claim source id
   */
  public static ClaimSourceId fromId(String id) {
    return Arrays.stream(values())
        .filter(c -> c.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown ClaimSourceId: " + id));
  }

  Optional<ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome() {
    return switch (this) {
      case NATIONAL_CLAIMS_HISTORY -> Optional.of(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
      default -> Optional.empty();
    };
  }
}
