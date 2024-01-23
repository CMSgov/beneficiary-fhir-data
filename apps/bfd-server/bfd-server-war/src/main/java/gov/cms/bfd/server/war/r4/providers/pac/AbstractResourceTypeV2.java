package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Abstract base class for implementations of {@link ResourceTypeV2} that provides functionality
 * common to all implementations.
 *
 * @param <TResource> the {@link IBaseResource} type
 * @param <TEntity> the JPA entity type
 */
@Getter
@AllArgsConstructor
public abstract class AbstractResourceTypeV2<TResource extends IBaseResource, TEntity>
    implements ResourceTypeV2<TResource, TEntity> {
  /** Name used when parsing parameter string to find appropriate instance. */
  protected final String nameForParsing;

  /** Value returned by {@link ResourceTypeV2#getTypeLabel()}. */
  protected final String typeLabel;

  /** The JPA entity class. */
  protected final Class<TEntity> entityClass;

  /** The attribute holding the MBI in the entity class. */
  protected final String entityMbiRecordAttribute;

  /** The attribute holding the claim ID in the entity class. */
  protected final String entityIdAttribute;

  /** The attribute holding the end date for range queries in the entity class. */
  protected final List<String> entityServiceDateAttributes;

  /**
   * Scans the provided instances to find the first one whose {@link
   * AbstractResourceTypeV2#nameForParsing}* is equal to the provided string.
   *
   * @param <TResource> the type parameter for the resource
   * @param <TSubclass> the type parameter for the resource subclass
   * @param claimTypeText the lower-cased {@link ClaimResponseTypeV2#nameForParsing} value to parse
   *     search for
   * @param values The specific instances to search
   * @return the {@link AbstractResourceTypeV2} represented by the specified {@link String}
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
