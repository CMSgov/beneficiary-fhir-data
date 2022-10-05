package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Claim;

/**
 * Defines the various Beneficiary FHIR Data Server (BFD) claim types that are supported by {@link
 * R4ClaimResponseResourceProvider}.
 *
 * @param <TEntity> The entity class for this resource type.
 */
public final class ClaimTypeV2<TEntity> extends AbstractResourceTypeV2<Claim, TEntity> {
  /** Instance for FISS claims. */
  public static final ClaimTypeV2<RdaFissClaim> F =
      new ClaimTypeV2<>(
          "F",
          "fiss",
          RdaFissClaim.class,
          RdaFissClaim.Fields.mbiRecord,
          RdaFissClaim.Fields.dcn,
          List.of(RdaFissClaim.Fields.stmtCovFromDate, RdaFissClaim.Fields.stmtCovToDate),
          FissClaimTransformerV2::transform);

  /** Instance for MCS claims. */
  public static final ClaimTypeV2<RdaMcsClaim> M =
      new ClaimTypeV2<>(
          "M",
          "mcs",
          RdaMcsClaim.class,
          RdaMcsClaim.Fields.mbiRecord,
          RdaMcsClaim.Fields.idrClmHdIcn,
          List.of(RdaMcsClaim.Fields.idrHdrToDateOfSvc),
          McsClaimTransformerV2::transform);

  /** Immutable list of all possible instances of this class. */
  private static final List<ClaimTypeV2<?>> VALUES = List.of(F, M);

  /**
   * Constructor is private to ensure only instances defined in this class are allowed.
   *
   * @param nameForParsing name used when parsing parameter string to find appropriate instance
   * @param nameForMetrics value returned by {@link ResourceTypeV2#getNameForMetrics}
   * @param entityClass the entity class for the associated resource
   * @param entityMbiRecordAttribute the attribute name for the mbi value on the entity class
   * @param entityIdAttribute the attribute name for the ID of the entity class
   * @param entityServiceDateAttributes the attribute name for the service end date on the entity
   *     class
   * @param transformer the transformer used to convert from the given entity to the associated
   *     resource type
   */
  private ClaimTypeV2(
      String nameForParsing,
      String nameForMetrics,
      Class<TEntity> entityClass,
      String entityMbiRecordAttribute,
      String entityIdAttribute,
      List<String> entityServiceDateAttributes,
      ResourceTransformer<Claim> transformer) {
    super(
        nameForParsing,
        nameForMetrics,
        entityClass,
        entityMbiRecordAttribute,
        entityIdAttribute,
        entityServiceDateAttributes,
        transformer);
  }

  /**
   * Returns an immutable list of all possible instances of this class.
   *
   * @return an immutable list of all possible instances of this class.
   */
  public static List<ClaimTypeV2<?>> values() {
    return VALUES;
  }

  /**
   * Scans our instances to find the first one whose {@link AbstractResourceTypeV2#nameForParsing}
   * is equal to the provided string.
   *
   * @param claimTypeText the lower-cased {@link ClaimTypeV2#nameForParsing} value to parse back
   *     into a {@link ClaimTypeV2}
   * @return the {@link ClaimTypeV2} represented by the specified {@link String}
   */
  public static Optional<ResourceTypeV2<Claim, ?>> parse(String claimTypeText) {
    return AbstractResourceTypeV2.parse(claimTypeText, VALUES);
  }
}
