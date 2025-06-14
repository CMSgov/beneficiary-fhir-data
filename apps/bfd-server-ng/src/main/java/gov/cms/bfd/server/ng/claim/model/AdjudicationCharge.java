package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
public class AdjudicationCharge {
  @Column(name = "clm_sbmt_chrg_amt")
  private float submittedChargeAmount;

  ExplanationOfBenefit.AdjudicationComponent toFhir() {
    return AdjudicationChargeType.SUBMITTED_CHARGE_AMOUNT.toFhir(submittedChargeAmount);
  }
}
