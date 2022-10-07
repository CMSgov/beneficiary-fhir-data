package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.model.rda.EntityWithPhaseNumber;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceFilter;
import gov.cms.bfd.sharedutils.config.SemanticVersion;
import gov.cms.bfd.sharedutils.config.SemanticVersionRange;
import lombok.AllArgsConstructor;

/**
 * Restricts results to only claims having phase versions within a specific {@link
 * SemanticVersionRange}.
 */
@AllArgsConstructor
public class PhaseResourceFilter implements ResourceFilter {
  /** The range of semantic version numbers that are allowed to be returned to clients. */
  private final SemanticVersionRange allowedRange;

  /**
   * {@inheritDoc}
   *
   * <p>This implementation only filters entities that implement the {@link EntityWithPhaseNumber}
   * interface. It constructs a {@link SemanticVersion} from the claim's phase numbers and ensures
   * that the version falls within the range allowed by this filter. Any entity that does not
   * implement the interface is retained.
   *
   * @param entity the entity object
   * @return true if the claim should be returned to clients
   */
  @Override
  public boolean shouldRetain(Object entity) {
    var answer = true;
    if (entity instanceof EntityWithPhaseNumber) {
      final var phaseEntity = (EntityWithPhaseNumber) entity;
      final var phaseNum = phaseEntity.getPhase();
      final var phaseSeqNum = phaseEntity.getPhaseSeqNum();
      final var phaseVersionNumber =
          SemanticVersion.fromComponents(
              phaseNum != null ? phaseNum : 0, phaseSeqNum != null ? phaseSeqNum : 0, 0);
      answer = allowedRange.contains(phaseVersionNumber.orElse(SemanticVersion.ZERO));
    }
    return answer;
  }
}
