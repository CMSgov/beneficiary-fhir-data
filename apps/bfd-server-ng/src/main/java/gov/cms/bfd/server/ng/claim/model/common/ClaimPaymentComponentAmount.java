package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** ExplanationOfBeneift.PaymentComponent A payment component with a payment attached to it. */
@Embeddable
@Getter
public class ClaimPaymentComponentAmount implements ClaimPaymentComponentBase {
  @Embedded private ClaimPaymentComponent base;

  @Column(name = "clm_pmt_amt")
  private BigDecimal paymentAmount;

  /**
   * Return a payment component with an optional date and required amount.
   *
   * @return a PaymentComponent with an amount and optional date
   */
  @Override
  public Optional<ExplanationOfBenefit.PaymentComponent> toFhir() {
    var payment = new ExplanationOfBenefit.PaymentComponent().setAmount(USD.toFhir(paymentAmount));
    base.getPaymentDate().ifPresent(d -> payment.setDate(DateUtil.toDate(d)));
    return Optional.of(payment);
  }
}
