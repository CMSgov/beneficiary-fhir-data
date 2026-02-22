package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Catastrophic coverage codes. */
public record CatastrophicCoverageCode(String code, String display) {
  /** A - Attachment point met on this event. */
  public static final CatastrophicCoverageCode A =
      new CatastrophicCoverageCode("A", "Attachment point met on this event");

  /** C - Above attachment point. */
  public static final CatastrophicCoverageCode C =
      new CatastrophicCoverageCode("C", "Above attachment point");

  /**
   * Convert from a database code.
   *
   * @param code database code.
   * @return catastrophic coverage code and its matching display, or an empty string for the display
   *     if code is invalid.
   */
  public static Optional<CatastrophicCoverageCode> tryFromCode(String code) {
    return switch (code) {
      case "A" -> Optional.of(A);
      case "C" -> Optional.of(C);
      default -> Optional.of(new CatastrophicCoverageCode(code, ""));
    };
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
