package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.filter.BillablePeriodFilterParam;
import gov.cms.bfd.server.ng.claim.filter.ClaimTypeCodeFilterParam;
import gov.cms.bfd.server.ng.claim.filter.LastUpdatedFilterParam;
import gov.cms.bfd.server.ng.claim.filter.TagCriteriaFilterParam;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.input.TagCriterion;
import gov.cms.bfd.server.ng.util.LogUtil;
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
        LEFT JOIN FETCH c.claimInstitutional ci
        LEFT JOIN FETCH c.claimProfessional cp
        LEFT JOIN FETCH cl.claimLineInstitutional cli
        LEFT JOIN FETCH cl.claimLineProfessional clp
        LEFT JOIN FETCH c.claimFiss cf
        LEFT JOIN FETCH cli.ansiSignature a
        LEFT JOIN FETCH cl.claimLineRx clr
        LEFT JOIN FETCH c.contract ct
        LEFT JOIN FETCH ct.contractPlanContactInfo cc
        LEFT JOIN FETCH c.serviceProviderHistory p
        LEFT JOIN FETCH c.attendingProviderHistory ap
        LEFT JOIN FETCH c.operatingProviderHistory orp
        LEFT JOIN FETCH c.otherProviderHistory otp
        LEFT JOIN FETCH c.renderingProviderHistory rp
        LEFT JOIN FETCH c.prescribingProviderHistory pp
        LEFT JOIN FETCH c.billingProviderHistory bp
        LEFT JOIN FETCH c.referringProviderHistory rph
      """;

  /**
   * Search for a claim by its ID.
   *
   * @param claimUniqueId claim ID
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @return claim
   */
  public Optional<Claim> findById(
      long claimUniqueId, DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
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
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param limit limit
   * @param offset offset
   * @param tagCriteria tag criteria
   * @param claimTypeCodes claimTypeCodes
   * @return claims
   */
  public List<Claim> findByBeneXrefSk(
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      Optional<Integer> limit,
      Optional<Integer> offset,
      List<List<TagCriterion>> tagCriteria,
      List<ClaimTypeCode> claimTypeCodes) {
    var filterBuilders =
        List.of(
            new BillablePeriodFilterParam(claimThroughDate),
            new LastUpdatedFilterParam(lastUpdated),
            new ClaimTypeCodeFilterParam(claimTypeCodes),
            new TagCriteriaFilterParam(tagCriteria));
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
            %s
            """,
            CLAIM_TABLES_BASE, filters.filterClause());
    var claims =
        withParams(entityManager.createQuery(jpql, Claim.class), filters.params())
            .setParameter("limit", limit.orElse(5000))
            .setParameter("offset", offset.orElse(0))
            .setParameter("beneSk", beneSk)
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
