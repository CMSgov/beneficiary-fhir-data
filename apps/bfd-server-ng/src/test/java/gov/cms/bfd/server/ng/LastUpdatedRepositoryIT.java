package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.coverage.CoverageRepository;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressTables;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests verifying the logic of getting the "last updated" value */
@Transactional
public class LastUpdatedRepositoryIT extends IntegrationTestBase {

  private final EntityManager entityManager;
  private final ClaimRepository claimRepository;
  private final BeneficiaryRepository beneficiaryRepository;
  private final CoverageRepository coverageRepository;
  private final DataSource dataSource;

  @Autowired
  public LastUpdatedRepositoryIT(
      EntityManager entityManager,
      ClaimRepository claimRepository,
      BeneficiaryRepository beneficiaryRepository,
      CoverageRepository coverageRepository,
      DataSource dataSource) {
    this.entityManager = entityManager;
    this.claimRepository = claimRepository;
    this.beneficiaryRepository = beneficiaryRepository;
    this.coverageRepository = coverageRepository;
    this.dataSource = dataSource;
  }

  private static final ZonedDateTime BASE_TIME =
      ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  private static final String DELETE_LOAD_PROGRESS = "DELETE FROM idr.load_progress";
  private static final String SELECT_BFD_UPDATED_CLAIM =
      "SELECT bfd_updated_ts FROM idr.claim WHERE clm_uniq_id = ?";
  private static final String UPDATE_BFD_UPDATED_CLAIM =
      "UPDATE idr.claim SET bfd_updated_ts = cast(? as timestamptz) WHERE clm_uniq_id = ?";
  private static final String SELECT_BFD_UPDATED_BENEFICIARY =
      "SELECT bfd_updated_ts FROM idr.valid_beneficiary WHERE bene_sk = ?";
  private static final String UPDATE_BFD_UPDATED_BENEFICIARY =
      "UPDATE idr.valid_beneficiary SET bfd_updated_ts = cast(? as timestamptz) WHERE bene_sk = ?";
  private static final String PREFIX_ASSERT_MSG = "Prefix should be counted: ";

  private enum ResourceType {
    CLAIM,
    BENEFICIARY,
    COVERAGE
  }

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  private IReadTyped<Patient> patientRead() {
    return getFhirClient().read().resource(Patient.class);
  }

  private IReadTyped<Coverage> coverageRead() {
    return getFhirClient().read().resource(Coverage.class);
  }

  @BeforeEach
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
  void eobMetaLastUpdatedReflectsClaimBfdUpdatedTs() throws Exception {
    var expected = BASE_TIME.plusHours(42);
    String original = null;
    try (var conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try (var sel = conn.prepareStatement(SELECT_BFD_UPDATED_CLAIM)) {
        sel.setLong(1, Long.parseLong(CLAIM_ID_ADJUDICATED));
        var rs = sel.executeQuery();
        if (rs.next()) original = rs.getString(1);
      }

      try (var upd = conn.prepareStatement(UPDATE_BFD_UPDATED_CLAIM)) {
        upd.setString(1, expected.toString());
        upd.setLong(2, Long.parseLong(CLAIM_ID_ADJUDICATED));
        upd.executeUpdate();
      }
      conn.commit();
      try (var sel2 = conn.prepareStatement(SELECT_BFD_UPDATED_CLAIM)) {
        sel2.setLong(1, Long.parseLong(CLAIM_ID_ADJUDICATED));
        var rs2 = sel2.executeQuery();
        if (rs2.next()) {
          OffsetDateTime odt = rs2.getObject(1, OffsetDateTime.class);
          assertEquals(expected.toInstant(), odt.toInstant());
        }
      }
    }
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertEquals(expected.toInstant(), eob.getMeta().getLastUpdated().toInstant());

    // restore
    if (original != null) {
      try (var conn = dataSource.getConnection();
          var upd = conn.prepareStatement(UPDATE_BFD_UPDATED_CLAIM)) {
        upd.setString(1, original);
        upd.setLong(2, Long.parseLong(CLAIM_ID_ADJUDICATED));
        upd.executeUpdate();
      }
    }
  }

  @Test
  @Transactional
  void patientMetaLastUpdatedReflectsBeneficiaryBfdUpdatedTs() throws Exception {
    var expected = BASE_TIME.plusHours(24);

    String original = null;
    try (var conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try (var sel = conn.prepareStatement(SELECT_BFD_UPDATED_BENEFICIARY)) {
        sel.setLong(1, Long.parseLong(BENE_ID_ALL_PARTS_WITH_XREF));
        var rs = sel.executeQuery();
        if (rs.next()) original = rs.getString(1);
      }

      try (var upd = conn.prepareStatement(UPDATE_BFD_UPDATED_BENEFICIARY)) {
        upd.setString(1, expected.toString());
        upd.setLong(2, Long.parseLong(BENE_ID_ALL_PARTS_WITH_XREF));
        upd.executeUpdate();
      }
      conn.commit();
      try (var sel2 = conn.prepareStatement(SELECT_BFD_UPDATED_BENEFICIARY)) {
        sel2.setLong(1, Long.parseLong(BENE_ID_ALL_PARTS_WITH_XREF));
        var rs2 = sel2.executeQuery();
        if (rs2.next()) {
          OffsetDateTime odt = rs2.getObject(1, OffsetDateTime.class);
          assertEquals(expected.toInstant(), odt.toInstant());
        }
      }
    }

    var patient = patientRead().withId(BENE_ID_ALL_PARTS_WITH_XREF).execute();
    assertEquals(expected.toInstant(), patient.getMeta().getLastUpdated().toInstant());

    // restore
    if (original != null) {
      try (var conn = dataSource.getConnection();
          var upd = conn.prepareStatement(UPDATE_BFD_UPDATED_BENEFICIARY)) {
        upd.setString(1, original);
        upd.setLong(2, Long.parseLong(BENE_ID_ALL_PARTS_WITH_XREF));
        upd.executeUpdate();
      }
    }
  }

  @Test
  @Transactional
  void coverageMetaLastUpdatedReflectsBeneficiaryBfdUpdatedTs() throws Exception {
    var expected = BASE_TIME.plusHours(30);

    var beneId = BENE_ID_PART_A_ONLY;
    String original = null;
    try (var conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try (var sel = conn.prepareStatement(SELECT_BFD_UPDATED_BENEFICIARY)) {
        sel.setLong(1, Long.parseLong(beneId));
        var rs = sel.executeQuery();
        if (rs.next()) original = rs.getString(1);
      }

      try (var upd = conn.prepareStatement(UPDATE_BFD_UPDATED_BENEFICIARY)) {
        upd.setString(1, expected.toString());
        upd.setLong(2, Long.parseLong(beneId));
        upd.executeUpdate();
      }
      conn.commit();

      try (var sel2 = conn.prepareStatement(SELECT_BFD_UPDATED_BENEFICIARY)) {
        sel2.setLong(1, Long.parseLong(beneId));
        var rs2 = sel2.executeQuery();
        if (rs2.next()) {
          OffsetDateTime odt = rs2.getObject(1, OffsetDateTime.class);
          assertEquals(expected.toInstant(), odt.toInstant());
        }
      }
    }

    var coverageId = String.format("part-a-%s", beneId);
    var coverage = coverageRead().withId(coverageId).execute();
    assertEquals(expected.toInstant(), coverage.getMeta().getLastUpdated().toInstant());

    // restore
    if (original != null) {
      try (var conn = dataSource.getConnection();
          var upd = conn.prepareStatement(UPDATE_BFD_UPDATED_BENEFICIARY)) {
        upd.setString(1, original);
        upd.setLong(2, Long.parseLong(beneId));
        upd.executeUpdate();
      }
    }
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
    Supplier<java.time.ZonedDateTime> lastUpdatedSupplier;

    switch (type) {
      case CLAIM -> {
        prefixes = LoadProgressTables.claimTablePrefixes();
        lastUpdatedSupplier = claimRepository::claimLastUpdated;
      }
      case BENEFICIARY -> {
        prefixes = LoadProgressTables.beneficiaryTablePrefixes();
        lastUpdatedSupplier = beneficiaryRepository::beneficiaryLastUpdated;
      }
      case COVERAGE -> {
        prefixes = LoadProgressTables.coverageTablePrefixes();
        lastUpdatedSupplier = coverageRepository::coverageLastUpdated;
      }
      default -> throw new IllegalStateException("Unexpected type: " + type);
    }

    for (int i = 0; i < prefixes.size(); i++) {
      entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
      entityManager.flush();

      for (var p : prefixes) insertLoadProgressRow(p, BASE_TIME);

      var bumped = BASE_TIME.plusHours(10L + i);
      insertLoadProgressRow(prefixes.get(i), bumped);

      var actual = lastUpdatedSupplier.get();
      assertEquals(bumped.toInstant(), actual.toInstant(), PREFIX_ASSERT_MSG + prefixes.get(i));
    }
  }
}
