package gov.cms.bfd.server.ng.claim.model;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Common interface for all claim item types. */
public interface ClaimItemBase extends Comparable<ClaimItemBase> {

  /**
   * Returns the unique identifier for this claim item.
   *
   * @return the claim item identifier
   */
  ClaimItemId getClaimItemId();

  /**
   * Returns the procedure associated with this claim item, if present.
   *
   * @return the claim procedure,
   */
  Optional<ClaimProcedureBase> getProcedure();

  /**
   * Returns the HCPCS code for this claim line, if present.
   *
   * @return the claim line HCPCS code,
   */
  Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode();

  /**
   * Returns the ClaimRelatedCondition for this claim line, if present.
   *
   * @return the ClaimRelatedCondition
   */
  Optional<ClaimRelatedCondition> getClaimRelatedCondition();

  /**
   * Returns claim line professional information.
   *
   * @return the claim line professional info.
   */
  ClaimLineBase getClaimLine();

  @Override
  default int compareTo(@NotNull ClaimItemBase o) {
    return getClaimItemId().compareTo(o.getClaimItemId());
  }
}
