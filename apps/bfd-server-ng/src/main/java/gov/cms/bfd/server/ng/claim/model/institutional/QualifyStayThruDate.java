package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * The "Qualify Stay Thru Date" for a claim, representing the end date of the qualifying stay
 * period.
 */
public class QualifyStayThruDate {

  @Column(name = "bfd_clm_qlfy_stay_thru_dt")
  private Optional<LocalDate> bfdQualifyStayThruDate;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (bfdQualifyStayThruDate.isEmpty()) {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_QLFY_STAY_THRU_DT.toFhir())
            .setTiming(new DateType().setValue(DateUtil.toDate(bfdQualifyStayThruDate.get())));
    return Optional.of(component);
  }
}
