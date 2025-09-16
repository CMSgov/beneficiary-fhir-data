package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimSubmissionDate {
  @Column(name = "clm_submsn_dt")
  private LocalDate claimSubmissionDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setTiming(new DateType().setValue(DateUtil.toDate(claimSubmissionDate)));
    supportingInfo.setCategory(CarinSupportingInfoCategory.SUBMISSION_DATE.toFhir());
    return supportingInfo;
  }
}
