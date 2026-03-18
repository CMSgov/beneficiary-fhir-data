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
public sealed interface ClaimQueryCode permits ClaimQueryCode.Valid, ClaimQueryCode.Invalid {

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
   * @return claim query code or empty Optional if code is null or blank
   */
  static Optional<ClaimQueryCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimQueryCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps interface to FHIR spec.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return supportingInfoFactory
   */
  default ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_QUERY_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_QUERY_CODE)
                .setCode(getCode())
                .setDisplay(getDisplay())));
    return supportingInfo;
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimQueryCode {
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
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimQueryCode {
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
