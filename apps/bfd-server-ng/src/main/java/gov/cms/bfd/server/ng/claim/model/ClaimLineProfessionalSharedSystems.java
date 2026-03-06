package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Observation;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineProfessionalSharedSystems extends ClaimLineProfessionalBase {

  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineAdjudicationChargeProfessionalSharedSystems adjudicationCharge;

  @Override
  public Optional<Observation> toFhirObservation(int bfdRowId) {
    return Optional.empty();
  }

  @Override
  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent line) {
    var productOrService = new CodeableConcept();
    getHcpcsCode().toFhir().ifPresent(productOrService::addCoding);
    var quantity = getServiceUnitQuantity().toFhir();
    if (productOrService.isEmpty()) {
      ndc.toFhirCoding().ifPresent(productOrService::addCoding);
      ndc.getQualifier().ifPresent(quantity::setUnit);
    }
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    line.setQuantity(quantity);
  }
}
