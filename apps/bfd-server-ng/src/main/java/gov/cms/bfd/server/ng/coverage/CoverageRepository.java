package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.coverage.model.BeneficiaryCoverage;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.LogUtil;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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
    var today = LocalDate.now();

    // Note on sorting here. Although we filter out inactive enrollments we need to handle both
    // active and future coverages. We sort first by active coverage records by latest begin date.
    // In the case of rx enrollments, if multiple records have matching begin dates then we sort by
    // latest pdp rx info begin date.

    var beneficiaryCoverage =
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
                      LEFT JOIN FETCH b.beneficiaryStatus bs
                      LEFT JOIN FETCH b.beneficiaryEntitlementReason ber
                      LEFT JOIN FETCH b.beneficiaryThirdParties tp
                      LEFT JOIN FETCH b.beneficiaryEntitlements be
                      LEFT JOIN FETCH b.beneficiaryDualEligibility de
                      LEFT JOIN FETCH b.beneficiaryPartCDEnrollments ben
                      LEFT JOIN FETCH ben.enrollmentContract c
                      LEFT JOIN FETCH c.contractPlanContactInfo cc
                      LEFT JOIN FETCH b.beneficiaryLowIncomeSubsidies blis
                      WHERE b.beneSk = :id
                        AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
                        AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
                        AND b.beneSk = b.xrefSk
                        AND (ben IS NULL
                            OR EXISTS (
                            SELECT 1 FROM latestPartCDEnrollments e
                            WHERE e.row_num = 1
                                AND e.id.beneSk = ben.id.beneSk
                                AND e.id.enrollmentBeginDate = ben.id.enrollmentBeginDate
                                AND e.id.enrollmentProgramTypeCode = ben.id.enrollmentProgramTypeCode
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
                    lastUpdatedRange.getLowerBoundSqlOperator(),
                    lastUpdatedRange.getUpperBoundSqlOperator()),
                BeneficiaryCoverage.class)
            .setParameter("id", beneSk)
            .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
            .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
            .setParameter("today", today)
            .setParameter("beneSk", beneSk)
            .getResultList()
            .stream()
            .findFirst();

    beneficiaryCoverage.ifPresent(coverage -> LogUtil.logBeneSk(coverage.getBeneSk()));
    return beneficiaryCoverage;
  }
}
