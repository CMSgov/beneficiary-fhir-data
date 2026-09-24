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
class NoncoveredFromDate {
  @Column(name = "clm_ncvrd_from_dt")
  private Optional<LocalDate> noncoveredFromDate;

  @Column(name = "bfd_clm_ncvrd_from_dt")
  private Optional<LocalDate> bfdNoncoveredFromDate;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    Optional<LocalDate> localDate;
    if (noncoveredFromDate.isPresent()) {
      localDate = noncoveredFromDate;
    } else if (bfdNoncoveredFromDate.isPresent()) {
      localDate = bfdNoncoveredFromDate;
    } else {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_NCVRD_FROM_DT.toFhir())
            .setTiming(new DateType().setValue(DateUtil.toDate(localDate.get())));
    return Optional.of(component);
  }
}
