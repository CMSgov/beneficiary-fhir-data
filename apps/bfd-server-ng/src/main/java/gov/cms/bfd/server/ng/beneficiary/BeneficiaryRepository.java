package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryIdentity;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.interceptor.LoggingInterceptor;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/** Repository for querying beneficiary information. */
@Repository
@AllArgsConstructor
public class BeneficiaryRepository {
  private EntityManager entityManager;

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInterceptor.class);

  /**
   * Queries for current and historical MBIs and BENE_SKs, along with their start/end dates.
   * Beneficiary records with kill credit switch set to "1" or overshare mbi are filtered out
   *
   * @param beneXrefSk computed bene surrogate key
   * @return list of patient identities representing all active identities connected to the bene
   *     record
   */
  public List<BeneficiaryIdentity> getValidBeneficiaryIdentities(long beneXrefSk) {
    return entityManager
        .createQuery(
            """
            SELECT identity
            FROM BeneficiaryIdentity identity
            WHERE identity.xrefSk = :beneXrefSk
            """,
            BeneficiaryIdentity.class)
        .setParameter("beneXrefSk", beneXrefSk)
        .getResultList();
  }

  /**
   * Retrieves a {@link Beneficiary} record by its ID and last updated timestamp.
   *
   * @param beneSk bene surrogate key
   * @param lastUpdatedRange last updated search range
   * @return beneficiary record
   */
  public Optional<Beneficiary> findById(long beneSk, DateTimeRange lastUpdatedRange) {
    Optional<Beneficiary> optionalBeneficiary =
        entityManager
            .createQuery(
                String.format(
                    """
              SELECT bene
              FROM Beneficiary bene
              WHERE bene.beneSk = :beneSk
                AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR bene.meta.updatedTimestamp %s :lowerBound)
                AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR bene.meta.updatedTimestamp %s :upperBound)
              ORDER BY bene.obsoleteTimestamp DESC
              """,
                    lastUpdatedRange.getLowerBoundSqlOperator(),
                    lastUpdatedRange.getUpperBoundSqlOperator()),
                Beneficiary.class)
            .setParameter("beneSk", beneSk)
            .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
            .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
            .getResultList()
            .stream()
            .findFirst();

    logBeneSkIfPresent(optionalBeneficiary);
    return optionalBeneficiary;
  }

  /**
   * Retrieves the xrefSk from the beneSk.
   *
   * @param beneSk original beneSk
   * @return xrefSk for the bene
   */
  public Optional<Long> getXrefSkFromBeneSk(long beneSk) {
    return entityManager
        .createQuery(
            """
              SELECT bene.xrefSk
              FROM Beneficiary bene
              WHERE bene.beneSk = :beneSk
            """,
            Long.class)
        .setParameter("beneSk", beneSk)
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Retrieves the xrefSk from the mbi.
   *
   * @param mbi original beneSk
   * @return xrefSk for the bene
   */
  public Optional<Long> getXrefSkFromMbi(String mbi) {
    return entityManager
        .createQuery(
            """
            SELECT bene.xrefSk
            FROM Beneficiary bene
            WHERE bene.identifier.mbi = :mbi
          """,
            Long.class)
        .setParameter("mbi", mbi)
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Returns the last updated timestamp for the beneficiary data ingestion process.
   *
   * @return last updated timestamp
   */
  public ZonedDateTime beneficiaryLastUpdated() {
    return entityManager
        .createQuery(
            """
            SELECT MAX(p.batchCompletionTimestamp)
            FROM LoadProgress p
            WHERE p.tableName LIKE 'idr.beneficiary%'
            """,
            ZonedDateTime.class)
        .getResultList()
        .stream()
        .findFirst()
        .orElse(DateUtil.MIN_DATETIME);
  }

  private static void logBeneSkIfPresent(Optional<Beneficiary> beneficiaryCoverage) {
    beneficiaryCoverage
        .map(Beneficiary::getBeneSk)
        .filter(beneSk -> beneSk != null)
        .ifPresent(
            beneSk -> {
              LOGGER.atInfo().setMessage("bene_sk_requested").addKeyValue("bene_sk", beneSk).log();
            });
  }
}
