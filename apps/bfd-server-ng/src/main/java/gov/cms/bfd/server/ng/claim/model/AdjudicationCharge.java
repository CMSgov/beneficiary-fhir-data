package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.List;

@Embeddable
public class AdjudicationCharge {
  @Column(name = "clm_line_ncvrd_chrg_amt")
  private float noncoveredChargeAmount;

  @Column(name = "clm_line_alowd_chrg_amt")
  private float allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt")
  private float submittedChargeAmount;

  @Column(name = "clm_line_prvdr_pmt_amt")
  private float providerPaymentAmount;

  @Column(name = "clm_line_bene_pmt_amt")
  private float benePaymentAmount;

  @Column(name = "clm_line_bene_pd_amt")
  private float benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt")
  private float coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt")
  private float deductibleAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return List.of(
        AdjudicationChargeType.NONCOVERED_CHARGE_AMOUNT.toFhir(noncoveredChargeAmount),
        AdjudicationChargeType.ALLOWED_CHARGE_AMOUNT.toFhir(allowedChargeAmount),
        AdjudicationChargeType.SUBMITTED_CHARGE_AMOUNT.toFhir(submittedChargeAmount),
        AdjudicationChargeType.PROVIDER_PAYMENT_AMOUNT.toFhir(providerPaymentAmount),
        AdjudicationChargeType.BENE_PAYMENT_AMOUNT.toFhir(benePaymentAmount),
        AdjudicationChargeType.BENE_PAID_AMOUNT.toFhir(benePaidAmount),
        AdjudicationChargeType.COVERED_PAID_AMOUNT.toFhir(coveredPaidAmount),
        AdjudicationChargeType.MEDICARE_DEDUCTIBLE_AMOUNT.toFhir(deductibleAmount));
  }
}
