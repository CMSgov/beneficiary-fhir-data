package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceFilter;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
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
   * <p>This implementation treats the claim's {@link ResourceTypeV2#createPhaseVersionString} value
   * as a semantic version number and ensures that the number falls within the range allowed by this
   * filter.
   *
   * @param resourceType used to interrogate the entity object
   * @param entity the entity object
   * @return true if the claim should be returned to clients
   */
  @Override
  public boolean shouldRetain(ResourceTypeV2<?, ?> resourceType, Object entity) {
    final var phaseVersionString = resourceType.createPhaseVersionString(entity);
    final var phaseVersionNumber = SemanticVersion.parse(phaseVersionString);
    return allowedRange.contains(phaseVersionNumber.orElse(SemanticVersion.ZERO));
  }
}
