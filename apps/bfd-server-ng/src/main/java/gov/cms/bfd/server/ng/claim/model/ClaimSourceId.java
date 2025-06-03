package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ClaimSourceId {
  MEDICAID("N/A", "Medicaid", Optional.empty()),
  NATIONAL_CLAIMS_HISTORY("20000", "NationalClaimsHistory", Optional.of("Adjudicated")),
  FISS("21000", "FISS", Optional.of("PartiallyAdjudicated")),
  MCS("22000", "MCS", Optional.of("PartiallyAdjudicated")),
  VIPS("23000", "VIPS", Optional.of("PartiallyAdjudicated")),
  EDPS("24000", "EDPS", Optional.empty()),
  ENCOUNTER_MEDICAID_DUALS("25000", "EncounterMedicaidDuals", Optional.empty());

  private final String id;
  private final String source;
  private final Optional<String> adjudicationStatus;

  public static ClaimSourceId fromId(String id) {
    return Arrays.stream(values()).filter(c -> c.id.equals(id)).findFirst().get();
  }

  Optional<Coding> toFhirAdjudicationStatus() {
    return adjudicationStatus.map(
        s -> new Coding().setSystem(SystemUrls.BLUE_BUTTON_ADJUDICATION_STATUS).setCode(s));
  }
}
