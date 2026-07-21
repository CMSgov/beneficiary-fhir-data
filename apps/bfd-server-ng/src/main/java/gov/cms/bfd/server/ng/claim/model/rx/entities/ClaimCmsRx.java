package gov.cms.bfd.server.ng.claim.model.rx.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionFormatCode;
import gov.cms.bfd.server.ng.claim.model.rx.AdjudicationChargeRx;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The CMS profile of a Rx Claim. */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
public class ClaimCmsRx extends ClaimRxBase {

  @Embedded private AdjudicationChargeRx adjudicationCharge;

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimSubmissionFormatCode;

  @Override
  protected Optional<ExplanationOfBenefit.SupportingInformationComponent>
      submissionFormatSupportingInfo() {
    return claimSubmissionFormatCode
        .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
        .map(c -> c.toFhir(supportingInfoFactory));
  }

  @Override
  protected Optional<AdjudicationChargeRx> getAdjudicationChargeRx() {
    return Optional.of(adjudicationCharge);
  }
}
