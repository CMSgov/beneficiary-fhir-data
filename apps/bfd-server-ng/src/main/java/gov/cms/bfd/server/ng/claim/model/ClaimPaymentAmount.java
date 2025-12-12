package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

class ClaimPaymentAmount {
  @Column(name = "clm_pmt_amt")
  private double claimPaymentAmount;

  @Column(name = "clm_pd_dt")
  private Optional<LocalDate> claimPaymentDate;

  ExplanationOfBenefit.PaymentComponent toFhir() {
    var payment =
        new ExplanationOfBenefit.PaymentComponent().setAmount(USD.toFhir(claimPaymentAmount));
    claimPaymentDate.ifPresent(d -> payment.setDate(DateUtil.toDateAndSanitize(d)));
    return payment;
  }
}
