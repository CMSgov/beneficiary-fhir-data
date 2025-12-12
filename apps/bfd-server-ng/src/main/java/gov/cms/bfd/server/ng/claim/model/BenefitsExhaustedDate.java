package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class BenefitsExhaustedDate {
  @Column(name = "clm_mdcr_exhstd_dt")
  private Optional<LocalDate> benefitsExhaustedDate;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (benefitsExhaustedDate.isEmpty()) {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_MDCR_EXHSTD_DT.toFhir())
            .setTiming(new DateType().setValue(DateUtil.toDate(benefitsExhaustedDate.get())));
    return Optional.of(component);
  }
}
