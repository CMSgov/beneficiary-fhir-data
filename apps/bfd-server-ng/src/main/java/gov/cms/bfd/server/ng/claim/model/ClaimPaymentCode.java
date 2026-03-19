package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Claim payment codes. */
public sealed interface ClaimPaymentCode permits ClaimPaymentCode.Valid, ClaimPaymentCode.Invalid {
  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getDisplay();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim payment 80 100 code
   */
  static Optional<ClaimPaymentCode> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimPaymentCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return Extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_PMT_80_100_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_PMT_80_100_CD, getCode(), getDisplay()));
  }

  /** Claim payment codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimPaymentCode {
    /** 0 - 80%. */
    _0("0", "80%"),
    /** 1 - 100%. */
    _1("1", "100%"),
    /** 3 - 100% Limitation of liability only. */
    _3("3", "100% Limitation of liability only"),
    /** 4 - 75% reimbursement. */
    _4("4", "75% reimbursement");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimPaymentCode {
    @Override
    public String getDisplay() {
      return "";
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
