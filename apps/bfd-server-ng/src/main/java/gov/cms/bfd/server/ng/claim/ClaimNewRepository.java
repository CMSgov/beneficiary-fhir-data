package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.filter.BillablePeriodFilterParam;
import gov.cms.bfd.server.ng.claim.filter.LastUpdatedFilterParam;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.input.TagCriterion;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimNewRepository {

  private final ClaimAsyncService asyncService;

  private static final String CLAIM_PROFESSIONAL_SHARED_SYSTEMS =
      """
              SELECT c
              FROM ClaimProfessionalSharedSystems c
              JOIN FETCH c.beneficiary b
              JOIN FETCH c.claimItems AS cl
            """;

  private static final String CLAIM_PROFESSIONAL_NCH =
      """
              SELECT c
              FROM ClaimProfessionalNch c
              JOIN FETCH c.beneficiary b
              JOIN FETCH c.claimItems AS cl
            """;

  private static final String CLAIM_INSTITUTIONAL_SHARED_SYSTEMS =
      """
              SELECT c
              FROM ClaimInstitutionalSharedSystems c
              JOIN FETCH c.beneficiary b
              JOIN FETCH c.claimItems AS cl
            """;

  private static final String CLAIM_INSTITUTIONAL_NCH =
      """
              SELECT c
              FROM ClaimInstitutionalNch c
              JOIN FETCH c.beneficiary b
              JOIN FETCH c.claimItems AS cl
            """;

  private static final String CLAIM_RX =
      """
              SELECT c
              FROM ClaimRx c
              JOIN FETCH c.beneficiary b
            """;

  private static final Set<Class<? extends ClaimBase>> ALL_CLAIM_TYPES =
      Set.of(
          ClaimProfessionalNch.class,
          ClaimInstitutionalNch.class,
          ClaimProfessionalSharedSystems.class,
          ClaimInstitutionalSharedSystems.class,
          ClaimRx.class);

  /**
   * Search for a claim by its ID.
   *
   * @param claimUniqueId claim ID
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @return claim
   */
  @Timed(value = "application.claim.search_by_id")
  public Optional<ClaimBase> findById(
      long claimUniqueId,
      @MeterTag(
              key = "hasClaimThroughDate",
              expression = "lowerBound.isPresent() || upperBound.isPresent()")
          DateTimeRange claimThroughDate,
      @MeterTag(
              key = "hasLastUpdated",
              expression = "lowerBound.isPresent() || upperBound.isPresent()")
          DateTimeRange lastUpdated) {
    var paramBuilders =
        List.of(
            new BillablePeriodFilterParam(claimThroughDate),
            new LastUpdatedFilterParam(lastUpdated));
    var params = asyncService.getFilters(paramBuilders);

    var professionalSharedSystemsClaims =
        asyncService.findByIdInClaimType(
            CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
            ClaimProfessionalSharedSystems.class,
            claimUniqueId,
            params);

    var professionalNchClaims =
        asyncService.findByIdInClaimType(
            CLAIM_PROFESSIONAL_NCH, ClaimProfessionalNch.class, claimUniqueId, params);

    var institutionalSharedSystemsClaims =
        asyncService.findByIdInClaimType(
            CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
            ClaimInstitutionalSharedSystems.class,
            claimUniqueId,
            params);

    var institutionalNchClaims =
        asyncService.findByIdInClaimType(
            CLAIM_INSTITUTIONAL_NCH, ClaimInstitutionalNch.class, claimUniqueId, params);

    var rxClaims = asyncService.findByIdInClaimType(CLAIM_RX, ClaimRx.class, claimUniqueId, params);

    // Wait for all queries
    CompletableFuture.allOf(
            professionalNchClaims,
            professionalSharedSystemsClaims,
            institutionalSharedSystemsClaims,
            institutionalNchClaims,
            rxClaims)
        .join();

    var allClaims = new ArrayList<ClaimBase>();
    professionalNchClaims.join().ifPresent(allClaims::add);
    professionalSharedSystemsClaims.join().ifPresent(allClaims::add);
    institutionalSharedSystemsClaims.join().ifPresent(allClaims::add);
    institutionalNchClaims.join().ifPresent(allClaims::add);
    rxClaims.join().ifPresent(allClaims::add);

    var result = allClaims.stream().findFirst();

    result.ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));

    return result;
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

    var claimTypes = determineClaimTypesToQuery(criteria.sources(), criteria.tagCriteria());
    var futures = new ArrayList<CompletableFuture<? extends List<? extends ClaimBase>>>();

    // Execute all four queries. If tag source id or meta source provided, filter before we execute
    if (claimTypes.contains(ClaimProfessionalSharedSystems.class)) {
      futures.add(
          asyncService.fetchClaims(
              CLAIM_PROFESSIONAL_SHARED_SYSTEMS, ClaimProfessionalSharedSystems.class, criteria));
    }

    if (claimTypes.contains(ClaimProfessionalNch.class)) {
      futures.add(
          asyncService.fetchClaims(CLAIM_PROFESSIONAL_NCH, ClaimProfessionalNch.class, criteria));
    }

    if (claimTypes.contains(ClaimInstitutionalSharedSystems.class)) {
      futures.add(
          asyncService.fetchClaims(
              CLAIM_INSTITUTIONAL_SHARED_SYSTEMS, ClaimInstitutionalSharedSystems.class, criteria));
    }

    if (claimTypes.contains(ClaimInstitutionalNch.class)) {
      futures.add(
          asyncService.fetchClaims(CLAIM_INSTITUTIONAL_NCH, ClaimInstitutionalNch.class, criteria));
    }

    if (claimTypes.contains(ClaimRx.class)) {
      futures.add(asyncService.fetchClaims(CLAIM_RX, ClaimRx.class, criteria));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    Stream<ClaimBase> claims = futures.stream().flatMap(f -> f.join().stream());
    return claims.sorted(Comparator.comparing(ClaimBase::getClaimUniqueId)).toList();
  }

  private Set<Class<? extends ClaimBase>> determineClaimTypesToQuery(
      List<List<MetaSourceSk>> sources, List<List<TagCriterion>> tagCriteria) {

    var hasFinalAction =
        tagCriteria.stream()
            .flatMap(List::stream)
            .anyMatch(TagCriterion.FinalActionCriterion.class::isInstance);
    if (hasFinalAction) {
      return ALL_CLAIM_TYPES;
    }

    var eligible = new HashSet<>(ALL_CLAIM_TYPES);

    if (!sources.isEmpty()) {
      var metaSources = sources.stream().flatMap(List::stream).collect(Collectors.toSet());
      var allowedByMetaSource =
          metaSources.stream()
              .flatMap(ms -> mapMetaSourceToClaimType(ms).stream())
              .collect(Collectors.toSet());
      eligible.retainAll(allowedByMetaSource);
    }

    if (!tagCriteria.isEmpty()) {
      var tagSourceIds =
          tagCriteria.stream()
              .flatMap(List::stream)
              .mapMulti(this::extractSourceId)
              .collect(Collectors.toSet());

      if (!tagSourceIds.isEmpty()) {
        var allowedByTagSource =
            tagSourceIds.stream()
                .flatMap(src -> mapClaimSourceIdToClaimType(src).stream())
                .collect(Collectors.toSet());

        eligible.retainAll(allowedByTagSource);
      }
    }
    return eligible;
  }

  private Set<Class<? extends ClaimBase>> mapMetaSourceToClaimType(MetaSourceSk metaSourceSk) {
    return switch (metaSourceSk) {
      case NCH -> Set.of(ClaimProfessionalNch.class, ClaimInstitutionalNch.class);
      case DDPS -> Set.of(ClaimRx.class);
      case VMS, MCS, FISS ->
          Set.of(ClaimProfessionalSharedSystems.class, ClaimInstitutionalSharedSystems.class);
    };
  }

  private Set<Class<? extends ClaimBase>> mapClaimSourceIdToClaimType(ClaimSourceId sourceId) {
    return switch (sourceId) {
      case NATIONAL_CLAIMS_HISTORY ->
          Set.of(ClaimProfessionalNch.class, ClaimInstitutionalNch.class, ClaimRx.class);
      case VMS, MCS, FISS ->
          Set.of(ClaimProfessionalSharedSystems.class, ClaimInstitutionalSharedSystems.class);
      default -> ALL_CLAIM_TYPES;
    };
  }

  private void extractSourceId(TagCriterion criterion, Consumer<ClaimSourceId> consumer) {
    if (criterion instanceof TagCriterion.SourceIdCriterion(ClaimSourceId sourceId)) {
      consumer.accept(sourceId);
    }
  }
}
