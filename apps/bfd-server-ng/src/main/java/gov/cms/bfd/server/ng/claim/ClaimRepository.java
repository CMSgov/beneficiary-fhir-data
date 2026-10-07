package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.claim.filter.*;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.institutional.entities.ClaimInstitutionalBasisNch;
import gov.cms.bfd.server.ng.claim.model.institutional.entities.ClaimInstitutionalBasisSharedSystems;
import gov.cms.bfd.server.ng.claim.model.institutional.entities.ClaimInstitutionalCmsNch;
import gov.cms.bfd.server.ng.claim.model.institutional.entities.ClaimInstitutionalCmsSharedSystems;
import gov.cms.bfd.server.ng.claim.model.institutional.entities.ClaimInstitutionalRegularNch;
import gov.cms.bfd.server.ng.claim.model.institutional.entities.ClaimInstitutionalRegularSharedSystems;
import gov.cms.bfd.server.ng.claim.model.priorauth.entities.PriorAuthorization;
import gov.cms.bfd.server.ng.claim.model.professional.entities.ClaimProfessionalBasisNch;
import gov.cms.bfd.server.ng.claim.model.professional.entities.ClaimProfessionalBasisSharedSystems;
import gov.cms.bfd.server.ng.claim.model.professional.entities.ClaimProfessionalCmsNch;
import gov.cms.bfd.server.ng.claim.model.professional.entities.ClaimProfessionalCmsSharedSystems;
import gov.cms.bfd.server.ng.claim.model.professional.entities.ClaimProfessionalRegularNch;
import gov.cms.bfd.server.ng.claim.model.professional.entities.ClaimProfessionalRegularSharedSystems;
import gov.cms.bfd.server.ng.claim.model.rx.entities.ClaimRxBase;
import gov.cms.bfd.server.ng.claim.model.rx.entities.ClaimRxBasis;
import gov.cms.bfd.server.ng.claim.model.rx.entities.ClaimRxCms;
import gov.cms.bfd.server.ng.claim.model.rx.entities.ClaimRxRegular;
import gov.cms.bfd.server.ng.input.ClaimIdSearchCriteria;
import gov.cms.bfd.server.ng.input.ClaimProfile;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.util.MetricRecorder;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimRepository {

  private final ClaimAsyncService asyncService;
  private final MetricRecorder metricRecorder;

  private static String claimQueryString(
      Class<? extends ClaimBase> entityClass, String itemsJoinType) {
    return """
      SELECT c
      FROM %s c
      JOIN FETCH c.beneficiary b
      %s JOIN FETCH c.claimItems cl
    """
        .formatted(entityClass.getSimpleName(), itemsJoinType);
  }

  private static String rxClaimQueryString(Class<? extends ClaimRxBase> entityClass) {
    return """
      SELECT c
      FROM %s c
      JOIN FETCH c.beneficiary b
    """
        .formatted(entityClass.getSimpleName());
  }

  // Shared Systems queries use a LEFT JOIN FETCH on claimItems.
  private static final List<ClaimTypeDefinition> ALL_CLAIM_TYPES =
      List.of(
          // CMS
          new ClaimTypeDefinition(
              claimQueryString(ClaimProfessionalCmsSharedSystems.class, "LEFT"),
              ClaimProfessionalCmsSharedSystems.class,
              SystemType.SS,
              ClaimProfile.CMS),
          new ClaimTypeDefinition(
              claimQueryString(ClaimProfessionalCmsNch.class, ""),
              ClaimProfessionalCmsNch.class,
              SystemType.NCH,
              ClaimProfile.CMS),
          new ClaimTypeDefinition(
              claimQueryString(ClaimInstitutionalCmsSharedSystems.class, "LEFT"),
              ClaimInstitutionalCmsSharedSystems.class,
              SystemType.SS,
              ClaimProfile.CMS),
          new ClaimTypeDefinition(
              claimQueryString(ClaimInstitutionalCmsNch.class, ""),
              ClaimInstitutionalCmsNch.class,
              SystemType.NCH,
              ClaimProfile.CMS),
          new ClaimTypeDefinition(
              rxClaimQueryString(ClaimRxCms.class),
              ClaimRxCms.class,
              SystemType.DDPS,
              ClaimProfile.CMS),

          // REGULAR
          new ClaimTypeDefinition(
              claimQueryString(ClaimProfessionalRegularSharedSystems.class, "LEFT"),
              ClaimProfessionalRegularSharedSystems.class,
              SystemType.SS,
              ClaimProfile.REGULAR),
          new ClaimTypeDefinition(
              claimQueryString(ClaimProfessionalRegularNch.class, ""),
              ClaimProfessionalRegularNch.class,
              SystemType.NCH,
              ClaimProfile.REGULAR),
          new ClaimTypeDefinition(
              claimQueryString(ClaimInstitutionalRegularSharedSystems.class, "LEFT"),
              ClaimInstitutionalRegularSharedSystems.class,
              SystemType.SS,
              ClaimProfile.REGULAR),
          new ClaimTypeDefinition(
              claimQueryString(ClaimInstitutionalRegularNch.class, ""),
              ClaimInstitutionalRegularNch.class,
              SystemType.NCH,
              ClaimProfile.REGULAR),
          new ClaimTypeDefinition(
              rxClaimQueryString(ClaimRxRegular.class),
              ClaimRxRegular.class,
              SystemType.DDPS,
              ClaimProfile.REGULAR),

          // BASIS
          new ClaimTypeDefinition(
              claimQueryString(ClaimProfessionalBasisSharedSystems.class, "LEFT"),
              ClaimProfessionalBasisSharedSystems.class,
              SystemType.SS,
              ClaimProfile.BASIS),
          new ClaimTypeDefinition(
              claimQueryString(ClaimProfessionalBasisNch.class, ""),
              ClaimProfessionalBasisNch.class,
              SystemType.NCH,
              ClaimProfile.BASIS),
          new ClaimTypeDefinition(
              claimQueryString(ClaimInstitutionalBasisSharedSystems.class, "LEFT"),
              ClaimInstitutionalBasisSharedSystems.class,
              SystemType.SS,
              ClaimProfile.BASIS),
          new ClaimTypeDefinition(
              claimQueryString(ClaimInstitutionalBasisNch.class, ""),
              ClaimInstitutionalBasisNch.class,
              SystemType.NCH,
              ClaimProfile.BASIS),
          new ClaimTypeDefinition(
              rxClaimQueryString(ClaimRxBasis.class),
              ClaimRxBasis.class,
              SystemType.DDPS,
              ClaimProfile.BASIS));

  /**
   * Search for a claim by its ID.
   *
   * @param criteria is search criteria
   * @return claim
   */
  @Timed(value = "application.claim.search_by_id")
  public List<ClaimBase> findByIds(
      @MeterTag(key = "hasServiceUpdated", expression = "hasServiceUpdated()")
          @MeterTag(key = "hasLastUpdated", expression = "hasLastUpdated()")
          @MeterTag(key = "hasOutcomes", expression = "hasOutcomes()")
          @MeterTag(key = "hasSources", expression = "hasSources()")
          ClaimIdSearchCriteria criteria) {
    if (criteria.claimUniqueIds() == null || criteria.claimUniqueIds().isEmpty()) {
      return Collections.emptyList();
    }
    var paramBuilders =
        List.of(
            new BillablePeriodFilterParam(criteria.serviceDate()),
            new LastUpdatedFilterParam(criteria.lastUpdated()),
            new OutcomeFilterParam(criteria.outcomes()),
            new SourceFilterParam(criteria.sources()));

    var claimFutures =
        ALL_CLAIM_TYPES.stream()
            .filter(d -> d.matchesProfile(ClaimProfile.CMS))
            .map(
                d ->
                    asyncService.findByIdsInClaimType(
                        d.baseQuery(),
                        d.claimClass(),
                        d.systemType(),
                        criteria.claimUniqueIds(),
                        paramBuilders))
            .toList();

    CompletableFuture.allOf(claimFutures.toArray(new CompletableFuture[0])).join();

    var allClaims = new ArrayList<ClaimBase>();
    claimFutures.forEach(f -> allClaims.addAll(f.join()));

    return allClaims;
  }

  /**
   * Returns claims for the given beneficiary.
   *
   * @param criteria filter criteria
   * @return claims
   */
  @Timed(value = "application.claim.search_by_bene")
  public ClaimAndAuthResult findByBeneXrefSk(
      @MeterTag(key = "hasClaimThroughDate", expression = "hasClaimThroughDate()")
          @MeterTag(key = "hasLastUpdated", expression = "hasLastUpdated()")
          @MeterTag(key = "hasTags", expression = "hasTags()")
          @MeterTag(key = "hasClaimTypeCodes", expression = "hasClaimTypeCodes()")
          @MeterTag(key = "hasOutcomes", expression = "hasOutcomes()")
          @MeterTag(key = "hasSources", expression = "hasSources()")
          ClaimSearchCriteria criteria) {

    List<DbFilterBuilder> filterBuilders =
        List.of(
            new BillablePeriodFilterParam(criteria.claimThroughDate()),
            new LastUpdatedFilterParam(criteria.lastUpdated()),
            new ClaimTypeCodeFilterParam(criteria.claimTypeCodes()),
            new TagCriteriaFilterParam(criteria.tagCriteria()),
            new OutcomeFilterParam(criteria.outcomes()),
            new SourceFilterParam(criteria.sources()));

    var claimFutures =
        ALL_CLAIM_TYPES.stream()
            .filter(
                claimTypeDefinition ->
                    claimTypeDefinition.matchesSystemType(filterBuilders)
                        && claimTypeDefinition.matchesProfile(criteria.profile()))
            .map(
                d ->
                    asyncService.fetchClaims(
                        d.baseQuery(), d.claimClass(), d.systemType(), criteria, filterBuilders))
            .toList();

    var includePriorAuth = filterBuilders.stream().allMatch(DbFilterBuilder::shouldQueryPriorAuth);
    CompletableFuture<List<PriorAuthorization>> priorAuthFuture =
        includePriorAuth
            ? asyncService.fetchPriorAuth(criteria.mbi())
            : CompletableFuture.completedFuture(Collections.emptyList());

    List<CompletableFuture<?>> allFutures = new ArrayList<>(claimFutures);
    allFutures.add(priorAuthFuture);
    CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();

    metricRecorder.recordDistribution(
        "application.claim.search_by_bene.fan_out", allFutures.size());

    Stream<ClaimBase> claimStream = claimFutures.stream().flatMap(f -> f.join().stream());
    var claims = claimStream.sorted(Comparator.comparing(ClaimBase::getClaimUniqueId)).toList();
    var priorAuths = priorAuthFuture.join();
    return new ClaimAndAuthResult(claims, priorAuths);
  }

  /**
   * Wrapper for the parallel results of claims and prior auth queries.
   *
   * @param claims list of claims found
   * @param priorAuths list of prior authorizations found
   */
  public record ClaimAndAuthResult(List<ClaimBase> claims, List<PriorAuthorization> priorAuths) {}
}
