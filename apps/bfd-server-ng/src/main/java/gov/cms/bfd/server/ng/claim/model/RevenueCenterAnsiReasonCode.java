package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

@Getter
@AllArgsConstructor
public enum RevenueCenterAnsiReasonCode {
  /** 1 - Deductible Amount. */
  _1("1", "Deductible Amount"),
  /** 2 - Coinsurance Amount. */
  _2("2", "Coinsurance Amount"),
  /** 3 - Co-pay Amount. */
  _3("3", "Co-pay Amount"),
  /**
   * 4 - The procedure code is inconsistent with the modifier used or a required modifier is
   * missing.
   */
  _4(
      "4",
      "The procedure code is inconsistent with the modifier used or a required modifier is missing."),
  /** 5 - The procedure code/bill type is inconsistent with the place of service. */
  _5("5", "The procedure code/bill type is inconsistent with the place of service."),
  /** 6 - The procedure code is inconsistent with the patient&#x27;s age. */
  _6("6", "The procedure code is inconsistent with the patient's age."),
  /** 7 - The procedure code is inconsistent with the patient&#x27;s gender. */
  _7("7", "The procedure code is inconsistent with the patient's gender."),
  /** 8 - The procedure code is inconsistent with the provider type. */
  _8("8", "The procedure code is inconsistent with the provider type."),
  /** 9 - The diagnosis is inconsistent with the patient&#x27;s age. */
  _9("9", "The diagnosis is inconsistent with the patient's age."),
  /** 10 - The diagnosis is inconsistent with the patient&#x27;s gender. */
  _10("10", "The diagnosis is inconsistent with the patient's gender."),
  /** 11 - The diagnosis is inconsistent with the procedure. */
  _11("11", "The diagnosis is inconsistent with the procedure."),
  /** 12 - The diagnosis is inconsistent with the provider type. */
  _12("12", "The diagnosis is inconsistent with the provider type."),
  /** 13 - The date of death precedes the date of service. */
  _13("13", "The date of death precedes the date of service."),
  /** 14 - The date of birth follows the date of service. */
  _14("14", "The date of birth follows the date of service."),
  /**
   * 15 - Claim/service adjusted because the submitted authorization number is missing, invalid, or
   * does not apply to the billed services or provider.
   */
  _15(
      "15",
      "Claim/service adjusted because the submitted authorization number is missing, invalid, or does not apply to the billed services or provider."),
  /** 16 - Claim/service lacks information which is needed for adjudication. */
  _16("16", "Claim/service lacks information which is needed for adjudication."),
  /**
   * 17 - Claim/service adjusted because requested information was not provided or was
   * insufficient/incomplete.
   */
  _17(
      "17",
      "Claim/service adjusted because requested information was not provided or was insufficient/incomplete."),
  /** 18 - Duplicate claim/service. */
  _18("18", "Duplicate claim/service."),
  /**
   * 19 - Claim denied because this is a work-related injury/illness and thus the liability of the
   * Worker&#x27;s Compensation Carrier.
   */
  _19(
      "19",
      "Claim denied because this is a work-related injury/illness and thus the liability of the Worker's Compensation Carrier."),
  /** 20 - Claim denied because this injury/illness is covered by the liability carrier. */
  _20("20", "Claim denied because this injury/illness is covered by the liability carrier."),
  /** 21 - Claim denied because this injury/illness is the liability of the no-fault carrier. */
  _21("21", "Claim denied because this injury/illness is the liability of the no-fault carrier."),
  /**
   * 22 - Claim adjusted because this care may be covered by another payer per coordination of
   * benefits.
   */
  _22(
      "22",
      "Claim adjusted because this care may be covered by another payer per coordination of benefits."),
  /** 23 - Claim adjusted because charges have been paid by another payer. */
  _23("23", "Claim adjusted because charges have been paid by another payer."),
  /**
   * 24 - Payment for charges adjusted. Charges are covered under a capitation agreement/managed
   * care plan.
   */
  _24(
      "24",
      "Payment for charges adjusted. Charges are covered under a capitation agreement/managed care plan."),
  /** 25 - Payment denied. Your Stop loss deductible has not been met. */
  _25("25", "Payment denied. Your Stop loss deductible has not been met."),
  /** 26 - Expenses incurred prior to coverage. */
  _26("26", "Expenses incurred prior to coverage."),
  /** 27 - Expenses incurred after coverage terminated. */
  _27("27", "Expenses incurred after coverage terminated."),
  /** 28 - Coverage not in effect at the time the service was provided. */
  _28("28", "Coverage not in effect at the time the service was provided."),
  /** 29 - The time limit for filing has expired. */
  _29("29", "The time limit for filing has expired."),
  /**
   * 30 - Claim/service adjusted because the patient has not met the required eligibility, spend
   * down, waiting, or residency requirements.
   */
  _30(
      "30",
      "Claim/service adjusted because the patient has not met the required eligibility, spend down, waiting, or residency requirements."),
  /** 31 - Claim denied as patient cannot be identified as our insured. */
  _31("31", "Claim denied as patient cannot be identified as our insured."),
  /** 32 - Our records indicate that this dependent is not an eligible dependent as defined. */
  _32("32", "Our records indicate that this dependent is not an eligible dependent as defined."),
  /** 33 - Claim denied. Insured has no dependent coverage. */
  _33("33", "Claim denied. Insured has no dependent coverage."),
  /** 34 - Claim denied. Insured has no coverage for newborns. */
  _34("34", "Claim denied. Insured has no coverage for newborns."),
  /** 35 - Benefit maximum has been reached. */
  _35("35", "Benefit maximum has been reached."),
  /** 36 - Balance does not exceed copayment amount. */
  _36("36", "Balance does not exceed copayment amount."),
  /** 37 - Balance does not exceed deductible amount. */
  _37("37", "Balance does not exceed deductible amount."),
  /** 38 - Services not provided or authorized by designated (network) providers. */
  _38("38", "Services not provided or authorized by designated (network) providers."),
  /** 39 - Services denied at the time authorization/pre-certification was requested. */
  _39("39", "Services denied at the time authorization/pre-certification was requested."),
  /** 40 - Charges do not meet qualifications for emergency/urgent care. */
  _40("40", "Charges do not meet qualifications for emergency/urgent care."),
  /** 41 - Discount agreed to in Preferred Provider contract. */
  _41("41", "Discount agreed to in Preferred Provider contract."),
  /** 42 - Charges exceed our fee schedule or maximum allowable amount. */
  _42("42", "Charges exceed our fee schedule or maximum allowable amount."),
  /** 43 - Gramm-Rudman reduction. */
  _43("43", "Gramm-Rudman reduction."),
  /** 44 - Prompt-pay discount. */
  _44("44", "Prompt-pay discount."),
  /** 45 - Charges exceed your contracted/legislated fee arrangement. */
  _45("45", "Charges exceed your contracted/legislated fee arrangement."),
  /** 46 - This (these) service(s) is(are) not covered. */
  _46("46", "This (these) service(s) is(are) not covered."),
  /** 47 - This (these) diagnosis(es) is(are) not covered, missing, or are invalid. */
  _47("47", "This (these) diagnosis(es) is(are) not covered, missing, or are invalid."),
  /** 48 - This (these) procedure(s) is(are) not covered. */
  _48("48", "This (these) procedure(s) is(are) not covered."),
  /**
   * 49 - These are non-covered services because this is a routine exam or screening procedure done
   * in conjunction with a routine exam.
   */
  _49(
      "49",
      "These are non-covered services because this is a routine exam or screening procedure done in conjunction with a routine exam."),
  /**
   * 50 - These are non-covered services because this is not deemed a &#x27;medical necessity&#x27;
   * by the payer.
   */
  _50(
      "50",
      "These are non-covered services because this is not deemed a 'medical necessity' by the payer."),
  /** 51 - These are non-covered services because this a pre-existing condition. */
  _51("51", "These are non-covered services because this a pre-existing condition."),
  /**
   * 52 - The referring/prescribing/rendering provider is not eligible to
   * refer/prescribe/order/perform the service billed.
   */
  _52(
      "52",
      "The referring/prescribing/rendering provider is not eligible to refer/prescribe/order/perform the service billed."),
  /** 53 - Services by an immediate relative or a member of the same household are not covered. */
  _53("53", "Services by an immediate relative or a member of the same household are not covered."),
  /** 54 - Multiple physicians/assistants are not covered in this case. */
  _54("54", "Multiple physicians/assistants are not covered in this case."),
  /**
   * 55 - Claim/service denied because procedure/treatment is deemed experimental/investigational by
   * the payer.
   */
  _55(
      "55",
      "Claim/service denied because procedure/treatment is deemed experimental/investigational by the payer."),
  /**
   * 56 - Claim/service denied because procedure/treatment has not been deemed &#x27;proven to be
   * effective&#x27; by payer.
   */
  _56(
      "56",
      "Claim/service denied because procedure/treatment has not been deemed 'proven to be effective' by payer."),
  /**
   * 57 - Claim/service adjusted because the payer deems the information submitted does not support
   * this level of service, this many services, this length of service, or this dosage.
   */
  _57(
      "57",
      "Claim/service adjusted because the payer deems the information submitted does not support this level of service, this many services, this length of service, or this dosage."),
  /**
   * 58 - Claim/service adjusted because treatment was deemed by the payer to have been rendered in
   * an inappropriate or invalid place of service.
   */
  _58(
      "58",
      "Claim/service adjusted because treatment was deemed by the payer to have been rendered in an inappropriate or invalid place of service."),
  /** 59 - Charges are adjusted based on multiple surgery rules or concurrent anesthesia rules. */
  _59("59", "Charges are adjusted based on multiple surgery rules or concurrent anesthesia rules."),
  /**
   * 60 - Charges for outpatient services with the proximity to inpatient services are not covered.
   */
  _60(
      "60",
      "Charges for outpatient services with the proximity to inpatient services are not covered."),
  /** 61 - Charges adjusted as penalty for failure to obtain second surgical opinion. */
  _61("61", "Charges adjusted as penalty for failure to obtain second surgical opinion."),
  /**
   * 62 - Claim/service denied/reduced for absence of, or exceeded, precertification/authorization.
   */
  _62(
      "62",
      "Claim/service denied/reduced for absence of, or exceeded, precertification/authorization."),
  /** 63 - Correction to a prior claim. INACTIVE. */
  _63("63", "Correction to a prior claim. INACTIVE"),
  /** 64 - Denial reversed per Medical Review. INACTIVE. */
  _64("64", "Denial reversed per Medical Review. INACTIVE"),
  /** 65 - Procedure code was incorrect. This payment reflects the correct code. INACTIVE. */
  _65("65", "Procedure code was incorrect. This payment reflects the correct code. INACTIVE"),
  /** 66 - Blood Deductible. */
  _66("66", "Blood Deductible."),
  /** 67 - Lifetime reserve days. INACTIVE. */
  _67("67", "Lifetime reserve days. INACTIVE"),
  /** 68 - DRG weight. INACTIVE. */
  _68("68", "DRG weight. INACTIVE"),
  /** 69 - Day outlier amount. */
  _69("69", "Day outlier amount."),
  /** 70 - Cost outlier amount. */
  _70("70", "Cost outlier amount."),
  /** 71 - Primary Payer amount. */
  _71("71", "Primary Payer amount."),
  /** 72 - Coinsurance day. INACTIVE. */
  _72("72", "Coinsurance day. INACTIVE"),
  /** 73 - Administrative days. INACTIVE. */
  _73("73", "Administrative days. INACTIVE"),
  /** 74 - Indirect Medical Education Adjustment. */
  _74("74", "Indirect Medical Education Adjustment."),
  /** 75 - Direct Medical Education Adjustment. */
  _75("75", "Direct Medical Education Adjustment."),
  /** 76 - Disproportionate Share Adjustment. */
  _76("76", "Disproportionate Share Adjustment."),
  /** 77 - Covered days. INACTIVE. */
  _77("77", "Covered days. INACTIVE"),
  /** 78 - Non-covered days/room charge adjustment. */
  _78("78", "Non-covered days/room charge adjustment."),
  /** 79 - Cost report days. INACTIVE. */
  _79("79", "Cost report days. INACTIVE"),
  /** 80 - Outlier days. INACTIVE. */
  _80("80", "Outlier days. INACTIVE"),
  /** 81 - Discharges. INACTIVE. */
  _81("81", "Discharges. INACTIVE"),
  /** 82 - PIP days. INACTIVE. */
  _82("82", "PIP days. INACTIVE"),
  /** 83 - Total visits. INACTIVE. */
  _83("83", "Total visits. INACTIVE"),
  /** 84 - Capital adjustments. INACTIVE. */
  _84("84", "Capital adjustments. INACTIVE"),
  /** 85 - Interest amount. INACTIVE. */
  _85("85", "Interest amount. INACTIVE"),
  /** 86 - Statutory adjustment. INACTIVE. */
  _86("86", "Statutory adjustment. INACTIVE"),
  /** 87 - Transfer amounts. */
  _87("87", "Transfer amounts."),
  /**
   * 88 - Adjustment amount represents collection against receivable created in prior overpayment.
   */
  _88(
      "88",
      "Adjustment amount represents collection against receivable created in prior overpayment."),
  /** 89 - Professional fees removed from charges. */
  _89("89", "Professional fees removed from charges."),
  /** 90 - Ingredient cost adjustment. */
  _90("90", "Ingredient cost adjustment."),
  /** 91 - Dispensing fee adjustment. */
  _91("91", "Dispensing fee adjustment."),
  /** 92 - Claim paid in full. INACTIVE. */
  _92("92", "Claim paid in full. INACTIVE"),
  /** 93 - No claim level adjustment. INACTIVE. */
  _93("93", "No claim level adjustment. INACTIVE"),
  /** 94 - Process in excess of charges. */
  _94("94", "Process in excess of charges."),
  /** 95 - Benefits adjusted. Plan procedures not followed. */
  _95("95", "Benefits adjusted. Plan procedures not followed."),
  /** 96 - Non-covered charges. */
  _96("96", "Non-covered charges."),
  /** 97 - Payment is included in allowance for another service/procedure. */
  _97("97", "Payment is included in allowance for another service/procedure."),
  /**
   * 98 - The hospital must file the Medicare claim for this inpatient non-physician service.
   * INACTIVE.
   */
  _98(
      "98",
      "The hospital must file the Medicare claim for this inpatient non-physician service. INACTIVE"),
  /** 99 - Medicare Secondary Payer Adjustment Amount. INACTIVE. */
  _99("99", "Medicare Secondary Payer Adjustment Amount. INACTIVE"),
  /** 100 - Payment made to patient/insured/responsible party. */
  _100("100", "Payment made to patient/insured/responsible party."),
  /**
   * 101 - Predetermination: anticipated payment upon completion of services or claim adjudication.
   */
  _101(
      "101",
      "Predetermination: anticipated payment upon completion of services or claim adjudication."),
  /** 102 - Major medical adjustment. */
  _102("102", "Major medical adjustment."),
  /** 103 - Provider promotional discount (i.e. Senior citizen discount). */
  _103("103", "Provider promotional discount (i.e. Senior citizen discount)."),
  /** 104 - Managed care withholding. */
  _104("104", "Managed care withholding."),
  /** 105 - Tax withholding. */
  _105("105", "Tax withholding."),
  /** 106 - Patient payment option/election not in effect. */
  _106("106", "Patient payment option/election not in effect."),
  /**
   * 107 - Claim/service denied because the related or qualifying claim/service was not paid or
   * identified on the claim.
   */
  _107(
      "107",
      "Claim/service denied because the related or qualifying claim/service was not paid or identified on the claim."),
  /** 108 - Claim/service reduced because rent/purchase guidelines were not met. */
  _108("108", "Claim/service reduced because rent/purchase guidelines were not met."),
  /**
   * 109 - Claim not covered by this payer/contractor. You must send the claim to the correct
   * payer/contractor.
   */
  _109(
      "109",
      "Claim not covered by this payer/contractor. You must send the claim to the correct payer/contractor."),
  /** 110 - Billing date predates service date. */
  _110("110", "Billing date predates service date."),
  /** 111 - Not covered unless the provider accepts assignment. */
  _111("111", "Not covered unless the provider accepts assignment."),
  /**
   * 112 - Claim/service adjusted as not furnished directly to the patient and/or not documented.
   */
  _112(
      "112",
      "Claim/service adjusted as not furnished directly to the patient and/or not documented."),
  /**
   * 113 - Claim denied because service/procedure was provided outside the United States or as a
   * result of war.
   */
  _113(
      "113",
      "Claim denied because service/procedure was provided outside the United States or as a result of war."),
  /** 114 - Procedure/PRODuct not approved by the Food and Drug Administration. */
  _114("114", "Procedure/PRODuct not approved by the Food and Drug Administration."),
  /** 115 - Claim/service adjusted as procedure postponed or canceled. */
  _115("115", "Claim/service adjusted as procedure postponed or canceled."),
  /**
   * 116 - Claim/service denied. The advance indemnification notice signed by the patient did not
   * comply with requirements.
   */
  _116(
      "116",
      "Claim/service denied. The advance indemnification notice signed by the patient did not comply with requirements."),
  /**
   * 117 - Claim/service adjusted because transportation is only covered to the closest facility
   * that can provide the necessary care.
   */
  _117(
      "117",
      "Claim/service adjusted because transportation is only covered to the closest facility that can provide the necessary care."),
  /** 118 - Charges reduced for ESRD network support. */
  _118("118", "Charges reduced for ESRD network support."),
  /** 119 - Benefit maximum for this time period has been reached. */
  _119("119", "Benefit maximum for this time period has been reached."),
  /** 120 - Patient is covered by a managed care plan. INACTIVE. */
  _120("120", "Patient is covered by a managed care plan. INACTIVE"),
  /** 121 - Indemnification adjustment. */
  _121("121", "Indemnification adjustment."),
  /** 122 - Psychiatric reduction. */
  _122("122", "Psychiatric reduction."),
  /** 123 - Payer refund due to overpayment. INACTIVE. */
  _123("123", "Payer refund due to overpayment. INACTIVE"),
  /** 124 - Payer refund amount - not our patient. INACTIVE. */
  _124("124", "Payer refund amount - not our patient. INACTIVE"),
  /** 125 - Claim/service adjusted due to a submission/billing error(s). */
  _125("125", "Claim/service adjusted due to a submission/billing error(s)."),
  /** 126 - Deductible - Major Medical. */
  _126("126", "Deductible - Major Medical."),
  /** 127 - Coinsurance - Major Medical. */
  _127("127", "Coinsurance - Major Medical."),
  /** 128 - Newborn&#x27;s services are covered in the mother&#x27;s allowance. */
  _128("128", "Newborn's services are covered in the mother's allowance."),
  /** 129 - Claim denied - prior processing information appears incorrect. */
  _129("129", "Claim denied - prior processing information appears incorrect."),
  /** 130 - Paper claim submission fee. */
  _130("130", "Paper claim submission fee."),
  /** 131 - Claim specific negotiated discount. */
  _131("131", "Claim specific negotiated discount."),
  /** 132 - Prearranged demonstration project adjustment. */
  _132("132", "Prearranged demonstration project adjustment."),
  /** 133 - The disposition of this claim/service is pending further review. */
  _133("133", "The disposition of this claim/service is pending further review."),
  /** 134 - Technical fees removed from charges. */
  _134("134", "Technical fees removed from charges."),
  /** 135 - Claim denied. Interim bills cannot be processed. */
  _135("135", "Claim denied. Interim bills cannot be processed."),
  /** 136 - Claim adjusted. Plan procedures of a prior payer were not followed. */
  _136("136", "Claim adjusted. Plan procedures of a prior payer were not followed."),
  /**
   * 137 - Payment/Reduction for Regulatory Surcharges, Assessments, Allowances or Health Related
   * Taxes.
   */
  _137(
      "137",
      "Payment/Reduction for Regulatory Surcharges, Assessments, Allowances or Health Related Taxes."),
  /** 138 - Claim/service denied. Appeal procedures not followed or time limits not met. */
  _138("138", "Claim/service denied. Appeal procedures not followed or time limits not met."),
  /** 139 - Contracted funding agreement - subscriber is employed by the provider of services. */
  _139("139", "Contracted funding agreement - subscriber is employed by the provider of services."),
  /** 140 - Patient/Insured health identification number and name do not match. */
  _140("140", "Patient/Insured health identification number and name do not match."),
  /** 141 - Claim adjustment because the claim spans eligible and ineligible periods of coverage. */
  _141(
      "141",
      "Claim adjustment because the claim spans eligible and ineligible periods of coverage."),
  /** 142 - Claim adjusted by the monthly Medicaid patient liability amount. */
  _142("142", "Claim adjusted by the monthly Medicaid patient liability amount."),
  /** A0 - Patient refund amount. */
  A0("A0", "Patient refund amount"),
  /** A1 - Claim denied charges. */
  A1("A1", "Claim denied charges."),
  /** A2 - Contractual adjustment. */
  A2("A2", "Contractual adjustment."),
  /** A3 - Medicare Secondary Payer liability met. INACTIVE. */
  A3("A3", "Medicare Secondary Payer liability met. INACTIVE"),
  /** A4 - Medicare Claim PPS Capital Day Outlier Amount. */
  A4("A4", "Medicare Claim PPS Capital Day Outlier Amount."),
  /** A5 - Medicare Claim PPS Capital Cost Outlier Amount. */
  A5("A5", "Medicare Claim PPS Capital Cost Outlier Amount."),
  /** A6 - Prior hospitalization or 30 day transfer requirement not met. */
  A6("A6", "Prior hospitalization or 30 day transfer requirement not met."),
  /** A7 - Presumptive Payment Adjustment. */
  A7("A7", "Presumptive Payment Adjustment."),
  /** A8 - Claim denied; ungroupable DRG. */
  A8("A8", "Claim denied; ungroupable DRG."),
  /** B1 - Non-covered visits. */
  B1("B1", "Non-covered visits."),
  /** B2 - Covered visits. INACTIVE. */
  B2("B2", "Covered visits. INACTIVE"),
  /** B3 - Covered charges. INACTIVE. */
  B3("B3", "Covered charges. INACTIVE"),
  /** B4 - Late filing penalty. */
  B4("B4", "Late filing penalty."),
  /**
   * B5 - Claim/service adjusted because coverage/program guidelines were not met or were exceeded.
   */
  B5(
      "B5",
      "Claim/service adjusted because coverage/program guidelines were not met or were exceeded."),
  /**
   * B6 - This service/procedure is adjusted when performed/billed by this type of provider, by this
   * type of facility, or by a provider of this specialty.
   */
  B6(
      "B6",
      "This service/procedure is adjusted when performed/billed by this type of provider, by this type of facility, or by a provider of this specialty."),
  /**
   * B7 - This provider was not certified/eligible to be paid for this procedure/service on this
   * date of service.
   */
  B7(
      "B7",
      "This provider was not certified/eligible to be paid for this procedure/service on this date of service."),
  /**
   * B8 - Claim/service not covered/reduced because alternative services were available, and should
   * have been utilized.
   */
  B8(
      "B8",
      "Claim/service not covered/reduced because alternative services were available, and should have been utilized."),
  /** B9 - Services not covered because the patient is enrolled in a Hospice. */
  B9("B9", "Services not covered because the patient is enrolled in a Hospice."),
  /**
   * B10 - Allowed amount has been reduced because a component of the basic procedure/test was paid.
   * The beneficiary is not liable for more than the charge limit for the basic procedure/test.
   */
  B10(
      "B10",
      "Allowed amount has been reduced because a component of the basic procedure/test was paid. The beneficiary is not liable for more than the charge limit for the basic procedure/test."),
  /**
   * B11 - The claim/service has been transferred to the proper payer/processor for processing.
   * Claim/service not covered by this payer/processor.
   */
  B11(
      "B11",
      "The claim/service has been transferred to the proper payer/processor for processing. Claim/service not covered by this payer/processor."),
  /** B12 - Services not documented in patients&#x27; medical records. */
  B12("B12", "Services not documented in patients' medical records."),
  /**
   * B13 - Previously paid. Payment for this claim/service may have been provided in a previous
   * payment.
   */
  B13(
      "B13",
      "Previously paid. Payment for this claim/service may have been provided in a previous payment."),
  /**
   * B14 - Claim/service denied because only one visit or consultation per physician per day is
   * covered.
   */
  B14(
      "B14",
      "Claim/service denied because only one visit or consultation per physician per day is covered."),
  /** B15 - Claim/service adjusted because this procedure/service is not paid separately. */
  B15("B15", "Claim/service adjusted because this procedure/service is not paid separately."),
  /** B16 - Claim/service adjusted because &#x27;New Patient&#x27; qualifications were not met. */
  B16("B16", "Claim/service adjusted because 'New Patient' qualifications were not met."),
  /**
   * B17 - Claim/service adjusted because this service was not prescribed by a physician, not
   * prescribed prior to delivery, the prescription is incomplete, or the prescription is not
   * current.
   */
  B17(
      "B17",
      "Claim/service adjusted because this service was not prescribed by a physician, not prescribed prior to delivery, the prescription is incomplete, or the prescription is not current."),
  /**
   * B18 - Claim/service denied because this procedure code/modifier was invalid on the date of
   * service or claim submission.
   */
  B18(
      "B18",
      "Claim/service denied because this procedure code/modifier was invalid on the date of service or claim submission."),
  /** B19 - Claim/service adjusted because of the finding of a Review Organization. INACTIVE. */
  B19("B19", "Claim/service adjusted because of the finding of a Review Organization. INACTIVE"),
  /**
   * B20 - Charges adjusted because procedure/service was partially or fully furnished by another
   * provider.
   */
  B20(
      "B20",
      "Charges adjusted because procedure/service was partially or fully furnished by another provider."),
  /**
   * B21 - The charges were reduced because the service/care was partially furnished by another
   * physician. INACTIVE.
   */
  B21(
      "B21",
      "The charges were reduced because the service/care was partially furnished by another physician. INACTIVE"),
  /** B22 - This claim/service is adjusted based on the diagnosis. */
  B22("B22", "This claim/service is adjusted based on the diagnosis."),
  /**
   * B23 - Claim/service denied because this provider has failed an aspect of a proficiency testing
   * program.
   */
  B23(
      "B23",
      "Claim/service denied because this provider has failed an aspect of a proficiency testing program."),
  /** W1 - Workers Compensation State Fee Schedule Adjustment. */
  W1("W1", "Workers Compensation State Fee Schedule Adjustment.");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim admission type code
   */
  public static Optional<RevenueCenterAnsiReasonCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  public Optional<List<Coding>> toFhirCodings() {
    return Optional.of(
        List.of(
            new Coding().setSystem(SystemUrls.X12_CLAIM_ADJUSTMENT_REASON_CODES).setCode(code),
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ANSI_RSN_CODE)
                .setCode(code)));
  }
}
