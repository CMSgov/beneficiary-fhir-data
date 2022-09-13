package gov.cms.bfd.server.war.r4.providers.pac.common;

import gov.cms.bfd.server.war.r4.providers.pac.AbstractR4ResourceProvider;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Interface to allow for more generic logic in {@link AbstractR4ResourceProvider}.
 *
 * @param <TResource> The base resource that the given type configuration is for.
 * @param <TEntity> the JPA entity class of objects used to generate a resource.
 */
public interface ResourceTypeV2<TResource extends IBaseResource, TEntity> {

  /** @return a name for use when building drop wizard metric names */
  String getNameForMetrics();

  /**
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link
   *     ResourceTypeV2} in the database
   */
  Class<TEntity> getEntityClass();

  /** @return the JPA {@link Entity} field used as the entity's {@link Id} */
  String getEntityIdAttribute();

  /** @return The attribute name for the entity's mbiRecord attribute. */
  String getEntityMbiRecordAttribute();

  /** @return The attribute name for the entity's service end date attribute. */
  String getEntityEndDateAttribute();

  /**
   * @return the {@link ServiceDateSubquerySpec} if one is defined for this resource or an empty
   *     {@link Optional} if none is needed.
   */
  Optional<ServiceDateSubquerySpec> getServiceDateSubquerySpec();

  /**
   * @return the {@link ResourceTransformer} to use to transform the JPA {@link Entity} instances
   *     into FHIR instances
   */
  ResourceTransformer<TResource> getTransformer();

  /**
   * Defines parameters used to build a service date sub-query that uses the maximum date some
   * detail records of the claim entity. Used to allow MCS claims to use the max service date from
   * its detail records as an alternative (or in addition to) the service date in the claim record
   * itself.
   */
  @Getter
  @AllArgsConstructor
  class ServiceDateSubquerySpec {
    /**
     * Name of the attribute in the parent entity that contains a join to get instances of the
     * detail entity.
     */
    private final String detailJoinAttribute;
    /** Entity class for the detail records. */
    private final Class<?> detailEntityClass;
    /** Name of the attribute in the detail record that contains the claim id. */
    private final String claimIdAttribute;
    /** Name of the attribute in the detail record that contains the service date. */
    private final String dateAttribute;
  }
}
