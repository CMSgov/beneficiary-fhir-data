package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.coverage.model.BeneficiaryCoverage;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressTables;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.LogUtil;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository for querying coverage information. */
@Repository
@AllArgsConstructor
public class CoverageRepository {
  private final EntityManager entityManager;

  /**
   * Retrieves a {@link BeneficiaryCoverage} record by its ID and last updated timestamp.
   *
   * @param beneSk bene surrogate key
   * @param lastUpdatedRange last updated search range
   * @return beneficiary record
   */
  public Optional<BeneficiaryCoverage> searchBeneficiaryWithCoverage(
      long beneSk, DateTimeRange lastUpdatedRange) {

    var beneficiaryCoverage =
        entityManager
            .createQuery(
                String.format(
                    """
                SELECT b
                FROM BeneficiaryCoverage b
                LEFT JOIN FETCH b.beneficiaryStatus bs
                LEFT JOIN FETCH b.beneficiaryEntitlementReason ber
                LEFT JOIN FETCH b.beneficiaryThirdParties tp
                LEFT JOIN FETCH b.beneficiaryEntitlements be
                LEFT JOIN FETCH b.beneficiaryDualEligibility de
                WHERE b.beneSk = :id
                  AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
                  AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
                  AND b.beneSk = b.xrefSk
                ORDER BY b.obsoleteTimestamp DESC
                """,
                    lastUpdatedRange.getLowerBoundSqlOperator(),
                    lastUpdatedRange.getUpperBoundSqlOperator()),
                BeneficiaryCoverage.class)
            .setParameter("id", beneSk)
            .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
            .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
            .getResultList()
            .stream()
            .findFirst();

    beneficiaryCoverage.ifPresent(coverage -> LogUtil.logBeneSk(coverage.getBeneSk()));
    return beneficiaryCoverage;
  }

  /**
   * Returns the last updated timestamp for all coverage related tables.
   *
   * <p>Uses the {@code load_progress.batch_completion_timestamp} max across the set of coverage
   * tables defined in {@link LoadProgressTables#coverageTablePrefixes()}.
   *
   * @return last updated timestamp for coverage
   */
  public ZonedDateTime coverageLastUpdated() {
    var prefixes = LoadProgressTables.coverageTablePrefixes();
    return entityManager
        .createQuery(
            """
            SELECT MAX(p.batchCompletionTimestamp)
            FROM LoadProgress p
            WHERE p.tableName IN :tables
            """,
            ZonedDateTime.class)
        .setParameter("tables", prefixes)
        .getResultList()
        .stream()
        .findFirst()
        .orElse(DateUtil.MIN_DATETIME);
  }
}
