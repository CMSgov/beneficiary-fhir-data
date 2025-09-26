package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.coverage.CoverageRepository;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressTables;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests verifying the logic of getting the "last updated" value */
@Transactional
public class LastUpdatedRepositoryIT extends IntegrationTestBase {

  private final EntityManager entityManager;
  private final ClaimRepository claimRepository;
  private final BeneficiaryRepository beneficiaryRepository;
  private final CoverageRepository coverageRepository;
  private final javax.sql.DataSource dataSource;

  @Autowired
  public LastUpdatedRepositoryIT(
      EntityManager entityManager,
      ClaimRepository claimRepository,
      BeneficiaryRepository beneficiaryRepository,
      CoverageRepository coverageRepository,
      javax.sql.DataSource dataSource) {
    this.entityManager = entityManager;
    this.claimRepository = claimRepository;
    this.beneficiaryRepository = beneficiaryRepository;
    this.coverageRepository = coverageRepository;
    this.dataSource = dataSource;
  }

  private static final ZonedDateTime BASE_TIME =
      ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  private static final String DELETE_LOAD_PROGRESS = "DELETE FROM idr.load_progress";
  private static final String PREFIX_ASSERT_MSG = "Prefix should be counted: ";

  private enum ResourceType {
    CLAIM,
    BENEFICIARY,
    COVERAGE
  }

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @BeforeEach
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void cleanLoadProgress() {
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
  }

  @Test
  @Transactional
  void claimLastUpdatedReturnsMaxBatchCompletionTimestampAcrossClaimTables() {
    var tables = LoadProgressTables.claimTablePrefixes();

    for (int i = 0; i < tables.size(); i++) {
      insertLoadProgressRow(tables.get(i), BASE_TIME.plusHours(i));
    }

    var expected = BASE_TIME.plusHours(tables.size() - 1L);
    var actual = claimRepository.claimLastUpdated();
    assertEquals(
        expected.toInstant(),
        actual.toInstant(),
        "Should return max batch completion for claim tables");
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertNotNull(eob.getMeta().getLastUpdated(), "EOB meta.lastUpdated should be set");
  }

  @Test
  @Transactional
  void eobMetaLastUpdatedReflectsClaimBfdUpdatedTs() {

    var expected = BASE_TIME.plusHours(42);
    var prefixes = LoadProgressTables.claimTablePrefixes();

    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();

    for (var p : prefixes) insertLoadProgressRow(p, BASE_TIME);

    insertLoadProgressRow(prefixes.get(0), expected);

    var actual = claimRepository.claimLastUpdated();
    assertEquals(expected.toInstant(), actual.toInstant(), "Should pick the bumped claim prefix");
  }

  @Test
  @Transactional
  void patientMetaLastUpdatedReflectsBeneficiaryBfdUpdatedTs() {
    var expected = BASE_TIME.plusHours(24);
    var prefixes = LoadProgressTables.beneficiaryTablePrefixes();

    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();

    for (var p : prefixes) insertLoadProgressRow(p, BASE_TIME);
    insertLoadProgressRow(prefixes.get(0), expected);

    var actual = beneficiaryRepository.beneficiaryLastUpdated();
    assertEquals(
        expected.toInstant(), actual.toInstant(), "Should pick the bumped beneficiary prefix");
  }

  @Test
  @Transactional
  void coverageMetaLastUpdatedReflectsBeneficiaryBfdUpdatedTs() {
    // Validate coverage last updated computation via load_progress without changing
    // beneficiary rows.
    var expected = BASE_TIME.plusHours(30);
    var prefixes = LoadProgressTables.coverageTablePrefixes();

    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();

    for (var p : prefixes) insertLoadProgressRow(p, BASE_TIME);
    insertLoadProgressRow(prefixes.get(0), expected);

    var actual = coverageRepository.coverageLastUpdated();
    assertEquals(
        expected.toInstant(), actual.toInstant(), "Should pick the bumped coverage prefix");
  }

  @Test
  void beneficiaryLastUpdatedReturnsMaxBatchCompletionTimestampAcrossBeneficiaryTables() {
    var tables = LoadProgressTables.beneficiaryTablePrefixes();
    for (int i = 0; i < tables.size(); i++) {
      insertLoadProgressRow(tables.get(i), BASE_TIME.plusMinutes(i));
    }
    var expected = BASE_TIME.plusMinutes(tables.size() - 1L);
    var actual = beneficiaryRepository.beneficiaryLastUpdated();
    assertEquals(
        expected.toInstant(),
        actual.toInstant(),
        "Should return max batch completion for beneficiary tables");
  }

  private void insertLoadProgressRow(String tablePrefix, ZonedDateTime ts) {
    var sql =
        "INSERT INTO idr.load_progress(table_name, last_id, last_ts, batch_start_ts, batch_complete_ts) VALUES ('"
            + tablePrefix
            + "', '0', cast('"
            + ts.toString()
            + "' as timestamptz), cast('"
            + ts.toString()
            + "' as timestamptz), cast('"
            + ts.toString()
            + "' as timestamptz)) ON CONFLICT (table_name) DO UPDATE SET batch_complete_ts = EXCLUDED.batch_complete_ts, batch_start_ts = EXCLUDED.batch_start_ts";
    entityManager.createNativeQuery(sql).executeUpdate();
    entityManager.flush();
  }

  @ParameterizedTest
  @EnumSource(ResourceType.class)
  void loadProgressPrefixesAreAllUsed(ResourceType type) {
    List<String> prefixes;

    switch (type) {
      case CLAIM -> prefixes = LoadProgressTables.claimTablePrefixes();
      case BENEFICIARY -> prefixes = LoadProgressTables.beneficiaryTablePrefixes();
      case COVERAGE -> prefixes = LoadProgressTables.coverageTablePrefixes();
      default -> throw new IllegalStateException("Unexpected type: " + type);
    }

    for (int i = 0; i < prefixes.size(); i++) {
      entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
      entityManager.flush();

      for (var p : prefixes) insertLoadProgressRow(p, BASE_TIME);

      var bumped = BASE_TIME.plusHours(10L + i);
      insertLoadProgressRow(prefixes.get(i), bumped);

      var actual =
          switch (type) {
            case CLAIM -> claimRepository.claimLastUpdated();
            case BENEFICIARY -> beneficiaryRepository.beneficiaryLastUpdated();
            case COVERAGE -> coverageRepository.coverageLastUpdated();
          };
      assertEquals(bumped.toInstant(), actual.toInstant(), PREFIX_ASSERT_MSG + prefixes.get(i));
    }
  }

  @Test
  void eobMetaReflectsChildTableBfdUpdatedTs() {
    // Read the claim's current bfd_updated_ts and pick an expected value that is
    // guaranteed to be newer than the claim's timestamp.
    ZonedDateTime claimTs;
    try {
      try (var conn = dataSource.getConnection();
          var stmt = conn.createStatement();
          var rs =
              stmt.executeQuery(
                  "SELECT bfd_updated_ts FROM idr.claim WHERE clm_uniq_id = "
                      + CLAIM_ID_ADJUDICATED)) {
        if (!rs.next())
          throw new IllegalStateException("Seeded claim not found: " + CLAIM_ID_ADJUDICATED);
        var ts = rs.getTimestamp("bfd_updated_ts");
        claimTs = ZonedDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to read seeded claim timestamp", e);
    }

    var expected = claimTs.plusHours(1);

    // Update claim_ansi_signature rows associated with the seeded claim used in tests.
    var sql =
        "UPDATE idr.claim_ansi_signature SET bfd_updated_ts = cast('"
            + expected.toString()
            + "' as timestamptz) WHERE clm_ansi_sgntr_sk IN (SELECT cli.clm_ansi_sgntr_sk FROM idr.claim_line_institutional cli WHERE cli.clm_uniq_id = "
            + CLAIM_ID_ADJUDICATED
            + ")";

    try {
      try (var conn = dataSource.getConnection();
          var stmt = conn.createStatement()) {

        stmt.executeUpdate(sql);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to update child timestamps for seeded claim", e);
    }

    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertNotNull(eob.getMeta(), "EOB meta should be present");
    var expectedMs = expected.toInstant().truncatedTo(ChronoUnit.MILLIS);
    var actualMs = eob.getMeta().getLastUpdated().toInstant().truncatedTo(ChronoUnit.MILLIS);
    assertEquals(expectedMs, actualMs);
  }
}
