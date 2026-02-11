package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimProcessDate {
  @Column(name = "clm_cms_proc_dt")
  private Optional<LocalDate> claimProcessDate;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return claimProcessDate.map(
        date ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_CMS_PROC_DT.toFhir())
                .setTiming(new DateType().setValue(DateUtil.toDate(date))));
  }
}
