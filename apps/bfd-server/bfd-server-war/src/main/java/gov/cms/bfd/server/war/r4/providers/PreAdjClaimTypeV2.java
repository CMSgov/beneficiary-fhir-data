package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.server.war.commons.PreAdjClaimTypeTransformerV2;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * Enumerates the various Beneficiary FHIR Data Server (BFD) claim types that are supported by
 * {@link R4ClaimResponseResourceProvider}.
 */
public enum PreAdjClaimTypeV2 implements IPreAdjClaimTypeV2 {

  // TODO: Complete null fields when entity available
  F(PreAdjFissClaim.class, PreAdjFissClaim.Fields.dcn, PreAdjFissClaimTransformerV2::transform),

  M(null, null, PreAdjMcsClaimTransformerV2::transform);

  private final Class<?> entityClass;
  private final String entityIdAttribute;
  private final PreAdjClaimTypeTransformerV2 transformer;

  /**
   * Enum constant constructor.
   *
   * @param entityClass the value to use for {@link #getEntityClass()}
   * @param entityIdAttribute the value to use for {@link #getEntityIdAttribute()}
   * @param transformer the value to use for {@link #getTransformer()}
   */
  PreAdjClaimTypeV2(
      Class<?> entityClass, String entityIdAttribute, PreAdjClaimTypeTransformerV2 transformer) {
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
   * @return the {@link PreAdjClaimTypeTransformerV2} to use to transform the JPA {@link Entity}
   *     instances into FHIR {@link ClaimResponse} instances
   */
  public PreAdjClaimTypeTransformerV2 getTransformer() {
    return transformer;
  }

  /**
   * @param claimTypeText the lower-cased {@link PreAdjClaimTypeV2#name()} value to parse back into
   *     a {@link PreAdjClaimTypeV2}
   * @return the {@link PreAdjClaimTypeV2} represented by the specified {@link String}
   */
  public static Optional<IPreAdjClaimTypeV2> parse(String claimTypeText) {
    for (PreAdjClaimTypeV2 claimType : PreAdjClaimTypeV2.values())
      if (claimType.name().toLowerCase().equals(claimTypeText)) return Optional.of(claimType);
    return Optional.empty();
  }
}
