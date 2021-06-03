package gov.cms.bfd.server.war.r4.providers.preadj.common;

import gov.cms.bfd.server.war.r4.providers.preadj.ClaimResponseTypeV2;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hl7.fhir.r4.model.ClaimResponse;

/** Interface to allow for easier mocking during testing. */
public interface IClaimResponseTypeV2 {

  /**
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link
   *     ClaimResponseTypeV2} in the database
   */
  Class<?> getEntityClass();

  /** @return the JPA {@link Entity} field used as the entity's {@link Id} */
  String getEntityIdAttribute();

  /**
   * @return the {@link ClaimResponseTypeTransformerV2} to use to transform the JPA {@link Entity}
   *     instances into FHIR {@link ClaimResponse} instances
   */
  ClaimResponseTypeTransformerV2 getTransformer();
}
