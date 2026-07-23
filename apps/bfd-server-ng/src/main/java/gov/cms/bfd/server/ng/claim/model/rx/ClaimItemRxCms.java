package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineRxNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;

/** Rx Claim Item for CMS profile. */
@Getter
@Embeddable
public class ClaimItemRxCms implements ClaimItemBase {
  @Embedded private ClaimLineRxCms claimLine;
  @Embedded private ClaimLineRxNumber claimLineRxNum;

  @Override
  public Optional<ClaimProcedureBase> getProcedure() {
    return Optional.empty();
  }

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.empty();
  }

  @Override
  public ClaimItemId getClaimItemId() {
    return new ClaimItemId();
  }
}
