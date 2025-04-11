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
                hstry.mbi,
                hstry.beneSk,
                hstry.mbi = bene.mbi AS isCurrentMbi
              )
              FROM Beneficiary bene
              JOIN BeneficiaryHistory hstry ON bene.xrefSk = hstry.xrefSk
              WHERE
                bene.beneSk = :beneSk
                AND (hstry.mbi IS NOT NULL OR hstry.beneSk != bene.beneSk)
                AND NOT EXISTS (SELECT 1 FROM OvershareMbi om WHERE om.mbi = bene.mbi or om.mbi = hstry.mbi)
              GROUP BY hstry.mbi, hstry.beneSk, bene.mbi
          """)
  List<HistoricalIdentity> getHistoricalIdentities(@Param("beneSk") long beneSk);

  @Query(value = "SELECT b FROM Beneficiary b WHERE beneSk = :beneSk")
  Beneficiary getById(@Param("beneSk") long beneSk);
}
