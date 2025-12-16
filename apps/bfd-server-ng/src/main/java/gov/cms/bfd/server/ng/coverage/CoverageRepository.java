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

    // Note on sorting here. We sort first by active coverage records. Among active records sort by
    // latest begin date. Among future coverage records, sort by nearest begin date. In the case of
    // rx enrollments ,if multiple records have matching begin dates then we sort by latest pdp rx
    // info begin date.

    var beneficiaryCoverage =
        entityManager
            .createQuery(
                String.format(
                    """
                      WITH latestEnrollments AS (
                          SELECT e.id AS id,
                              DENSE_RANK() OVER (
                                  PARTITION BY e.id.enrollmentProgramTypeCode
                                  ORDER BY
                                      CASE
                                          WHEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate <= :today
                                               AND :today <= e.beneficiaryEnrollmentPeriod.enrollmentEndDate
                                          THEN 1
                                          ELSE 2
                                      END ASC,
                                      CASE
                                          WHEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate <= :today
                                               AND :today <= e.beneficiaryEnrollmentPeriod.enrollmentEndDate
                                          THEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate
                                      END DESC,
                                      CASE
                                          WHEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate > :today
                                          THEN e.beneficiaryEnrollmentPeriod.enrollmentEndDate
                                      END ASC
                              ) AS row_num
                          FROM BeneficiaryMAPartDEnrollment e
                          WHERE e.id.beneSk = :beneSk
                      ),
                      latestEnrollmentsRx AS (
                          SELECT rx.id AS id,
                                 rx.enrollmentBeginDate AS enrollmentBeginDate,
                                 rx.contractNumber AS beneContractNumber,
                                 rx.planNumber AS benePbpNumber,
                              ROW_NUMBER() OVER (
                                  PARTITION BY rx.id.beneSk, rx.enrollmentBeginDate, rx.contractNumber, rx.planNumber
                                  ORDER BY
                                      CASE
                                          WHEN rx.enrollmentBeginDate <= :today
                                          THEN 1
                                          ELSE 2
                                      END ASC,
                                      CASE
                                          WHEN rx.enrollmentBeginDate <= :today
                                          THEN rx.enrollmentBeginDate
                                      END DESC,
                                      CASE
                                          WHEN rx.enrollmentBeginDate > :today
                                          THEN rx.enrollmentBeginDate
                                      END ASC,
                                      rx.id.enrollmentPdpRxInfoBeginDate DESC
                              ) AS row_num
                          FROM BeneficiaryMAPartDEnrollmentRx rx
                          WHERE rx.id.beneSk = :beneSk
                      ),
                      latestLis AS (
                          SELECT lis.id AS id,
                              ROW_NUMBER() OVER (
                                  ORDER BY
                                      CASE
                                          WHEN lis.id.benefitRangeBeginDate <= :today
                                               AND :today <= lis.benefitRangeEndDate
                                          THEN 1
                                          ELSE 2
                                      END ASC,
                                      CASE
                                          WHEN lis.id.benefitRangeBeginDate <= :today
                                               AND :today <= lis.benefitRangeEndDate
                                          THEN lis.id.benefitRangeBeginDate
                                      END DESC,
                                      CASE
                                          WHEN lis.id.benefitRangeBeginDate > :today
                                            THEN lis.id.benefitRangeBeginDate
                                      END ASC
                              ) AS row_num
                          FROM BeneficiaryLowIncomeSubsidy lis
                          WHERE lis.id.beneSk = :beneSk
                      )
                      SELECT b
                      FROM BeneficiaryCoverage b
                      LEFT JOIN FETCH b.beneficiaryStatus bs
                      LEFT JOIN FETCH b.beneficiaryEntitlementReason ber
                      LEFT JOIN FETCH b.beneficiaryThirdParties tp
                      LEFT JOIN FETCH b.beneficiaryEntitlements be
                      LEFT JOIN FETCH b.beneficiaryDualEligibility de
                      LEFT JOIN FETCH b.beneficiaryMAPartDEnrollments ben
                      LEFT JOIN FETCH ben.enrollmentContract c
                      LEFT JOIN FETCH c.contractPlanContactInfo cc
                      LEFT JOIN FETCH b.beneficiaryMAPartDEnrollmentsRx berx
                      LEFT JOIN FETCH b.beneficiaryLowIncomeSubsidies blis
                      WHERE b.beneSk = :id
                        AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
                        AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
                        AND b.beneSk = b.xrefSk
                        AND (ben IS NULL
                            OR EXISTS (
                            SELECT 1 FROM latestEnrollments e
                            WHERE e.row_num = 1
                                AND e.id.beneSk = ben.id.beneSk
                                AND e.id.enrollmentBeginDate = ben.id.enrollmentBeginDate
                                AND e.id.enrollmentProgramTypeCode = ben.id.enrollmentProgramTypeCode
                        ))
                        AND ((ben.id.enrollmentProgramTypeCode <> '2'
                            AND ben.id.enrollmentProgramTypeCode <> '3')
                            OR berx IS NULL
                            OR EXISTS (
                            SELECT 1 FROM latestEnrollmentsRx e
                            WHERE e.row_num = 1
                                AND e.id.beneSk = ben.id.beneSk
                                AND e.enrollmentBeginDate = ben.id.enrollmentBeginDate
                                AND e.beneContractNumber = ben.contractNumber
                                AND e.benePbpNumber = ben.planNumber
                                AND e.id.enrollmentPdpRxInfoBeginDate = berx.id.enrollmentPdpRxInfoBeginDate
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
