package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class AdjudicationCharge {

  @Column(name = "clm_alowd_chrg_amt")
  private float allowedChargeAmount;

  @Column(name = "clm_sbmt_chrg_amt")
  private float submittedChargeAmount;

  @Column(name = "clm_bene_pmt_amt")
  private double benePaymentAmount;

  @Column(name = "clm_prvdr_pmt_amt")
  private double providerPaymentAmount;

  List<ExplanationOfBenefit.TotalComponent> toFhir() {
      return new ArrayList<>(List.of(
        AdjudicationChargeType.ALLOWED_CHARGE_AMOUNT.toFhirTotal(allowedChargeAmount),
        AdjudicationChargeType.SUBMITTED_CHARGE_AMOUNT.toFhirTotal(submittedChargeAmount),
        AdjudicationChargeType.BENE_PAYMENT_AMOUNT.toFhirTotal(benePaymentAmount),
        AdjudicationChargeType.PROVIDER_PAYMENT_AMOUNT.toFhirTotal(providerPaymentAmount)));
  }
}
