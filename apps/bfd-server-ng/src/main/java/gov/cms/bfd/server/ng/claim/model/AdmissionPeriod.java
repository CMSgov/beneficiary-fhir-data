package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Period;

@Embeddable
class AdmissionPeriod {
  @Column(name = "clm_actv_care_from_dt")
  private LocalDate claimActiveCareFromDate;

  @Column(name = "clm_dschrg_dt")
  private LocalDate claimDischargeDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    var period = new Period();
    period.setStartElement(DateUtil.toFhirDate(claimActiveCareFromDate));
    period.setEndElement(DateUtil.toFhirDate(claimDischargeDate));
    supportingInfo.setTiming(period);
    supportingInfo.setCategory(CarinSupportingInfoCategory.ACTIVE_CARE_FROM_DATE.toFhir());
    return supportingInfo;
  }
}
