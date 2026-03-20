package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeProfessionalSharedSystems
    extends ClaimLineAdjudicationChargeProfessionalBase {

  @Column(name = "clm_line_otaf_amt")
  private BigDecimal providerObligationToAcceptFullAmount;

  @Override
  Stream<ExplanationOfBenefit.AdjudicationComponent> subClassCharges() {
    return Stream.of(
        AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
            providerObligationToAcceptFullAmount));
  }
}
