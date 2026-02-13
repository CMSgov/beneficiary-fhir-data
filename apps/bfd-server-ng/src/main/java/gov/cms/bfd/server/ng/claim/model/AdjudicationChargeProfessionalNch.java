package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class AdjudicationChargeProfessionalNch implements AdjudicationChargeBase {

  @Column(name = "clm_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_bene_pmt_amt")
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  @Column(name = "clm_blood_chrg_amt")
  private BigDecimal bloodChargeAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return List.of(
        AdjudicationChargeType.ALLOWED_CHARGE_AMOUNT.toFhirTotal(allowedChargeAmount),
        AdjudicationChargeType.SUBMITTED_CHARGE_AMOUNT.toFhirTotal(submittedChargeAmount),
        AdjudicationChargeType.BENE_PAYMENT_AMOUNT.toFhirTotal(benePaymentAmount),
        AdjudicationChargeType.PROVIDER_PAYMENT_AMOUNT.toFhirTotal(providerPaymentAmount),
        AdjudicationChargeType.BENE_PART_B_DEDUCTIBLE_AMOUNT.toFhirTotal(deductibleAmount));
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.BLOOD_CHARGE_AMOUNT.toFhirAdjudication(bloodChargeAmount));
  }
}
