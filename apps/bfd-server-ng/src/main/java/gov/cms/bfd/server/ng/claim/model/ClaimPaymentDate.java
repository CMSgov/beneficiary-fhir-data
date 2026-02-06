package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

class ClaimPaymentDate {

  @Column(name = "clm_pd_dt")
  private Optional<LocalDate> paymentDate;

  ExplanationOfBenefit.PaymentComponent toFhir() {
    var payment = new ExplanationOfBenefit.PaymentComponent();
    paymentDate.ifPresent(d -> payment.setDate(DateUtil.toDate(d)));
    return payment;
  }
}
