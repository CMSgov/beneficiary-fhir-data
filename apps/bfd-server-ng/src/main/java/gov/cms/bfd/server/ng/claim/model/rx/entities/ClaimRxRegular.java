package gov.cms.bfd.server.ng.claim.model.rx.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionFormatCode;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.rx.AdjudicationRx;
import gov.cms.bfd.server.ng.claim.model.rx.ClaimItemRx;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The regular profile pharmacy claim, Rx-Regular. */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
public class ClaimRxRegular extends ClaimRxBase {

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimSubmissionFormatCode;

  @Embedded private AdjudicationRx adjudicationCharge;
  @Embedded private ClaimItemRx claimItem;

  // region Overrides

  @Override
  protected Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.of(adjudicationCharge);
  }

  @Override
  protected Optional<ExplanationOfBenefit.SupportingInformationComponent>
      getSubmissionFormatSupportingInfo() {
    return claimSubmissionFormatCode
        .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
        .map(c -> c.toFhir(supportingInfoFactory));
  }

  @Override
  protected ClaimItemBase getClaimItem() {
    return claimItem;
  }

  // endregion

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.DDPS;
  }
}
