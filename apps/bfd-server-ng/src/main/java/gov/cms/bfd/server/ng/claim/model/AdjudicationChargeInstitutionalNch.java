package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class AdjudicationChargeInstitutionalNch {

  @Column(name = "clm_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return List.of(
        AdjudicationChargeType.ALLOWED_CHARGE_AMOUNT.toFhirTotal(allowedChargeAmount),
        AdjudicationChargeType.SUBMITTED_CHARGE_AMOUNT.toFhirTotal(submittedChargeAmount),
        AdjudicationChargeType.PROVIDER_PAYMENT_AMOUNT.toFhirTotal(providerPaymentAmount),
        AdjudicationChargeType.BENE_PART_B_DEDUCTIBLE_AMOUNT.toFhirTotal(deductibleAmount));
  }
}
