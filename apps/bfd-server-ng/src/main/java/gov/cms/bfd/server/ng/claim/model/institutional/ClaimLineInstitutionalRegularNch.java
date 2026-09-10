package gov.cms.bfd.server.ng.claim.model.institutional;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line for regular nch. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLineInstitutionalRegularNch extends ClaimLineInstitutionalBase {

  @Embedded private ClaimAnsiSignature ansiSignature;

  @Override
  protected void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    ansiSignature.toFhir().ifPresent(line::addAdjudication);
  }
}
