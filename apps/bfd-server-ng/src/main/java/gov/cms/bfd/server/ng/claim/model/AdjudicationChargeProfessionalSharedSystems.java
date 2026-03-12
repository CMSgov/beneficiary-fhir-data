package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class AdjudicationChargeProfessionalSharedSystems implements AdjudicationChargeBase {

  @Column(name = "clm_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_bene_pmt_amt")
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_bene_intrst_pd_amt")
  private BigDecimal beneInterestPaidAmount;

  @Column(name = "clm_bene_pmt_coinsrnc_amt")
  private BigDecimal beneCoinsuranceAmount;

  @Column(name = "clm_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  @Column(name = "clm_mdcr_coinsrnc_amt")
  private BigDecimal coinsuranceAmount;

  @Column(name = "clm_ncvrd_chrg_amt")
  private BigDecimal noncoveredChargeAmount;

  @Column(name = "clm_othr_tp_pd_amt")
  private BigDecimal otherThirdPartyPayerPaidAmount;

  @Column(name = "clm_blood_lblty_amt")
  private BigDecimal bloodLiabilityAmount;

  @Column(name = "clm_cob_ptnt_resp_amt")
  private BigDecimal cobPatientResponsibilityAmount;

  @Column(name = "clm_prvdr_otaf_amt")
  private BigDecimal providerObligationToAcceptAmount;

  @Column(name = "clm_prvdr_rmng_due_amt")
  private BigDecimal remainingAmountToProvider;

  @Column(name = "clm_blood_ncvrd_chrg_amt")
  private BigDecimal bloodNoncoveredChargeAmount;

  @Column(name = "clm_prvdr_intrst_pd_amt")
  private BigDecimal providerInterestPaidAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return List.of(
        AdjudicationChargeType.ALLOWED_CHARGE_AMOUNT.toFhirTotal(allowedChargeAmount),
        AdjudicationChargeType.SUBMITTED_CHARGE_AMOUNT.toFhirTotal(submittedChargeAmount),
        AdjudicationChargeType.BENE_PAYMENT_AMOUNT.toFhirTotal(benePaymentAmount),
        AdjudicationChargeType.PROVIDER_PAYMENT_AMOUNT.toFhirTotal(providerPaymentAmount),
        AdjudicationChargeType.BENE_PART_B_DEDUCTIBLE_AMOUNT.toFhirTotal(deductibleAmount),
        AdjudicationChargeType.BENE_PART_A_COINSURANCE_LIABILITY_AMOUNT.toFhirTotal(
            coinsuranceAmount),
        AdjudicationChargeType.INPATIENT_NON_COVERED_CHARGE_AMOUNT.toFhirTotal(
            noncoveredChargeAmount),
        AdjudicationChargeType.OTHER_THIRD_PARTY_PAYER_PAID_AMOUNT.toFhirTotal(
            otherThirdPartyPayerPaidAmount),
        AdjudicationChargeType.BENE_COINSURANCE_AMOUNT.toFhirTotal(beneCoinsuranceAmount));
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.BENE_INTEREST_PAID_AMOUNT.toFhirAdjudication(beneInterestPaidAmount),
        AdjudicationChargeType.BENE_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT.toFhirAdjudication(
            bloodLiabilityAmount),
        AdjudicationChargeType.BLOOD_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
            bloodNoncoveredChargeAmount),
        AdjudicationChargeType.COB_PATIENT_RESPONSIBILITY_AMOUNT.toFhirAdjudication(
            cobPatientResponsibilityAmount),
        AdjudicationChargeType.PROVIDER_INTEREST_PAID_AMOUNT.toFhirAdjudication(
            providerInterestPaidAmount),
        AdjudicationChargeType.PROVIDER_OBLIGATION_TO_ACCEPT_AMOUNT.toFhirAdjudication(
            providerObligationToAcceptAmount),
        AdjudicationChargeType.REMAINING_AMOUNT_TO_PROVIDER.toFhirAdjudication(
            remainingAmountToProvider));
  }
}
