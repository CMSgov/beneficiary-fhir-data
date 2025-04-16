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
              SELECT new HistoricalIdentity(
                history.beneSk,
                history.mbi,
                mbi_history.effectiveDate,
                mbi_history.obsoleteDate,
                history.mbi = bene.mbi AS isCurrentMbi
              )
              FROM Beneficiary bene
              LEFT JOIN BeneficiaryHistory history ON bene.xrefSk = history.xrefSk
              LEFT JOIN BeneficiaryMbi mbi_history
                ON history.mbi = mbi_history.mbi
                AND mbi_history.obsoleteDate < gov.cms.bfd.server.ng.converter.IdrConstants.DEFAULT_DATE
              WHERE
                bene.beneSk = :beneSk
                AND (history.mbi != "" OR history.beneSk != bene.beneSk)
                AND NOT EXISTS (SELECT 1 FROM OvershareMbi om WHERE om.mbi = bene.mbi or om.mbi = history.mbi)
              GROUP BY history.mbi, history.beneSk, bene.mbi, mbi_history.effectiveDate, mbi_history.obsoleteDate
          """)
  List<HistoricalIdentity> getHistoricalIdentities(@Param("beneSk") long beneSk);

  @Query(value = "SELECT b FROM Beneficiary b WHERE beneSk = :beneSk")
  Beneficiary getById(@Param("beneSk") long beneSk);
}
