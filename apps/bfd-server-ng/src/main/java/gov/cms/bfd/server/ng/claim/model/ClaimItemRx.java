package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;

/** Claim item table. */
@Getter
@Embeddable
public class ClaimItemRx implements ClaimItemBase {
  @Embedded private ClaimLineRxInfo claimLine;
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
