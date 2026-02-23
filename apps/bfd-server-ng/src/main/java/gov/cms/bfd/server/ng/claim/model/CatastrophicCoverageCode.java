package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Catastrophic coverage codes. */
@RequiredArgsConstructor
@Getter
public enum CatastrophicCoverageCode {
  /** A - Attachment point met on this event. */
  A("A", "Attachment point met on this event"),
  /** C - Above attachment point. */
  C("C", "Above attachment point"),
  /** INVALID - Represents an invalid code that we still want to capture. */
  INVALID("", "");

  private final String code;
  private final String display;
  private String invalidValue;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return catastrophic coverage code
   */
  public static Optional<CatastrophicCoverageCode> tryFromCode(String code) {
    return Optional.of(Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(captureInvalidValue(code)));
  }

  /**
   * Captures an invalid value and maps it to the INVALID enum.
   *
   * @param invalidValue the invalid value to capture
   * @return INVALID catastrophic coverage code
   */
  public static CatastrophicCoverageCode captureInvalidValue(String invalidValue) {
    var invalidCatastrophicCoverageCode = CatastrophicCoverageCode.INVALID;
    invalidCatastrophicCoverageCode.invalidValue = invalidValue;
    return invalidCatastrophicCoverageCode;
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_CTSTRPHC_CVRG_IND_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CATASTROPHIC_COVERAGE_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
