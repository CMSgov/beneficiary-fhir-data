package gov.cms.bfd.server.war.r4.providers.preadj;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTransformer;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * Enumerates the various Beneficiary FHIR Data Server (BFD) claim types that are supported by
 * {@link R4ClaimResponseResourceProvider}.
 */
public enum ClaimResponseTypeV2 implements ResourceTypeV2<ClaimResponse> {
  F(PreAdjFissClaim.class, PreAdjFissClaim.Fields.dcn, FissClaimResponseTransformerV2::transform);

  // TODO: [DCGEO-88, DCGEO-98] Complete null fields when entity available
  // M(null, null, McsClaimResponseTransformerV2::transform);

  private final Class<?> entityClass;
  private final String entityIdAttribute;
  private final ResourceTransformer<ClaimResponse> transformer;

  /**
   * Enum constant constructor.
   *
   * @param entityClass the value to use for {@link #getEntityClass()}
   * @param entityIdAttribute the value to use for {@link #getEntityIdAttribute()}
   * @param transformer the value to use for {@link #getTransformer()}
   */
  ClaimResponseTypeV2(
      Class<?> entityClass,
      String entityIdAttribute,
      ResourceTransformer<ClaimResponse> transformer) {
    this.entityClass = entityClass;
    this.entityIdAttribute = entityIdAttribute;
    this.transformer = transformer;
  }

  /**
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link
   *     ClaimResponseTypeV2} in the database
   */
  public Class<?> getEntityClass() {
    return entityClass;
  }

  /** @return the JPA {@link Entity} field used as the entity's {@link Id} */
  public String getEntityIdAttribute() {
    return entityIdAttribute;
  }

  /**
   * @return the {@link ResourceTransformer} to use to transform the JPA {@link Entity} instances
   *     into FHIR {@link ClaimResponse} instances
   */
  public ResourceTransformer<ClaimResponse> getTransformer() {
    return transformer;
  }

  /**
   * @param claimTypeText the lower-cased {@link ClaimResponseTypeV2#name()} value to parse back
   *     into a {@link ClaimResponseTypeV2}
   * @return the {@link ClaimResponseTypeV2} represented by the specified {@link String}
   */
  public static Optional<ResourceTypeV2<ClaimResponse>> parse(String claimTypeText) {
    for (ClaimResponseTypeV2 claimType : ClaimResponseTypeV2.values())
      if (claimType.name().toLowerCase().equals(claimTypeText)) return Optional.of(claimType);
    return Optional.empty();
  }
}
