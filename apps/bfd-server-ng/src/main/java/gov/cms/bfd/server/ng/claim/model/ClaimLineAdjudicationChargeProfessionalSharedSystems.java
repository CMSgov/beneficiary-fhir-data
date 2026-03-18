package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeProfessionalSharedSystems
    extends ClaimLineAdjudicationChargeProfessionalBase {

  @Column(name = "clm_line_otaf_amt")
  private BigDecimal providerObligationToAcceptFullAmount;

  @Override
  Stream<ExplanationOfBenefit.AdjudicationComponent> subClassCharges() {
    var benefitPaymentStatus = new ExplanationOfBenefit.AdjudicationComponent();
    benefitPaymentStatus.setCategory(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                    .setCode("benefitpaymentstatus")
                    .setDisplay("Benefit Payment Status")));
    benefitPaymentStatus.setReason(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_PAYER_ADJUDICATION_STATUS)
                    .setCode("other")
                    .setDisplay("Other")));
    return Stream.of(
        AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
            providerObligationToAcceptFullAmount),
        benefitPaymentStatus);
  }
}
