package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryIdentity;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository for querying beneficiary information. */
@Repository
@AllArgsConstructor
public class BeneficiaryRepository {
  private final EntityManager entityManager;

  /**
   * Queries for current and historical MBIs and BENE_SKs, along with their start/end dates.
   * Beneficiary records with kill credit switch set to "1" or overshare mbi are filtered out
   *
   * @param beneXrefSk computed bene surrogate key
   * @return list of patient identities representing all active identities connected to the bene
   *     record
   */
  @Timed(value = "application.beneficiary.search_identities")
  public List<BeneficiaryIdentity> getValidBeneficiaryIdentities(long beneXrefSk) {
    return entityManager
        .createQuery(
            """
            SELECT identity
            FROM BeneficiaryIdentity identity
            WHERE identity.id.xrefSk = :beneXrefSk
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
  @Timed(value = "application.beneficiary.search_by_id")
  public Optional<Beneficiary> findById(
      long beneSk,
      @MeterTag(
              key = "hasLastUpdated",
              expression = "lowerBound.isPresent() || upperBound.isPresent()")
          DateTimeRange lastUpdatedRange) {
    var optionalBeneficiary =
        entityManager
            .createQuery(
                String.format(
                    """
              SELECT bene
              FROM Beneficiary bene
              WHERE bene.beneSk = :beneSk
                AND ((cast(:lowerBound AS ZonedDateTime)) IS NULL OR bene.patientMeta.updatedTimestamp %s :lowerBound)
                AND ((cast(:upperBound AS ZonedDateTime)) IS NULL OR bene.patientMeta.updatedTimestamp %s :upperBound)
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

    optionalBeneficiary.ifPresent(beneficiary -> LogUtil.logBeneSk(beneficiary.getBeneSk()));
    return optionalBeneficiary;
  }

  /**
   * Retrieves the xrefSk from the beneSk.
   *
   * @param beneSk original beneSk
   * @return xrefSk for the bene
   */
  @Timed(value = "application.beneficiary.search_xref_by_bene_sk")
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
   * @param mbi Medicare Beneficiary Identifier
   * @return xrefSk for the bene
   */
  @Timed(value = "application.beneficiary.search_xref_by_mbi")
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
}
