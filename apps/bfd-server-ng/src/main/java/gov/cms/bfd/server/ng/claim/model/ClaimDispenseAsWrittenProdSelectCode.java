package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Dispense as Written Product Selection Codes. */
@Embeddable
public class ClaimDispenseAsWrittenProdSelectCode {
  @Column(name = "clm_daw_prod_slctn_cd")
  private String dispenseAsWrittenProdSelectCode;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    String code = dispenseAsWrittenProdSelectCode;
    if (code == null || code.trim().isEmpty()) {
      code = "0";
    }

    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.DAW_CODE.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding().setSystem(SystemUrls.HL7_CLAIM_DAW_PROD_SELECT_CODE).setCode(code)));

    return supportingInfo;
  }
}
