package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ActiveCareThroughDate {
  @Column(name = "clm_actv_care_thru_dt")
  private LocalDate activeCareThroughDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_ACTV_CARE_THRU_DT.toFhir())
        .setTiming(new DateType().setValue(DateUtil.toDate(activeCareThroughDate)));
  }
}
