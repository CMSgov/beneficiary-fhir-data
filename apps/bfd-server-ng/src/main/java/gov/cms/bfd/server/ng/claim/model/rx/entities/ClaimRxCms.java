package gov.cms.bfd.server.ng.claim.model.rx.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcessDate;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionFormatCode;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.rx.AdjudicationRx;
import gov.cms.bfd.server.ng.claim.model.rx.ClaimItemRxCms;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * The CMS profile of a Rx Claim. Resource - ExplanationOfBenefit Varied Domain - [ Rx ] Profile - [
 * CMS ] Source - [ Rx ]
 */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
public class ClaimRxCms extends ClaimRxBase {

  @Embedded private AdjudicationRx adjudicationCharge;
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;
  @Embedded private ClaimProcessDate claimProcessDate;
  @Embedded private ClaimItemRxCms claimItem;

  @Override
  protected Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.of(adjudicationCharge);
  }

  @Override
  protected Optional<ClaimProcessDate> getClaimProcessDate() {
    return Optional.of(claimProcessDate);
  }

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimSubmissionFormatCode;

  @Override
  protected Optional<ExplanationOfBenefit.SupportingInformationComponent>
      getSubmissionFormatSupportingInfo() {
    return claimSubmissionFormatCode
        .filter(_ -> getClaimTypeCode().isClaimSubtype(PDE))
        .map(c -> c.toFhir(supportingInfoFactory));
  }

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.DDPS;
  }

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.of(claimIdrLoadDate);
  }

  @Override
  protected ClaimItemBase getClaimItem() {
    return claimItem;
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent> getLineSupportingInfo() {
    return Stream.concat(
            claimItem.claimRxSupportingInfoToFhir(supportingInfoFactory).stream(),
            claimItem.getClaimLineRxNum().toFhir(supportingInfoFactory).stream())
        .toList();
  }

  @Override
  protected Optional<BigDecimal> getTotalDrugCostAmount() {
    return Optional.of(claimItem.getTotalDrugCost());
  }
}
