package gov.cms.bfd.server.war.r4.providers.pac.common;

/**
 * Implementations of this interface are used to limit which claims are used to generate responses
 * in {@link gov.cms.bfd.server.war.r4.providers.pac.AbstractR4ResourceProvider}.
 */
@FunctionalInterface
public interface ResourceFilter {
  /** Instance that returns false for every entity passed to {@link ResourceFilter#shouldRetain}. */
  ResourceFilter RetainNothing = (entity) -> false;

  /** Instance that returns true for every entity passed to {@link ResourceFilter#shouldRetain}. */
  ResourceFilter RetainEverything = (entity) -> true;

  /**
   * Determine if the claim should be included in the result set.
   *
   * @param entity the entity object
   * @return true if the claim should be included in results
   */
  boolean shouldRetain(Object entity);
}
