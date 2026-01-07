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
  /** "" - innetwork - In Network. */
  IN_NETWORK("", "innetwork", "In Network"),
  /** 2 - outofnetwork - Out Of Network. */
  O("O", "outofnetwork", "Out Of Network"),
  /** 3 - other - Other. */
  M("M", "other", "Other");

  private final String idrCode;
  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param idrCode database code
   * @return claim pricing reason code
   */
  public static Optional<ClaimPricingReasonCode> tryFromCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst();
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
