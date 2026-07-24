package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.claim.filter.*;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimIdSearchCriteria;
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

  private static final String CLAIM_PROFESSIONAL_SHARED_SYSTEMS =
      """
        SELECT c
        FROM ClaimProfessionalSharedSystems c
        JOIN FETCH c.beneficiary b
        LEFT JOIN FETCH c.claimItems cl
      """;

  private static final String CLAIM_PROFESSIONAL_NCH =
      """
        SELECT c
        FROM ClaimProfessionalNch c
        JOIN FETCH c.beneficiary b
        JOIN FETCH c.claimItems cl
      """;

  private static final String CLAIM_INSTITUTIONAL_SHARED_SYSTEMS =
      """
        SELECT c
        FROM ClaimInstitutionalSharedSystems c
        JOIN FETCH c.beneficiary b
        LEFT JOIN FETCH c.claimItems cl
      """;

  private static final String CLAIM_INSTITUTIONAL_NCH =
      """
        SELECT c
        FROM ClaimInstitutionalNch c
        JOIN FETCH c.beneficiary b
        JOIN FETCH c.claimItems cl
      """;

  private static final String CLAIM_RX =
      """
        SELECT c
        FROM ClaimRx c
        JOIN FETCH c.beneficiary b
      """;

  private static final List<ClaimTypeDefinition> ALL_CLAIM_TYPES =
      List.of(
          new ClaimTypeDefinition(
              CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
              ClaimProfessionalSharedSystems.class,
              SystemType.SS),
          new ClaimTypeDefinition(
              CLAIM_PROFESSIONAL_NCH, ClaimProfessionalNch.class, SystemType.NCH),
          new ClaimTypeDefinition(
              CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
              ClaimInstitutionalSharedSystems.class,
              SystemType.SS),
          new ClaimTypeDefinition(
              CLAIM_INSTITUTIONAL_NCH, ClaimInstitutionalNch.class, SystemType.NCH),
          new ClaimTypeDefinition(CLAIM_RX, ClaimRx.class, SystemType.DDPS));

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

    var professionalSharedSystemsClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
            ClaimProfessionalSharedSystems.class,
            ClaimProfessionalSharedSystems.getSystemType(),
            criteria.claimUniqueIds(),
            paramBuilders);

    var professionalNchClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_PROFESSIONAL_NCH,
            ClaimProfessionalNch.class,
            ClaimProfessionalNch.getSystemType(),
            criteria.claimUniqueIds(),
            paramBuilders);

    var institutionalSharedSystemsClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
            ClaimInstitutionalSharedSystems.class,
            ClaimInstitutionalSharedSystems.getSystemType(),
            criteria.claimUniqueIds(),
            paramBuilders);

    var institutionalNchClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_INSTITUTIONAL_NCH,
            ClaimInstitutionalNch.class,
            ClaimInstitutionalNch.getSystemType(),
            criteria.claimUniqueIds(),
            paramBuilders);

    var rxClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_RX,
            ClaimRx.class,
            ClaimRx.getSystemType(),
            criteria.claimUniqueIds(),
            paramBuilders);

    // Wait for all queries
    CompletableFuture.allOf(
            professionalNchClaims,
            professionalSharedSystemsClaims,
            institutionalSharedSystemsClaims,
            institutionalNchClaims,
            rxClaims)
        .join();

    var allClaims = new ArrayList<ClaimBase>();
    allClaims.addAll(professionalNchClaims.join());
    allClaims.addAll(professionalSharedSystemsClaims.join());
    allClaims.addAll(institutionalSharedSystemsClaims.join());
    allClaims.addAll(institutionalNchClaims.join());
    allClaims.addAll(rxClaims.join());

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
            .filter(claimTypeDefinition -> claimTypeDefinition.matchesSystemType(filterBuilders))
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
