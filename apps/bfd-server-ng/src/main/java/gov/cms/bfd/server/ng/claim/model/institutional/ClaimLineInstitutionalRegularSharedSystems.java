package gov.cms.bfd.server.ng.claim.model.institutional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line institutional regular shared systems. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineInstitutionalRegularSharedSystems extends ClaimLineInstitutionalBase {

  @Embedded private ClaimLineAdjudicationChargeInstitutionalSharedSystems adjudicationCharge;

  @Override
  protected void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    adjudicationCharge.toFhir().forEach(line::addAdjudication);
  }
}
