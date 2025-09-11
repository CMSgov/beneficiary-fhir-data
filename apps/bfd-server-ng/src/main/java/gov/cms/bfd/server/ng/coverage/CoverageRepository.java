package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.coverage.model.BeneficiaryCoverage;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.interceptor.LoggingInterceptor;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/** Repository for querying coverage information. */
@Repository
@AllArgsConstructor
public class CoverageRepository {
  private EntityManager entityManager;

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInterceptor.class);

  /**
   * Retrieves a {@link BeneficiaryCoverage} record by its ID and last updated timestamp.
   *
   * @param beneSk bene surrogate key
   * @param lastUpdatedRange last updated search range
   * @return beneficiary record
   */
  public Optional<BeneficiaryCoverage> searchBeneficiaryWithCoverage(
      long beneSk, DateTimeRange lastUpdatedRange) {

    Optional<BeneficiaryCoverage> beneficiaryCoverage =
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

    logBeneSkIfPresent(beneficiaryCoverage);
    return beneficiaryCoverage;
  }

  private static void logBeneSkIfPresent(Optional<BeneficiaryCoverage> beneficiaryCoverage) {
    beneficiaryCoverage
        .map(BeneficiaryCoverage::getBeneSk)
        .filter(beneSk -> beneSk != null)
        .ifPresent(
            beneSk -> {
              LOGGER.atInfo().setMessage("bene_sk_requested").addKeyValue("bene_sk", beneSk).log();
            });
  }
}
