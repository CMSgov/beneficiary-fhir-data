package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.TypedQuery;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** Repository methods for claims. */
@Component
@AllArgsConstructor
public class ClaimRepository {
  private EntityManager entityManager;

  /**
   * Search for a claim by its ID.
   *
   * @param claimUniqueId claim ID
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

  public List<Claim> findByBeneXrefSk(
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      Optional<Integer> limit,
      Optional<Integer> offset) {
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
                    WHERE c.beneficiary.xrefSk = :beneSk
                    %s
                    ORDER BY c.claimUniqueId
                    """,
                    getDateFilters(claimThroughDate, lastUpdated)),
                Claim.class),
            claimThroughDate,
            lastUpdated)
        .setParameter("beneSk", beneSk)
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
