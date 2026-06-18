package gov.cms.bfd.server.ng.coverage;

import static gov.cms.bfd.server.ng.util.MetricTimer.*;

import gov.cms.bfd.server.ng.coverage.model.BeneficiaryCoverage;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.CoverageSearchCriteria;
import gov.cms.bfd.server.ng.log.QueryTelemetryUtil;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.MetricTimer;
import io.micrometer.core.aop.MeterTag;
import io.micrometer.core.instrument.Tags;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for querying coverage information. */
@Transactional(readOnly = true)
@Repository
@AllArgsConstructor
public class CoverageRepository {
  @PersistenceContext private EntityManager entityManager;
  private final DateUtil dateUtil;
  private final QueryTelemetryUtil queryTelemetryUtil;
  private final MetricTimer metricTimer;

  /**
   * Retrieves a {@link BeneficiaryCoverage} record by its ID and last updated timestamp.
   *
   * @param criteria Coverage search criteria
   * @return beneficiary record
   */
  public Optional<BeneficiaryCoverage> searchBeneficiaryWithCoverage(
      @MeterTag(key = "hasLastUpdated", expression = "hasLastUpdated()")
          CoverageSearchCriteria criteria) {
    var benefitDate = dateUtil.nowAoe();

    // Note on sorting here. Although we filter out inactive enrollments we need to handle both
    // active and future coverages. We sort first by active coverage records by latest begin date.
    // In the case of rx enrollments, if multiple records have matching begin dates then we sort by
    // latest pdp rx info begin date.

    var query =
        entityManager
            .createQuery(
                String.format(
                    """
                        WITH latestPartCDEnrollments AS (
                            SELECT e.id AS id,
                                ROW_NUMBER() OVER (
                                    PARTITION BY
                                      e.id.beneSk,
                                      e.id.enrollmentProgramTypeCode
                                    ORDER BY
                                        CASE
                                            WHEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate <= :today
                                                 AND (e.beneficiaryEnrollmentPeriod.enrollmentEndDate IS NULL
                                                      OR :today <= e.beneficiaryEnrollmentPeriod.enrollmentEndDate)
                                            THEN 1
                                            ELSE 2
                                        END,
                                        e.beneficiaryEnrollmentPeriod.enrollmentBeginDate DESC,
                                        e.id.enrollmentPdpRxInfoBeginDate DESC
                                ) AS row_num
                            FROM BeneficiaryPartCDEnrollment e
                            WHERE e.id.beneSk = :beneSk
                            AND e.beneficiaryEnrollmentPeriod.enrollmentEndDate >= :today
                        ),
                        latestLis AS (
                            SELECT lis.id AS id,
                                ROW_NUMBER() OVER (
                                    ORDER BY lis.id.benefitRangeBeginDate DESC
                                ) AS row_num
                            FROM BeneficiaryLowIncomeSubsidy lis
                            WHERE lis.id.beneSk = :beneSk
                            AND lis.benefitRangeEndDate >= :today
                        )
                        SELECT b
                        FROM BeneficiaryCoverage b
                        LEFT JOIN FETCH b.coverageOptional.beneficiaryStatus bs
                        LEFT JOIN FETCH b.coverageOptional.beneficiaryEntitlementReason ber
                        LEFT JOIN FETCH b.beneficiaryThirdParties tp
                        LEFT JOIN FETCH b.beneficiaryEntitlements be
                        LEFT JOIN FETCH b.coverageOptional.beneficiaryDualEligibility de
                        LEFT JOIN FETCH b.beneficiaryPartCDEnrollments ben
                        LEFT JOIN FETCH ben.enrollmentOptional.enrollmentContract c
                        LEFT JOIN FETCH c.contractOptional.contractPlanContactInfo cc
                        LEFT JOIN FETCH b.beneficiaryLowIncomeSubsidies blis
                        WHERE b.beneSk = :beneSk
                          AND (
                              CAST(:lowerBound AS ZonedDateTime) IS NULL
                              OR GREATEST(
                                    b.meta.partACoverageUpdatedTs,
                                    b.meta.partBCoverageUpdatedTs,
                                    b.meta.partCCoverageUpdatedTs,
                                    b.meta.partDCoverageUpdatedTs,
                                    b.meta.partDualCoverageUpdatedTs
                                ) %s :lowerBound
                            )
                          AND (
                              CAST(:upperBound AS ZonedDateTime) IS NULL
                              OR LEAST(
                                    b.meta.partACoverageUpdatedTs,
                                    b.meta.partBCoverageUpdatedTs,
                                    b.meta.partCCoverageUpdatedTs,
                                    b.meta.partDCoverageUpdatedTs,
                                    b.meta.partDualCoverageUpdatedTs
                                ) %s :upperBound
                            )
                          AND b.beneSk = b.xrefSk
                          AND (ben IS NULL
                              OR EXISTS (
                              SELECT 1 FROM latestPartCDEnrollments e
                              WHERE e.row_num = 1
                                  AND e.id.beneSk = ben.id.beneSk
                                  AND e.id.enrollmentBeginDate = ben.id.enrollmentBeginDate
                                  AND e.id.enrollmentProgramTypeCode = ben.id.enrollmentProgramTypeCode
                                  AND e.id.enrollmentPdpRxInfoBeginDate = ben.id.enrollmentPdpRxInfoBeginDate
                          ))
                          AND (blis IS NULL
                              OR EXISTS (
                              SELECT 1 FROM latestLis e
                              WHERE e.row_num = 1
                                  AND e.id.beneSk = blis.id.beneSk
                                  AND e.id.benefitRangeBeginDate = blis.id.benefitRangeBeginDate
                          ))
                        ORDER BY b.obsoleteTimestamp DESC
                      """,
                    criteria.lastUpdated().getLowerBoundSqlOperator(),
                    criteria.lastUpdated().getUpperBoundSqlOperator()),
                BeneficiaryCoverage.class)
            .setParameter("lowerBound", criteria.lastUpdated().getLowerBoundDateTime().orElse(null))
            .setParameter("upperBound", criteria.lastUpdated().getUpperBoundDateTime().orElse(null))
            .setParameter("today", benefitDate)
            .setParameter("beneSk", criteria.beneSk());

    return metricTimer.recordMetric(
        "application.coverage.search_by_bene",
        () ->
            queryTelemetryUtil.executeAndTrack("searchBeneficiaryWithCoverage", query).stream()
                .findFirst(),
        coverageOptional -> {
          if (coverageOptional.isEmpty()) {
            return Tags.empty();
          }
          var coverage = coverageOptional.get();

          return Tags.of(
              HAS_PART_C,
              String.valueOf(coverage.getEnrollment(CoveragePart.PART_C).isPresent()),
              HAS_PART_D,
              String.valueOf(coverage.getEnrollment(CoveragePart.PART_D).isPresent()),
              HAS_LIS,
              String.valueOf(!coverage.getBeneficiaryLowIncomeSubsidies().isEmpty()));
        });
  }
}
