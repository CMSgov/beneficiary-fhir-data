package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.server.war.commons.PreAdjClaimTypeTransformerV2;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hl7.fhir.r4.model.ClaimResponse;

/** Interface to allow for easier mocking during testing. */
public interface IPreAdjClaimTypeV2 {

  /**
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link
   *     PreAdjClaimResponseTypeV2} in the database
   */
  Class<?> getEntityClass();

  /** @return the JPA {@link Entity} field used as the entity's {@link Id} */
  String getEntityIdAttribute();

  /**
   * @return the {@link PreAdjClaimTypeTransformerV2} to use to transform the JPA {@link Entity}
   *     instances into FHIR {@link ClaimResponse} instances
   */
  PreAdjClaimTypeTransformerV2 getTransformer();
}
