package gov.cms.bfd.server.war.r4.providers.preadj;

import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaMcsClaim;
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
      RdaFissClaim.class,
      RdaFissClaim.Fields.mbiRecord,
      RdaFissClaim.Fields.dcn,
      RdaFissClaim.Fields.stmtCovToDate,
      FissClaimTransformerV2::transform),

  M(
      RdaMcsClaim.class,
      RdaMcsClaim.Fields.mbiRecord,
      RdaMcsClaim.Fields.idrClmHdIcn,
      RdaMcsClaim.Fields.idrHdrToDateOfSvc,
      McsClaimTransformerV2::transform);

  private final Class<?> entityClass;
  private final String entityMbiRecordAttribute;
  private final String entityIdAttribute;
  private final String entityEndDateAttribute;
  private final ResourceTransformer<Claim> transformer;

  /**
   * Enum constant constructor.
   *
   * @param entityClass the entity class for the associated resource
   * @param entityMbiAttribute the attribute name for the mbi value on the entity class
   * @param entityMbiHashAttribute the attribute name for the mbiHash value on the entity class
   * @param entityIdAttribute the attribute name for the ID of the entity class
   * @param entityEndDateAttribute the attribute name for the service end date on the entity class
   * @param transformer the transformer used to convert from the given entity to the associated
   *     resource type
   */
  ClaimTypeV2(
      Class<?> entityClass,
      String entityMbiRecordAttribute,
      String entityIdAttribute,
      String entityEndDateAttribute,
      ResourceTransformer<Claim> transformer) {
    this.entityClass = entityClass;
    this.entityMbiRecordAttribute = entityMbiRecordAttribute;
    this.entityIdAttribute = entityIdAttribute;
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
  public String getEntityMbiRecordAttribute() {
    return entityMbiRecordAttribute;
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
