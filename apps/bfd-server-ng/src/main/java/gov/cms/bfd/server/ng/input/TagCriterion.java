package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.model.ClaimFinalAction;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;

/** Parsed criteria from _tag. Expansion requires deliberate addition in TagCriterion. */
public sealed interface TagCriterion
    permits TagCriterion.SourceIdCriterion, TagCriterion.FinalActionCriterion {

  /**
   * Filter claims by source.
   *
   * @param sourceId desired source
   */
  record SourceIdCriterion(MetaSourceSk sourceId) implements TagCriterion {}

  /**
   * Criterion filtering by {@link ClaimFinalAction}.
   *
   * @param finalAction final action enum value
   */
  record FinalActionCriterion(ClaimFinalAction finalAction) implements TagCriterion {}
}
