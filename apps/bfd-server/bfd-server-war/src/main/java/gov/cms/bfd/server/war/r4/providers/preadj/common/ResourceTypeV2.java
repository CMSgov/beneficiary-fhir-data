package gov.cms.bfd.server.war.r4.providers.preadj.common;

import gov.cms.bfd.server.war.r4.providers.preadj.AbstractR4ResourceProvider;
import gov.cms.bfd.server.war.r4.providers.preadj.ClaimResponseTypeV2;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * Interface to allow for more generic logic in {@link AbstractR4ResourceProvider}.
 *
 * @param <T> The base resource that the given type configuration is for.
 */
public interface ResourceTypeV2<T extends IBaseResource> {

  /**
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link
   *     ClaimResponseTypeV2} in the database
   */
  Class<?> getEntityClass();

  /** @return the JPA {@link Entity} field used as the entity's {@link Id} */
  String getEntityIdAttribute();

  /** @return The attribute name for the entity's mbi attribute. */
  String getEntityMbiAttribute();

  /** @return The attribute name for the entity's mbi hash attribute. */
  String getEntityMbiHashAttribute();

  /** @return The attribute name for the entity's service start date attribute. */
  String getEntityStartDateAttribute();

  /** @return The attribute name for the entity's service end date attribute. */
  String getEntityEndDateAttribute();

  /**
   * @return the {@link ResourceTransformer} to use to transform the JPA {@link Entity} instances
   *     into FHIR {@link ClaimResponse} instances
   */
  ResourceTransformer<T> getTransformer();
}
