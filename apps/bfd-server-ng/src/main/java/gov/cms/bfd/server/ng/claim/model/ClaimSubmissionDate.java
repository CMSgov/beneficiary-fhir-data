package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimSubmissionDate {
  @Column(name = "clm_submsn_dt")
  private Optional<LocalDate> claimSubmissionDate;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (claimSubmissionDate.isEmpty()) {
      return Optional.empty();
    }

    var component = supportingInfoFactory.createSupportingInfo();
    component.setTiming(new DateType().setValue(DateUtil.toDate(claimSubmissionDate.get())));
    component.setCategory(CarinSupportingInfoCategory.SUBMISSION_DATE.toFhir());
    return Optional.of(component);
  }
}
