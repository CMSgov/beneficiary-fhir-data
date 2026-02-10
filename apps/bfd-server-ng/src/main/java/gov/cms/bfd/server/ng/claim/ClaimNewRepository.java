package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.filter.BillablePeriodFilterParam;
import gov.cms.bfd.server.ng.claim.filter.LastUpdatedFilterParam;
import gov.cms.bfd.server.ng.claim.model.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.ClaimInstitutionalNch;
import gov.cms.bfd.server.ng.claim.model.ClaimInstitutionalSharedSystems;
import gov.cms.bfd.server.ng.claim.model.ClaimProfessionalNch;
import gov.cms.bfd.server.ng.claim.model.ClaimProfessionalSharedSystems;
import gov.cms.bfd.server.ng.claim.model.ClaimRx;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.input.TagCriterion;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
    //    var allResults = new ArrayList<ClaimBase>();
    var futures =
        List.of(
            asyncService.findByIdInClaimType(
                CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
                ClaimProfessionalSharedSystems.class,
                claimUniqueId,
                params),
            asyncService.findByIdInClaimType(
                CLAIM_PROFESSIONAL_NCH, ClaimProfessionalNch.class, claimUniqueId, params),
            asyncService.findByIdInClaimType(
                CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
                ClaimInstitutionalSharedSystems.class,
                claimUniqueId,
                params),
            asyncService.findByIdInClaimType(
                CLAIM_INSTITUTIONAL_NCH, ClaimInstitutionalNch.class, claimUniqueId, params),
            asyncService.findByIdInClaimType(CLAIM_RX, ClaimRx.class, claimUniqueId, params));

    // Wait for all queries
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    var result =
        futures.stream()
            .map(CompletableFuture::join)
            .flatMap(Optional::stream)
            .map(ClaimBase.class::cast)
            .findFirst();

    result.ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));

    return result;
  }

  /**
   * Returns claims for the given beneficiary.
   *
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param limit limit
   * @param offset offset
   * @param tagCriteria tag criteria
   * @param claimTypeCodes claimTypeCodes
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
      @MeterTag(key = "hasLimit", expression = "isPresent()") Optional<Integer> limit,
      @MeterTag(key = "hasOffset", expression = "isPresent()") Optional<Integer> offset,
      @MeterTag(key = "hasTags", expression = "size() > 0") List<List<TagCriterion>> tagCriteria,
      @MeterTag(key = "hasClaimTypeCodes", expression = "size() > 0")
          List<ClaimTypeCode> claimTypeCodes) {

    // Execute all four queries
    var professionalSharedSystemsClaims =
        asyncService.fetchClaims(
            CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
            ClaimProfessionalSharedSystems.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes);

    var professionalNchClaims =
        asyncService.fetchClaims(
            CLAIM_PROFESSIONAL_NCH,
            ClaimProfessionalNch.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes);

    var institutionalSharedSystemsClaims =
        asyncService.fetchClaims(
            CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
            ClaimInstitutionalSharedSystems.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes);

    var institutionalNchClaims =
        asyncService.fetchClaims(
            CLAIM_INSTITUTIONAL_NCH,
            ClaimInstitutionalNch.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes);

    var rxClaims =
        asyncService.fetchClaims(
            CLAIM_RX,
            ClaimRx.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes);

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

    // Sort, apply offset/limit
    return allClaims.stream()
        .sorted(Comparator.comparing(ClaimBase::getClaimUniqueId))
        .skip(offset.orElse(0))
        .limit(limit.orElse(5000))
        .toList();
  }
}
