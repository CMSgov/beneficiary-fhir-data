package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.coverage.model.BeneficiaryCoverage;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.patient.PatientIdentity;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository for querying beneficiary information. */
@Repository
@AllArgsConstructor
public class BeneficiaryRepository {
  private EntityManager entityManager;

  /**
   * Queries for current and historical MBIs and BENE_SKs, along with their start/end dates.
   * Beneficiary records with kill credit switch set to "1" or overshare mbi are filtered out
   *
   * @param beneXrefSk computed bene surrogate key
   * @return list of patient identities representing all active identities connected to the bene
   *     record
   */
  public List<PatientIdentity> getValidBeneficiaryIdentities(long beneXrefSk) {
    return entityManager
        .createQuery(
            """
            SELECT new PatientIdentity(
                ROW_NUMBER() OVER (ORDER BY bene.beneSk) rowId,
                bene.beneSk,
                bene.xrefSk,
                bene.identity.mbi,
                mbiId.effectiveDate,
                mbiId.obsoleteDate
              )
            FROM Beneficiary bene
            LEFT JOIN BeneficiaryMbiId mbiId
              ON bene.identity.mbi = mbiId.mbi
              AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
            WHERE bene.xrefSk = :beneXrefSk
            AND NOT EXISTS (
                SELECT 1
                FROM BeneficiaryXref bx
                WHERE bx.beneSk = bene.beneSk
                  AND bx.beneXrefSk = bene.xrefSk
                  AND bx.beneKillCred = '1'
            )
            GROUP BY bene.beneSk, bene.xrefSk, bene.identity.mbi, mbiId.effectiveDate, mbiId.obsoleteDate
            """,
            PatientIdentity.class)
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
    return searchBeneficiary("beneSk", String.valueOf(beneSk), lastUpdatedRange);
  }

  /**
   * Retrieves a {@link Beneficiary} record by its MBI and last updated timestamp.
   *
   * @param mbi bene MBI
   * @param lastUpdatedRange last updated search range
   * @return beneficiary record
   */
  public Optional<Beneficiary> findByIdentifier(String mbi, DateTimeRange lastUpdatedRange) {
    return searchBeneficiary("identity.mbi", mbi, lastUpdatedRange);
  }

  /**
   * Retrieves a {@link Beneficiary} record by its ID and last updated timestamp.
   *
   * @param beneSk bene surrogate key
   * @param lastUpdatedRange last updated search range
   * @return beneficiary record
   */
  public Optional<BeneficiaryCoverage> searchBeneficiaryWithCoverage(
      long beneSk, DateTimeRange lastUpdatedRange) {
    return searchBeneficiaryWithCoverage(String.valueOf(beneSk), lastUpdatedRange);
  }

  /**
   * Retrieves the xrefSk from the beneSk.
   *
   * @param beneSk original beneSk
   * @return xrefSk for the bene
   */
  public Optional<Long> getXrefBeneSk(long beneSk) {
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

  private Optional<Beneficiary> searchBeneficiary(
      String idColumnName, String idColumnValue, DateTimeRange lastUpdatedRange) {
    return entityManager
        .createQuery(
            String.format(
                """
                SELECT bene
                FROM Beneficiary bene
                WHERE bene.%s = :id
                  AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR bene.meta.updatedTimestamp %s :lowerBound)
                  AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR bene.meta.updatedTimestamp %s :upperBound)
                ORDER BY bene.obsoleteTimestamp DESC
                """,
                idColumnName,
                lastUpdatedRange.getLowerBoundSqlOperator(),
                lastUpdatedRange.getUpperBoundSqlOperator()),
            Beneficiary.class)
        .setParameter("id", idColumnValue)
        .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
        .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
        .getResultList()
        .stream()
        .findFirst();
  }

  private Optional<BeneficiaryCoverage> searchBeneficiaryWithCoverage(
      String idColumnValue, DateTimeRange lastUpdatedRange) {

    return entityManager
        .createQuery(
            String.format(
                """
                  SELECT b
                  FROM BeneficiaryCoverage b
                  LEFT JOIN FETCH b.beneficiaryStatus bs
                  LEFT JOIN FETCH b.beneficiaryEntitlementReason ber
                  LEFT JOIN FETCH b.beneficiaryThirdParties tp
                  LEFT JOIN FETCH b.beneficiaryEntitlements be
                  WHERE b.beneSk = :id
                    AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
                    AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
                    AND b.beneSk = b.xrefSk
                  ORDER BY b.obsoleteTimestamp DESC
                  """,
                lastUpdatedRange.getLowerBoundSqlOperator(),
                lastUpdatedRange.getUpperBoundSqlOperator()),
            BeneficiaryCoverage.class)
        .setParameter("id", idColumnValue)
        .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
        .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
        .getResultList()
        .stream()
        .findFirst();
  }
}
