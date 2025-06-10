package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.time.LocalDate;

@Embeddable
public class ClaimSubmissionDate {
  @Column(name = "clm_submsn_dt")
  private LocalDate claimSubmissionDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    supportingInfo.setTiming(new DateTimeType().setValue(DateUtil.toDate(claimSubmissionDate)));
    supportingInfo.setCategory(CarinSupportingInfoCategory.SUBMISSION_DATE.toFhir());
    return supportingInfo;
  }
}
