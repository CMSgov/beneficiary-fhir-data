package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Adjudication Charge Base interface. */
public interface AdjudicationEmbedded {

  /**
   * toFhirAdjudication().
   *
   * @return a list of eob.AdjudicationComponent
   */
  List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication();

  /**
   * toFhirTotal(), optional.
   *
   * @return a list of eob.TotalComponent
   */
  default List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return List.of();
  }

  /**
   * Constructs a common adjudication component representing the payment status for professional
   * claims.
   *
   * @return the adjudication component with benefit payment status and adjudication status
   */
  static ExplanationOfBenefit.AdjudicationComponent getProfessionalBenefitPaymentStatus() {
    var component = new ExplanationOfBenefit.AdjudicationComponent();
    component.setCategory(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                    .setCode("benefitpaymentstatus")
                    .setDisplay("Benefit Payment Status")));
    component.setReason(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_PAYER_ADJUDICATION_STATUS)
                    .setCode("other")
                    .setDisplay("Other")));
    return component;
  }
}
