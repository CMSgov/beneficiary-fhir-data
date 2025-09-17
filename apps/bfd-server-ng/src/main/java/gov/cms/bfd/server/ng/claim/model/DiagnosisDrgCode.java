package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class DiagnosisDrgCode {
  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "dgns_drg_cd")
  private Optional<Integer> diagnosisDrgCode;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (diagnosisDrgCode.isEmpty()) {
      return Optional.empty();
    }
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setCategory(CarinSupportingInfoCategory.DIAGNOSIS_DRG_CODE.toFhir());
    supportingInfo.setCode(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.CMS_MS_DRG)
                .setCode(String.valueOf(diagnosisDrgCode.get()))));

    return Optional.of(supportingInfo);
  }
}
