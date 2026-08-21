package gov.cms.bfd.server.ng.claim.model.common;

import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Interface for an ExplanationofBenefit.PaymentComponent. */
public interface ClaimPaymentComponentBase {

  /**
   * toFhir().
   *
   * @return an ExplanationOfBenefit.PaymentComponent
   */
  Optional<ExplanationOfBenefit.PaymentComponent> toFhir();
}
