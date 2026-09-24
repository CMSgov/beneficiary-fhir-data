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

@Embeddable
class NoncoveredThroughDate {
  @Column(name = "clm_ncvrd_thru_dt")
  private Optional<LocalDate> noncoveredThroughDate;

  @Column(name = "bfd_clm_ncvrd_thru_dt")
  private Optional<LocalDate> bfdNoncoveredThroughDate;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    Optional<LocalDate> localDate;
    if (noncoveredThroughDate.isPresent()) {
      localDate = noncoveredThroughDate;
    } else if (bfdNoncoveredThroughDate.isPresent()) {
      localDate = bfdNoncoveredThroughDate;
    } else {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_NCVRD_THRU_DT.toFhir())
            .setTiming(new DateType().setValue(DateUtil.toDate(localDate.get())));
    return Optional.of(component);
  }
}
