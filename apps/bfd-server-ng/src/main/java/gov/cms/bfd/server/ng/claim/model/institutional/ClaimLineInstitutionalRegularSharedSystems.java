package gov.cms.bfd.server.ng.claim.model.institutional;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line institutional regular shared systems. */
@Embeddable
@Getter
public class ClaimLineInstitutionalRegularSharedSystems extends ClaimLineInstitutionalBase {

  @Embedded private ClaimLineAdjudicationInstitutionalSharedSystems adjudicationCharge;

  @Override
  protected void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    adjudicationCharge.toFhirAdjudication().forEach(line::addAdjudication);
  }
}
