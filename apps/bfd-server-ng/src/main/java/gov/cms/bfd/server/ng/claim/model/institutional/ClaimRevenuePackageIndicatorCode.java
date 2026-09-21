package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** The "Revenue Package Indicator Code" for a claim. */
public sealed interface ClaimRevenuePackageIndicatorCode
    permits ClaimRevenuePackageIndicatorCode.Valid, ClaimRevenuePackageIndicatorCode.Invalid {

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
   * @return beneficiary low income subsidy copayment level code or empty Optional if code is null
   *     or blank
   */
  static Optional<ClaimRevenuePackageIndicatorCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimRevenuePackageIndicatorCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Create a FHIR Extension.
   *
   * @return Extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_REVENUE_PACKAGE_INDICATOR_CODE)
        .setValue(
            new Coding(
                SystemUrls.BLUE_BUTTON_CODE_SYSTEM_REVENUE_PACKAGE_INDICATOR_CODE,
                getCode(),
                getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimRevenuePackageIndicatorCode {
    /** Not Packaged Code. */
    NOT_PACKAGE("NOT PACKAGED", "0"),
    /** Packaged Code. */
    PACKAGED("PACKAGED SERVICE (SERVICE INDICATOR N)", "1"),
    /** Artificial Code. */
    ARTIFICIAL("ARTIFICIAL CHARGES FOR SURGICAL PROCEDURE (EFF. 7/2004)", "2"),
    /** Partial Code. */
    PARTIAL(
        "PACKAGED AS PART OF PARTIAL HOSPITALIZATION PER DIEM OR DAILY MENTAL HEALTH SERVICE PER DIEM",
        "3"),
    /** Drug Admin Code. */
    DRUG_ADMIN("Drug Admin", "4"),
    /** FQHC. */
    FQHC("Federally Qualified Health Centers (FQHC) DIEM", "5"),
    /** NOCOIN. */
    NOCOIN("FQHC NOCOIN", "6");

    private final String display;
    private final String code;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimRevenuePackageIndicatorCode {
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
