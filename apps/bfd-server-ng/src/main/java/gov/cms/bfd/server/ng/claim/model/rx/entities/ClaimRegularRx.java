package gov.cms.bfd.server.ng.claim.model.rx.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionFormatCode;
import jakarta.persistence.Column;
import java.util.Optional;
import javax.annotation.processing.Generated;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The regular profile pharmacy claim. */
@Generated("TODO - Remove after query optimization implementation")
public class ClaimRegularRx extends ClaimRxBase {

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimSubmissionFormatCode;

  @Override
  protected Optional<ExplanationOfBenefit.SupportingInformationComponent>
      submissionFormatSupportingInfo() {
    return claimSubmissionFormatCode
        .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
        .map(c -> c.toFhir(supportingInfoFactory));
  }
}
