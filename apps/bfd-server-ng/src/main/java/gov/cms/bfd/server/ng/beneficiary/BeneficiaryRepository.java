package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.patient.PatientIdentity;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    var beneficiariesFromMbi = searchBeneficiaryNoRange("identity.mbi", mbi).stream().toList();
    var beneCount = (int) beneficiariesFromMbi.stream().map(Beneficiary::getXrefSk).distinct().count();

    if (beneCount == 0) {
      var beneHistory = searchBeneficiaryHistory("identity.mbi", mbi);
      var beneXrefSK = beneHistory.stream().filter(bene -> bene.getBeneXrefSk() != 0L).distinct().toList();
      if (beneXrefSK.size() == 1) {
        var beneficiaries = searchBeneficiaryNoRange("xrefSk", beneXrefSK.stream().findFirst().toString());

        return beneficiaries.stream().flatMap(bene -> searchBeneficiaryAndHistory(bene.getBeneXrefSk()).stream()).toList();
      }
      else {
        return Collections.emptyList();
      }
    }
    else if (beneCount == 1) {
      if (beneficiariesFromMbi.stream().map(Beneficiary::getBeneXrefSk).anyMatch(beneXrefSk -> beneXrefSk == 0L)) {
          var beneHistory = searchBeneficiaryHistory("identity.xrefSk", String.valueOf(beneficiariesFromMbi.getFirst().getBeneSk()));
      }
      else if (beneficiariesFromMbi.getFirst().getBeneXrefSk() == beneficiariesFromMbi.getFirst().getBeneSk()) {
        var beneficiaries = searchBeneficiaryNoRange("xrefSk", String.valueOf(beneficiariesFromMbi.getFirst().getBeneXrefSk()));

        return beneficiaries.stream().flatMap(bene -> searchBeneficiaryAndHistory(bene.getBeneXrefSk()).stream()).toList();
      }
      else {

      }
    }
    else {
      if (beneficiariesFromMbi.stream().filter(bene -> bene.getBeneXrefSk() != 0L).count() == 1) {

      }
      else {
        return Collections.emptyList();
      }
    }
      return List.of();
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
  public Optional<Beneficiary> searchCurrentBeneficiary(
      long beneSk, String partTypeCode, DateTimeRange lastUpdatedRange) {
    return searchCurrentBeneficiary(
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

  private Optional<Beneficiary> searchCurrentBeneficiary(
      String idColumnName,
      String idColumnValue,
      String partTypeCode,
      DateTimeRange lastUpdatedRange) {
    return entityManager
        .createQuery(
            String.format(
                """
                            SELECT b
                            FROM Beneficiary b
                            JOIN b.beneficiaryThirdParties tp
                            JOIN b.beneficiaryEntitlements be
                            WHERE b.%s = :id
                              AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :lowerBound)
                              AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR b.meta.updatedTimestamp %s :upperBound)
                              AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                              AND b.beneSk = b.xrefSk
                              AND tp.id.thirdPartyTypeCode = :partTypeCode
                              AND be.id.medicareEntitlementTypeCode = :partTypeCode
                            """,
                idColumnName,
                lastUpdatedRange.getLowerBoundSqlOperator(),
                lastUpdatedRange.getUpperBoundSqlOperator()),
            Beneficiary.class)
        .setParameter("id", idColumnValue)
        .setParameter("partTypeCode", partTypeCode)
        .setParameter("lowerBound", lastUpdatedRange.getLowerBoundDateTime().orElse(null))
        .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null))
        .getResultList()
        .stream()
        .findFirst();
  }

  private Optional<Beneficiary> searchBeneficiaryNoRange(
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
          .getResultList()
          .stream()
          .findFirst();
  }

  private Optional<Beneficiary> searchBeneficiaryHistory(
          String idColumnName, String idColumnValue) {
    return entityManager
            .createQuery(
                    String.format(
                            """
                            SELECT b
                            FROM Beneficiary b
                            JOIN BeneficiaryHistory bh
                              ON b.xrefSk = bh.xrefSk
                            WHERE b.%s = :id
                              AND NOT EXISTS(SELECT 1 FROM OvershareMbi om WHERE om.mbi = b.identity.mbi)
                            """,
                            idColumnName),
                    Beneficiary.class)
            .setParameter("id", idColumnValue)
            .getResultList()
            .stream()
            .findFirst();
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

