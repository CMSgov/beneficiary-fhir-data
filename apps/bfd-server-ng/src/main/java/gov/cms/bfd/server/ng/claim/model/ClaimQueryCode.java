package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim query codes. */
@AllArgsConstructor
@Getter
@SuppressWarnings("java:S115")
public enum ClaimQueryCode {
  /** 0 - CREDIT ADJUSTMENT. */
  _0("0", "CREDIT ADJUSTMENT"),
  /** 1 - INTERIM BILL. */
  _1("1", "INTERIM BILL"),
  /** 2 - HOME HEALTH AGENCY (HHA) BENEFITS EXHAUSTED (OBSOLETE 7/98). */
  _2("2", "HOME HEALTH AGENCY (HHA) BENEFITS EXHAUSTED (OBSOLETE 7/98)"),
  /** 3 - FINAL BILL. */
  _3("3", "FINAL BILL"),
  /** 4 - DISCHARGE NOTICE (OBSOLETE 7/98). */
  _4("4", "DISCHARGE NOTICE (OBSOLETE 7/98)"),
  /** 5 - DEBIT ADJUSTMENT. */
  _5("5", "DEBIT ADJUSTMENT"),
  /** C - CREDIT. */
  C("C", "CREDIT"),
  /** D - DEBIT. */
  D("D", "DEBIT");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim query code
   */
  public static Optional<ClaimQueryCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_QUERY_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_QUERY_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
