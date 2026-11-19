package gov.cms.bfd.server.ng.claim.model.common;

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
   * Returns claim line professional information.
   *
   * @return the claim line professional info.
   */
  ClaimLineBase getClaimLine();

  /**
   * Returns the procedure associated with this claim item, if present. Rx claims do not have
   * procedures.
   *
   * @return the claim procedure,
   */
  default Optional<ProcedureBase> getProcedureOptional() {
    return Optional.empty();
  }

  /**
   * Returns the NDC code associated with this claim item, if present.
   *
   * @return the claim line NDC code,
   */
  default Optional<ClaimLineNdc> getClaimLineNdc() {
    return Optional.empty();
  }

  @Override
  default int compareTo(@NotNull ClaimItemBase o) {
    return getClaimItemId().compareTo(o.getClaimItemId());
  }
}
