package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.time.LocalDate;

@Embeddable
public class QualifyStayFromDate {
    @Column(name = "clm_qlfy_stay_from_dt")
    private LocalDate qualifyStayFromDate;

    ExplanationOfBenefit.SupportingInformationComponent toFhir(
            SupportingInfoFactory supportingInfoFactory) {
        return supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_QLFY_STAY_FROM_DT.toFhir())
                .setTiming(new DateType().setValue(DateUtil.toDate(qualifyStayFromDate)));
    }
}
