package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.claim.filter.*;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
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
  private final MeterRegistry meterRegistry;

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
   * @param claimUniqueIds claim IDs
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @return claim
   */
  @Timed(value = "application.claim.search_by_id")
  public List<ClaimBase> findByIds(
      List<Long> claimUniqueIds,
      @MeterTag(
              key = "hasClaimThroughDate",
              expression = "lowerBound.isPresent() || upperBound.isPresent()")
          DateTimeRange claimThroughDate,
      @MeterTag(
              key = "hasLastUpdated",
              expression = "lowerBound.isPresent() || upperBound.isPresent()")
          DateTimeRange lastUpdated) {
    if (claimUniqueIds == null || claimUniqueIds.isEmpty()) {
      return Collections.emptyList();
    }
    var paramBuilders =
        List.of(
            new BillablePeriodFilterParam(claimThroughDate),
            new LastUpdatedFilterParam(lastUpdated));

    var professionalSharedSystemsClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
            ClaimProfessionalSharedSystems.class,
            ClaimProfessionalSharedSystems.getSystemType(),
            claimUniqueIds,
            paramBuilders);

    var professionalNchClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_PROFESSIONAL_NCH,
            ClaimProfessionalNch.class,
            ClaimProfessionalNch.getSystemType(),
            claimUniqueIds,
            paramBuilders);

    var institutionalSharedSystemsClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
            ClaimInstitutionalSharedSystems.class,
            ClaimInstitutionalSharedSystems.getSystemType(),
            claimUniqueIds,
            paramBuilders);

    var institutionalNchClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_INSTITUTIONAL_NCH,
            ClaimInstitutionalNch.class,
            ClaimInstitutionalNch.getSystemType(),
            claimUniqueIds,
            paramBuilders);

    var rxClaims =
        asyncService.findByIdsInClaimType(
            CLAIM_RX, ClaimRx.class, ClaimRx.getSystemType(), claimUniqueIds, paramBuilders);

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
  public List<ClaimBase> findByBeneXrefSk(
      @MeterTag(key = "hasClaimThroughDate", expression = "hasClaimThroughDate()")
          @MeterTag(key = "hasLastUpdated", expression = "hasLasUpdated()")
          @MeterTag(key = "hasTags", expression = "hasTags()")
          @MeterTag(key = "hasClaimTypeCodes", expression = "hasClaimTypeCodes()")
          @MeterTag(key = "hasSources", expression = "hasSources()")
          ClaimSearchCriteria criteria) {

    List<DbFilterBuilder> filterBuilders =
        List.of(
            new BillablePeriodFilterParam(criteria.claimThroughDate()),
            new LastUpdatedFilterParam(criteria.lastUpdated()),
            new ClaimTypeCodeFilterParam(criteria.claimTypeCodes()),
            new TagCriteriaFilterParam(criteria.tagCriteria()),
            new SourceFilterParam(criteria.sources()));

    var futures =
        ALL_CLAIM_TYPES.stream()
            .filter(claimTypeDefinition -> claimTypeDefinition.matchesSystemType(filterBuilders))
            .map(
                d ->
                    asyncService.fetchClaims(
                        d.baseQuery(), d.claimClass(), d.systemType(), criteria, filterBuilders))
            .toList();

    DistributionSummary.builder("application.claim.search_by_bene.fan_out")
        .register(meterRegistry)
        .record(futures.size());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    Stream<ClaimBase> claims = futures.stream().flatMap(f -> f.join().stream());
    return claims.sorted(Comparator.comparing(ClaimBase::getClaimUniqueId)).toList();
  }
}
