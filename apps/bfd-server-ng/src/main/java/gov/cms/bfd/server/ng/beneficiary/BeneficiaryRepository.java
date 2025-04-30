package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.Identity;
import gov.cms.bfd.server.ng.types.DateTimeRange;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** Repository for querying beneficiary information. */
@Component
@AllArgsConstructor
public class BeneficiaryRepository {
  private EntityManager entityManager;

  // TODO: this has not yet been thoroughly tested with edge cases.
  // It will likely need some adjustments.

  /**
   * Queries for current and historical MBIs and BENE_SKs, along with their start/end dates.
   *
   * @param beneSk bene surrogate key
   * @return list of identities
   */
  // This query has a few phases:
  // 1. Pull MBI information for the current bene_sk/mbi pair
  //
  // 2. Pull MBI information for historical bene_sk/mbi pairs (these must be two distinct steps
  // because there may not be a history record for the current MBI)
  //
  // 3. Use GROUP BY to filter out duplicates. There's additional info in these tables besides just
  // historical identity information, so there could be any number of duplicates relative to the
  // small amount of information we're pulling.
  //
  // NOTE - it would be simpler to do the WHERE NOT EXISTS on OvershareMBI after the UNION, but
  // that doesn't appear to be supported by the JQL parser.
  public List<Identity> getPatientIdentities(long beneSk) {
    return entityManager
        .createQuery(
            """
              WITH allBeneInfo AS (
                SELECT
                  bene.beneSk beneSk,
                  bene.xrefSk xrefSk,
                  bene.mbi mbi,
                  mbiId.effectiveDate effectiveDate,
                  mbiId.obsoleteDate obsoleteDate
                FROM
                  Beneficiary bene
                  LEFT JOIN BeneficiaryMbiId mbiId
                    ON bene.mbi = mbiId.mbi
                    AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                WHERE bene.beneSk = :beneSk
                AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = bene.mbi)
                UNION
                SELECT
                  beneHistory.beneSk beneSk,
                  beneHistory.xrefSk xrefSk,
                  beneHistory.mbi mbi,
                  mbiId.effectiveDate effectiveDate,
                  mbiId.obsoleteDate obsoleteDate
                FROM Beneficiary bene
                JOIN BeneficiaryHistory beneHistory
                  ON beneHistory.xrefSk = bene.xrefSk
                LEFT JOIN BeneficiaryMbiId mbiId
                  ON mbiId.mbi = beneHistory.mbi
                  AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                WHERE bene.beneSk = :beneSk
                AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = bene.mbi)
              )
              SELECT new Identity(ROW_NUMBER() OVER (ORDER BY abi.beneSk) rowId, abi.beneSk, abi.xrefSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate)
              FROM allBeneInfo abi
              GROUP BY abi.beneSk, abi.mbi, abi.xrefSk, abi.effectiveDate, abi.obsoleteDate
            """,
            Identity.class)
        .setParameter("beneSk", beneSk)
        .getResultList();
  }

  /**
   * Retrieve a {@link Beneficiary} record by its ID and last updated timestamp.
   *
   * @param beneSk bene surrogate key
   * @param lastUpdatedRange last updated search range
   * @return beneficiary record
   */
  public Optional<Beneficiary> findById(long beneSk, DateTimeRange lastUpdatedRange) {
    return entityManager
        .createQuery(
            String.format(
                """
          SELECT b
          FROM Beneficiary b
          WHERE b.beneSk = :beneSk
            AND ((cast(:lowerBound AS LocalDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
            AND ((cast(:upperBound AS LocalDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
            AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.mbi)
          """,
                lastUpdatedRange.lowerBoundOperator(), lastUpdatedRange.upperBoundOperator()),
            Beneficiary.class)
        .setParameter("beneSk", beneSk)
        .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
        .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Retrieve a {@link Beneficiary} record by its MBI and last updated timestamp.
   *
   * @param mbi bene MBI
   * @param lastUpdatedRange last updated search range
   * @return beneficiary record
   */
  public Optional<Beneficiary> findByIdentifier(String mbi, DateTimeRange lastUpdatedRange) {
    return entityManager
        .createQuery(
            String.format(
                """
                SELECT b
                FROM Beneficiary b
                WHERE b.mbi = :mbi
                  AND (b.meta.updatedTimestamp %s :lowerBound OR (cast(:lowerBound AS LocalDateTime)) IS NULL)
                  AND (b.meta.updatedTimestamp %s :upperBound OR (cast(:upperBound AS LocalDateTime)) IS NULL)
                  AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.mbi)
                """,
                lastUpdatedRange.lowerBoundOperator(), lastUpdatedRange.upperBoundOperator()),
            Beneficiary.class)
        .setParameter("mbi", mbi)
        .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
        .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Returns the last updated timestamp for the beneficiary data ingestion process.
   *
   * @return last updated timestamp
   */
  public LocalDateTime beneficiaryLastUpdated() {
    return entityManager
        .createQuery(
            """
              SELECT MAX(p.batchCompletionTimestamp)
              FROM LoadProgress p
              WHERE p.tableName IN ("idr.beneficiary", "idr.beneficiary_history", "idr.beneficiary_mbi_history")
              """,
            LocalDateTime.class)
        .getResultList()
        .stream()
        .findFirst()
        .get();
  }
}
