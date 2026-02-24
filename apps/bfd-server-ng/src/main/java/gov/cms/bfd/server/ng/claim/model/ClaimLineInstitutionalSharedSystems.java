package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line info. */
@Embeddable
@Getter
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineInstitutionalSharedSystems extends ClaimLineInstitutionalBase {

  @Embedded private ClaimLineAdjudicationChargeInstitutionalSharedSystems adjudicationCharge;

  @Override
  void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    adjudicationCharge.toFhir().forEach(line::addAdjudication);
  }
}
