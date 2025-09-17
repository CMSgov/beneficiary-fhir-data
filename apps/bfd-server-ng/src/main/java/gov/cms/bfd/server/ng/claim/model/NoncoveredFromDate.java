package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class NoncoveredFromDate {
  @Column(name = "clm_ncvrd_from_dt")
  private LocalDate noncoveredFromDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_NCVRD_FROM_DT.toFhir())
        .setTiming(new DateType().setValue(DateUtil.toDate(noncoveredFromDate)));
  }
}
