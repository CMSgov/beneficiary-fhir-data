package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.filter.*;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
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
public class ClaimRepository {

  private final EntityManager entityManager;

  private static final String CLAIM_TABLES_BASE =
      """
        SELECT c
        FROM Claim c
        JOIN FETCH c.beneficiary b
        JOIN FETCH c.claimDateSignature AS cds
        JOIN FETCH c.claimItems AS cl
        LEFT JOIN FETCH c.claimOptional.claimInstitutional ci
        LEFT JOIN FETCH c.claimOptional.claimProfessional cp
        LEFT JOIN FETCH cl.claimItemOptional.claimLineInstitutional cli
        LEFT JOIN FETCH cl.claimItemOptional.claimLineProfessional clp
        LEFT JOIN FETCH c.claimOptional.claimFiss cf
        LEFT JOIN FETCH cli.claimLineInstitutionalOptional.ansiSignature a
        LEFT JOIN FETCH cl.claimItemOptional.claimLineRx clr
        LEFT JOIN FETCH c.claimOptional.contract ct
        LEFT JOIN FETCH ct.contractOptional.contractPlanContactInfo cc
        LEFT JOIN FETCH c.claimOptional.serviceProviderHistory p
        LEFT JOIN FETCH c.claimOptional.attendingProviderHistory ap
        LEFT JOIN FETCH c.claimOptional.operatingProviderHistory orp
        LEFT JOIN FETCH c.claimOptional.otherProviderHistory otp
        LEFT JOIN FETCH c.claimOptional.renderingProviderHistory rp
        LEFT JOIN FETCH c.claimOptional.prescribingProviderHistory pp
        LEFT JOIN FETCH c.claimOptional.billingProviderHistory bp
        LEFT JOIN FETCH c.claimOptional.referringProviderHistory rph
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
  public Optional<Claim> findById(
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
    var jpql =
        String.format(
            """
              %s
              WHERE c.claimUniqueId = :claimUniqueId
              AND (ct.contractPbpSk IS NULL OR ct.contractVersionRank = 1)
              %s
            """,
            CLAIM_TABLES_BASE, params.filterClause());
    var results =
        withParams(entityManager.createQuery(jpql, Claim.class), params.params())
            .setParameter("claimUniqueId", claimUniqueId)
            .getResultList();

    var optionalClaim = results.stream().findFirst();
    optionalClaim.ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
    return optionalClaim;
  }

  /**
   * Returns claims for the given beneficiary.
   *
   * @param criteria filter criteria
   * @return claims
   */
  @Timed(value = "application.claim.search_by_bene")
  public List<Claim> findByBeneXrefSk(
      @MeterTag(key = "hasClaimThroughDate", expression = "hasClaimThroughDate()")
          @MeterTag(key = "hasLastUpdated", expression = "hasLasUpdated()")
          @MeterTag(key = "hasLimit", expression = "hasLimit()")
          @MeterTag(key = "hasOffset", expression = "hasOffset()")
          @MeterTag(key = "hasTags", expression = "hasTags()")
          @MeterTag(key = "hasClaimTypeCodes", expression = "hasClaimTypeCodes()")
          @MeterTag(key = "hasSources", expression = "hasSources()")
          ClaimSearchCriteria criteria) {
    var filterBuilders =
        List.of(
            new BillablePeriodFilterParam(criteria.claimThroughDate()),
            new LastUpdatedFilterParam(criteria.lastUpdated()),
            new ClaimTypeCodeFilterParam(criteria.claimTypeCodes()),
            new TagCriteriaFilterParam(criteria.tagCriteria()),
            new SourceFilterParam(criteria.sources()));
    var filters = getFilters(filterBuilders);
    // Some of the filters here appear redundant, but joining on the entire primary key for
    // beneficiaries (bene_sk + effective timestamp) helps query performance significantly
    var jpql =
        String.format(
            """
            WITH benes AS (
                  SELECT b.beneSk beneSk, b.effectiveTimestamp effectiveTimestamp
                  FROM Beneficiary b
                  WHERE b.xrefSk = :beneSk AND b.latestTransactionFlag = 'Y'
             ),
             claims AS (
                SELECT c.claimUniqueId claimUniqueId
                FROM Claim c
                WHERE EXISTS (
                  SELECT 1 FROM benes b2
                    WHERE b2.beneSk = c.beneficiary.beneSk
                    AND b2.effectiveTimestamp = c.beneficiary.effectiveTimestamp
                )
                ORDER BY c.claimUniqueId
                OFFSET :offset ROWS
                FETCH NEXT :limit ROWS ONLY
             )
            %s
            WHERE c.claimUniqueId IN (SELECT claimUniqueId FROM claims)
            AND EXISTS (
              SELECT 1 FROM benes b2
              WHERE b2.beneSk = b.beneSk
              AND b2.effectiveTimestamp = b.effectiveTimestamp
            )
            AND (ct.contractPbpSk IS NULL OR ct.contractVersionRank = 1)
            %s
            """,
            CLAIM_TABLES_BASE, filters.filterClause());
    var claims =
        withParams(entityManager.createQuery(jpql, Claim.class), filters.params())
            .setParameter("limit", criteria.limit().orElse(5000))
            .setParameter("offset", criteria.offset().orElse(0))
            .setParameter("beneSk", criteria.beneSk())
            .getResultList();

    claims.stream()
        .findFirst()
        .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
    return claims;
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
