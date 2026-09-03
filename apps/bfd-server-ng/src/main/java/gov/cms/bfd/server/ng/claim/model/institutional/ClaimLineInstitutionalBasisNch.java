package gov.cms.bfd.server.ng.claim.model.institutional;

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
public class ClaimLineInstitutionalBasisNch extends ClaimLineInstitutionalBase {

  @Embedded private ClaimAnsiSignature ansiSignature;

  @Override
  protected void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    ansiSignature.toFhir().ifPresent(line::addAdjudication);
  }
}
