package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.server.war.commons.PreAdjClaimResponseTypeTransformerV2;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * Enumerates the various Beneficiary FHIR Data Server (BFD) claim types that are supported by
 * {@link R4ClaimResponseResourceProvider}.
 */
public enum PreAdjClaimResponseTypeV2 implements IPreAdjClaimResponseTypeV2 {

  // TODO: Complete null fields when entity available
  F(
      PreAdjFissClaim.class,
      PreAdjFissClaim.Fields.dcn,
      PreAdjFissClaimResponseTransformerV2::transform),

  M(null, null, PreAdjMcsClaimResponseTransformerV2::transform);

  private final Class<?> entityClass;
  private final String entityIdAttribute;
  private final PreAdjClaimResponseTypeTransformerV2 transformer;

  /**
   * Enum constant constructor.
   *
   * @param entityClass the value to use for {@link #getEntityClass()}
   * @param entityIdAttribute the value to use for {@link #getEntityIdAttribute()}
   * @param transformer the value to use for {@link #getTransformer()}
   */
  PreAdjClaimResponseTypeV2(
      Class<?> entityClass,
      String entityIdAttribute,
      PreAdjClaimResponseTypeTransformerV2 transformer) {
    this.entityClass = entityClass;
    this.entityIdAttribute = entityIdAttribute;
    this.transformer = transformer;
  }

  /**
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link
   *     PreAdjClaimResponseTypeV2} in the database
   */
  public Class<?> getEntityClass() {
    return entityClass;
  }

  /** @return the JPA {@link Entity} field used as the entity's {@link Id} */
  public String getEntityIdAttribute() {
    return entityIdAttribute;
  }

  /**
   * @return the {@link PreAdjClaimResponseTypeTransformerV2} to use to transform the JPA {@link
   *     Entity} instances into FHIR {@link ClaimResponse} instances
   */
  public PreAdjClaimResponseTypeTransformerV2 getTransformer() {
    return transformer;
  }

  /**
   * @param claimTypeText the lower-cased {@link PreAdjClaimResponseTypeV2#name()} value to parse
   *     back into a {@link PreAdjClaimResponseTypeV2}
   * @return the {@link PreAdjClaimResponseTypeV2} represented by the specified {@link String}
   */
  public static Optional<IPreAdjClaimResponseTypeV2> parse(String claimTypeText) {
    for (PreAdjClaimResponseTypeV2 claimType : PreAdjClaimResponseTypeV2.values())
      if (claimType.name().toLowerCase().equals(claimTypeText)) return Optional.of(claimType);
    return Optional.empty();
  }
}
