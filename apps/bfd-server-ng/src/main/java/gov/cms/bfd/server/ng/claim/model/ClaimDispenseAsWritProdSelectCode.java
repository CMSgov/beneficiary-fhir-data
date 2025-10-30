package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
public class ClaimDispenseAsWritProdSelectCode {
  @Column(name = "clm_daw_prod_slctn_cd")
  private Optional<String> dispenseAsWritProdSelectCode;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (dispenseAsWritProdSelectCode.isEmpty()) {
      return Optional.empty();
    }
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.DAW_CODE.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.HL7_CLAIM_DAW_PROD_SELECT_CODE)
                .setCode(dispenseAsWritProdSelectCode.get())));

    return Optional.of(supportingInfo);
  }
}
