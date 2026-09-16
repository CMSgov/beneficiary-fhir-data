package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication data shared by CMS (both sources). */
@Embeddable
class ClaimLineAdjudicationProfessionalCms implements AdjudicationEmbedded {

  @Embedded private ClaimLineAdjudicationProfessionalRegular baseAdjudications;

  @Column(name = "clm_line_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_carr_psych_ot_lmt_amt")
  private BigDecimal therapyAmountAppliedToLimit;

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.concat(
            baseAdjudications.toFhirAdjudication().stream(),
            Stream.of(
                AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
                    providerPaymentAmount),
                AdjudicationChargeType.LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT.toFhirAdjudication(
                    therapyAmountAppliedToLimit)))
        .toList();
  }
}
