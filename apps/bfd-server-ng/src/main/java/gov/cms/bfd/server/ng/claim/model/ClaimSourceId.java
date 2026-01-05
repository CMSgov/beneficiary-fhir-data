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
  NATIONAL_CLAIMS_HISTORY("20000", "NCH", Optional.of("NationalClaimsHistory")),
  /** FISS Claims. */
  FISS("21000", "FISS", Optional.of("SharedSystem")),
  /** MCS Claims. */
  MCS("22000", "MCS", Optional.of("SharedSystem")),
  /** VMS Claims. */
  VIPS("23000", "VMS", Optional.of("SharedSystem")),
  /** EDPS Claims. */
  EDPS("24000", "EDPS", Optional.empty()),
  /** Dual claims. */
  ENCOUNTER_MEDICAID_DUALS("25000", "EncounterMedicaidDuals", Optional.empty());

  private final String id;
  private final String source;
  private final Optional<String> systemType;

  /**
   * Converts from a database identifier.
   *
   * @param id database id
   * @return Claim source id
   */
  public static ClaimSourceId fromId(String id) {
    return Arrays.stream(values()).filter(c -> c.id.equals(id)).findFirst().get();
  }

  Optional<Coding> toFhirSystemType() {
    return systemType.map(
        s -> new Coding().setSystem(SystemUrls.BLUE_BUTTON_SYSTEM_TYPE).setCode(s));
  }

  Optional<ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome() {
    return switch (this) {
      case NATIONAL_CLAIMS_HISTORY -> Optional.of(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
      default -> Optional.empty();
    };
  }
}
