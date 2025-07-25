package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Patient status info. */
@AllArgsConstructor
@Getter
public enum PatientStatusCode {
  /** 01 - DISCHARGED TO HOME OR SELF CARE (ROUTINE DISCHARGE). */
  _01("01", "DISCHARGED TO HOME OR SELF CARE (ROUTINE DISCHARGE)"),
  /** 02 - DISCHARGED/TRANSFERRED TO A SHORT TERM GENERAL HOSPITAL FOR INPATIENT CARE. */
  _02("02", "DISCHARGED/TRANSFERRED TO A SHORT TERM GENERAL HOSPITAL FOR INPATIENT CARE"),
  /** 03 - DISCHARGED/TRANSFERRED TO SKILLED NURSING FACILITY (SNF) WITH MEDICARE CERTIFICATION. */
  _03("03", "DISCHARGED/TRANSFERRED TO SKILLED NURSING FACILITY (SNF) WITH MEDICARE CERTIFICATION"),
  /** 04 - DISCHARGED/TRANSFERRED TO A FACILITY THAT PROVIDES CUSTODIAL OR SUPPORTIVE CARE. */
  _04("04", "DISCHARGED/TRANSFERRED TO A FACILITY THAT PROVIDES CUSTODIAL OR SUPPORTIVE CARE"),
  /** 05 - DISCHARGED/TRANSFERRED TO A DESIGNATED CANCER CENTER OR CHILDREN’S HOSPITAL. */
  _05("05", "DISCHARGED/TRANSFERRED TO A DESIGNATED CANCER CENTER OR CHILDREN’S HOSPITAL"),
  /**
   * 06 - DISCHARGED/TRANSFERRED TO HOME UNDER CARE OF ORGANIZED HOME HEALTH SERVICE ORGANIZATION.
   */
  _06(
      "06",
      "DISCHARGED/TRANSFERRED TO HOME UNDER CARE OF ORGANIZED HOME HEALTH SERVICE ORGANIZATION"),
  /** 07 - LEFT AGAINST MEDICAL ADVICE OR DISCONTINUED CARE. */
  _07("07", "LEFT AGAINST MEDICAL ADVICE OR DISCONTINUED CARE"),
  /** 08 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _08("08", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 09 - ADMITTED AS AN INPATIENT TO THIS HOSPITAL. */
  _09("09", "ADMITTED AS AN INPATIENT TO THIS HOSPITAL"),
  /** 10 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _10("10", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 11 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _11("11", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 12 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _12("12", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 13 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _13("13", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 14 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _14("14", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 15 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _15("15", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 16 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _16("16", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 17 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _17("17", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 18 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _18("18", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 19 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _19("19", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 20 - EXPIRED. */
  _20("20", "EXPIRED"),
  /** 21 - DISCHARGED/TRANSFERRED TO COURT/LAW ENFORCEMENT. */
  _21("21", "DISCHARGED/TRANSFERRED TO COURT/LAW ENFORCEMENT"),
  /** 22 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _22("22", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 23 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _23("23", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 24 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _24("24", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 25 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _25("25", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 26 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _26("26", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 27 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _27("27", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 28 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _28("28", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 29 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _29("29", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 30 - STILL PATIENT. */
  _30("30", "STILL PATIENT"),
  /** 31 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _31("31", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 32 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _32("32", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 33 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _33("33", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 34 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _34("34", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 35 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _35("35", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 36 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _36("36", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 37 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _37("37", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 38 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _38("38", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 39 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _39("39", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 40 - EXPIRED AT HOME. */
  _40("40", "EXPIRED AT HOME"),
  /** 41 - EXPIRED IN A MEDICAL FACILITY (E.G., HOSPITAL, SNF, ICF, OR FREE STANDING HOSPICE). */
  _41("41", "EXPIRED IN A MEDICAL FACILITY (E.G., HOSPITAL, SNF, ICF, OR FREE STANDING HOSPICE)"),
  /** 42 - EXPIRED - PLACE UNKNOWN. */
  _42("42", "EXPIRED - PLACE UNKNOWN"),
  /** 43 - DISCHARGED/TRANSFERRED TO A FEDERAL HEALTH CARE FACILITY. */
  _43("43", "DISCHARGED/TRANSFERRED TO A FEDERAL HEALTH CARE FACILITY"),
  /** 44 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _44("44", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 45 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _45("45", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 46 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _46("46", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 47 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _47("47", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 48 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _48("48", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 49 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _49("49", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 50 - HOSPICE - HOME. */
  _50("50", "HOSPICE - HOME"),
  /** 51 - HOSPICE - MEDICAL FACILITY (CERTIFIED) PROVIDING HOSPICE LEVEL OF CARE. */
  _51("51", "HOSPICE - MEDICAL FACILITY (CERTIFIED) PROVIDING HOSPICE LEVEL OF CARE"),
  /** 52 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _52("52", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 53 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _53("53", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 54 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _54("54", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 55 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _55("55", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 56 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _56("56", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 57 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _57("57", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 58 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _58("58", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 59 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _59("59", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 60 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _60("60", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 61 - DISCHARGED/TRANSFERRED TO A HOSPITAL-BASED MEDICARE APPROVED SWING BED. */
  _61("61", "DISCHARGED/TRANSFERRED TO A HOSPITAL-BASED MEDICARE APPROVED SWING BED"),
  /**
   * 62 - DISCHARGED/TRANSFERRED TO AN INPATIENT REHABILITATION FACILITY (IRF) INCLUDING
   * REHABILITATION DISTINCT PART UNITS OF A HOSPITAL.
   */
  _62(
      "62",
      "DISCHARGED/TRANSFERRED TO AN INPATIENT REHABILITATION FACILITY (IRF) INCLUDING REHABILITATION DISTINCT PART UNITS OF A HOSPITAL"),
  /** 63 - DISCHARGED/TRANSFERRED TO A MEDICARE CERTIFIED LONG TERM CARE HOSPITAL (LTCH). */
  _63("63", "DISCHARGED/TRANSFERRED TO A MEDICARE CERTIFIED LONG TERM CARE HOSPITAL (LTCH)"),
  /**
   * 64 - DISCHARGED/TRANSFERRED TO A NURSING FACILITY CERTIFIED UNDER MEDICAID BUT NOT CERTIFIED
   * UNDER MEDICARE.
   */
  _64(
      "64",
      "DISCHARGED/TRANSFERRED TO A NURSING FACILITY CERTIFIED UNDER MEDICAID BUT NOT CERTIFIED UNDER MEDICARE"),
  /**
   * 65 - DISCHARGED/TRANSFERRED TO A PSYCHIATRIC HOSPITAL OR PSYCHIATRIC DISTINCT PART UNIT OF A
   * HOSPITAL.
   */
  _65(
      "65",
      "DISCHARGED/TRANSFERRED TO A PSYCHIATRIC HOSPITAL OR PSYCHIATRIC DISTINCT PART UNIT OF A HOSPITAL"),
  /** 66 - DISCHARGED/TRANSFERRED TO A CRITICAL ACCESS HOSPITAL (CAH). */
  _66("66", "DISCHARGED/TRANSFERRED TO A CRITICAL ACCESS HOSPITAL (CAH)"),
  /** 67 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _67("67", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 68 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _68("68", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /**
   * 69 - DISCHARGED/TRANSFERRED TO A DESIGNATED DISASTER ALTERNATIVE CARE SITE (EFFECTIVE 10/1/13).
   */
  _69(
      "69",
      "DISCHARGED/TRANSFERRED TO A DESIGNATED DISASTER ALTERNATIVE CARE SITE (EFFECTIVE 10/1/13)"),
  /**
   * 70 - DISCHARGED/TRANSFERRED TO ANOTHER TYPE OF HEALTH CARE INSTITUTION NOT DEFINED ELSEWHERE IN
   * THIS CODE LIST.
   */
  _70(
      "70",
      "DISCHARGED/TRANSFERRED TO ANOTHER TYPE OF HEALTH CARE INSTITUTION NOT DEFINED ELSEWHERE IN THIS CODE LIST"),
  /** 71 - DISCONTINUED 4/1/03. */
  _71("71", "DISCONTINUED 4/1/03"),
  /** 72 - DISCONTINUED 4/1/03. */
  _72("72", "DISCONTINUED 4/1/03"),
  /** 73 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _73("73", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 74 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _74("74", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 75 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _75("75", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 76 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _76("76", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 77 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _77("77", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 78 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _78("78", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 79 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _79("79", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 80 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _80("80", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /**
   * 81 - DISCHARGED TO HOME OR SELF CARE WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION
   * (EFFECTIVE 10/1/13).
   */
  _81(
      "81",
      "DISCHARGED TO HOME OR SELF CARE WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 82 - DISCHARGED/TRANSFERRED TO A SHORT TERM GENERAL HOSPITAL FOR INPATIENT CARE WITH A PLANNED
   * ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _82(
      "82",
      "DISCHARGED/TRANSFERRED TO A SHORT TERM GENERAL HOSPITAL FOR INPATIENT CARE WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 83 - DISCHARGED/TRANSFERRED TO A SKILLED NURSING FACILITY (SNF) WITH MEDICARE CERTIFICATION
   * WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _83(
      "83",
      "DISCHARGED/TRANSFERRED TO A SKILLED NURSING FACILITY (SNF) WITH MEDICARE CERTIFICATION WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 84 - DISCHARGED/TRANSFERRED TO A FACILITY THAT PROVIDES CUSTODIAL OR SUPPORTIVE CARE WITH A
   * PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _84(
      "84",
      "DISCHARGED/TRANSFERRED TO A FACILITY THAT PROVIDES CUSTODIAL OR SUPPORTIVE CARE WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 85 - DISCHARGED/TRANSFERRED TO A DESIGNATED CANCER CENTER OR CHILDREN’S HOSPITAL WITH A PLANNED
   * ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _85(
      "85",
      "DISCHARGED/TRANSFERRED TO A DESIGNATED CANCER CENTER OR CHILDREN’S HOSPITAL WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 86 - DISCHARGED/TRANSFERRED TO HOME UNDER CARE OF ORGANIZED HOME HEALTH SERVICE ORGANIZATION
   * WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _86(
      "86",
      "DISCHARGED/TRANSFERRED TO HOME UNDER CARE OF ORGANIZED HOME HEALTH SERVICE ORGANIZATION WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 87 - DISCHARGED/TRANSFERRED TO COURT/LAW ENFORCEMENT WITH A PLANNED ACUTE CARE HOSPITAL
   * INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _87(
      "87",
      "DISCHARGED/TRANSFERRED TO COURT/LAW ENFORCEMENT WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 88 - DISCHARGED/TRANSFERRED TO A FEDERAL HEALTH CARE FACILITY WITH A PLANNED ACUTE CARE
   * HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _88(
      "88",
      "DISCHARGED/TRANSFERRED TO A FEDERAL HEALTH CARE FACILITY WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 89 - DISCHARGED/TRANSFERRED TO A HOSPITAL-BASED MEDICARE APPROVED SWING BED WITH A PLANNED
   * ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _89(
      "89",
      "DISCHARGED/TRANSFERRED TO A HOSPITAL-BASED MEDICARE APPROVED SWING BED WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 90 - DISCHARGED/TRANSFERRED TO AN INPATIENT REHABILITATION FACILITY (IRF) INCLUDING
   * REHABILITATION DISTINCT PART UNITS OF A HOSPITAL WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT
   * READMISSION (EFFECTIVE 10/1/13).
   */
  _90(
      "90",
      "DISCHARGED/TRANSFERRED TO AN INPATIENT REHABILITATION FACILITY (IRF) INCLUDING REHABILITATION DISTINCT PART UNITS OF A HOSPITAL WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 91 - DISCHARGED/TRANSFERRED TO A MEDICARE CERTIFIED LONG TERM CARE HOSPITAL (LTCH) WITH A
   * PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _91(
      "91",
      "DISCHARGED/TRANSFERRED TO A MEDICARE CERTIFIED LONG TERM CARE HOSPITAL (LTCH) WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 92 - DISCHARGED/TRANSFERRED TO A NURSING FACILITY CERTIFIED UNDER MEDICAID BUT NOT CERTIFIED
   * UNDER MEDICARE WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _92(
      "92",
      "DISCHARGED/TRANSFERRED TO A NURSING FACILITY CERTIFIED UNDER MEDICAID BUT NOT CERTIFIED UNDER MEDICARE WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 93 - DISCHARGED/TRANSFERRED TO A PSYCHIATRIC HOSPITAL OR PSYCHIATRIC DISTINCT PART UNIT OF A
   * HOSPITAL WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _93(
      "93",
      "DISCHARGED/TRANSFERRED TO A PSYCHIATRIC HOSPITAL OR PSYCHIATRIC DISTINCT PART UNIT OF A HOSPITAL WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 94 - DISCHARGED/TRANSFERRED TO A CRITICAL ACCESS HOSPITAL (CAH) WITH A PLANNED ACUTE CARE
   * HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _94(
      "94",
      "DISCHARGED/TRANSFERRED TO A CRITICAL ACCESS HOSPITAL (CAH) WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /**
   * 95 - DISCHARGED/TRANSFERRED TO ANOTHER TYPE OF HEALTH CARE INSTITUTION NOT DEFINED ELSEWHERE IN
   * THIS CODE LIST WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13).
   */
  _95(
      "95",
      "DISCHARGED/TRANSFERRED TO ANOTHER TYPE OF HEALTH CARE INSTITUTION NOT DEFINED ELSEWHERE IN THIS CODE LIST WITH A PLANNED ACUTE CARE HOSPITAL INPATIENT READMISSION (EFFECTIVE 10/1/13)"),
  /** 96 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _96("96", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 97 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _97("97", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 98 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _98("98", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** 99 - RESERVED FOR NATIONAL ASSIGNMENT. */
  _99("99", "RESERVED FOR NATIONAL ASSIGNMENT"),
  /** NA. */
  NA("", "NO DESCRIPTION AVAILABLE");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return patient status code
   */
  public static Optional<PatientStatusCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.PATIENT_STATUS_CODE.toFhir())
        .setCode(
            new CodeableConcept()
                .addCoding(
                    new Coding().setSystem(SystemUrls.NUBC_PATIENT_DISCHARGE_STATUS).setCode(code))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PATIENT_STATUS_CODE)
                        .setCode(code)
                        .setDisplay(display)));
  }
}
