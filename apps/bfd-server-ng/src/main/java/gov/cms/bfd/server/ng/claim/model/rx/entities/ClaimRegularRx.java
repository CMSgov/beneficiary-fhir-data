package gov.cms.bfd.server.ng.claim.model.rx.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionFormatCode;
import gov.cms.bfd.server.ng.claim.model.rx.AdjudicationChargeRx;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Optional;
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The regular profile pharmacy claim. */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimRegularRx extends ClaimRxBase {

  // region Adjudication Charge
  @Embedded private AdjudicationChargeRx adjudicationCharge;

  @Override
  protected Optional<AdjudicationChargeRx> getAdjudicationChargeRx() {
    return Optional.of(adjudicationCharge);
  }

  // endregion

  // region Claim Submission Format Code
  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimSubmissionFormatCode;

  @Override
  protected Optional<ExplanationOfBenefit.SupportingInformationComponent>
      submissionFormatSupportingInfo() {
    return claimSubmissionFormatCode
        .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
        .map(c -> c.toFhir(supportingInfoFactory));
  }
  // endregion

}
