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
  _1("1", "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF A PERSONAL PHYSICIAN"),
  _2("2", "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF THIS FACILITY'S CLINIC PHYSICIAN"),
  _3(
      "3",
      "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF AN HEALTH MAINTENANCE ORGANIZATION (HMO)PHYSICIAN"),
  _4("4", "THE PATIENT WAS ADMITTED AS AN INPATIENT TRANSFER FROM AN ACUTE CARE FACILITY"),
  _5("5", "THE PATIENT WAS ADMITTED AS AN INPATIENT TRANSFER FROM A SNF"),
  _6(
      "6",
      "THE PATIENT WAS ADMITTED AS A TRANSFER FROM A HEALTH CARE FACILITY OTHER THAN AN ACUTE CARE FACILITY OR SNF"),
  _7(
      "7",
      "THE PATIENT WAS ADMITTED UPON THE RECOMMENDATION OF THIS FACILITY'S EMERGENCY ROOM PHYSICIAN"),
  _8(
      "8",
      "THE PATIENT WAS ADMITTED UPON THE DIRECTION OF A COURT OF LAW OR UPON THE REQUEST OF A LAW ENFORCEMENT AGENCY'S REPRESENTATIVE"),
  _9("9", "THE MEANS BY WHICH THE PATIENT WAS ADMITTED IS NOT KNOWN"),
  A(
      "A",
      "PATIENT WAS ADMITTED/REFERRED TO THIS FACILITY AS A TRANSFER FROM A CRITICAL ACCESS HOSPITAL"),
  B(
      "B",
      "BENEFICIARIES ARE PERMITTED TO TRANSFER FROM ONE HHA TO ANOTHER UNRELATED HHA UNDER HH PPS.(EFF. 10/00)"),
  C(
      "C",
      "IF A BENEFICIARY IS DISCHARGED FROM AN HHA AND THEN READMITTED WITHIN THE ORIGINAL 60-DAY EPISODE THE ORIGINAL EPISODE MUST BE CLOSED EARLY AND A NEW ONE CREATED");

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
        .setCategory(BlueButtonSupportingInfoCategory.CLM_HHA_RFRL_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_HHA_REFERAL_CODE)
                    .setCode(code)
                    .setDisplay(display)));
  }
}
