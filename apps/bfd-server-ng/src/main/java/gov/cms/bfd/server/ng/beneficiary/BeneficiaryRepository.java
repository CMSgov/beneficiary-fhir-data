package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryHistory;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.patient.PatientIdentity;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository for querying beneficiary information. */
@Repository
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
  public List<PatientIdentity> getPatientIdentities(long beneSk) {
    return entityManager
        .createQuery(
            """
              WITH allBeneInfo AS (
                SELECT
                  bene.beneSk beneSk,
                  bene.xrefSk xrefSk,
                  bene.identity.mbi mbi,
                  mbiId.effectiveDate effectiveDate,
                  mbiId.obsoleteDate obsoleteDate
                FROM
                  Beneficiary bene
                  LEFT JOIN BeneficiaryMbiId mbiId
                    ON bene.identity.mbi = mbiId.mbi
                    AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                WHERE bene.beneSk = :beneSk
                AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = bene.identity.mbi)
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
                AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = bene.identity.mbi)
              )
              SELECT new PatientIdentity(ROW_NUMBER() OVER (ORDER BY abi.beneSk) rowId, abi.beneSk, abi.xrefSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate)
              FROM allBeneInfo abi
              GROUP BY abi.beneSk, abi.mbi, abi.xrefSk, abi.effectiveDate, abi.obsoleteDate
            """,
            PatientIdentity.class)
        .setParameter("beneSk", beneSk)
        .getResultList();
  }

  public List<PatientIdentity> getPatientIdentitiesByMbi(String mbi) {
    List<Beneficiary> beneficiariesFromMbi = searchBeneficiaryNoRange("identity.mbi", mbi);

    if (beneficiariesFromMbi.isEmpty()) {
      // Try to find a matching history entry
      List<Long> validXrefSk = searchBeneficiaryHistory("mbi", mbi).stream()
              .map(BeneficiaryHistory::getBeneXrefSk)
              .filter(sk -> sk != 0L)
              .distinct()
              .toList();

      if (validXrefSk.size() == 1) {
        return searchBeneficiaryAndHistory(validXrefSk.getFirst());
      } else {
        return Collections.emptyList();
      }
    }

    // There is at least one current Beneficiary
    if (beneficiariesFromMbi.size() == 1 ) {
      long beneSk = beneficiariesFromMbi.getFirst().getBeneSk();
      long xrefSk = beneficiariesFromMbi.getFirst().getXrefSk();

      // Case: xrefSk is 0 (invalid or incomplete), fallback to historical lookup
      if (xrefSk == 0L) {
        return getBeneficiaryHistoryIdentities("beneSk", String.valueOf(beneSk));
      }

      // Case: same value for beneSk and xrefSk
      if (xrefSk == beneSk) {
        return searchBeneficiaryAndHistory(xrefSk);
      }

      // Case: standard xref lookup
      return getValidBeneficiaryHistory(beneSk, xrefSk);
    }

    // Multiple xrefSk entries, see if exactly one is valid
    List<Long> validXrefs = beneficiariesFromMbi.stream()
            .map(Beneficiary::getXrefSk)
            .filter(sk -> sk != 0L)
            .distinct()
            .toList();

    if (validXrefs.size() == 1) {
      return searchBeneficiaryAndHistory(validXrefs.getFirst());
    }

    return Collections.emptyList();
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
   * @param partTypeCode Part type
   * @return beneficiary record
   */
  public Optional<Beneficiary> searchBeneficiaryWithCoverage(
      long beneSk, Optional<String> partTypeCode, DateTimeRange lastUpdatedRange) {
    return searchBeneficiaryWithCoverage(
        "beneSk", String.valueOf(beneSk), partTypeCode, lastUpdatedRange);
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
              SELECT b.xrefSk
              FROM Beneficiary b
              WHERE b.beneSk = :beneSk
              AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
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
            WHERE p.tableName IN (
              "idr.beneficiary",
              "idr.beneficiary_history",
              "idr.beneficiary_mbi_id"
            )
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
                SELECT b
                FROM Beneficiary b
                WHERE b.%s = :id
                  AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
                  AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
                  AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                ORDER BY b.obsoleteTimestamp DESC
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

  private Optional<Beneficiary> searchBeneficiaryWithCoverage(
      String idColumnName,
      String idColumnValue,
      Optional<String> partTypeCode,
      DateTimeRange lastUpdatedRange) {
    // UTC -12 is "Anywhere on Earth time", sometimes used to specify deadlines in the absence of a
    // specific time zone.
    // https://en.wikipedia.org/wiki/Anywhere_on_Earth
    var currentDate = OffsetDateTime.now(ZoneOffset.ofHours(-12)).toLocalDate();
    return entityManager
        .createQuery(
            String.format(
                """
                  SELECT b
                  FROM Beneficiary b
                  LEFT JOIN b.beneficiaryThirdParties tp ON
                    (:partTypeCode IS NULL OR tp.id.thirdPartyTypeCode = :partTypeCode)
                    AND tp.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
                    AND tp.id.benefitRangeBeginDate <= :referenceDate
                    AND tp.id.benefitRangeEndDate >= :referenceDate
                  LEFT JOIN b.beneficiaryEntitlements be ON
                    (:partTypeCode IS NULL OR be.id.medicareEntitlementTypeCode = :partTypeCode)
                    AND be.idrTransObsoleteTimestamp >= gov.cms.bfd.server.ng.IdrConstants.DEFAULT_ZONED_DATE
                    AND be.id.benefitRangeBeginDate <= :referenceDate
                    AND be.id.benefitRangeEndDate >= :referenceDate
                  WHERE b.%s = :id
                    AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
                    AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
                    AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                    AND b.beneSk = b.xrefSk
                  ORDER BY b.obsoleteTimestamp DESC
                  """,
                idColumnName,
                lastUpdatedRange.getLowerBoundSqlOperator(),
                lastUpdatedRange.getUpperBoundSqlOperator()),
            Beneficiary.class)
        .setParameter("id", idColumnValue)
        .setParameter("referenceDate", currentDate)
        .setParameter("partTypeCode", partTypeCode.orElse(null))
        .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
        .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
        .getResultList()
        .stream()
        .findFirst();
  }

  private List<Beneficiary> searchBeneficiaryNoRange(
          String idColumnName, String idColumnValue) {
    return entityManager
        .createQuery(
              String.format(
                """
                SELECT b
                FROM Beneficiary b
                WHERE b.%s = :id
                  AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                """,
                idColumnName),
            Beneficiary.class)
          .setParameter("id", idColumnValue)
          .getResultList();
  }

  private List<BeneficiaryHistory> searchBeneficiaryHistory(
          String idColumnName, String idColumnValue) {
    return entityManager
            .createQuery(
                    String.format(
                    """
                    SELECT bh.beneSk, bh.beneXrefSk, bh.xrefSk, bh.mbi
                    FROM BeneficiaryHistory bh
                    WHERE bh.%s = :id
                      AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                    GROUP BY bh.beneSk, bh.beneXrefSk, bh.xrefSk, bh.mbi
                    """,
                    idColumnName),
            BeneficiaryHistory.class)
            .setParameter("id", idColumnValue)
            .getResultList();
  }

  private List<PatientIdentity> getValidBeneficiaryHistory(long beneSk, long beneXrefSk) {
    return entityManager
            .createQuery(
                    """
                    SELECT new PatientIdentity(ROW_NUMBER() OVER (ORDER BY bh.beneXrefSk) rowId, bh.beneSk, bh.beneXrefSk, bh.xrefSk, bh.mbi)
                    FROM BeneficiaryHistory bh
                    WHERE bh.beneSk IN (
                        SELECT b.beneSk
                        FROM Beneficiary b
                        WHERE b.beneXrefSk = :beneXrefSk
                    )
                    AND NOT EXISTS (
                        SELECT 1
                        FROM BeneficiaryXref bx
                        WHERE bx.beneSk = bh.beneSk
                          AND bx.beneXrefSk = bh.beneXrefSk
                          AND bx.beneKillCred = '1'
                    )
                    AND NOT EXISTS (
                        SELECT 1
                        FROM BeneficiaryXref bx_init
                        WHERE bx_init.beneSk = :beneSk
                          AND bx_init.beneXrefSk = :beneXrefSk
                          AND bx_init.beneKillCred = '1'
                    )
                    GROUP BY bh.beneSk, bh.beneXrefSk, bh.xrefSk, bh.mbi
                    """,
                    PatientIdentity.class)
            .setParameter("beneSk", beneSk)
            .setParameter("beneXrefSk", beneXrefSk)
            .getResultList();
  }

  private List<PatientIdentity> getBeneficiaryHistoryIdentities(
          String idColumnName, String idColumnValue) {
    return entityManager
            .createQuery(
                    String.format(
                            """
                            SELECT new PatientIdentity(ROW_NUMBER() OVER (ORDER BY bh.beneXrefSk) rowId, bh.beneSk, bh.beneXrefSk, bh.xrefSk, bh.mbi)
                            FROM BeneficiaryHistory bh
                            WHERE bh.%s = :id
                              AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                            GROUP BY bh.beneSk, bh.beneXrefSk, bh.xrefSk, bh.mbi
                            """,
                            idColumnName),
                    PatientIdentity.class)
            .setParameter("id", idColumnValue)
            .getResultList();
  }

  private List<PatientIdentity> searchBeneficiaryAndHistory(long beneXrefSk) {
    return entityManager
            .createQuery(
                """
                WITH allBeneInfo AS (
                  SELECT
                    b.beneSk beneSk,
                    b.beneXrefSk beneXrefSk,
                    b.identity.mbi mbi,
                    mbiId.effectiveDate effectiveDate,
                    mbiId.obsoleteDate obsoleteDate
                  FROM Beneficiary b
                  JOIN BeneficiaryXref bx
                    ON b.beneXrefSk = bx.beneXrefSk and b.beneSK = bx.beneSK
                  LEFT JOIN BeneficiaryMbiId mbiId
                    ON b.identity.mbi = mbiId.mbi
                    AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                  WHERE b.beneXrefSk = :beneXrefSk
                  AND bx.beneKillCred != 1
                  AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                  UNION
                  SELECT
                    bh.beneSk beneSk,
                    bh.beneXrefSk beneXrefSk,
                    bh.mbi mbi,
                    mbiId.effectiveDate effectiveDate,
                    mbiId.obsoleteDate obsoleteDate
                  FROM Beneficiary b
                  JOIN BeneficiaryHistory bh
                    ON b.beneSK = bh.beneSK
                  LEFT JOIN BeneficiaryMbiId mbiId
                    ON b.identity.mbi = mbiId.mbi
                    AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                  WHERE b.beneXrefSk = :beneXrefSk
                )
                SELECT new PatientIdentity(ROW_NUMBER() OVER (ORDER BY abi.beneXrefSk) rowId, abi.beneXrefSk, abi.xrefSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate)
                FROM allBeneInfo abi
                GROUP BY abi.beneSk, abi.mbi, abi.xrefSk, abi.effectiveDate, abi.obsoleteDate
                """,
                PatientIdentity.class
            )
            .setParameter("beneXrefSk", beneXrefSk)
            .getResultList();
  }

  private List<PatientIdentity> searchBeneficiaryXref(long beneXrefSk) {
    return entityManager
            .createQuery(
                    """
                    WITH allBeneInfo AS (
                      SELECT
                        b.beneSk beneSk,
                        b.beneXrefSk beneXrefSk,
                        b.identity.mbi mbi,
                        mbiId.effectiveDate effectiveDate,
                        mbiId.obsoleteDate obsoleteDate
                      FROM Beneficiary b
                      JOIN BeneficiaryXref bx
                        ON b.beneXrefSk = bx.beneXrefSk and b.beneSK = bx.beneSK
                      LEFT JOIN BeneficiaryMbiId mbiId
                        ON b.identity.mbi = mbiId.mbi
                        AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                      WHERE b.beneXrefSk = :beneXrefSk
                      AND bx.beneKillCred != 1
                      AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                      UNION
                      SELECT
                        bh.beneSk beneSk,
                        bh.beneXrefSk beneXrefSk,
                        bh.mbi mbi,
                        mbiId.effectiveDate effectiveDate,
                        mbiId.obsoleteDate obsoleteDate
                      FROM Beneficiary b
                      JOIN BeneficiaryHistory bh
                        ON b.beneSK = bh.beneSK
                      LEFT JOIN BeneficiaryMbiId mbiId
                        ON b.identity.mbi = mbiId.mbi
                        AND mbiId.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                      WHERE b.beneXrefSk = :beneXrefSk
                    )
                    SELECT new PatientIdentity(ROW_NUMBER() OVER (ORDER BY abi.beneXrefSk) rowId, abi.beneXrefSk, abi.xrefSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate)
                    FROM allBeneInfo abi
                    GROUP BY abi.beneSk, abi.mbi, abi.xrefSk, abi.effectiveDate, abi.obsoleteDate
                    """,
                    PatientIdentity.class
            )
            .setParameter("beneXrefSk", beneXrefSk)
            .getResultList();
  }
}

