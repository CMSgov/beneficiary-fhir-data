package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** Claim Final Action Indicator. */
@Getter
@AllArgsConstructor
public enum ClaimFinalAction {
  /** Final Action = Yes. */
  YES('Y', "FinalAction"),
  /** Final Action = No. */
  NO('N', "NotFinalAction");

  private final Character code;
  private final String finalAction;

  /**
   * Converts from a database code.
   *
   * @param code code
   * @return Claim final action
   */
  public static ClaimFinalAction fromCode(Character code) {
    return Arrays.stream(values()).filter(c -> c.code.equals(code)).findFirst().get();
  }

  /**
   * Converts to FA Coding.
   *
   * @return FHIR coding
   */
  public Optional<Coding> toFhirFinalAction() {
    return Optional.of(new Coding(SystemUrls.BLUE_BUTTON_FINAL_ACTION_STATUS, finalAction, null));
  }
}
