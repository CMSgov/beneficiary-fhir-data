package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationCharge {
  @Column(name = "clm_line_ncvrd_chrg_amt")
  private double noncoveredChargeAmount;

  @Column(name = "clm_line_alowd_chrg_amt")
  private double allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt")
  private double submittedChargeAmount;

  @Column(name = "clm_line_prvdr_pmt_amt")
  private double providerPaymentAmount;

  @Column(name = "clm_line_bene_pmt_amt")
  private double benePaymentAmount;

  @Column(name = "clm_line_bene_pd_amt")
  private double benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt")
  private double coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt")
  private double deductibleAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return List.of(
        AdjudicationChargeType.NONCOVERED_CHARGE_AMOUNT.toFhir(noncoveredChargeAmount),
        AdjudicationChargeType.ALLOWED_CHARGE_AMOUNT.toFhir(allowedChargeAmount),
        AdjudicationChargeType.PROVIDER_PAYMENT_AMOUNT.toFhir(providerPaymentAmount),
        AdjudicationChargeType.BENE_PAYMENT_AMOUNT.toFhir(benePaymentAmount),
        AdjudicationChargeType.BENE_PAID_AMOUNT.toFhir(benePaidAmount),
        AdjudicationChargeType.COVERED_PAID_AMOUNT.toFhir(coveredPaidAmount),
        AdjudicationChargeType.MEDICARE_DEDUCTIBLE_AMOUNT.toFhir(deductibleAmount));
  }
}
