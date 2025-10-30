package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Pharmacy service type codes. */
@AllArgsConstructor
@Getter
public enum PharmacySrvcTypeCode {
  /** 00 - (Unknown value – rarely populated) */
  _00("00", "(Unknown value – rarely populated)"),
  /**
   * 05 - Therapy change. Physician determined that a change in therapy was required – either the
   * medication was used faster than expected, or a different dosage form is needed.
   */
  _05(
      "05",
      "Therapy change. Physician determined that a change in therapy was required – either the medication was used faster than expected, or a different dosage form is needed."),
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
  /** 14 - LTC leave of absence – short fill required for take-home use. */
  _14("14", "LTC leave of absence – short fill required for take-home use"),
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
   * 19 - Split billing. The quantity dispensed is the remainder billed to a subsequent payer after
   * Medicare Part A benefits expired (partial payment under Part A).
   */
  _19(
      "19",
      "Split billing. The quantity dispensed is the remainder billed to a subsequent payer after Medicare Part A benefits expired (partial payment under Part A)."),
  /**
   * 21 - LTC dispensing rule for <=14 day supply is not applicable due to CMS exclusion or the fact
   * that the manufacturer’s packaging does not allow for special dispensing.
   */
  _21(
      "21",
      "LTC dispensing rule for <=14 day supply is not applicable due to CMS exclusion or the fact that the manufacturer’s packaging does not allow for special dispensing"),
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
  /** 33 - LTC dispensing, other <=7 day cycle. */
  _33("33", "LTC dispensing, other <=7 day cycle"),
  /** 34 - LTC dispensing, 14-day supply. */
  _34("34", "LTC dispensing, 14-day supply"),
  /** 35 - LTC dispensing, other 8-14 day dispensing not listed above. */
  _35("35", "LTC dispensing, other 8-14 day dispensing not listed above"),
  /**
   * 36 - LTC dispensing, outside short cycle, determined to be Part D after originally submitted to
   * another payer.
   */
  _36(
      "36",
      "LTC dispensing, outside short cycle, determined to be Part D after originally submitted to another payer"),
  /** 42 - The prescriber ID submitted has been validated and is active (rarely populated). */
  _42("42", "The prescriber ID submitted has been validated and is active (rarely populated)"),
  /**
   * 43 - For the prescriber ID submitted, the associated DEA number has been renewed or the renewal
   * is in progress (rarely populated).
   */
  _43(
      "43",
      "For the prescriber ID submitted, the associated DEA number has been renewed or the renewal is in progress (rarely populated)"),
  /** 44 - (Unknown value – rarely populated). */
  _44("44", "(Unknown value – rarely populated)"),
  /**
   * 45 - For the prescriber ID submitted, the associated DEA number is a valid hospital DEA number
   * with suffix (rarely populated).
   */
  _45(
      "45",
      "For the prescriber ID submitted, the associated DEA number is a valid hospital DEA number with suffix (rarely populated)");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim dispense status code
   */
  public static Optional<PharmacySrvcTypeCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(BlueButtonSupportingInfoCategory.CLM_PHRMCY_SRVC_TYPE_CD.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PHARMACY_SRVC_TYPE_CODE)
                .setCode(code)
                .setDisplay(display)));
    return supportingInfo;
  }
}
