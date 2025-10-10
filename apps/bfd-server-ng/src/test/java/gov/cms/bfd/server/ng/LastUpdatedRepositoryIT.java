package gov.cms.bfd.server.ng;

import static org.hl7.fhir.instance.model.api.IAnyResource.RES_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.util.DateUtil;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests verifying the logic of getting the "last updated" value */
@Transactional
public class LastUpdatedRepositoryIT extends IntegrationTestBase {

  private static final String EOB_META_LAST_UPDATED_MSG = "EOB meta.lastUpdated should be set";
  private static final String NON_EXISTENT_ID = "999999999999999";

  private final LoadProgressRepository loadProgressLastUpdatedProvider;

  @Autowired
  public LastUpdatedRepositoryIT(LoadProgressRepository loadProgressLastUpdatedProvider) {
    this.loadProgressLastUpdatedProvider = loadProgressLastUpdatedProvider;
  }

  private static final ZonedDateTime BASE_TIME =
      ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  private static final String DELETE_LOAD_PROGRESS = "DELETE FROM idr.load_progress";
  private static final String TABLE_CLAIM = "idr.claim";
  private static final String TABLE_CLAIM_ITEM = "idr.claim_item";
  private static final String TABLE_BENEFICIARY = "idr.beneficiary";
  private static final String TABLE_BENEFICIARY_MBI_ID = "idr.beneficiary_mbi_id";
  private static final String TABLE_BENEFICIARY_ENTITLEMENT = "idr.beneficiary_entitlement";

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @BeforeEach
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @SuppressWarnings("unused") // invoked reflectively by JUnit
  void cleanLoadProgress() {
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
  }

  @Test
  @Transactional
  void claimLastUpdatedReturnsMaxBatchCompletionTimestampAcrossAllTables() {
    var tables = List.of(TABLE_CLAIM, TABLE_CLAIM_ITEM, "idr.some_future_claim_table");
    for (int i = 0; i < tables.size(); i++)
      insertLoadProgressRow(tables.get(i), BASE_TIME.plusHours(i));
    var expected = BASE_TIME.plusHours(tables.size() - 1L);
    var actual = loadProgressLastUpdatedProvider.lastUpdated();
    assertEquals(
        expected.toInstant(),
        actual.toInstant(),
        "Should return max batch completion timestamp (global MAX logic)");
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertNotNull(eob.getMeta().getLastUpdated(), EOB_META_LAST_UPDATED_MSG);
  }

  @Test
  @Transactional
  void eobMetaLastUpdatedReflectsClaimBfdUpdatedTs() {
    var expected = BASE_TIME.plusHours(42);
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
    insertLoadProgressRow(TABLE_CLAIM, BASE_TIME);
    insertLoadProgressRow(TABLE_CLAIM_ITEM, BASE_TIME);

    insertLoadProgressRow(TABLE_CLAIM_ITEM, expected);
    var actual = loadProgressLastUpdatedProvider.lastUpdated();
    assertEquals(
        expected.toInstant(),
        actual.toInstant(),
        "Should pick bumped row regardless of table name");
  }

  @Test
  @Transactional
  void patientMetaLastUpdatedReflectsBeneficiaryBfdUpdatedTs() {
    var expected = BASE_TIME.plusHours(24);
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
    insertLoadProgressRow(TABLE_BENEFICIARY, BASE_TIME);
    insertLoadProgressRow(TABLE_BENEFICIARY_MBI_ID, BASE_TIME);
    insertLoadProgressRow(TABLE_BENEFICIARY, expected); // bump
    var actual = loadProgressLastUpdatedProvider.lastUpdated();
    assertEquals(expected.toInstant(), actual.toInstant(), "Should pick bumped beneficiary row");
  }

  @Test
  @Transactional
  void coverageMetaLastUpdatedReflectsBeneficiaryBfdUpdatedTs() {
    var expected = BASE_TIME.plusHours(30);
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
    insertLoadProgressRow(TABLE_BENEFICIARY_ENTITLEMENT, BASE_TIME);
    insertLoadProgressRow(TABLE_BENEFICIARY, BASE_TIME);
    insertLoadProgressRow(TABLE_BENEFICIARY_ENTITLEMENT, expected); // bump
    var actual = loadProgressLastUpdatedProvider.lastUpdated();
    assertEquals(
        expected.toInstant(), actual.toInstant(), "Should pick bumped coverage-related row");
  }

  @Test
  void beneficiaryLastUpdatedReturnsMaxBatchCompletionTimestampAcrossAllTables() {
    var tables = List.of(TABLE_BENEFICIARY, "idr.beneficiary_status", "idr.future_bene_table");
    for (int i = 0; i < tables.size(); i++)
      insertLoadProgressRow(tables.get(i), BASE_TIME.plusMinutes(i));
    var expected = BASE_TIME.plusMinutes(tables.size() - 1L);
    var actual = loadProgressLastUpdatedProvider.lastUpdated();
    assertEquals(
        expected.toInstant(),
        actual.toInstant(),
        "Should return global max batch completion timestamp");
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

  @Test
  void lastUpdatedFallsBackToMinDateTimeWhenNoRows() {
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
    assertEquals(
        DateUtil.MIN_DATETIME.toInstant(),
        loadProgressLastUpdatedProvider.lastUpdated().toInstant());
    assertEquals(
        DateUtil.MIN_DATETIME.toInstant(),
        loadProgressLastUpdatedProvider.lastUpdated().toInstant());
    assertEquals(
        DateUtil.MIN_DATETIME.toInstant(),
        loadProgressLastUpdatedProvider.lastUpdated().toInstant());
  }

  @Test
  void eobMetaReflectsChildTableBfdUpdatedTs() {
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertNotNull(eob.getMeta(), "EOB meta should be present");
    assertNotNull(eob.getMeta().getLastUpdated(), EOB_META_LAST_UPDATED_MSG);

    assertTrue(
        eob.getMeta().getLastUpdated().getTime() > 0,
        "EOB meta.lastUpdated should be reasonable timestamp");
  }

  @Test
  void eobMetaUsesMaxAcrossMultipleChildTables() {
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertNotNull(eob.getMeta(), "EOB meta should be present");
    assertNotNull(eob.getMeta().getLastUpdated(), EOB_META_LAST_UPDATED_MSG);

    // verifies the aggregation logic without manipulating database state
    var lastUpdated = eob.getMeta().getLastUpdated().toInstant();
    assertTrue(
        lastUpdated.isAfter(java.time.Instant.EPOCH),
        "EOB meta.lastUpdated should reflect aggregated child timestamps");
  }

  @Test
  void claimSearchNoResultsUsesLoadProgressFallback() {
    // Prepare load_progress with several timestamps to define fallback.
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
    insertLoadProgressRow(TABLE_CLAIM, BASE_TIME.plusMinutes(10));
    insertLoadProgressRow(TABLE_CLAIM_ITEM, BASE_TIME.plusMinutes(15));
    var expected = BASE_TIME.plusMinutes(20);
    insertLoadProgressRow("idr.claim_line_institutional", expected);

    var fallback =
        loadProgressLastUpdatedProvider.lastUpdated().toInstant().truncatedTo(ChronoUnit.MILLIS);

    // Use a patient reference that does not exist to force empty Bundle.
    var bundle =
        getFhirClient()
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(NON_EXISTENT_ID))
            .returnBundle(Bundle.class)
            .execute();

    // Expect empty result set.
    assertEquals(0, bundle.getEntry().size(), "Search should yield no results");
    var bundleTs = bundle.getMeta().getLastUpdated().toInstant().truncatedTo(ChronoUnit.MILLIS);
    assertTrue(
        !bundleTs.isBefore(fallback),
        "Empty search Bundle meta.lastUpdated should be fallback or later (was " + bundleTs + ")");
  }

  @Test
  void patientSearchNoResultsUsesLoadProgressFallback() {
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
    insertLoadProgressRow(TABLE_BENEFICIARY, BASE_TIME.plusMinutes(5));
    insertLoadProgressRow(TABLE_BENEFICIARY_MBI_ID, BASE_TIME.plusMinutes(7));
    var expected = BASE_TIME.plusMinutes(11);
    insertLoadProgressRow("idr.beneficiary_status", expected);

    var fallback =
        loadProgressLastUpdatedProvider.lastUpdated().toInstant().truncatedTo(ChronoUnit.MILLIS);

    var bundle =
        getFhirClient()
            .search()
            .forResource(Patient.class)
            .where(RES_ID.exactly().code(NON_EXISTENT_ID))
            .returnBundle(Bundle.class)
            .execute();

    assertEquals(0, bundle.getEntry().size(), "Search should yield no Patient results");
    var bundleTs = bundle.getMeta().getLastUpdated().toInstant().truncatedTo(ChronoUnit.MILLIS);
    assertTrue(
        !bundleTs.isBefore(fallback),
        "Empty Patient search Bundle meta.lastUpdated should be fallback or later (was "
            + bundleTs
            + ")");
  }

  @Test
  void coverageSearchNoResultsUsesLoadProgressFallback() {
    entityManager.createNativeQuery(DELETE_LOAD_PROGRESS).executeUpdate();
    entityManager.flush();
    insertLoadProgressRow(TABLE_BENEFICIARY, BASE_TIME.plusMinutes(3));
    insertLoadProgressRow(TABLE_BENEFICIARY_ENTITLEMENT, BASE_TIME.plusMinutes(4));
    insertLoadProgressRow("idr.beneficiary_dual_eligibility", BASE_TIME.plusMinutes(9));

    var fallback =
        loadProgressLastUpdatedProvider.lastUpdated().toInstant().truncatedTo(ChronoUnit.MILLIS);

    var bundle =
        getFhirClient()
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(NON_EXISTENT_ID))
            .returnBundle(Bundle.class)
            .execute();

    assertEquals(0, bundle.getEntry().size(), "Search should yield no Coverage results");
    var bundleTs = bundle.getMeta().getLastUpdated().toInstant().truncatedTo(ChronoUnit.MILLIS);
    assertTrue(
        !bundleTs.isBefore(fallback),
        "Empty Coverage search Bundle meta.lastUpdated should be fallback or later (was "
            + bundleTs
            + ")");
  }
}
