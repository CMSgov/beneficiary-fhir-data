package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.model.rda.RdaMcsDetail;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
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
          RdaFissClaim.Fields.dcn,
          RdaFissClaim.Fields.stmtCovToDate,
          FissClaimResponseTransformerV2::transform,
          Optional.empty());

  /** Instance for MCS claims. */
  public static final ClaimResponseTypeV2<RdaMcsClaim> M =
      new ClaimResponseTypeV2<>(
          "M",
          "mcs",
          RdaMcsClaim.class,
          RdaMcsClaim.Fields.mbiRecord,
          RdaMcsClaim.Fields.idrClmHdIcn,
          RdaMcsClaim.Fields.idrHdrToDateOfSvc,
          McsClaimResponseTransformerV2::transform,
          Optional.of(
              new ServiceDateSubquerySpec(
                  RdaMcsClaim.Fields.details,
                  RdaMcsDetail.class,
                  RdaMcsDetail.Fields.idrDtlToDate,
                  RdaMcsDetail.Fields.idrDtlToDate)));

  /** Immutable list of all possible instances of this class. */
  private static final List<ClaimResponseTypeV2<?>> VALUES = List.of(F, M);

  /**
   * Constructor is private to ensure only instances defined in this class are allowed.
   *
   * @param nameForParsing value for {@link ResourceTypeV2#getNameForMetrics()}
   * @param nameForMetrics value returned by {@link ResourceTypeV2#getNameForMetrics}
   * @param entityClass the entity class for the associated resource
   * @param entityMbiRecordAttribute the attribute name for the mbi value on the entity class
   * @param entityIdAttribute the attribute name for the ID of the entity class
   * @param entityEndDateAttribute the attribute name for the service end date on the entity class
   * @param transformer the transformer used to convert from the given entity to the associated
   *     resource type
   * @param serviceDateSubquerySpec value for {@link ResourceTypeV2#getServiceDateSubquerySpec()}
   */
  private ClaimResponseTypeV2(
      String nameForParsing,
      String nameForMetrics,
      Class<TEntity> entityClass,
      String entityMbiRecordAttribute,
      String entityIdAttribute,
      String entityEndDateAttribute,
      ResourceTransformer<ClaimResponse> transformer,
      Optional<ServiceDateSubquerySpec> serviceDateSubquerySpec) {
    super(
        nameForParsing,
        nameForMetrics,
        entityClass,
        entityMbiRecordAttribute,
        entityIdAttribute,
        entityEndDateAttribute,
        transformer,
        serviceDateSubquerySpec);
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
