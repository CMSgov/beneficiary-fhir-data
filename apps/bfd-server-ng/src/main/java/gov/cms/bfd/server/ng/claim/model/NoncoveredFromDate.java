package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.time.LocalDate;

@Embeddable
public class NoncoveredFromDate {
  @Column(name = "clm_ncvrd_from_dt")
  private LocalDate noncoveredFromDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_NCVRD_FROM_DT.toFhir())
        .setTiming(new DateTimeType().setValue(DateUtil.toDate(noncoveredFromDate)));
  }
}
