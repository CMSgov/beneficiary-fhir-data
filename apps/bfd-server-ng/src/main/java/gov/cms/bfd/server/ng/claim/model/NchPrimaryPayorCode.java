package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class NchPrimaryPayorCode {
  @Column(name = "clm_nch_prmry_pyr_cd")
  private Optional<String> nchPrimaryPayorCode;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return nchPrimaryPayorCode.map(
        code ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_NCH_PRMRY_PYR_CD.toFhir())
                .setCode(
                    new CodeableConcept(
                        new Coding()
                            .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PRIMARY_PAYOR_CODE)
                            .setCode(code))));
  }
}
