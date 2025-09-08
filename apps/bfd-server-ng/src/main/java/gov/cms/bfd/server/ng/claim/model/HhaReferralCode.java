package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Getter
@AllArgsConstructor
public enum HhaReferralCode {
  PHYSICIAN("1", "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF A PERSONAL PHYSICIAN"),
  CLINICAL(
      "2", "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF THIS FACILITY'S CLINIC PHYSICIAN"),
  HMO_REFERRAL(
      "3",
      "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF AN HEALTH MAINTENANCE ORGANIZATION (HMO)PHYSICIAN"),
  TRANSFER_FROM_HOSPITAL(
      "4", "THE PATIENT WAS ADMITTED AS AN INPATIENT TRANSFER FROM AN ACUTE CARE FACILITY"),
  TRANSFER_FROM_A_SKILLED_NURSING_FACILITY(
      "5", "THE PATIENT WAS ADMITTED AS AN INPATIENT TRANSFER FROM A SNF"),
  TRANSFER_FROM_ANOTHER_HEALTH_CARE_FACILITY(
      "6",
      "THE PATIENT WAS ADMITTED AS A TRANSFER FROM A HEALTH CARE FACILITY OTHER THAN AN ACUTE CARE FACILITY OR SNF"),
  EMERGENCY_ROOM(
      "7",
      "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF THIS FACILITY'S EMERGENCY ROOM PHYSICIAN"),
  COURT_LAW_ENFORCEMENT(
      "8",
      "THE PATIENT WAS ADMITTED UPON THE DIRECTION OF A COURT OF LAW OR UPON THE REQUEST OF A LAW ENFORCEMENT AGENCY'S REPRESENTATIVE"),
  INFORMATION_NOT_AVAILABLE("9", "THE MEANS BY WHICH THE PATIENT WAS ADMITTED IS NOT KNOWN"),
  TRANSFER_FROM_A_CRITICAL_ACCESS_HOSPITAL(
      "A",
      "PATIENT WAS ADMITTED/REFERRED TO THIS FACILITY AS A TRANSFER FROM A CRITICAL ACCESS HOSPITAL"),
  TRANSFER_FROM_ANOTHER_HHA(
      "B",
      "BENEFICIARIES ARE PERMITTED TO TRANSFER FROM ONE HHA TO ANOTHER UNRELATED HHA UNDER HH PPS.(EFF. 10/00)"),
  READMISSION_TO_SAME_HHA(
      "C",
      "IF A BENEFICIARY IS DISCHARGED FROM AN HHA AND THEN READMITTED WITHIN THE ORIGINAL 60-DAY EPISODE THE ORIGINAL EPISODE MUST BE CLOSED EARLY AND A NEW ONE CREATED"),
  NO_DESCRIPTION_AVAILABLE("~", "NO DESCRIPTION AVAILABLE");

  private final String code;
  private final String display;

  /**
   * Converts from a database code.
   *
   * @param code database code.
   * @return paid switch
   */
  public static Optional<HhaReferralCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(c -> c.code.equals(code)).findFirst();
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_HHA_LUP_IND_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_HHA_LUPA_INDICATOR_CODE)
                    .setCode(code)
                    .setDisplay(display)));
  }
}
