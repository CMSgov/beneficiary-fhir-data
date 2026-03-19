package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim disposition codes. */
public sealed interface ClaimDispositionCode
    permits ClaimDispositionCode.Valid, ClaimDispositionCode.Invalid {

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
   * @return claim disposition code
   */
  static Optional<ClaimDispositionCode> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimDispositionCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @param supportingInfoFactory the supportingInfoFactory containing the other mappings.
   * @return supportingInfoFactory
   */
  default ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_DISP_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_DISPOSITION_CODE)
                    .setDisplay(getDisplay())
                    .setCode(getCode())));
  }

  /**
   * Enum for all known, valid claim disposition codes.
   */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimDispositionCode {
    /** 1 - DEBIT ACCEPTED. */
    _1("1", "DEBIT ACCEPTED"),
    /** 2 - DEBIT ACCEPTED (AUTOMATIC ADJUSTMENT) APPLICABLE THROUGH 4/4/93. */
    _2("2", "DEBIT ACCEPTED (AUTOMATIC ADJUSTMENT) APPLICABLE THROUGH 4/4/93"),
    /** 3 - CANCEL ACCEPTED. */
    _3("3", "CANCEL ACCEPTED"),
    /** 4 - OUTPATIENT HISTORY ONLY ACCEPTED. */
    _4("4", "OUTPATIENT HISTORY ONLY ACCEPTED"),
    /** 50 - BENEFICIARY NOT IN FILE. */
    _50("50", "BENEFICIARY NOT IN FILE"),
    /** 51 - TRUE NOT IN FILE. */
    _51("51", "TRUE NOT IN FILE"),
    /** 52 - MASTER RECORD AT ANOTHER CWF SITE. */
    _52("52", "MASTER RECORD AT ANOTHER CWF SITE"),
    /** 53 - RECORD IN CMS ALPHA MATCH. */
    _53("53", "RECORD IN CMS ALPHA MATCH"),
    /** 54 - CROSS-REFERENCE HIC. */
    _54("54", "CROSS-REFERENCE HIC"),
    /** 55 - NAME/SEX MIS-MATCH. */
    _55("55", "NAME/SEX MIS-MATCH"),
    /** 57 - BENEFICIARY RECORD ARCHIVED, ONLY SKELETON EXISTS. */
    _57("57", "BENEFICIARY RECORD ARCHIVED, ONLY SKELETON EXISTS"),
    /** 58 - BENEFICIARY RECORD BLOCKED FOR CROSS-REFERENCE. */
    _58("58", "BENEFICIARY RECORD BLOCKED FOR CROSS-REFERENCE"),
    /** 59 - BENEFICIARY RECORD FROZEN FOR CLERICAL CORRECTION. */
    _59("59", "BENEFICIARY RECORD FROZEN FOR CLERICAL CORRECTION"),
    /** 60 - ABEND ERROR. */
    _60("60", "ABEND ERROR"),
    /** 61 - CROSS-REFERENCE/DATA BASE PROBLEM. */
    _61("61", "CROSS-REFERENCE/DATA BASE PROBLEM"),
    /** AA - AUTOMATIC ADJUSTMENT. */
    AA("AA", "AUTOMATIC ADJUSTMENT"),
    /** AB - TRANSACTION CAUSED CICS ABNORMAL END OF JOB. */
    AB("AB", "TRANSACTION CAUSED CICS ABNORMAL END OF JOB"),
    /** BT - HISTORY CLAIM NOT PRESENT TO SUPPORT SPELL OF ILLNESS. */
    BT("BT", "HISTORY CLAIM NOT PRESENT TO SUPPORT SPELL OF ILLNESS"),
    /** CI - CICS PROCESSING ERROR. */
    CI("CI", "CICS PROCESSING ERROR"),
    /** CR - CROSSOVER REJECT. */
    CR("CR", "CROSSOVER REJECT"),
    /** ER - CONSISTENCY EDIT REJECT. */
    ER("ER", "CONSISTENCY EDIT REJECT"),
    /** RD - TRANSACTION ERROR. */
    RD("RD", "TRANSACTION ERROR"),
    /** RT - RETRIEVE PENDING. */
    RT("RT", "RETRIEVE PENDING"),
    /** SV - SECURITY VIOLATION. */
    SV("SV", "SECURITY VIOLATION"),
    /** UR - UTILIZATION REJECT. */
    UR("UR", "UTILIZATION REJECT");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimDispositionCode {
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
