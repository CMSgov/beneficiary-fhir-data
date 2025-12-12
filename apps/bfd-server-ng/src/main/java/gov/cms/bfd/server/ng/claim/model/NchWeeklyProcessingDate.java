package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class NchWeeklyProcessingDate {
  @Column(name = "clm_nch_wkly_proc_dt")
  private Optional<LocalDate> weeklyProcessingDate;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (weeklyProcessingDate.isEmpty()) {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_NCH_WKLY_PROC_DT.toFhir())
            .setTiming(
                new DateType().setValue(DateUtil.toDateAndSanitize(weeklyProcessingDate.get())));
    return Optional.of(component);
  }
}
