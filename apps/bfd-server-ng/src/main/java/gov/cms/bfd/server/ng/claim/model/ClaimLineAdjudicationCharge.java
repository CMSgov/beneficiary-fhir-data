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
        AdjudicationChargeType.LINE_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
            noncoveredChargeAmount),
        AdjudicationChargeType.LINE_ALLOWED_CHARGE_AMOUNT.toFhirAdjudication(allowedChargeAmount),
        AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
            providerPaymentAmount),
        AdjudicationChargeType.LINE_BENE_PAYMENT_AMOUNT.toFhirAdjudication(benePaymentAmount),
        AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount),
        AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(coveredPaidAmount),
        AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(
            deductibleAmount));
  }
}
