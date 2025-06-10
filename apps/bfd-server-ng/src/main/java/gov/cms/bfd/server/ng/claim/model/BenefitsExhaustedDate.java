package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
public class BenefitsExhaustedDate {
  @Column(name = "clm_mdcr_exhstd_dt")
  private LocalDate benefitsExhaustedDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.BENEFITS_EXHAUSTED_DATE.toFhir())
        .setTiming(new DateTimeType().setValue(DateUtil.toDate(benefitsExhaustedDate)));
  }
}
