package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim supplier type codes. In practice, only certain values will be populated, since this uses a
 * subset of provider codes, but we bind to that codesystem for ease.
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
    // claim supplier type code is a subset of the overall claim provider type code set so we use
    // the same extension and code system here
    return new Extension(SystemUrls.EXT_PROVIDER_TYPE_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_PROVIDER_TYPE_CD, getCode(), getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimSupplierTypeCode {
    /**
     * 0 - CLINICS, GROUPS, ASSOCIATIONS, INTERVENTION, OR OTHER ENTITIES FOR WHICH THE CARRIER'S
     * OWN ID NUMBER HAS BEEN ASSIGNED.
     */
    _0(
        "0",
        "CLINICS, GROUPS, ASSOCIATIONS, INTERVENTION, OR OTHER ENTITIES FOR WHICH THE CARRIER'S OWN ID NUMBER HAS BEEN ASSIGNED."),
    /**
     * 1 - PHYSICIANS OR SUPPLIERS BILLING AS SOLO-PRACTIONERS FOR WHOM SOCIAL SECURITY NUMBERS
     * (SSN) ARE SHOWN IN THE PHYSICIAN ID CODE FIELD.
     */
    _1(
        "1",
        "PHYSICIANS OR SUPPLIERS BILLING AS SOLO-PRACTIONERS FOR WHOM SOCIAL SECURITY NUMBERS (SSN) ARE SHOWN IN THE PHYSICIAN ID CODE FIELD."),
    /**
     * 2 - PHYSICIANS OR SUPPLIERS BILLING AS SOLO-PRACTITIONERS FOR THE CARRIER'S OWN PHYSICIAN ID
     * CODE IS SHOWN.
     */
    _2(
        "2",
        "PHYSICIANS OR SUPPLIERS BILLING AS SOLO-PRACTITIONERS FOR THE CARRIER'S OWN PHYSICIAN ID CODE IS SHOWN."),
    /** 3 - SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP). */
    _3("3", "SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP)"),
    /**
     * 4 - SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP) FOR WHOM THE CARRIER'S OWN CODE HAS BEEN
     * SHOWN.
     */
    _4(
        "4",
        "SUPPLIERS (OTHER THAN SOLE PROPRIETORSHIP) FOR WHOM THE CARRIER'S OWN CODE HAS BEEN SHOWN."),
    /**
     * 5 - INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM EMPLOYER ID NUMBERS ARE
     * USED IN CODING THE ID FIELD.
     */
    _5(
        "5",
        "INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM EMPLOYER ID NUMBERS ARE USED IN CODING THE ID FIELD."),
    /**
     * 6 - INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM THE CARRIER'S OWN ID NUMBER
     * IS SHOWN.
     */
    _6(
        "6",
        "INSTITUTIONAL PROVIDERS AND INDEPENDENT LABORATORIES FOR WHOM THE CARRIER'S OWN ID NUMBER IS SHOWN."),
    /**
     * 7 - CLINICS, GROUPS, ASSOCIATIONS, OR PARTNERSHIPS, FOR WHICH EMPLOYER ID NUMBERS ARE USED IN
     * CODING THE ID FIELD.
     */
    _7(
        "7",
        "CLINICS, GROUPS, ASSOCIATIONS, OR PARTNERSHIPS, FOR WHICH EMPLOYER ID NUMBERS ARE USED IN CODING THE ID FIELD."),
    /** 8 - OTHER ENTITIES FOR WHOM EMPLOYER ID NUMBERS ARE USED IN CODING THE ID FIELD. */
    _8("8", "OTHER ENTITIES FOR WHOM EMPLOYER ID NUMBERS ARE USED IN CODING THE ID FIELD"),
    /** 20 - PHYSICIAN. */
    _20("20", "PHYSICIAN"),
    /** 21 - OSTEOPATH. */
    _21("21", "OSTEOPATH"),
    /** 22 - INDEPENDENT DIAGNOSTIC TESTING FACILITY (IDTF). */
    _22("22", "INDEPENDENT DIAGNOSTIC TESTING FACILITY (IDTF)"),
    /** 23 - MULTIPLE PHYSICIAN PRACTICE. */
    _23("23", "MULTIPLE PHYSICIAN PRACTICE"),
    /** 24 - AMBULATORY SURGICAL CENTER. */
    _24("24", "AMBULATORY SURGICAL CENTER"),
    /** 25 - INDEPENDENT LAB. */
    _25("25", "INDEPENDENT LAB"),
    /** 27 - CLINICAL PSYCHOLOGIST. */
    _27("27", "CLINICAL PSYCHOLOGIST"),
    /** 28 - NURSE. */
    _28("28", "NURSE"),
    /** 29 - CHIROPRACTOR. */
    _29("29", "CHIROPRACTOR"),
    /** 30 - PODIATRIST. */
    _30("30", "PODIATRIST"),
    /** 31 - PHYSICAL THERAPIST. */
    _31("31", "PHYSICAL THERAPIST"),
    /** 32 - DIETITIAN/NUTRITIONIST. */
    _32("32", "DIETITIAN/NUTRITIONIST"),
    /** 33 - OPTOMETRIST. */
    _33("33", "OPTOMETRIST"),
    /** 34 - AUDIOLOGIST. */
    _34("34", "AUDIOLOGIST"),
    /** 35 - PSYCHOLOGIST. */
    _35("35", "PSYCHOLOGIST"),
    /** 36 - DENTIST. */
    _36("36", "DENTIST"),
    /** 37 - MAMMOGRAPHY SCREENING CENTER. */
    _37("37", "MAMMOGRAPHY SCREENING CENTER"),
    /** 38 - NURSE PRACTITIONER/CLINICAL NURSE SPECIALIST. */
    _38("38", "NURSE PRACTITIONER/CLINICAL NURSE SPECIALIST"),
    /** 39 - MACILLOFACIAL SURGEON. */
    _39("39", "MACILLOFACIAL SURGEON"),
    /** 40 - MISCELLANEOUS. */
    _40("40", "MISCELLANEOUS"),
    /** 50 - SUPPLIER. */
    _50("50", "SUPPLIER"),
    /** 51 - FREESTANDING CARDIAC CATH LAB. */
    _51("51", "FREESTANDING CARDIAC CATH LAB"),
    /** 55 - OPTICIAN. */
    _55("55", "OPTICIAN"),
    /** 56 - CLINICAL SOCIAL WORKER. */
    _56("56", "CLINICAL SOCIAL WORKER"),
    /** 60 - HEALTH AGENCY. */
    _60("60", "HEALTH AGENCY"),
    /** 61 - AMBULANCE. */
    _61("61", "AMBULANCE"),
    /** 64 - DRUG AND DEPARTMENT STORE. */
    _64("64", "DRUG AND DEPARTMENT STORE"),
    /** 65 - PORTABLE X-RAY. */
    _65("65", "PORTABLE X-RAY"),
    /** 99 - HOSPITAL. */
    _99("99", "HOSPITAL"),
    /** ~ - NO DESCRIPTION AVAILABLE. */
    _TILDE("~", "NO DESCRIPTION AVAILABLE");

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
