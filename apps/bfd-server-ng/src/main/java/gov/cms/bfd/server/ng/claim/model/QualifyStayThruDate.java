package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.time.LocalDate;

public class QualifyStayThruDate {
    @Column(name = "clm_qlfy_stay_thru_dt")
    private LocalDate qualifyStayThruDate;

    ExplanationOfBenefit.SupportingInformationComponent toFhir(
            SupportingInfoFactory supportingInfoFactory) {
        return supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_QLFY_STAY_THRU_DT.toFhir())
                .setTiming(new DateType().setValue(DateUtil.toDate(qualifyStayThruDate)));
    }
}
