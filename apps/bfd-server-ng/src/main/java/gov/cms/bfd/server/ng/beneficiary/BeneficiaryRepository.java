package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.HistoricalIdentity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface BeneficiaryRepository extends Repository<Beneficiary, Long> {
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
                    AND mbiHistory.obsoleteDate < gov.cms.bfd.server.ng.converter.IdrConstants.DEFAULT_DATE
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
                  AND mbiHistory.obsoleteDate < gov.cms.bfd.server.ng.converter.IdrConstants.DEFAULT_DATE
                WHERE bene.beneSk = :beneSk
              )
              SELECT new HistoricalIdentity(ROW_NUMBER() OVER (ORDER BY abi.beneSk) id, abi.beneSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate)
              FROM allBeneInfo abi
              GROUP BY abi.beneSk, abi.mbi, abi.effectiveDate, abi.obsoleteDate
          """)
  List<HistoricalIdentity> getHistoricalIdentities(@Param("beneSk") long beneSk);

  @Query(value = "SELECT b FROM Beneficiary b WHERE beneSk = :beneSk")
  Beneficiary getById(@Param("beneSk") long beneSk);
}
