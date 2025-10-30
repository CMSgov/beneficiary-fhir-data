package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@AllArgsConstructor
@Getter
public enum ClaimSubmissionCode {
  /** 01 - Community/retail pharmacy */
  _01("01", "Community/retail pharmacy"),
  /** 02 - Compounding pharmacy */
  _02("02", "Compounding pharmacy"),
  /** 03 - Home infusion therapy provider */
  _03("03", "Home infusion therapy provider"),
  /** 04 - Institutional pharmacy */
  _04("04", "Institutional pharmacy"),
  /** 05 - Long-term care pharmacy */
  _05("05", "Long-term care pharmacy"),
  /** 06 - Mail order pharmacy */
  _06("06", "Mail order pharmacy"),
  /** 07 - Managed care organization (MCO) pharmacy */
  _07("07", "Managed care organization (MCO) pharmacy"),
  /** 08 - Specialty care pharmacy */
  _08("99", "Specialty care pharmacy"),
  /** 18 - Other */
  _99("99", "Other");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return Pharmacy service type code
   */
  public static Optional<ClaimSubmissionCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_LTC_DSPNSNG_MTHD_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_SUBMISSION_CLARIFICATION_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
