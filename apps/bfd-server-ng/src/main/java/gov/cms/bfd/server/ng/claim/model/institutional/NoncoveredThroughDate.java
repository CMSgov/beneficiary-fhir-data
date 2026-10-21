package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Latest Claim Occurrence event for "Not Covered Thru Date", the end of the period. */
@Embeddable
class NoncoveredThroughDate {

  @Column(name = "clm_ncvrd_thru_dt")
  private Optional<LocalDate> noncoveredThroughDate;

  @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return noncoveredThroughDate.map(
        localDate ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_NCVRD_THRU_DT.toFhir())
                .setTiming(new DateType().setValue(DateUtil.toDate(localDate))));
  }
}
