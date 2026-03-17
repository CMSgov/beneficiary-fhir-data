package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim payment codes. Suppress SonarQube warning that constant names should comply with naming
 * conventions.
 */
@AllArgsConstructor
@Getter
public enum ClaimPaymentCode {
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

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim payment 80 100 code
   */
  public static Optional<ClaimPaymentCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_PMT_80_100_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_PMT_80_100_CD, code, display));
  }
}
