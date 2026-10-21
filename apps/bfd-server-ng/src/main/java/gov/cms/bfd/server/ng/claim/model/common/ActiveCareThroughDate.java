package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Latest Claim Related Occurrence event for "Active Care Thru Date", the end of active care period.
 */
@Embeddable
public class ActiveCareThroughDate {

  @Column(name = "bfd_clm_actv_care_thru_dt")
  private Optional<LocalDate> bfdActiveCareThroughDate;

  @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
  public Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return bfdActiveCareThroughDate.map(
        localDate ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_ACTV_CARE_THRU_DT.toFhir())
                .setTiming(new DateType().setValue(DateUtil.toDate(localDate))));
  }
}
