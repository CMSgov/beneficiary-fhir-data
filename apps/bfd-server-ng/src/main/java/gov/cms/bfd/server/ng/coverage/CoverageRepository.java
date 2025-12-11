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
    LocalDate today = LocalDate.now();

    var beneficiaryCoverage =
        entityManager
            .createQuery(
                String.format(
                    """
                      WITH latestEnrollments AS (
                          SELECT e.id AS id,
                              row_number() over (
                                  ORDER BY
                                      CASE
                                          WHEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate <= :today THEN 1
                                          ELSE 2
                                      END ASC,
                                      e.beneficiaryEnrollmentPeriod.enrollmentBeginDate desc
                              ) AS row_num
                          FROM BeneficiaryMAPartDEnrollment e
                          WHERE e.id.beneSk = :beneSk
                      ),
                      latestEnrollmentsRx AS (
                          SELECT rx.id AS id,
                              row_number() over (
                                  ORDER BY
                                      CASE
                                          WHEN rx.enrollmentBeginDate <= :today THEN 1
                                          ELSE 2
                                      END ASC,
                                      rx.enrollmentBeginDate desc
                              ) AS row_num
                          FROM BeneficiaryMAPartDEnrollmentRx rx
                          WHERE rx.id.beneSk = :beneSk
                      ),
                      latestLis AS (
                          SELECT lis.id AS id,
                              row_number() over (
                                  ORDER BY
                                      CASE
                                          WHEN lis.id.benefitRangeBeginDate <= :today THEN 1
                                          ELSE 2
                                      END ASC,
                                      lis.id.benefitRangeBeginDate desc
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
                        AND (ben IS NULL OR EXISTS (
                            select 1 from latestEnrollments e
                            where e.row_num = 1
                                and e.id.beneSk = ben.id.beneSk
                                and e.id.enrollmentBeginDate = ben.id.enrollmentBeginDate
                                and e.id.enrollmentProgramTypeCode = ben.id.enrollmentProgramTypeCode
                        ))
                        AND (berx IS NULL OR EXISTS (
                            select 1 from latestEnrollmentsRx e
                            where e.row_num = 1
                                and e.id.beneSk = berx.id.beneSk
                                and e.id.enrollmentPdpRxInfoBeginDate = berx.id.enrollmentPdpRxInfoBeginDate
                        ))
                        AND (blis IS NULL OR EXISTS (
                            select 1 from latestLis e
                            where e.row_num = 1
                                and e.id.beneSk = blis.id.beneSk
                                and e.id.benefitRangeBeginDate = blis.id.benefitRangeBeginDate
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
