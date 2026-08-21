package gov.cms.bfd.server.ng.claim.model.priorauth;

import gov.cms.bfd.server.ng.claim.model.common.ClaimType;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

/** Claim type prior auth code. */
public sealed interface ClaimTypePriorAuth
    permits ClaimTypePriorAuth.Valid, ClaimTypePriorAuth.Invalid {
  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCmsCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getCode();

  /**
   * Gets the CMS display value.
   *
   * @return the CMS display
   */
  String getDisplay();

  /**
   * Gets the insurance type value.
   *
   * @return the insurance type
   */
  String getInsuranceType();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim type prior auth code or empty Optional if code is null or blank
   */
  static Optional<ClaimTypePriorAuth> tryFromCode(String code) {
    if (code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(ClaimTypePriorAuth.Valid.values())
            .filter(v -> v.cmsCode.equals(code))
            .map(v -> (ClaimTypePriorAuth) v)
            .findFirst()
            .orElseGet(() -> new ClaimTypePriorAuth.Invalid(code)));
  }

  /**
   * Converts the insurance information into a FHIR InsuranceComponent.
   *
   * @return an InsuranceComponent
   */
  default ExplanationOfBenefit.InsuranceComponent toFhirInsurance() {
    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    insurance.setCoverage(new Reference().setDisplay(getInsuranceType()));
    return insurance;
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return CodeableConcept
   */
  default CodeableConcept toFhir() {
    return new CodeableConcept()
        .addCoding(
            new Coding()
                .setSystem(SystemUrls.HL7_CLAIM_TYPE)
                .setCode(getCode())
                .setDisplay(getCode()))
        .addCoding(
            new Coding()
                .setSystem(SystemUrls.BLUEBUTTON_CLAIM_TYPE)
                .setCode(getCmsCode())
                .setDisplay(getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S1192")
  enum Valid implements ClaimTypePriorAuth {
    /** B - professional - Part B - Part B. */
    B("B", ClaimType.PROFESSIONAL.getCode(), "Part B", "Part B"),
    /** D - professional - Durable Medical Equipment - Part B. */
    D("D", ClaimType.PROFESSIONAL.getCode(), "Durable Medical Equipment", "Part B"),
    /** I - institutional - Inpatient - Part A. */
    I("I", ClaimType.INSTITUTIONAL.getCode(), "Inpatient", "Part A"),
    /** O - institutional - Outpatient - Part B. */
    O("O", ClaimType.INSTITUTIONAL.getCode(), "Outpatient", "Part B"),
    /** H - institutional - Home Health - Part A. */
    H("H", ClaimType.INSTITUTIONAL.getCode(), "Home Health", "Part A"),
    /** C - institutional - Hospice - Part A. */
    C("C", ClaimType.INSTITUTIONAL.getCode(), "Hospice", "Part A");

    private final String cmsCode;
    private final String code;
    private final String display;
    private final String insuranceType;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimTypePriorAuth {
    @Override
    public String getInsuranceType() {
      return "";
    }

    @Override
    public String getDisplay() {
      return "";
    }

    @Override
    public String getCode() {
      return "";
    }

    @Override
    public String getCmsCode() {
      return code;
    }
  }
}
