package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.claim.model.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Yep.
 */
@Embeddable
public class ClaimIDRLoadDate {
  @Column(name = "clm_idr_ld_dt")
  private LocalDate claimIDRLoadDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_IDR_LD_DT.toFhir())
        .setTiming(new DateType().setValue(DateUtil.toDate(claimIDRLoadDate)));
  }
}
