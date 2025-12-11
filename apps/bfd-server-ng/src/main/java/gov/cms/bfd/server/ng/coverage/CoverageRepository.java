package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.coverage.model.*;
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
                      WITH lastestEnrollments AS (
                                      SELECT e.id AS id,
                                          row_number() over (
                                              ORDER BY
                                                  CASE
                                                      WHEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate <= :today THEN 1
                                                      ELSE 2
                                                  END ASC,
                                                  e.beneficiaryEnrollmentPeriod.enrollmentBeginDate desc
                                          ) AS rn
                                      FROM BeneficiaryMAPartDEnrollment e
                                      WHERE e.id.beneSk = :beneSk
                      ),
                      lastestEnrollmentsRx AS (
                                      SELECT rx AS id,
                                          row_number() over (
                                              ORDER BY
                                                  CASE
                                                      WHEN rx.enrollmentBeginDate <= :today THEN 1
                                                      ELSE 2
                                                  END ASC,
                                                  rx.enrollmentBeginDate desc
                                          ) AS rn
                                      FROM BeneficiaryMAPartDEnrollmentRx rx
                                      WHERE rx.id.beneSk = :beneSk
                      ),
                      lastestLis AS (
                                      SELECT lis.id AS id,
                                          row_number() over (
                                              ORDER BY
                                                  CASE
                                                      WHEN lis.id.benefitRangeBeginDate <= :today THEN 1
                                                      ELSE 2
                                                  END ASC,
                                                  lis.id.benefitRangeBeginDate desc
                                          ) AS rn
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
                        AND ben.id in (select id from lastestEnrollments where rn = 1)
                        AND berx.id in (select id from lastestEnrollmentsRx where rn = 1)
                        AND blis.id in (select id from lastestLis where rn = 1)
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

  /*public Optional<BeneficiaryCoverage> searchBeneficiaryWithCoverage(
      long beneSk, DateTimeRange lastUpdatedRange) {

    LocalDate today = LocalDate.now();
    Optional<BeneficiaryEnrollmentId> latestEnrollmentId = findLatestEnrollmentId(beneSk, today);
    Optional<BeneficiaryMAPartDEnrollmentRxId> latestRxId = findLatestEnrollmentRxId(beneSk, today);
    Optional<BeneficiaryLowIncomeSubsidyId> latestLisId =
        findLatestLowIncomeSubsidyId(beneSk, today);

    String baseQuery =
        """
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
            """;

    if (latestEnrollmentId.isPresent()) {
      baseQuery += " AND ben.id = :latestEnrollmentId";
    }

    if (latestRxId.isPresent()) {
      baseQuery += " AND berx.id = :latestRxId";
    }

    if (latestLisId.isPresent()) {
      baseQuery += " AND blis.id = :latestLisId";
    }

    baseQuery += " ORDER BY b.obsoleteTimestamp DESC";

    TypedQuery<BeneficiaryCoverage> query =
        entityManager
            .createQuery(
                String.format(
                    baseQuery,
                    lastUpdatedRange.getLowerBoundSqlOperator(),
                    lastUpdatedRange.getUpperBoundSqlOperator()),
                BeneficiaryCoverage.class)
            .setParameter("id", beneSk)
            .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
            .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null));

    latestEnrollmentId.ifPresent(id -> query.setParameter("latestEnrollmentId", id));
    latestRxId.ifPresent(id -> query.setParameter("latestRxId", id));
    latestLisId.ifPresent(id -> query.setParameter("latestLisId", id));

    var beneficiaryCoverage = query.getResultList().stream().findFirst();

    beneficiaryCoverage.ifPresent(coverage -> LogUtil.logBeneSk(coverage.getBeneSk()));
    return beneficiaryCoverage;
  }

  public Optional<BeneficiaryEnrollmentId> findLatestEnrollmentId(long beneSk, LocalDate today) {
    return entityManager
        .createQuery(
            """
                                  SELECT e.id
                                  FROM BeneficiaryMAPartDEnrollment e
                                  WHERE e.id.beneSk = :beneSk
                                  ORDER BY
                                    CASE
                                        WHEN e.beneficiaryEnrollmentPeriod.enrollmentBeginDate <= :today THEN 1
                                        ELSE 2
                                    END ASC,
                                    e.beneficiaryEnrollmentPeriod.enrollmentBeginDate desc
                                  """,
            BeneficiaryEnrollmentId.class)
        .setParameter("today", today)
        .setParameter("beneSk", beneSk)
        .getResultList()
        .stream()
        .findFirst();
  }

  public Optional<BeneficiaryMAPartDEnrollmentRxId> findLatestEnrollmentRxId(
      long beneSk, LocalDate today) {
    return entityManager
        .createQuery(
            """
                        SELECT e.id
                        FROM BeneficiaryMAPartDEnrollmentRx e
                        WHERE e.id.beneSk = :beneSk
                        ORDER BY
                          CASE
                              WHEN e.enrollmentBeginDate <= :today THEN 1
                              ELSE 2
                          END ASC,
                          e.enrollmentBeginDate desc
                        """,
            BeneficiaryMAPartDEnrollmentRxId.class)
        .setParameter("today", today)
        .setParameter("beneSk", beneSk)
        .getResultList()
        .stream()
        .findFirst();
  }

  public Optional<BeneficiaryLowIncomeSubsidyId> findLatestLowIncomeSubsidyId(
      long beneSk, LocalDate today) {
    return entityManager
        .createQuery(
            """
                        SELECT lis.id
                        FROM BeneficiaryLowIncomeSubsidy lis
                        WHERE lis.id.beneSk = :beneSk
                        ORDER BY
                          CASE
                              WHEN lis.id.benefitRangeBeginDate <= :today THEN 1
                              ELSE 2
                          END ASC,
                          lis.id.benefitRangeBeginDate desc
                        """,
            BeneficiaryLowIncomeSubsidyId.class)
        .setParameter("today", today)
        .setParameter("beneSk", beneSk)
        .getResultList()
        .stream()
        .findFirst();
  }*/
}
