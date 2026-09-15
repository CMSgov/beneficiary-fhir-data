package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * ExplanationOfBenefit.PaymentComponent The base information for generating a Payment Component.
 */
@Embeddable
@Getter
public class ClaimPaymentComponent implements ClaimPaymentComponentBase {
  @Column(name = "clm_pd_dt")
  private Optional<LocalDate> paymentDate;

  /**
   * Simple payment component created with optional date.
   *
   * @return an optional PaymentComponent if a date exists
   */
  @Override
  public Optional<ExplanationOfBenefit.PaymentComponent> toFhir() {
    return paymentDate.map(
        d -> new ExplanationOfBenefit.PaymentComponent().setDate(DateUtil.toDate(d)));
  }
}
