package gov.cms.bfd.server.war.r4.providers.preadj;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTransformer;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * Enumerates the various Beneficiary FHIR Data Server (BFD) claim types that are supported by
 * {@link R4ClaimResponseResourceProvider}.
 */
public enum ClaimTypeV2 implements ResourceTypeV2<Claim> {
  F(
      PreAdjFissClaim.class,
      PreAdjFissClaim.Fields.mbi,
      PreAdjFissClaim.Fields.mbiHash,
      PreAdjFissClaim.Fields.dcn,
      PreAdjFissClaim.Fields.stmtCovFromDate,
      PreAdjFissClaim.Fields.stmtCovToDate,
      FissClaimTransformerV2::transform),

  M(
      PreAdjMcsClaim.class,
      PreAdjMcsClaim.Fields.idrClaimMbi,
      PreAdjMcsClaim.Fields.idrClaimMbiHash,
      PreAdjMcsClaim.Fields.idrClmHdIcn,
      PreAdjMcsClaim.Fields.idrHdrFromDateOfSvc,
      PreAdjMcsClaim.Fields.idrHdrToDateOfSvc,
      McsClaimTransformerV2::transform);

  private final Class<?> entityClass;
  private final String entityMbiAttribute;
  private final String entityMbiHashAttribute;
  private final String entityIdAttribute;
  private final String entityStartDateAttribute;
  private final String entityEndDateAttribute;
  private final ResourceTransformer<Claim> transformer;

  /**
   * Enum constant constructor.
   *
   * @param entityClass the value to use for {@link #getEntityClass()}
   * @param entityMbiAttribute the value to use for {@link #getEntityMbiAttribute()}
   * @param entityMbiHashAttribute the value to use for {@link #getEntityMbiHashAttribute()}
   * @param entityIdAttribute the value to use for {@link #getEntityIdAttribute()}
   * @param transformer the value to use for {@link #getTransformer()}
   */
  ClaimTypeV2(
      Class<?> entityClass,
      String entityMbiAttribute,
      String entityMbiHashAttribute,
      String entityIdAttribute,
      String entityStartDateAttribute,
      String entityEndDateAttribute,
      ResourceTransformer<Claim> transformer) {
    this.entityClass = entityClass;
    this.entityMbiAttribute = entityMbiAttribute;
    this.entityMbiHashAttribute = entityMbiHashAttribute;
    this.entityIdAttribute = entityIdAttribute;
    this.entityStartDateAttribute = entityStartDateAttribute;
    this.entityEndDateAttribute = entityEndDateAttribute;
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

  /** @return The attribute name for the entity's mbi attribute. */
  public String getEntityMbiAttribute() {
    return entityMbiAttribute;
  }

  /** @return The attribute name for the entity's mbi hash attribute. */
  public String getEntityMbiHashAttribute() {
    return entityMbiHashAttribute;
  }

  public String getEntityStartDateAttribute() {
    return entityStartDateAttribute;
  }

  public String getEntityEndDateAttribute() {
    return entityEndDateAttribute;
  }

  /**
   * @return the {@link ResourceTransformer} to use to transform the JPA {@link Entity} instances
   *     into FHIR {@link ClaimResponse} instances
   */
  public ResourceTransformer<Claim> getTransformer() {
    return transformer;
  }

  /**
   * @param claimTypeText the lower-cased {@link ClaimTypeV2#name()} value to parse back into a
   *     {@link ClaimTypeV2}
   * @return the {@link ClaimTypeV2} represented by the specified {@link String}
   */
  public static Optional<ResourceTypeV2<Claim>> parse(String claimTypeText) {
    for (ClaimTypeV2 claimType : ClaimTypeV2.values())
      if (claimType.name().toLowerCase().equals(claimTypeText)) return Optional.of(claimType);
    return Optional.empty();
  }
}
