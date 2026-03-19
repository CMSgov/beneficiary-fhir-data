package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Beneficiary Low Income Subsidy Copayment Level Code. */
public sealed interface BeneficiaryLISCopaymentLevelCode
    permits BeneficiaryLISCopaymentLevelCode.Valid, BeneficiaryLISCopaymentLevelCode.Invalid {

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
   * @return beneficiary low income subsidy copayment level code or empty Optional if code is null
   *     or blank
   */
  static Optional<BeneficiaryLISCopaymentLevelCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (BeneficiaryLISCopaymentLevelCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps interface to FHIR spec.
   *
   * @return FHIR Extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_BENE_LIS_COPMT_LVL_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_BENE_LIS_COPMT_LVL_CD, getCode(), null));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements BeneficiaryLISCopaymentLevelCode {
    /** 1 - High. */
    _1("1", "High"),
    /** 4 - 15% Copayment. */
    _4("4", "15% Copayment");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements BeneficiaryLISCopaymentLevelCode {
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
