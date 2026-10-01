package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Adjudication fields for institutional claims, regular profile, shared systems. */
@Embeddable
public class AdjudicationInstitutionalRegularSharedSystems implements AdjudicationEmbedded {

  @Embedded AdjudicationInstitutionalRegular adjudicationChargeBase;

  @Column(name = "clm_bene_pmt_amt")
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_othr_tp_pd_amt")
  private BigDecimal otherThirdPartyPayerPaidAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return Stream.concat(
            adjudicationChargeBase.toFhirTotal().stream(),
            Stream.of(
                AdjudicationChargeType.BENE_PAYMENT_AMOUNT.toFhirTotal(benePaymentAmount),
                AdjudicationChargeType.OTHER_THIRD_PARTY_PAYER_PAID_AMOUNT.toFhirTotal(
                    otherThirdPartyPayerPaidAmount)))
        .toList();
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(); // no-op, regular doesn't have the fields to create an adjudication component
  }
}
