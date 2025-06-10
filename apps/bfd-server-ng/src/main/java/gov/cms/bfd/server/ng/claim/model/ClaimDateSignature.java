package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Period;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "claim_date_signature", schema = "idr")
public class ClaimDateSignature {
  @Column(name = "clm_dt_sgntr_sk")
  private long claimDateSignatureSk;

  @Embedded private AdmissionPeriod admissionPeriod;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private NchWeeklyProcessingDate nchWeeklyProcessingDate;
  @Embedded private ActiveCareThroughDate activeCareThroughDate;
  @Embedded private NoncoveredFromDate noncoveredFromDate;
  @Embedded private NoncoveredThroughDate noncoveredThroughDate;
  @Embedded private BenefitsExhaustedDate benefitsExhaustedDate;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return List.of(
        admissionPeriod.toFhir(supportingInfoFactory),
        claimSubmissionDate.toFhir(supportingInfoFactory),
        nchWeeklyProcessingDate.toFhir(supportingInfoFactory),
        activeCareThroughDate.toFhir(supportingInfoFactory),
        noncoveredFromDate.toFhir(supportingInfoFactory),
        noncoveredThroughDate.toFhir(supportingInfoFactory),
        benefitsExhaustedDate.toFhir(supportingInfoFactory));
  }
}
