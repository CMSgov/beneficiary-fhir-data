package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineRxNumber;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Rx Claim Item for CMS profile. */
@Getter
@Embeddable
public class ClaimItemRxCms implements ClaimItemBase {
  @Embedded private ClaimLineRxCms claimLine;
  @Embedded private ClaimLineRxNumber claimLineRxNum;

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.empty();
  }

  @Override
  public ClaimItemId getClaimItemId() {
    return new ClaimItemId();
  }

  // region Delegate Methods

  /**
   * Get total drug cost from the claim line's adjudication charge.
   *
   * @return total drug cost from contained adjudication charge
   */
  public BigDecimal getTotalDrugCost() {
    return claimLine.getAdjudicationCharge().getTotalDrugCost();
  }

  /**
   * Get the list of supporting information components from the claim line, pass back up to claim.
   *
   * @param factory supporting info factory
   * @return the list of supporting info components
   */
  public List<ExplanationOfBenefit.SupportingInformationComponent> claimRxSupportingInfoToFhir(
      SupportingInfoFactory factory) {
    return claimLine.toFhirSupportingInfo(factory);
  }

  // endregion
}
