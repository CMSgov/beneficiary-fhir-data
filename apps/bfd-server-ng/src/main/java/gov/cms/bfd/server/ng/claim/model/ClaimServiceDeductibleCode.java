package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim service deductible codes.
 */
public sealed interface ClaimServiceDeductibleCode
    permits ClaimServiceDeductibleCode.Valid, ClaimServiceDeductibleCode.Invalid {

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
   * @return claim service deductible code or empty Optional if code is null or blank
   */
  static Optional<ClaimServiceDeductibleCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimServiceDeductibleCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_SRVC_DDCTBL_SW_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_SRVC_DDCTBL_SW, getCode(), getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimServiceDeductibleCode {
    /** 0 - Service Subject to Deductible. */
    _0("0", "Service Subject to Deductible"),
    /** 1 - Service Not Subject to Deductible. */
    _1("1", "Service Not Subject to Deductible");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimServiceDeductibleCode {
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
