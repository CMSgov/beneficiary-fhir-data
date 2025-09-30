package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressLastUpdatedProvider;
import gov.cms.bfd.server.ng.util.LogUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimRepository {
  private final EntityManager entityManager;
  private final LoadProgressLastUpdatedProvider loadProgressLastUpdatedProvider;

  private static final String CLAIM_TABLES_BASE =
      """
      SELECT c
      FROM Claim c
      JOIN FETCH c.beneficiary b
      JOIN FETCH c.claimDateSignature AS cds
      JOIN FETCH c.claimItems AS cl
      LEFT JOIN FETCH c.claimInstitutional ci
      LEFT JOIN FETCH cl.claimLineInstitutional cli
      LEFT JOIN FETCH cli.ansiSignature a
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
    return queryById(claimUniqueId, claimThroughDate, lastUpdated, CLAIM_TABLES_BASE);
  }

  /**
   * Returns claims for the given beneficiary.
   *
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param limit limit
   * @param offset offset
   * @param sourceIds claim sourceIds
   * @return claims
   */
  public List<Claim> findByBeneXrefSk(
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      Optional<Integer> limit,
      Optional<Integer> offset,
      List<ClaimSourceId> sourceIds) {
    // JPQL doesn't support LIMIT/OFFSET unfortunately, so we have to load this separately.
    // setMaxResults will only limit the results in memory rather than at the database level.
    var claimIds =
        entityManager
            .createNativeQuery(
                """
                        SELECT c.clm_uniq_id
                        FROM idr.claim c
                        JOIN idr.beneficiary b ON b.bene_sk = c.bene_sk
                        WHERE b.bene_xref_efctv_sk_computed = :beneSk
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
            CLAIM_TABLES_BASE, getFilters(claimThroughDate, lastUpdated));
    var claims =
        withParams(
                entityManager.createQuery(jpql, Claim.class),
                claimThroughDate,
                lastUpdated,
                sourceIds)
            .setParameter("claimIds", claimIds)
            .getResultList();

    claims.stream()
        .findFirst()
        .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
    return claims;
  }

  /**
   * Returns the most recent batch completion timestamp across all LoadProgress rows.
   *
   * @return the most recent timestamp.
   */
  public ZonedDateTime claimLastUpdated() {
    return loadProgressLastUpdatedProvider.lastUpdated();
  }

  private String getFilters(DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
    return String.format(
        """
        AND ((cast(:claimThroughDateLowerBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateLowerBound)
        AND ((cast(:claimThroughDateUpperBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateUpperBound)
        AND ((cast(:lastUpdatedLowerBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedLowerBound)
        AND ((cast(:lastUpdatedUpperBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedUpperBound)
        AND (:hasSourceIds = false OR c.claimSourceId IN :sourceIds)
        """,
        claimThroughDate.getLowerBoundSqlOperator(),
        claimThroughDate.getUpperBoundSqlOperator(),
        lastUpdated.getLowerBoundSqlOperator(),
        lastUpdated.getUpperBoundSqlOperator());
  }

  private <T> TypedQuery<T> withParams(
      TypedQuery<T> query,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      List<ClaimSourceId> sourceIds) {
    return query
        .setParameter(
            "claimThroughDateLowerBound", claimThroughDate.getLowerBoundDate().orElse(null))
        .setParameter(
            "claimThroughDateUpperBound", claimThroughDate.getUpperBoundDate().orElse(null))
        .setParameter("lastUpdatedLowerBound", lastUpdated.getLowerBoundDateTime().orElse(null))
        .setParameter("lastUpdatedUpperBound", lastUpdated.getUpperBoundDateTime().orElse(null))
        .setParameter("hasSourceIds", !sourceIds.isEmpty())
        .setParameter("sourceIds", sourceIds);
  }

  private Optional<Claim> queryById(
      long claimUniqueId, DateTimeRange claimThroughDate, DateTimeRange lastUpdated, String base) {
    var jpql =
        String.format(
            """
              %s
              WHERE c.claimUniqueId = :claimUniqueId
              %s
            """,
            base, getFilters(claimThroughDate, lastUpdated));
    var results =
        withParams(
                entityManager.createQuery(jpql, Claim.class),
                claimThroughDate,
                lastUpdated,
                new ArrayList<>())
            .setParameter("claimUniqueId", claimUniqueId)
            .getResultList();

    return results.stream().findFirst();
  }
}
