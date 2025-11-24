package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Period;

@Embeddable
class AdmissionPeriod {
  @Column(name = "clm_actv_care_from_dt")
  private Optional<LocalDate> claimActiveCareFromDate;

  @Column(name = "clm_dschrg_dt")
  private Optional<LocalDate> claimDischargeDate;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (claimActiveCareFromDate.isEmpty()) {
      return null;
    }

    var supportingInfo = supportingInfoFactory.createSupportingInfo();
    var period = new Period();
    period.setStartElement(DateUtil.toFhirDate(claimActiveCareFromDate.get()));
    claimDischargeDate.ifPresent(date -> period.setEndElement(DateUtil.toFhirDate(date)));

    supportingInfo.setTiming(period);
    supportingInfo.setCategory(CarinSupportingInfoCategory.ACTIVE_CARE_FROM_DATE.toFhir());
    return supportingInfo;
  }
}
