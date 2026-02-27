package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class AdjudicationChargeInstitutionalNch implements AdjudicationChargeBase {

  @Column(name = "clm_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  @Column(name = "clm_blood_ncvrd_chrg_amt")
  private BigDecimal bloodNoncoveredChargeAmount;

  @Column(name = "clm_ncvrd_chrg_amt")
  private BigDecimal noncoveredChargeAmount;

  @Column(name = "clm_mdcr_coinsrnc_amt")
  private BigDecimal coinsuranceAmount;

  @Column(name = "clm_blood_chrg_amt")
  private BigDecimal bloodChargeAmount;

  @Column(name = "clm_blood_lblty_amt")
  private BigDecimal bloodLiabilityAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return List.of(
        AdjudicationChargeType.ALLOWED_CHARGE_AMOUNT.toFhirTotal(allowedChargeAmount),
        AdjudicationChargeType.SUBMITTED_CHARGE_AMOUNT.toFhirTotal(submittedChargeAmount),
        AdjudicationChargeType.PROVIDER_PAYMENT_AMOUNT.toFhirTotal(providerPaymentAmount),
        AdjudicationChargeType.BENE_PART_B_DEDUCTIBLE_AMOUNT.toFhirTotal(deductibleAmount),
        AdjudicationChargeType.BENE_PART_A_COINSURANCE_LIABILITY_AMOUNT.toFhirTotal(
            coinsuranceAmount),
        AdjudicationChargeType.INPATIENT_NON_COVERED_CHARGE_AMOUNT.toFhirTotal(
            noncoveredChargeAmount));
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.BLOOD_CHARGE_AMOUNT.toFhirAdjudication(bloodChargeAmount),
        AdjudicationChargeType.BENE_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT.toFhirAdjudication(
            bloodLiabilityAmount),
        AdjudicationChargeType.BLOOD_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
            bloodNoncoveredChargeAmount));
  }
}
