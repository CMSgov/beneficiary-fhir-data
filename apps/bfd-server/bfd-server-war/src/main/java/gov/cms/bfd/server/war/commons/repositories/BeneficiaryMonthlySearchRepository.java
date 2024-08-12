package gov.cms.bfd.server.war.commons.repositories;

import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryMonthly;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for searching beneficiary_monthly entries. */
@Repository
public interface BeneficiaryMonthlySearchRepository
    extends JpaRepository<BeneficiaryMonthly, Long> {
  /**
   * Determine whether a beneficiary exists for the contract and period.
   *
   * @param yearMonth year month
   * @param contractId contract ID
   * @return whether a record exists
   */
  @Query(
      value =
          """
          SELECT EXISTS(
              SELECT 1
              FROM BeneficiaryMonthly
              WHERE yearMonth = :yearMonth AND partDContractNumberId = :contractId
          )
          """)
  boolean beneficiaryExists(
      @Param("yearMonth") LocalDate yearMonth, @Param("contractId") String contractId);

  /**
   * Find all beneficiaries for the contract and period.
   *
   * @param yearMonth year month
   * @param contractId contract ID
   * @param pageable pagination info
   * @return benes
   */
  @Query(
      value =
          """
          SELECT bm.parentBeneficiary
          FROM BeneficiaryMonthly bm
          WHERE (
            bm.parentBeneficiary.xrefGroupId IS NULL
            OR EXISTS (
                  SELECT 1
                  FROM CurrentBeneficiary
                  WHERE beneficiaryId = bm.parentBeneficiary.beneficiaryId)
            )
            AND bm.yearMonth = :yearMonth
            AND bm.partDContractNumberId = :contractId
          ORDER BY bm.parentBeneficiary.beneficiaryId
          """)
  List<Beneficiary> getBeneficiariesByDateAndContract(
      @Param("yearMonth") LocalDate yearMonth,
      @Param("contractId") String contractId,
      Pageable pageable);
}
