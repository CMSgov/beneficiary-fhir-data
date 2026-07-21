package gov.cms.bfd.server.ng.claim.model.rx.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcessDate;
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

  // region Adjudication Charge
  @Embedded private AdjudicationChargeRx adjudicationCharge;

  @Override
  protected Optional<AdjudicationChargeRx> getAdjudicationChargeRx() {
    return Optional.of(adjudicationCharge);
  }

  // endregion

  // region Claim Process Date
  @Embedded private ClaimProcessDate claimProcessDate;

  @Override
  protected Optional<ClaimProcessDate> getClaimProcessDate() {
    return Optional.of(claimProcessDate);
  }

  // endregion

  // region Claim Submission Format Code
  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimSubmissionFormatCode;

  @Override
  protected Optional<ExplanationOfBenefit.SupportingInformationComponent>
      submissionFormatSupportingInfo() {
    return claimSubmissionFormatCode
        .filter(_ -> getClaimTypeCode().isClaimSubtype(PDE))
        .map(c -> c.toFhir(supportingInfoFactory));
  }

  // endregion

  // region Claim IDR Load Date
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.ofNullable(claimIdrLoadDate);
  }
  // endregion
}
