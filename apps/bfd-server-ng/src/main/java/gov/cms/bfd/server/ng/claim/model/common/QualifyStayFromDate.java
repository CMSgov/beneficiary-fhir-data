package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Latest Claim Occurrence event for "Qualify Stay From Date", the beginning of the period. */
@Embeddable
public class QualifyStayFromDate {

  @Column(name = "bfd_clm_qlfy_stay_from_dt")
  private Optional<LocalDate> bfdQualifyStayFromDate;

  @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
  public Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return bfdQualifyStayFromDate.map(
        localDate ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_QLFY_STAY_FROM_DT.toFhir())
                .setTiming(new DateType().setValue(DateUtil.toDate(localDate))));
  }
}
