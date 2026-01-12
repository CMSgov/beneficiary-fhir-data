package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.input.TagCriterion;
import gov.cms.bfd.server.ng.input.TagCriterion.FinalActionCriterion;
import gov.cms.bfd.server.ng.input.TagCriterion.SourceIdCriterion;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
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
    var jpql =
        String.format(
            """
              %s
              WHERE c.claimUniqueId = :claimUniqueId
              %s
            """,
            CLAIM_TABLES_BASE, getFilters(claimThroughDate, lastUpdated, Collections.emptyList()));
    var results =
        withParams(
                entityManager.createQuery(jpql, Claim.class),
                claimThroughDate,
                lastUpdated,
                new ArrayList<>(),
                new ArrayList<>())
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
  @Timed(value = "application.claim.search_by_bene")
  public List<Claim> findByBeneXrefSk(
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
    // JPQL doesn't support LIMIT/OFFSET unfortunately, so we have to load this
    // separately.
    // setMaxResults will only limit the results in memory rather than at the
    // database level.

    // We need to get a distinct list of bene_sk values here because there will be
    // duplicates
    // since this is a history table.
    var claimIds =
        entityManager
            .createNativeQuery(
                """
                WITH benes AS (
                    SELECT DISTINCT b.bene_sk
                    FROM idr.beneficiary b
                    WHERE b.bene_xref_efctv_sk_computed = :beneSk
                )
                SELECT c.clm_uniq_id
                FROM idr.claim c
                JOIN benes b ON b.bene_sk = c.bene_sk
                ORDER BY c.clm_uniq_id
                LIMIT :limit
                OFFSET :offset
                """,
                Long.class)
            .setParameter("beneSk", beneSk)
            .setParameter("limit", limit.orElse(5000))
            .setParameter("offset", offset.orElse(0))
            .getResultList();
    var jpql =
        String.format(
            """
            %s
            WHERE c.claimUniqueId IN (:claimIds)
            %s
            """,
            CLAIM_TABLES_BASE, getFilters(claimThroughDate, lastUpdated, tagCriteria));
    var query =
        withParams(
                entityManager.createQuery(jpql, Claim.class),
                claimThroughDate,
                lastUpdated,
                tagCriteria,
                claimTypeCodes)
            .setParameter("claimIds", claimIds);

    var claims = query.getResultList();

    claims.stream()
        .findFirst()
        .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
    return claims;
  }

  private String getFilters(
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      List<List<TagCriterion>> tagCriteria) {
    var sb = new StringBuilder();
    sb.append(
        String.format(
            """
            AND ((cast(:claimThroughDateLowerBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateLowerBound)
            AND ((cast(:claimThroughDateUpperBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateUpperBound)
            AND ((cast(:lastUpdatedLowerBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedLowerBound)
            AND ((cast(:lastUpdatedUpperBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedUpperBound)
            AND (:hasClaimTypeCodes = false OR c.claimTypeCode IN :claimTypeCodes)
            """,
            claimThroughDate.getLowerBoundSqlOperator(),
            claimThroughDate.getUpperBoundSqlOperator(),
            lastUpdated.getLowerBoundSqlOperator(),
            lastUpdated.getUpperBoundSqlOperator()));

    for (var i = 0; i < tagCriteria.size(); i++) {
      var orList = tagCriteria.get(i);
      if (orList.isEmpty()) {
        continue;
      }
      var clauses = new ArrayList<String>();
      for (var j = 0; j < orList.size(); j++) {
        var criterion = orList.get(j);
        switch (criterion) {
          case SourceIdCriterion _ -> clauses.add("c.claimSourceId = :tag_" + i + "_" + j);
          case FinalActionCriterion _ -> clauses.add("c.finalAction = :tag_" + i + "_" + j);
        }
      }
      sb.append(" AND (").append(String.join(" OR ", clauses)).append(")");
    }
    return sb.toString();
  }

  private <T> TypedQuery<T> withParams(
      TypedQuery<T> query,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      List<List<TagCriterion>> tagCriteria,
      List<ClaimTypeCode> claimTypeCodes) {
    query
        .setParameter("claimThroughDateLowerBound", claimThroughDate.getLowerBoundDate())
        .setParameter("claimThroughDateUpperBound", claimThroughDate.getUpperBoundDate())
        .setParameter("lastUpdatedLowerBound", lastUpdated.getLowerBoundDateTime().orElse(null))
        .setParameter("lastUpdatedUpperBound", lastUpdated.getUpperBoundDateTime().orElse(null))
        .setParameter("hasClaimTypeCodes", !claimTypeCodes.isEmpty())
        .setParameter("claimTypeCodes", claimTypeCodes);

    for (var i = 0; i < tagCriteria.size(); i++) {
      var orList = tagCriteria.get(i);
      for (var j = 0; j < orList.size(); j++) {
        var criterion = orList.get(j);
        var paramName = "tag_" + i + "_" + j;
        switch (criterion) {
          case SourceIdCriterion(var sourceId) -> query.setParameter(paramName, sourceId);
          case FinalActionCriterion(var finalAction) -> query.setParameter(paramName, finalAction);
        }
      }
    }
    return query;
  }
}
