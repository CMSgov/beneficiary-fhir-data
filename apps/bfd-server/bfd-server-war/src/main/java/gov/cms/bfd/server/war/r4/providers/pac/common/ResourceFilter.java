package gov.cms.bfd.server.war.r4.providers.pac.common;

import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Implementations of this interface are used to limit which claims are used to generate responses
 * in {@link gov.cms.bfd.server.war.r4.providers.pac.AbstractR4ResourceProvider}.
 *
 * @param <T> The specific fhir resource the concrete provider will serve.
 */
@FunctionalInterface
public interface ResourceFilter<T extends IBaseResource> {
  /**
   * Determine if the claim should be included in the result set. The class of the entity object
   * must be the same as that of the {@link ResourceTypeV2#getEntityClass()}.
   *
   * @param resourceType used to interrogate the entity object
   * @param entity the entity object
   * @return true if the claim should be included in results
   */
  boolean shouldRetain(ResourceTypeV2<T, ?> resourceType, Object entity);
}
