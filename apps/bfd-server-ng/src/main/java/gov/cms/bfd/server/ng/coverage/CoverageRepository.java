package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.coverage.model.BeneficiaryCoverage;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressTables;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.LogUtil;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
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
   * @return last updated timestamp for coverage related data or {@link DateUtil#MIN_DATETIME} when
   *     none is available
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

  /**
   * Returns the latest updated timestamp for a beneficiary's coverage data across specific tables.
   *
   * @param beneficiaryId the ID of the beneficiary
   * @return the latest updated timestamp for the beneficiary's coverage data
   */
  public ZonedDateTime coverageLastUpdatedForBene(long beneficiaryId) {

    try {
      var jpql =
          """
          SELECT b
          FROM BeneficiaryCoverage b
          LEFT JOIN FETCH b.beneficiaryStatus bs
          LEFT JOIN FETCH b.beneficiaryEntitlementReason ber
          LEFT JOIN FETCH b.beneficiaryThirdParties tp
          LEFT JOIN FETCH b.beneficiaryEntitlements be
          LEFT JOIN FETCH b.beneficiaryDualEligibility de
          WHERE b.beneSk = :id
            AND b.beneSk = b.xrefSk
          ORDER BY b.obsoleteTimestamp DESC
          """;
      var results =
          entityManager
              .createQuery(jpql, BeneficiaryCoverage.class)
              .setParameter("id", beneficiaryId)
              .getResultList();
      if (results != null && !results.isEmpty()) {
        var bene = results.get(0);
        var latestOpt = bene.coverageUpdatedTimestamps().max(ZonedDateTime::compareTo);
        if (latestOpt.isPresent()) return latestOpt.get();
      }
    } catch (Exception e) {
      // If anything goes wrong (missing table/column, permissions, etc.) fall back to
      // the safer native per table scan below.
    }

    var tables = List.of("idr.beneficiary_status_latest", "idr.beneficiary_third_party_latest");

    return tables.stream()
        .map(table -> fetchLastUpdatedFromTable(table, beneficiaryId))
        .filter(Objects::nonNull)
        .max(ZonedDateTime::compareTo)
        .orElse(DateUtil.MIN_DATETIME);
  }

  private ZonedDateTime fetchLastUpdatedFromTable(String table, long beneficiaryId) {
    var sql = String.format("SELECT MAX(bfd_updated_ts) FROM %s WHERE bene_id = :beneId", table);
    var query = entityManager.createNativeQuery(sql);
    query.setParameter("beneId", beneficiaryId);
    Object res;
    try {
      res = query.getSingleResult();
    } catch (Exception e) {
      // Could be missing table/permission or a DB error
      // return null so caller can fall back.
      return null;
    }
    return convertToZonedDateTime(res);
  }

  private ZonedDateTime convertToZonedDateTime(Object value) {
    if (value == null) return null;
    try {
      if (value instanceof ZonedDateTime zonedDateTime) return zonedDateTime;
      if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toZonedDateTime();
      if (value instanceof Timestamp timestamp) return timestamp.toInstant().atZone(ZoneOffset.UTC);
      return ZonedDateTime.parse(value.toString());
    } catch (Exception ex) {
      // ignore parse failure. Caller will treat as null
      return null;
    }
  }
}
