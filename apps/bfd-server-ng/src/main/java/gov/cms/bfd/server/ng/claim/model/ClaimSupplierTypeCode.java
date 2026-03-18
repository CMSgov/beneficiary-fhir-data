package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim supplier type codes. Suppress SonarQube warning that constant names should comply with
 * naming conventions.
 */
public sealed interface ClaimSupplierTypeCode
    permits ClaimSupplierTypeCode.Valid, ClaimSupplierTypeCode.Invalid {

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
   * @return claim supplier type code or empty Optional if code is null or blank
   */
  static Optional<ClaimSupplierTypeCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimSupplierTypeCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return extension
   */
  default Extension toFhir() {
    // claim supplier type code is a subset of the overall claim provider type code
    // set so we use
    // the same extension and code system here
    return new Extension(SystemUrls.EXT_PROVIDER_TYPE_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_PROVIDER_TYPE_CD, getCode(), getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimSupplierTypeCode {
    /**
     * 0 - CLINICS, GROUPS, ASSOCIATIONS, PARTNERSHIPS, OR OTHER ENTITIES FOR WHOM THE CARRIER'S OWN
     * ID NUMBER HAS BEEN ASSIGNED.
     */
    _0(
        "0",
        "CLINICS, GROUPS, ASSOCIATIONS, PARTNERSHIPS, OR OTHER ENTITIES FOR WHOM THE CARRIER'S OWN ID NUMBER HAS BEEN ASSIGNED."),
    /**
     * 1 - PHYSICIANS OR SUPPLIERS BILLING AS SOLO PRACTITIONERS FOR WHOM SSN'S ARE SHOWN IN THE
     * PHYSICIAN ID CODE FIELD.
     */
    _1(
        "1",
        "PHYSICIANS OR SUPPLIERS BILLING AS SOLO PRACTITIONERS FOR WHOM SSN'S ARE SHOWN IN THE PHYSICIAN ID CODE FIELD."),
    /**
     * 2 - PHYSICIANS OR SUPPLIERS BILLING AS SOLO PRACTITIONERS FOR WHOM THE CARRIER'S OWN
     * PHYSICIAN ID CODE IS SHOWN.
     */
    _2(
        "2",
        "PHYSICIANS OR SUPPLIERS BILLING AS SOLO PRACTITIONERS FOR WHOM THE CARRIER'S OWN PHYSICIAN ID CODE IS SHOWN."),
    /**
     * 3 - SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP)FOR WHOM EI NUMBERS ARE USED IN CODING THE ID
     * FIELD.
     */
    _3(
        "3",
        "SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP)FOR WHOM EI NUMBERS ARE USED IN CODING THE ID FIELD."),
    /**
     * 4 - SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP)FOR WHOM THE CARRIER'S OWN CODE HAS BEEN SHOWN.
     */
    _4(
        "4",
        "SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP)FOR WHOM THE CARRIER'S OWN CODE HAS BEEN SHOWN."),
    /**
     * 5 - INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM EI NUMBERS ARE USED IN
     * CODING THE ID FIELD.
     */
    _5(
        "5",
        "INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM EI NUMBERS ARE USED IN CODING THE ID FIELD."),
    /**
     * 6 - INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM THE CARRIER'S OWN ID NUMBER
     * IS SHOWN.
     */
    _6(
        "6",
        "INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM THE CARRIER'S OWN ID NUMBER IS SHOWN."),
    /**
     * 7 - CLINICS, GROUPS, ASSOCIATIONS, OR PARTNERSHIPS FOR WHOM EI NUMBERS ARE USED IN CODING THE
     * ID FIELD.
     */
    _7(
        "7",
        "CLINICS, GROUPS, ASSOCIATIONS, OR PARTNERSHIPS FOR WHOM EI NUMBERS ARE USED IN CODING THE ID FIELD."),
    /**
     * 8 - OTHER ENTITIES FOR WHOM EI NUMBERS ARE USED IN CODING THE ID FIELD OR PROPRIETORSHIP FOR
     * WHOM EI NUMBERS ARE USED IN CODING THE ID FIELD.
     */
    _8(
        "8",
        "OTHER ENTITIES FOR WHOM EI NUMBERS ARE USED IN CODING THE ID FIELD OR PROPRIETORSHIP FOR WHOM EI NUMBERS ARE USED IN CODING THE ID FIELD.");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimSupplierTypeCode {
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
