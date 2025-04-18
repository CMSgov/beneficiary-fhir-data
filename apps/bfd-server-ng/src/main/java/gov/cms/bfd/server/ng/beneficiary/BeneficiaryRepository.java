package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.Identity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Repository for querying beneficiary information. */
public interface BeneficiaryRepository extends Repository<Beneficiary, Long> {
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
  @Query(
      value =
          """
              WITH allBeneInfo AS (
                SELECT
                  bene.beneSk beneSk,
                  bene.mbi mbi,
                  mbiHistory.effectiveDate effectiveDate,
                  mbiHistory.obsoleteDate obsoleteDate
                FROM
                  Beneficiary bene
                  LEFT JOIN BeneficiaryMbi mbiHistory
                    ON bene.mbi = mbiHistory.mbi
                    AND mbiHistory.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                WHERE bene.beneSk = :beneSk
                UNION
                SELECT
                  beneHistory.beneSk beneSk,
                  beneHistory.mbi mbi,
                  mbiHistory.effectiveDate effectiveDate,
                  mbiHistory.obsoleteDate obsoleteDate
                FROM Beneficiary bene
                JOIN BeneficiaryHistory beneHistory
                  ON beneHistory.xrefSk = bene.xrefSk
                LEFT JOIN BeneficiaryMbi mbiHistory
                  ON mbiHistory.mbi = beneHistory.mbi
                  AND mbiHistory.obsoleteDate < gov.cms.bfd.server.ng.IdrConstants.DEFAULT_DATE
                WHERE bene.beneSk = :beneSk
              )
              SELECT new Identity(ROW_NUMBER() OVER (ORDER BY abi.beneSk) id, abi.beneSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate)
              FROM allBeneInfo abi
              GROUP BY abi.beneSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate
          """)
  List<Identity> getPatientIdentities(@Param("beneSk") long beneSk);

  /**
   * Retrieve a {@link Beneficiary} record by its ID.
   *
   * @param beneSk bene surrogate key
   * @return beneficiary record
   */
  @Query(value = "SELECT b FROM Beneficiary b WHERE beneSk = :beneSk")
  Beneficiary getById(@Param("beneSk") long beneSk);
}
