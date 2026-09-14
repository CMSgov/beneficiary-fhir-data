package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@MappedSuperclass
abstract class ClaimLineAdjudicationProfessionalCms extends ClaimLineAdjudicationProfessional {

  @Column(name = "clm_line_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_carr_psych_ot_lmt_amt")
  private BigDecimal therapyAmountAppliedToLimit;

  @Override
  Stream<ExplanationOfBenefit.AdjudicationComponent> subClassCharges() {
    return Stream.concat(
        super.subClassCharges(),
        Stream.of(
            AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
                providerPaymentAmount),
            AdjudicationChargeType.LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT.toFhirAdjudication(
                therapyAmountAppliedToLimit)));
  }
}
