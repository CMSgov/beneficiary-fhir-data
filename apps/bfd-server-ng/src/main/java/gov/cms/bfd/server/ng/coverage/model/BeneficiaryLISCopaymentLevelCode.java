package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Beneficiary Low Income Subsidy Copayment Level Code. */
@RequiredArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum BeneficiaryLISCopaymentLevelCode {

  /** 1 - High. */
  _1("1", "High"),
  /** 4 - 15% Copayment. */
  _4("4", "15% Copayment");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return beneficiary low income subsidy copayment level code
   */
  public static Optional<BeneficiaryLISCopaymentLevelCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_BENE_LIS_COPMT_LVL_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_BENE_LIS_COPMT_LVL_CD_CD, code, null));
  }
}
