package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.filter.BillablePeriodFilterParam;
import gov.cms.bfd.server.ng.claim.filter.ClaimTypeCodeFilterParam;
import gov.cms.bfd.server.ng.claim.filter.LastUpdatedFilterParam;
import gov.cms.bfd.server.ng.claim.filter.TagCriteriaFilterParam;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository methods for claims. */
// NOTE: @Transactional is needed to ensure our custom transaction manager is used
@Transactional(readOnly = true)
@Repository
@AllArgsConstructor
public class ClaimNewRepository {

  private final EntityManager entityManager;

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
    var params = getFilters(paramBuilders);
    var allResults = new ArrayList<ClaimBase>();
    allResults.addAll(
        findByIdInClaimType(
                CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
                ClaimProfessionalSharedSystems.class,
                claimUniqueId,
                params)
            .stream()
            .toList());
    allResults.addAll(
        findByIdInClaimType(
                CLAIM_PROFESSIONAL_NCH, ClaimProfessionalNch.class, claimUniqueId, params)
            .stream()
            .toList());
    allResults.addAll(
        findByIdInClaimType(
                CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
                ClaimInstitutionalSharedSystems.class,
                claimUniqueId,
                params)
            .stream()
            .toList());
    allResults.addAll(
        findByIdInClaimType(
                CLAIM_INSTITUTIONAL_NCH, ClaimInstitutionalNch.class, claimUniqueId, params)
            .stream()
            .toList());
    allResults.addAll(
        findByIdInClaimType(CLAIM_RX, ClaimRx.class, claimUniqueId, params).stream().toList());

    var optionalClaim = allResults.stream().findFirst();
    optionalClaim.ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
    return optionalClaim;
  }

  private <T extends ClaimBase> Optional<T> findByIdInClaimType(
      String baseQuery, Class<T> claimClass, long claimUniqueId, DbFilter params) {

    var jpql =
        String.format(
            """
      %s
      WHERE c.claimUniqueId = :claimUniqueId
      %s
      """,
            baseQuery, params.filterClause());

    var results =
        withParams(entityManager.createQuery(jpql, claimClass), params.params())
            .setParameter("claimUniqueId", claimUniqueId)
            .getResultList();

    return results.stream().findFirst();
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

    var allClaims = new ArrayList<ClaimBase>();

    // Execute all four queries
    allClaims.addAll(
        fetchClaims(
            CLAIM_PROFESSIONAL_SHARED_SYSTEMS,
            ClaimProfessionalSharedSystems.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes));
    allClaims.addAll(
        fetchClaims(
            CLAIM_PROFESSIONAL_NCH,
            ClaimProfessionalNch.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes));
    allClaims.addAll(
        fetchClaims(
            CLAIM_INSTITUTIONAL_SHARED_SYSTEMS,
            ClaimInstitutionalSharedSystems.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes));
    allClaims.addAll(
        fetchClaims(
            CLAIM_INSTITUTIONAL_NCH,
            ClaimInstitutionalNch.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes));
    allClaims.addAll(
        fetchClaims(
            CLAIM_RX,
            ClaimRx.class,
            beneSk,
            claimThroughDate,
            lastUpdated,
            tagCriteria,
            claimTypeCodes));

    // Sort, apply offset/limit
    return allClaims.stream()
        .sorted(Comparator.comparing(ClaimBase::getClaimUniqueId))
        .skip(offset.orElse(0))
        .limit(limit.orElse(5000))
        .toList();
  }

  private <T extends ClaimBase> List<T> fetchClaims(
      String baseQuery,
      Class<T> claimClass,
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      List<List<TagCriterion>> tagCriteria,
      List<ClaimTypeCode> claimTypeCodes) {

    var filterBuilders =
        List.of(
            new BillablePeriodFilterParam(claimThroughDate),
            new LastUpdatedFilterParam(lastUpdated),
            new ClaimTypeCodeFilterParam(claimTypeCodes),
            new TagCriteriaFilterParam(tagCriteria));
    var filters = getFilters(filterBuilders);

    var jpql =
        String.format(
            """
        WITH benes AS (
            SELECT b.beneSk beneSk, b.effectiveTimestamp effectiveTimestamp
            FROM Beneficiary b
            WHERE b.xrefSk = :beneSk AND b.latestTransactionFlag = 'Y'
        )
        %s
        WHERE EXISTS (
            SELECT 1 FROM benes b2
            WHERE b2.beneSk = b.beneSk
            AND b2.effectiveTimestamp = b.effectiveTimestamp
        )
        %s
        ORDER BY c.claimUniqueId
        """,
            baseQuery, filters.filterClause());

    return withParams(entityManager.createQuery(jpql, claimClass), filters.params())
        .setParameter("beneSk", beneSk)
        .getResultList();
  }

  private <T extends DbFilterBuilder> DbFilter getFilters(List<T> builders) {
    var sb = new StringBuilder();
    var queryParams = new ArrayList<DbFilterParam>();
    for (var builder : builders) {
      var params = builder.getFilters("c");
      sb.append(params.filterClause());
      queryParams.addAll(params.params());
    }
    return new DbFilter(sb.toString(), queryParams);
  }

  private <T> TypedQuery<T> withParams(TypedQuery<T> query, List<DbFilterParam> params) {
    for (var param : params) {
      query.setParameter(param.name(), param.value());
    }
    return query;
  }
}
