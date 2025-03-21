package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.samhsa.FissTag;
import gov.cms.bfd.model.rda.samhsa.McsTag;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ClaimResponse;

/**
 * Defines the various Beneficiary FHIR Data Server (BFD) claim types that are supported by {@link
 * R4ClaimResponseResourceProvider}.
 *
 * @param <TEntity> The entity class for this resource type.
 */
public final class ClaimResponseTypeV2<TEntity>
    extends AbstractResourceTypeV2<ClaimResponse, TEntity> {
  /** Instance for FISS claims. */
  public static final ClaimResponseTypeV2<RdaFissClaim> F =
      new ClaimResponseTypeV2<>(
          "F",
          "fiss",
          RdaFissClaim.class,
          RdaFissClaim.Fields.mbiRecord,
          RdaFissClaim.Fields.claimId,
          List.of(RdaFissClaim.Fields.stmtCovFromDate, RdaFissClaim.Fields.stmtCovToDate),
          FissTag.class.getName());

  /** Instance for MCS claims. */
  public static final ClaimResponseTypeV2<RdaMcsClaim> M =
      new ClaimResponseTypeV2<>(
          "M",
          "mcs",
          RdaMcsClaim.class,
          RdaMcsClaim.Fields.mbiRecord,
          RdaMcsClaim.Fields.idrClmHdIcn,
          List.of(RdaMcsClaim.Fields.idrHdrFromDateOfSvc, RdaMcsClaim.Fields.idrHdrToDateOfSvc),
          McsTag.class.getName());

  /** Immutable list of all possible instances of this class. */
  private static final List<ClaimResponseTypeV2<?>> VALUES = List.of(F, M);

  /**
   * Constructor is private to ensure only instances defined in this class are allowed.
   *
   * @param nameForParsing name used when parsing parameter string to find appropriate instance
   * @param typeLabel value returned by {@link ResourceTypeV2#getTypeLabel()}
   * @param entityClass the entity class for the associated resource
   * @param entityMbiRecordAttribute the attribute name for the mbi value on the entity class
   * @param entityIdAttribute the attribute name for the ID of the entity class
   * @param entityServiceDateAttributes the attribute name for the service end date on the entity
   * @param entityTagType the entity security tag type class
   */
  private ClaimResponseTypeV2(
      String nameForParsing,
      String typeLabel,
      Class<TEntity> entityClass,
      String entityMbiRecordAttribute,
      String entityIdAttribute,
      List<String> entityServiceDateAttributes,
      String entityTagType) {
    super(
        nameForParsing,
        typeLabel,
        entityClass,
        entityMbiRecordAttribute,
        entityIdAttribute,
        entityServiceDateAttributes,
        entityTagType);
  }

  /**
   * Returns an immutable list of all possible instances.
   *
   * @return an immutable list of all possible instances.
   */
  public static List<ClaimResponseTypeV2<?>> values() {
    return VALUES;
  }

  /**
   * Scans our instances to find the first one whose {@link AbstractResourceTypeV2#nameForParsing}
   * is equal to the provided string.
   *
   * @param claimTypeText the lower-cased {@link ClaimResponseTypeV2#nameForParsing} value to parse
   *     back into a {@link ClaimResponseTypeV2}
   * @return the {@link ClaimResponseTypeV2} represented by the specified {@link String}
   */
  public static Optional<ResourceTypeV2<ClaimResponse, ?>> parse(String claimTypeText) {
    return AbstractResourceTypeV2.parse(claimTypeText, VALUES);
  }
}
