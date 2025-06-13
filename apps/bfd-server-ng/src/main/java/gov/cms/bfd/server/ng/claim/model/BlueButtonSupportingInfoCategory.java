package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

import java.util.Optional;

@AllArgsConstructor
public enum BlueButtonSupportingInfoCategory {
  CLM_NCH_WKLY_PROC_DT("CLM_NCH_WKLY_PROC_DT", "Weekly Process Date"),
  CLM_BLOOD_PT_FRNSH_QTY("CLM_BLOOD_PT_FRNSH_QTY", "Blood Pints Furnished Quantity"),
  CLM_MDCR_INSTNL_MCO_PD_SW("CLM_MDCR_INSTNL_MCO_PD_SW", "MCO Paid Switch"),
  CLM_MDCR_NCH_PTNT_STUS_IND_CD("CLM_MDCR_NCH_PTNT_STUS_IND_CD", "Patient Status Code"),
  CLM_ACTV_CARE_THRU_DT("CLM_ACTV_CARE_THRU_DT", "Covered Care Through Date"),
  CLM_NCVRD_FROM_DT("CLM_NCVRD_FROM_DT", "Noncovered Stay From Date"),
  CLM_NCVRD_THRU_DT("CLM_NCVRD_THRU_DT", "Noncovered Stay Through Date"),
  CLM_MDCR_EXHSTD_DT("CLM_MDCR_EXHSTD_DT", "Medicare Benefits Exhausted Date"),
  CLM_PPS_IND_CD("CLM_PPS_IND_CD", "Claim PPS Indicator Code"),
  CLM_NCH_PRMRY_PYR_CD("CLM_NCH_PRMRY_PYR_CD", "NCH Primary Payer Code");

  private final String code;
  private final String display;

  CodeableConcept toFhir() {
    var coding =
        new Coding()
            .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_SUPPORTING_INFORMATION)
            .setCode(code);
    display.ifPresent(coding::setDisplay);
    return new CodeableConcept()
        .addCoding(coding)
        .addCoding(
            new Coding()
                .setSystem(SystemUrls.HL7_CLAIM_INFORMATION)
                .setCode("info")
                .setDisplay("Information"));
  }
}
