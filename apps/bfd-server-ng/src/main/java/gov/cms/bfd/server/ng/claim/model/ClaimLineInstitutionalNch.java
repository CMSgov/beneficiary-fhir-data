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
@SuppressWarnings("java:S2201")
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pmd_uniq_trkng_num"))
public class ClaimLineInstitutionalNch extends ClaimLineInstitutionalBase {

  @Embedded private ClaimLineAdjudicationChargeInstitutionalNch adjudicationCharge;
  @Embedded private ClaimAnsiSignature ansiSignature;
  @Embedded private ClaimLineInstitutionalNchExtensions claimLineInstitutionalNchExtensions;

  @Override
  void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    adjudicationCharge.toFhir().forEach(line::addAdjudication);
    ansiSignature.toFhir().ifPresent(line::addAdjudication);
    claimLineInstitutionalNchExtensions.toFhir().forEach(line::addExtension);
  }
}
