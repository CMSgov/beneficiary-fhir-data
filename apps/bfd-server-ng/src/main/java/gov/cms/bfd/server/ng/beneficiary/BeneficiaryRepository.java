package gov.cms.bfd.server.ng.beneficiary;

import static gov.cms.bfd.server.ng.util.MetricRecorder.PATIENT_MATCH_OUTCOME;

import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.beneficiary.filter.PatientMatchFilter;
import gov.cms.bfd.server.ng.beneficiary.model.*;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.log.QueryTelemetryUtil;
import gov.cms.bfd.server.ng.util.LogUtil;
import gov.cms.bfd.server.ng.util.MetricRecorder;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.MeterTag;
import io.micrometer.core.instrument.Tags;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for querying beneficiary information. Suppress SonarQube about dynamically formatted
 * SQL queries being safe here. Ignore. These are internally generated.
 */
@Transactional(readOnly = true)
@Repository
@AllArgsConstructor
@SuppressWarnings("java:S2077")
public class BeneficiaryRepository {
  @PersistenceContext private EntityManager entityManager;
  private final MetricRecorder metricRecorder;
  private final QueryTelemetryUtil queryTelemetryUtil;

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
    var query =
        entityManager
            .createQuery(
                """
                 SELECT identity
                 FROM BeneficiaryIdentity identity
                 WHERE identity.id.xrefSk = :beneXrefSk
                """,
                BeneficiaryIdentity.class)
            .setParameter("beneXrefSk", beneXrefSk);
    return queryTelemetryUtil.executeAndTrack("getValidBeneficiaryIdentities", query);
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
    var query =
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
            .setParameter("upperBound", lastUpdatedRange.getUpperBoundDateTime().orElse(null));

    var optionalBeneficiary =
        queryTelemetryUtil.executeAndTrack("findById", query).stream().findFirst();

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
    var query =
        entityManager
            .createQuery(
                """
                 SELECT bene.xrefSk
                 FROM Beneficiary bene
                 WHERE bene.beneSk = :beneSk
               """,
                Long.class)
            .setParameter("beneSk", beneSk);
    return queryTelemetryUtil.executeAndTrack("getXrefSkFromBeneSk", query).stream().findFirst();
  }

  /**
   * Retrieves the xrefSk from the mbi.
   *
   * @param mbi Medicare Beneficiary Identifier
   * @return xrefSk for the bene
   */
  @Timed(value = "application.beneficiary.search_xref_by_mbi")
  public Optional<Long> getXrefSkFromMbi(String mbi) {
    var query =
        entityManager
            .createQuery(
                """
                 SELECT bene.xrefSk
                 FROM Beneficiary bene
                 WHERE bene.identifier.mbi = :mbi
                """,
                Long.class)
            .setParameter("mbi", mbi);

    return queryTelemetryUtil.executeAndTrack("getXrefSkFromMbi", query).stream().findFirst();
  }

  /**
   * Searches the database for a matching beneficiary, iterating through all valid match attempt
   * permutations until one is found or all attempts are exhausted.
   *
   * @param patientMatch patient match request
   * @return beneficiary, if found
   */
  public PatientMatchResult searchPatientMatch(PatientMatch patientMatch) {
    var queryBase =
        """
                      SELECT bene
                      FROM Beneficiary bene
                      WHERE bene.latestTransactionFlag = 'Y'
                      %s
                      ORDER BY bene.obsoleteTimestamp DESC
                    """;

    var result =
        metricRecorder.recordMetric(
            "application.beneficiary.patient_match.outcome",
            () -> {
              var combinationResults = new ArrayList<MatchCombinationResult>();

              for (var indexedScenario : patientMatch.getValidScenarios()) {
                var combinationIndex = indexedScenario.combinationIndex();
                var scenario = indexedScenario.entries();
                var filters =
                    new PatientMatchFilter(scenario).getFilters("bene", SystemType.UNKNOWN);

                var jpql =
                    entityManager.createQuery(
                        String.format(queryBase, filters.filterClause()), Beneficiary.class);
                var query = DbFilterParam.withParams(jpql, filters.params());
                var benes =
                    queryTelemetryUtil.executeAndTrack("searchPatientMatch", query).stream()
                        .toList();
                var matchedRecords =
                    benes.stream()
                        .map(b -> new MatchedRecord(b.getBeneSk(), b.getEffectiveTimestamp()))
                        .toList();
                combinationResults.add(
                    new MatchCombinationResult(
                        combinationIndex, PATIENT_MATCH_TYPE, matchedRecords));
                var uniqueXrefs =
                    benes.stream().map(BeneficiaryBase::getXrefSk).distinct().toList();

                if (uniqueXrefs.size() == 1) {

                  var matchedBene = benes.getFirst();
                  LogUtil.logBeneSk(matchedBene.getBeneSk());
                  var finalDetermination =
                      new FinalDetermination(combinationIndex, matchedRecords.getFirst());
                  return new PatientMatchResult(
                      combinationResults,
                      Optional.of(finalDetermination),
                      Optional.of(matchedBene));
                }
              }

              return new PatientMatchResult(combinationResults, Optional.empty(), Optional.empty());
            },
            r ->
                Tags.of(
                    PATIENT_MATCH_OUTCOME,
                    r.matchedBeneficiary().isPresent() ? "match" : "no_match"));

    metricRecorder.recordDistribution(
        "application.beneficiary.patient_match.scenarios_attempted",
        result.combinations().size(),
        PATIENT_MATCH_OUTCOME,
        result.matchedBeneficiary().isPresent() ? "match" : "no_match");

    return result;
  }
}
