package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.filter.BillablePeriodFilterParam;
import gov.cms.bfd.server.ng.claim.filter.LastUpdatedFilterParam;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.input.TagCriterion;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
// NOTE: @Transactional is needed to ensure our custom transaction manager is used
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

  private static final Set<Class<? extends ClaimBase>> ALL_CLAIM_CLASSES =
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
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param tagCriteria tag criteria
   * @param claimTypeCodes claimTypeCodes
   * @param sources claim sources
   * @return claims
   */
  @Timed(value = "application.claim.search_by_bene")
  public List<ClaimBase> findByBeneXrefSk(
      long beneSk,
      @MeterTag(
              key = "hasClaimThroughDate",
              expression = "lowerBound.isPresent() || upperBound.isPresent()")
          DateTimeRange claimThroughDate,
      @MeterTag(
              key = "hasLastUpdated",
              expression = "lowerBound.isPresent() || upperBound.isPresent()")
          DateTimeRange lastUpdated,
      @MeterTag(key = "hasTags", expression = "size() > 0") List<List<TagCriterion>> tagCriteria,
      @MeterTag(key = "hasClaimTypeCodes", expression = "size() > 0")
          List<ClaimTypeCode> claimTypeCodes,
      @MeterTag(key = "hasSources", expression = "size() > 0") List<List<MetaSourceSk>> sources) {

    var claimTypes = determineClaimTypesToQuery(sources, tagCriteria);
    var futures = new ArrayList<CompletableFuture<? extends List<? extends ClaimBase>>>();

    // Execute all four queries. If tag source id or meta source provided, filter before we execute
    if (claimTypes.contains(ClaimProfessionalSharedSystems.class)) {
      futures.add(
          asyncService.fetchClaims(
              CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
              ClaimProfessionalSharedSystems.class,
              beneSk,
              claimThroughDate,
              lastUpdated,
              tagCriteria,
              claimTypeCodes,
              sources));
    }

    if (claimTypes.contains(ClaimProfessionalNch.class)) {
      futures.add(
          asyncService.fetchClaims(
              CLAIM_PROFESSIONAL_NCH,
              ClaimProfessionalNch.class,
              beneSk,
              claimThroughDate,
              lastUpdated,
              tagCriteria,
              claimTypeCodes,
              sources));
    }

    if (claimTypes.contains(ClaimInstitutionalSharedSystems.class)) {
      futures.add(
          asyncService.fetchClaims(
              CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
              ClaimInstitutionalSharedSystems.class,
              beneSk,
              claimThroughDate,
              lastUpdated,
              tagCriteria,
              claimTypeCodes,
              sources));
    }

    if (claimTypes.contains(ClaimInstitutionalNch.class)) {
      futures.add(
          asyncService.fetchClaims(
              CLAIM_INSTITUTIONAL_NCH,
              ClaimInstitutionalNch.class,
              beneSk,
              claimThroughDate,
              lastUpdated,
              tagCriteria,
              claimTypeCodes,
              sources));
    }

    if (claimTypes.contains(ClaimRx.class)) {
      futures.add(
          asyncService.fetchClaims(
              CLAIM_RX,
              ClaimRx.class,
              beneSk,
              claimThroughDate,
              lastUpdated,
              tagCriteria,
              claimTypeCodes,
              sources));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return futures.stream()
        .flatMap(f -> f.join().stream())
        .map(ClaimBase.class::cast)
        .sorted(Comparator.comparing(ClaimBase::getClaimUniqueId))
        .toList();
  }

  private Set<Class<? extends ClaimBase>> determineClaimTypesToQuery(
      List<List<MetaSourceSk>> sources, List<List<TagCriterion>> tagCriteria) {

    var hasFinalAction =
        tagCriteria.stream()
            .flatMap(List::stream)
            .anyMatch(TagCriterion.FinalActionCriterion.class::isInstance);
    if (hasFinalAction) {
      return ALL_CLAIM_CLASSES;
    }

    var eligible = new HashSet<>(ALL_CLAIM_CLASSES);

    if (sources != null && !sources.isEmpty()) {
      var metaSources = sources.stream().flatMap(List::stream).collect(Collectors.toSet());
      var allowedByMetaSource =
          metaSources.stream()
              .flatMap(ms -> mapMetaSourceToClaimType(ms).stream())
              .collect(Collectors.toSet());
      eligible.addAll(allowedByMetaSource);
    }

    if (tagCriteria != null && !tagCriteria.isEmpty()) {
      var tagSourceIds =
          tagCriteria.stream()
              .flatMap(List::stream)
              .filter(TagCriterion.SourceIdCriterion.class::isInstance)
              .map(TagCriterion.SourceIdCriterion.class::cast)
              .map(TagCriterion.SourceIdCriterion::sourceId)
              .collect(Collectors.toSet());

      if (!tagSourceIds.isEmpty()) {
        var allowedByTagSource =
            tagSourceIds.stream()
                .flatMap(src -> mapClaimSourceIdToClaimType(src).stream())
                .collect(Collectors.toSet());

        eligible.addAll(allowedByTagSource);
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
      default -> ALL_CLAIM_CLASSES;
    };
  }
}
