package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The "Qualify Stay From Date" for a claim. */
@Embeddable
public class QualifyStayFromDate {
  @Column(name = "clm_qlfy_stay_from_dt")
  private Optional<LocalDate> qualifyStayFromDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (qualifyStayFromDate.isEmpty()) {
      return null;
    }

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_QLFY_STAY_FROM_DT.toFhir())
        .setTiming(new DateType().setValue(DateUtil.toDate(qualifyStayFromDate.get())));
  }
}
