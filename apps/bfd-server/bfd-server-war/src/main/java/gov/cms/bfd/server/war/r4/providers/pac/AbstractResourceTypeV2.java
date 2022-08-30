package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.util.Collection;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Abstract base class for implementations of {@link ResourceTypeV2} that provides functionality
 * common to all implementations.
 *
 * @param <TResource> the {@link IBaseResource} type
 * @param <TEntity> the JPA entity type
 */
public abstract class AbstractResourceTypeV2<TResource extends IBaseResource, TEntity>
    implements ResourceTypeV2<TResource, TEntity> {
  /** Name used when parsing parameter string to find appropriate instance. */
  protected final String nameForParsing;
  /** Value returned by {@link ResourceTypeV2#getNameForMetrics} */
  protected final String nameForMetrics;
  /** The JPA entity class. */
  protected final Class<TEntity> entityClass;
  /** The attribute holding the MBI in the entity class. */
  protected final String entityMbiRecordAttribute;
  /** The attribute holding the claim ID in the entity class. */
  protected final String entityIdAttribute;
  /** The attribute holding the end date for range queries in the entity class. */
  protected final String entityEndDateAttribute;
  /** The {@link ResourceTransformer} to convert an entity into a response object. */
  protected final ResourceTransformer<TResource> transformer;
  /** When populated this attribute defines values needed to produce a service date sub-query. */
  protected final Optional<ServiceDateSubquerySpec> serviceDateSubquerySpec;

  /**
   * Constructor intended for use by derived classes to set values in common fields.
   *
   * @param nameForParsing value for {@link ResourceTypeV2#getNameForMetrics()}
   * @param nameForMetrics value returned by {@link ResourceTypeV2#getNameForMetrics}
   * @param entityClass the entity class for the associated resource
   * @param entityMbiRecordAttribute the attribute name for the mbi value on the entity class
   * @param entityIdAttribute the attribute name for the ID of the entity class
   * @param entityEndDateAttribute the attribute name for the service end date on the entity class
   * @param transformer the transformer used to convert from the given entity to the associated
   *     resource type
   * @param serviceDateSubquerySpec value for {@link ResourceTypeV2#getServiceDateSubquerySpec()}
   */
  protected AbstractResourceTypeV2(
      String nameForParsing,
      String nameForMetrics,
      Class<TEntity> entityClass,
      String entityMbiRecordAttribute,
      String entityIdAttribute,
      String entityEndDateAttribute,
      ResourceTransformer<TResource> transformer,
      Optional<ServiceDateSubquerySpec> serviceDateSubquerySpec) {
    this.nameForParsing = nameForParsing;
    this.nameForMetrics = nameForMetrics;
    this.entityClass = entityClass;
    this.entityMbiRecordAttribute = entityMbiRecordAttribute;
    this.entityIdAttribute = entityIdAttribute;
    this.entityEndDateAttribute = entityEndDateAttribute;
    this.transformer = transformer;
    this.serviceDateSubquerySpec = serviceDateSubquerySpec;
  }

  @Override
  public String getNameForMetrics() {
    return nameForMetrics;
  }

  @Override
  public Class<TEntity> getEntityClass() {
    return entityClass;
  }

  @Override
  public String getEntityIdAttribute() {
    return entityIdAttribute;
  }

  @Override
  public String getEntityMbiRecordAttribute() {
    return entityMbiRecordAttribute;
  }

  @Override
  public String getEntityEndDateAttribute() {
    return entityEndDateAttribute;
  }

  @Override
  public ResourceTransformer<TResource> getTransformer() {
    return transformer;
  }

  @Override
  public Optional<ServiceDateSubquerySpec> getServiceDateSubquerySpec() {
    return serviceDateSubquerySpec;
  }

  /**
   * Scans the provided instances to find the first one whose {@link
   * AbstractResourceTypeV2#nameForParsing} is equal to the provided string.
   *
   * @param claimTypeText the lower-cased {@link ClaimResponseTypeV2#nameForParsing} value to parse
   *     search for
   * @param values The specific instances to search
   * @return the {@link AbstractResourceTypeV2} instance represented by the specified {@link String}
   * @param <TResource> the {@link IBaseResource} type of the returned {@link
   *     AbstractResourceTypeV2}
   * @param <TSubclass> the subclass extending {@link AbstractResourceTypeV2} being returned
   */
  public static <
          TResource extends IBaseResource, TSubclass extends AbstractResourceTypeV2<TResource, ?>>
      Optional<ResourceTypeV2<TResource, ?>> parse(
          String claimTypeText, Collection<TSubclass> values) {
    for (TSubclass claimType : values)
      if (claimType.nameForParsing.toLowerCase().equals(claimTypeText))
        return Optional.of(claimType);
    return Optional.empty();
  }
}
