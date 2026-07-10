package gov.cms.bfd.server.ng.claim.model;

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
  String getCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getDisplay();

  /**
   * Gets the CMS display value.
   *
   * @return the CMS display
   */
  String getCmsDisplay();

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
            .filter(v -> v.code.equals(code))
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
   * Maps the display type to a ClaimContext. It is temporarily being used in addCareTeam from
   * ProviderHistoryBase to determine the NPI type for non-prior authorization providers. This will
   * no longer be needed once we have separate NPI type fields for each npi in BFD-4661.
   *
   * @return a ClaimContext or an empty Optional if the display did not match
   */
  default Optional<ClaimContext> toContext() {
    if (getDisplay().equals(ClaimType.INSTITUTIONAL.getCode())) {
      return Optional.of(ClaimContext.INSTITUTIONAL);
    }
    if (getDisplay().equals(ClaimType.PROFESSIONAL.getCode())) {
      return Optional.of(ClaimContext.PROFESSIONAL);
    }
    return Optional.empty();
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
                .setDisplay(getDisplay()))
        .addCoding(
            new Coding()
                .setSystem(SystemUrls.BLUEBUTTON_CLAIM_TYPE)
                .setCode(getCode())
                .setDisplay(getCmsDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S1192")
  enum Valid implements ClaimTypePriorAuth {
    /** B - professional - Part B - Part B. */
    B("B", ClaimType.PROFESSIONAL.getCode(), "Part B", "Part B"),
    /** C - professional - Durable Medical Equipment - Part B. */
    D("D", ClaimType.PROFESSIONAL.getCode(), "Durable Medical Equipment", "Part B"),
    /** I - institutional - Inpatient - Part A. */
    I("I", ClaimType.INSTITUTIONAL.getCode(), "Inpatient", "Part A"),
    /** O - institutional - Outpatient - Part B. */
    O("O", ClaimType.INSTITUTIONAL.getCode(), "Outpatient", "Part B"),
    /** H - institutional - Home Health - Part A. */
    H("H", ClaimType.INSTITUTIONAL.getCode(), "Home Health", "Part A"),
    /** C - institutional - Hospice - Part A. */
    C("C", ClaimType.INSTITUTIONAL.getCode(), "Hospice", "Part A");

    private final String code;
    private final String display;
    private final String cmsDisplay;
    private final String insuranceType;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimTypePriorAuth {
    @Override
    public String getInsuranceType() {
      return "";
    }

    @Override
    public String getCmsDisplay() {
      return "";
    }

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
