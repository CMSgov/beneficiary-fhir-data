package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings("java:S115")
/** Submission clarification codes. */
public sealed interface ClaimSubmissionCode
    permits ClaimSubmissionCode.Valid, ClaimSubmissionCode.Invalid {

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
   * @return Claim submission code or empty Optional if code is null or blank
   */
  static Optional<ClaimSubmissionCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(
                v ->
                    v.code.equals(code)
                        // this handles a code given that starts with zero to match the single digit
                        // integer
                        || (v.code.startsWith("0") && v.code.substring(1).equals(code)))
            .map(v -> (ClaimSubmissionCode) v)
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
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_LTC_DSPNSNG_MTHD_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_SUBMISSION_CLARIFICATION_CODE)
                .setCode(getCode())
                .setDisplay(getDisplay())));
    return supportingInfo;
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimSubmissionCode {
    /** 00 - (Unknown value – rarely populated). */
    _00("00", "(Unknown value – rarely populated)"),
    /** 1 - No Override. */
    _01("01", "No Override"),
    /** 2 - Other Override. */
    _02("02", "Other Override"),
    /**
     * 3 - Vacation Supply. The pharmacist is indicating that the cardholder has requested a
     * vacation supply of the medicine.
     */
    _03(
        "03",
        "Vacation Supply. The pharmacist is indicating that the cardholder has requested a vacation supply of the medicine."),
    /** 4 - Lost/Damaged Prescription. */
    _04(
        "04",
        "Lost/Damaged Prescription. The pharmacist is indicating that the a replacement of medication that has been damaged or lost."),
    /**
     * 05 - Therapy change. Physician determined that a change in therapy was required – either the
     * medication was used faster than expected, or a different dosage form is needed.
     */
    _05(
        "05",
        "Therapy change. Physician determined that a change in therapy was required – either the medication was used faster than expected, or a different dosage form is needed."),
    /**
     * 06 - Continuation Dose After Starter Dose. The previous medication was a starter dose and now
     * additional medication is needed to continue treatment.
     */
    _06(
        "06",
        "Continuation Dose After Starter Dose. The previous medication was a starter dose and now additional medication is needed to continue treatment."),
    /**
     * 07 - Emergency supply of non-formulary drugs (or formulary drugs which typically require step
     * therapy or prior authorization). Medication has been determined by the physician to be
     * medically necessary.
     */
    _07(
        "07",
        "Emergency supply of non-formulary drugs (or formulary drugs which typically require step therapy or prior authorization). Medication has been determined by the physician to be medically necessary."),
    /** 08 - Process compound for approved ingredients. */
    _08("08", "Completely Process compound for approved ingredients"),
    /** 09 - Encounters. */
    _09("09", "Encounters"),
    /**
     * 10 - Meets Plan Limitations. The pharmacy certifies that the transaction is in compliance
     * with the program's policies and rules that are specific to the particular product being
     * billed.
     */
    _10(
        "10",
        "Meets Plan Limitations. The pharmacy certifies that the transaction is in compliance with the program's policies and rules that are specific to the particular product being billed."),
    /** 11 - Certification on File. */
    _11("11", "Certification on File"),
    /** 12 - DME Replacement Indicator. */
    _12("12", "DME Replacement Indicator"),
    /** 13 - Payer-Recognized Emergency/Disaster Assistance Request. */
    _13("13", "Payer-Recognized Emergency/Disaster Assistance Request"),
    /** 14 - LTC leave of absence – short fill required for take-home use. */
    _14("14", "LTC leave of absence – short fill required for take-home use"),
    /** 15 - Long Term Care Replacement Medication. */
    _15("15", "Long Term Care Replacement Medication"),
    /** 16 - LTC emergency box (e box) /automated dispensing machine. */
    _16("16", "LTC emergency box (e box) /automated dispensing machine"),
    /** 17 - LTC emergency supply remainder (remainder of drug from the emergency supply). */
    _17("17", "LTC emergency supply remainder (remainder of drug from the emergency supply)"),
    /**
     * 18 - LTC patient admit/readmission indicator. This status required new dispensing of
     * medication.
     */
    _18(
        "18",
        "LTC patient admit/readmission indicator. This status required new dispensing of medication."),
    /**
     * 19 - Split billing. The quantity dispensed is the remainder billed to a subsequent payer
     * after Medicare Part A benefits expired (partial payment under Part A).
     */
    _19(
        "19",
        "Split billing. The quantity dispensed is the remainder billed to a subsequent payer after Medicare Part A benefits expired (partial payment under Part A)."),
    /** 20 - 340B. */
    _20(
        "20",
        "340B. Purchased pursuant to rights available under Section 340B of the Public Health Act of 1992."),
    /**
     * 21 - LTC dispensing rule for less than or equal to 14 day supply is not applicable due to CMS
     * exclusion or the fact that the manufacturer's packaging does not allow for special
     * dispensing.
     */
    _21(
        "21",
        "LTC dispensing rule for <=14 day supply is not applicable due to CMS exclusion or the fact that the manufacturer's packaging does not allow for special dispensing"),
    /** 22 - LTC dispensing, 7-day supply. */
    _22("22", "LTC dispensing, 7-day supply"),
    /** 23 - LTC dispensing, 4-day supply. */
    _23("23", "LTC dispensing, 4-day supply"),
    /** 24 - LTC dispensing, 3-day supply. */
    _24("24", "LTC dispensing, 3-day supply"),
    /** 25 - LTC dispensing, 2-day supply. */
    _25("25", "LTC dispensing, 2-day supply"),
    /** 26 - LTC dispensing, 1-day supply. */
    _26("26", "LTC dispensing, 1-day supply"),
    /** 27 - LTC dispensing, 4-day supply, then 3-day supply. */
    _27("27", "LTC dispensing, 4-day supply, then 3-day supply"),
    /** 28 - LTC dispensing, 2-day supply, then 2-day supply, then 3-day supply. */
    _28("28", "LTC dispensing, 2-day supply, then 2-day supply, then 3-day supply"),
    /** 29 - LTC dispensing, daily during the week then multiple days (3) for weekend. */
    _29("29", "LTC dispensing, daily during the week then multiple days (3) for weekend"),
    /** 30 - LTC dispensing, per shift (multiple medication passes). */
    _30("30", "LTC dispensing, per shift (multiple medication passes)"),
    /** 31 - LTC dispensing, per medication pass. */
    _31("31", "LTC dispensing, per medication pass"),
    /** 32 - LTC dispensing, PRN on demand. */
    _32("32", "LTC dispensing, PRN on demand"),
    /** 33 - LTC dispensing, other less than or equal to 7 day cycle. */
    _33("33", "LTC dispensing, other <=7 day cycle"),
    /** 34 - LTC dispensing, 14-day supply. */
    _34("34", "LTC dispensing, 14-day supply"),
    /** 35 - LTC dispensing, other 8-14 day dispensing not listed above. */
    _35("35", "LTC dispensing, other 8-14 day dispensing not listed above"),
    /**
     * 36 - LTC dispensing, outside short cycle, determined to be Part D after originally submitted
     * to another payer.
     */
    _36(
        "36",
        "LTC dispensing, outside short cycle, determined to be Part D after originally submitted to another payer"),
    /** 42 - The prescriber ID submitted has been validated and is active (rarely populated). */
    _42("42", "The prescriber ID submitted has been validated and is active (rarely populated)"),
    /**
     * 43 - For the prescriber ID submitted, the associated DEA number has been renewed or the
     * renewal is in progress (rarely populated).
     */
    _43(
        "43",
        "For the prescriber ID submitted, the associated DEA number has been renewed or the renewal is in progress (rarely populated)"),
    /** 44 - associated prescriber DEA recently licensed or re-activated. */
    _44("44", "Associated prescriber DEA recently licensed or re-activated"),
    /**
     * 45 - For the prescriber ID submitted, the associated DEA number is a valid hospital DEA
     * number with suffix (rarely populated).
     */
    _45(
        "45",
        "For the prescriber ID submitted, the associated DEA number is a valid hospital DEA number with suffix (rarely populated)"),
    /** 46 - Prescriber has prescriptive authority for this drug DEA Schedule. */
    _46("46", "Prescriber has prescriptive authority for this drug DEA Schedule."),
    /**
     * 47 - Shortened Days Supply. Request plan benefit allowances be applied to the shortened days
     * supply not otherwise defined in a distinct shortened days supply value.
     */
    _47(
        "47",
        "Shortened Days Supply. Request plan benefit allowances be applied to the shortened days supply not otherwise defined in a distinct shortened days supply value."),
    /** 48 - Dispensed subsequent to a Shortened Days Supply Dispensing. */
    _48("48", "Dispensed Subsequent to a Shortened Days Supply Dispensing."),
    /** 49 - Prescriber does not currently have an active Type 1 NPI. */
    _49("49", "Prescriber does not currently have an active Type 1 NPI."),
    /** 50 - Prescriber's active Medicare Fee For Service enrollment status has been validated. */
    _50("50", "Prescriber's active Medicare Fee For Service enrollment status has been validated."),
    /** 51 - Pharmacy's active Medicare Fee For Service enrollment status has been validated. */
    _51("51", "Pharmacy's active Medicare Fee For Service enrollment status has been validated."),
    /** 52 - Prescriber's state license with prescriptive authority has been validated. */
    _52("52", "Prescriber's state license with prescriptive authority has been validated."),
    /** 53 - Prescriber NPI active and valid. */
    _53("53", "Prescriber NPI active and valid."),
    /** 54 - CMS Other Authorized Prescriber (OAP). */
    _54("54", "CMS Other Authorized Prescriber (OAP)."),
    /** 55 - Prescriber Enrollment in State Medicaid Program has been validated. */
    _55("55", "Prescriber Enrollment in State Medicaid Program has been validated."),
    /** 56 - Pharmacy Enrollment in State Medicaid Program has been validated. */
    _56("56", "Pharmacy Enrollment in State Medicaid Program has been validated."),
    /** 57 - Discharge Medication. */
    _57("57", "Discharge Medication"),
    /** 58 - Nominal Price. */
    _58("58", "Nominal Price."),
    /** 59 - Federal Supply Schedule. */
    _59("59", "Federal Supply Schedule"),
    /** 60 - Long Term Care Same Drug Strength and Dosage Form with Multiple Dosing Directions. */
    _60("60", "Long Term Care Same Drug Strength and Dosage Form with Multiple Dosing Directions"),
    /** 61 - Synchronization Fill - Shortened Days Supply. */
    _61("61", "Synchronization Fill - Shortened Days Supply"),
    /**
     * 62 - Shortened Days Supply of Same Drug, Strength and Dosage Form with Multiple NDCs
     * Dispensed.
     */
    _62(
        "62",
        "Shortened Days Supply of Same Drug, Strength and Dosage Form with Multiple NDCs Dispensed"),
    /** 63 - Mail Order Delay - Shortened Days Supply. */
    _63("63", "Mail Order Delay - Shortened Days Supply"),
    /** 64 - Trial Fill - Shortened Days Supply. */
    _64("64", "Trial Fill - Shortened Days Supply"),
    /** 65 - Individual Patient Emergency RX Fill Request. */
    _65("65", "Individual Patient Emergency RX Fill Request"),
    /**
     * 66 - Patient indicated Workers' Compensation Medicare Set-Aside Arrangement (WCMSA) benefit
     * does not apply to this claim.
     */
    _66(
        "66",
        "Patient indicated Workers' Compensation Medicare Set-Aside Arrangement (WCMSA) benefit does not apply to this claim."),
    /** 99 - Other(Unknown). */
    _99("99", "Other(Unknown)");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimSubmissionCode {
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
