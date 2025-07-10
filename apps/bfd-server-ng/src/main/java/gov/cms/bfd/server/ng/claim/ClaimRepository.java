package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimRepository {
  private EntityManager entityManager;

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
    return withDateParams(
            entityManager.createQuery(
                String.format(
                    """
                    SELECT c
                    FROM Claim c
                    JOIN c.claimLines cl
                    JOIN c.claimDateSignature cds
                    JOIN c.claimProcedures cp
                    LEFT JOIN c.claimInstitutional ci
                    LEFT JOIN cl.claimLineInstitutional cli
                    LEFT JOIN cli.ansiSignature as
                    LEFT JOIN c.claimValues cv
                    WHERE c.claimUniqueId = :claimUniqueId
                    %s
                    """,
                    getDateFilters(claimThroughDate, lastUpdated)),
                Claim.class),
            claimThroughDate,
            lastUpdated)
        .setParameter("claimUniqueId", claimUniqueId)
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Returns claims for the given beneficiary.
   *
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param limit limit
   * @param offset offset
   * @param queryParameterValue queryParameterValue
   * @return claims
   */
  public List<Claim> findByBeneXrefSk(
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      Optional<Integer> limit,
      Optional<Integer> offset,
      Optional<List<ClaimSourceId>> queryParameterValue) {

    StringBuilder jpqlBuilder =
        new StringBuilder(
            """
            SELECT c
            FROM Claim c
            JOIN c.claimLines cl
            JOIN c.claimDateSignature cds
            JOIN c.claimProcedures cp
            LEFT JOIN c.claimInstitutional ci
            LEFT JOIN cl.claimLineInstitutional cli
            LEFT JOIN cli.ansiSignature as
            LEFT JOIN c.claimValues cv
            WHERE c.beneficiary.xrefSk = :beneSk
            """);

    jpqlBuilder.append(getDateFilters(claimThroughDate, lastUpdated));

    if (queryParameterValue.isPresent() && !queryParameterValue.get().isEmpty()) {
      jpqlBuilder.append(" AND c.claimSourceId IN :sourceIds");
    }

    // Add the final ordering.
    jpqlBuilder.append(" ORDER BY c.claimUniqueId");

    TypedQuery<Claim> query = entityManager.createQuery(jpqlBuilder.toString(), Claim.class);

    query.setParameter("beneSk", beneSk);

    queryParameterValue.ifPresent(
        sourceIds -> {
          if (!sourceIds.isEmpty()) {
            query.setParameter("sourceIds", sourceIds);
          }
        });

    TypedQuery<Claim> finalQuery = withDateParams(query, claimThroughDate, lastUpdated);

    return finalQuery
        .setMaxResults(limit.orElse(5000))
        .setFirstResult(offset.orElse(0))
        .getResultList();
  }

  /**
   * Returns the last updated timestamp for the claims data ingestion process.
   *
   * @return last updated timestamp
   */
  public ZonedDateTime claimLastUpdated() {
    return entityManager
        .createQuery(
            """
            SELECT MAX(p.batchCompletionTimestamp)
            FROM LoadProgress p
            WHERE p.tableName LIKE 'idr.claim%'
            """,
            ZonedDateTime.class)
        .getResultList()
        .stream()
        .findFirst()
        .orElse(DateUtil.MIN_DATETIME);
  }

  private String getDateFilters(DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
    return String.format(
        """
        AND ((cast(:claimThroughDateLowerBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateLowerBound)
        AND ((cast(:claimThroughDateUpperBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateUpperBound)
        AND ((cast(:lastUpdatedLowerBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedLowerBound)
        AND ((cast(:lastUpdatedUpperBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedUpperBound)
        """,
        claimThroughDate.getLowerBoundSqlOperator(),
        claimThroughDate.getUpperBoundSqlOperator(),
        lastUpdated.getLowerBoundSqlOperator(),
        lastUpdated.getUpperBoundSqlOperator());
  }

  private <T> TypedQuery<T> withDateParams(
      TypedQuery<T> query, DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
    return query
        .setParameter(
            "claimThroughDateLowerBound", claimThroughDate.getLowerBoundDate().orElse(null))
        .setParameter(
            "claimThroughDateUpperBound", claimThroughDate.getUpperBoundDate().orElse(null))
        .setParameter("lastUpdatedLowerBound", lastUpdated.getLowerBoundDateTime().orElse(null))
        .setParameter("lastUpdatedUpperBound", lastUpdated.getUpperBoundDateTime().orElse(null));
  }
}
