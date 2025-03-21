package gov.cms.bfd.server.war.r4.providers.pac.common;

import gov.cms.bfd.server.war.r4.providers.pac.AbstractR4ResourceProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Interface to allow for more generic logic in {@link AbstractR4ResourceProvider}.
 *
 * @param <TResource> The base resource that the given type configuration is for.
 * @param <TEntity> the JPA entity class of objects used to generate a resource.
 */
public interface ResourceTypeV2<TResource extends IBaseResource, TEntity> {

  /**
   * Gets a label for the resource type that uniquely identifies it.
   *
   * @return the label
   */
  String getTypeLabel();

  /**
   * Gets the JPA {@link Entity} {@link Class} used to store instances of this {@link
   * ResourceTypeV2} in the database.
   *
   * @return the entity class
   */
  Class<TEntity> getEntityClass();

  /**
   * Gets the JPA {@link Entity} field used as the entity's {@link Id}.
   *
   * @return the entity id attribute
   */
  String getEntityIdAttribute();

  /**
   * Gets the attribute name for the entity's mbiRecord attribute.
   *
   * @return the mbi record attribute
   */
  String getEntityMbiRecordAttribute();

  /**
   * List of attribute names for the entity's service start and/or end dates.
   *
   * @return the list of attribute names
   */
  List<String> getEntityServiceDateAttributes();

  /**
   * List of tag Class for the entity.
   *
   * @return the list of attribute names
   */
  String getEntityTagType();
}
