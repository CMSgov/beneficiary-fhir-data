package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
public class ClaimPaymentAmount {
  @Column(name = "clm_pmt_amt")
  private BigDecimal claimPaymentAmount;

  @Column(name = "clm_pd_dt")
  private Optional<LocalDate> claimPaymentDate;

  public ExplanationOfBenefit.PaymentComponent toFhir() {
    var payment =
        new ExplanationOfBenefit.PaymentComponent().setAmount(USD.toFhir(claimPaymentAmount));
    claimPaymentDate.ifPresent(d -> payment.setDate(DateUtil.toDate(d)));
    return payment;
  }
}
