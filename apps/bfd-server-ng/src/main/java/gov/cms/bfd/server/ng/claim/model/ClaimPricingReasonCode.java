package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** Pricing Exception Codes. */
@Getter
@AllArgsConstructor
public enum ClaimPricingReasonCode {
  /** "" - innetwork. */
  IN_NETWORK("", "innetwork"),
  /** 2 - outofnetwork. */
  O("O", "outofnetwork"),
  /** 3 - other. */
  M("M", "other");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim pricing reason code
   */
  public static Optional<ClaimPricingReasonCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  /**
   * Converts this reason code into a list of FHIR {@link Coding} objects for multiple code systems.
   *
   * @return list of FHIR codings
   */
  Coding toFhir() {
    return new Coding()
        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_PAYER_ADJUDICATION_STATUS)
        .setCode(code)
        .setDisplay(display);
  }
}
