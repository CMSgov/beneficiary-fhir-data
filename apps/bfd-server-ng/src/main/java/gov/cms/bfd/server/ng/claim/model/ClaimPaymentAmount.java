package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

public class ClaimPaymentAmount {
  @Column(name = "clm_pmt_amt")
  private double claimPaymentAmount;

  ExplanationOfBenefit.PaymentComponent toFhir() {
    return new ExplanationOfBenefit.PaymentComponent().setAmount(USD.toFhir(claimPaymentAmount));
  }
}
