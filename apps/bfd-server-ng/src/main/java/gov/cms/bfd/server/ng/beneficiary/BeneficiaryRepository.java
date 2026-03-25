package gov.cms.bfd.server.ng.beneficiary;

import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.beneficiary.filter.PatientMatchFilter;
import gov.cms.bfd.server.ng.beneficiary.model.*;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository for querying beneficiary information. */
@Repository
@AllArgsConstructor
public class BeneficiaryRepository {
  private final EntityManager entityManager;

  private static final String PATIENT_MATCH_TYPE = "exact";

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

  public PatientMatchResult searchPatientMatch(PatientMatch patientMatch) {
    var scenarios = patientMatch.getValidScenarios();
    var combinationResults = new ArrayList<MatchCombinationResult>();
    var combinationIndex = 1;
    for (var scenario : scenarios) {
      var filters = new PatientMatchFilter(scenario).getFilters("bene", SystemType.UNKNOWN);

      var benes =
          DbFilterParam.withParams(
                  entityManager.createQuery(
                      String.format(
                          """
                  SELECT bene
                  FROM Beneficiary bene
                  WHERE bene.latestTransactionFlag = 'Y'
                  %s
                  ORDER BY bene.obsoleteTimestamp DESC
                  """,
                          filters.filterClause()),
                      Beneficiary.class),
                  filters.params())
              .getResultList()
              .stream()
              .toList();
      var matchedRecords =
          benes.stream()
              .map(b -> new MatchedRecord(b.getBeneSk(), b.getEffectiveTimestamp()))
              .toList();
      var combinationId = String.format("%02d", combinationIndex++);
      combinationResults.add(
          new MatchCombinationResult(combinationId, PATIENT_MATCH_TYPE, matchedRecords));
      var uniqueXrefs = benes.stream().map(BeneficiaryBase::getXrefSk).distinct().toList();

      if (uniqueXrefs.size() == 1) {
        var matchedBene = benes.getFirst();
        var finalDetermination =
            new FinalDetermination(combinationId, PATIENT_MATCH_TYPE, matchedRecords.getFirst());
        return new PatientMatchResult(
            combinationResults, Optional.of(finalDetermination), Optional.of(matchedBene));
      }
    }
    return new PatientMatchResult(combinationResults, Optional.empty(), Optional.empty());
  }
}
