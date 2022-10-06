package gov.cms.bfd.server.war.r4.providers.pac.common;

/**
 * Implementations of this interface are used to limit which claims are used to generate responses
 * in {@link gov.cms.bfd.server.war.r4.providers.pac.AbstractR4ResourceProvider}.
 */
@FunctionalInterface
public interface ResourceFilter {
  /** Instance that returns false for every entity passed to {@link ResourceFilter#shouldRetain}. */
  ResourceFilter RetainNothing = (resourceTypeV2, entity) -> false;

  /** Instance that returns true every entity passed to {@link ResourceFilter#shouldRetain}. */
  ResourceFilter RetainEverything = (resourceTypeV2, entity) -> true;

  /**
   * Determine if the claim should be included in the result set. The class of the entity object
   * must be the same as that of the {@link ResourceTypeV2#getEntityClass()}.
   *
   * @param resourceType used to interrogate the entity object
   * @param entity the entity object
   * @return true if the claim should be included in results
   */
  boolean shouldRetain(ResourceTypeV2<?, ?> resourceType, Object entity);
}
