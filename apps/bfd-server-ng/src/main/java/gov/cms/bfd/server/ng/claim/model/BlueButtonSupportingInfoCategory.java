package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

import java.util.Optional;

@AllArgsConstructor
public enum BlueButtonSupportingInfoCategory {
  BLOOD_PINTS("CLM_BLOOD_PT_FRNSH_QTY", Optional.of("Blood Pints Furnished Quantity")),
  MCO_PAID_SWITCH("CLM_MDCR_INSTNL_MCO_PD_SW", Optional.of("MCO Paid Switch")),
  WEEKLY_PROCESS_DATE("CLM_NCH_WKLY_PROC_DT", Optional.of("Weekly Process Date")),
  NONCONVERED_FROM_DATE("CLM_NCVRD_FROM_DT", Optional.of("Noncovered Stay From Date")),
  NONCOVERED_THROUGH_DATE("CLM_NCVRD_THRU_DT", Optional.of("Noncovered Stay Through Date")),
  CARE_THROUGH_DATE("CLM_ACTV_CARE_THRU_DT", Optional.of("Covered Care Through Date")),
  BENEFITS_EXHAUSTED_DATE("CLM_MDCR_EXHSTD_DT", Optional.of("Medicare Benefits Exhausted Date")),
  PRIMARY_PAYOR_CODE("CLM_NCH_PRMRY_PYR_CD", Optional.empty()),
  PPS_INDICATOR_CODE("CLM_PPS_IND_CD", Optional.of("Claim PPS Indicator Code"));

  private final String code;
  private final Optional<String> display;

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
