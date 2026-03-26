package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** Deductible coinsurance codes. */
public sealed interface ClaimLineDeductibleCoinsuranceCode
    permits ClaimLineDeductibleCoinsuranceCode.Valid, ClaimLineDeductibleCoinsuranceCode.Invalid {

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
   * @return coinsurance code
   */
  static Optional<ClaimLineDeductibleCoinsuranceCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimLineDeductibleCoinsuranceCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return Coding
   */
  default Coding toFhir() {
    return new Coding()
        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_DEDUCTIBLE_COINSURANCE_CODE)
        .setCode(getCode())
        .setDisplay(getDisplay());
  }

  /** Enum for all known, valid coinsurance codes. */
  @Getter
  @AllArgsConstructor
  enum Valid implements ClaimLineDeductibleCoinsuranceCode {
    /** 0 - Charges are subject to deductible and coinsurance. */
    _0("0", "Charges are subject to deductible and coinsurance"),
    /** 1 - Charges are not subject to deductible. */
    _1("1", "Charges are not subject to deductible"),
    /** 2 - Charges are not subject to coinsurance. */
    _2("2", "Charges are not subject to coinsurance"),
    /** 3 - Charges are not subject to deductible or coinsurance. */
    _3("3", "Charges are not subject to deductible or coinsurance"),
    /**
     * 4 - No charge or units associated with this revenue center code. (For multiple HCPCS per
     * single revenue center code) For revenue center code 0001, the following MSP override values
     * may be present:.
     */
    _4(
        "4",
        "No charge or units associated with this revenue center code. (For multiple HCPCS per single revenue center code) For revenue center code 0001, the following MSP override values may be present:"),
    /** M - Override code; EGHP (employer group health plan) services involved. */
    M("M", "Override code; EGHP (employer group health plan) services involved"),
    /** N - Override code; non-EGHP services involved. */
    N("N", "Override code; non-EGHP services involved"),
    /** X - Override code: MSP (Medicare is secondary payer) cost avoided. */
    X("X", "Override code: MSP (Medicare is secondary payer) cost avoided");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimLineDeductibleCoinsuranceCode {
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
