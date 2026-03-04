package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Beneficiary Low Income Subsidy Copayment Level Code. */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum BeneficiaryLISCopaymentLevelCode {

  /** 1 - High. */
  _1("1", "High"),
  /** 4 - 15% Copayment. */
  _4("4", "15% Copayment"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return beneficiary low income subsidy copayment level code
   */
  public static Optional<BeneficiaryLISCopaymentLevelCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(values())
            .filter(v -> v.code.equals(code))
            .findFirst()
            .orElse(handleInvalidValue(code)));
  }

  /**
   * Handles scenarios where code could not be mapped to a valid value.
   *
   * @param invalidValue the invalid value to capture
   * @return beneficiary low income subsidy copayment level code
   */
  public static BeneficiaryLISCopaymentLevelCode handleInvalidValue(String invalidValue) {
    var invalidBeneficiaryLISCopaymentLevelCode = BeneficiaryLISCopaymentLevelCode.INVALID;
    invalidBeneficiaryLISCopaymentLevelCode.code = invalidValue;
    return invalidBeneficiaryLISCopaymentLevelCode;
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_BENE_LIS_COPMT_LVL_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_BENE_LIS_COPMT_LVL_CD_CD, code, null));
  }
}
