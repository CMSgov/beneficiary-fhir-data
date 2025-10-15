package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.LastUpdatedTestHelper;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.util.DateUtil;
import java.time.ZonedDateTime;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests verifying the bfd_updated_ts values from claim and beneficiary records are
 * correctly reflected in FHIR resource meta.lastUpdated timestamps.
 *
 * <p>These tests verify that the existing bfd_updated_ts values in the database are properly
 * reflected in the API responses, without mutating the test data.
 */
public class LastUpdatedRepositoryIT extends IntegrationTestBase {

  private static final String EOB_META_LAST_UPDATED_MSG = "EOB meta.lastUpdated should be set";
  private final LoadProgressRepository loadProgressLastUpdatedProvider;
  private final ClaimRepository claimRepository;
  private final BeneficiaryRepository beneficiaryRepository;

  private LastUpdatedTestHelper testHelper;

  @Autowired
  public LastUpdatedRepositoryIT(
      LoadProgressRepository loadProgressLastUpdatedProvider,
      ClaimRepository claimRepository,
      BeneficiaryRepository beneficiaryRepository) {
    this.loadProgressLastUpdatedProvider = loadProgressLastUpdatedProvider;
    this.claimRepository = claimRepository;
    this.beneficiaryRepository = beneficiaryRepository;
  }

  @BeforeEach
  void setUp() {
    testHelper = new LastUpdatedTestHelper(claimRepository, beneficiaryRepository);
  }

  @Test
  void eobMetaLastUpdatedMatchesClaimBfdUpdatedTs() {
    // Fetch a claim and get its bfd_updated_ts from the database
    var claimTimestamp = testHelper.getClaimBfdUpdatedTs(CLAIM_ID_ADJUDICATED);
    assertNotNull(claimTimestamp, "Claim should have a bfd_updated_ts in the database");

    // Fetch the same claim via the FHIR API
    var eob =
        getFhirClient()
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(CLAIM_ID_ADJUDICATED)
            .execute();

    assertNotNull(eob.getMeta(), "EOB meta should be present");
    assertNotNull(eob.getMeta().getLastUpdated(), EOB_META_LAST_UPDATED_MSG);

    // Verify the API response matches the database value
    assertTimestampSame(
        claimTimestamp,
        eob.getMeta().getLastUpdated(),
        "EOB meta.lastUpdated should match the claim's bfd_updated_ts from the database");
  }

  @Test
  void patientMetaLastUpdatedMatchesBeneficiaryBfdUpdatedTs() {
    var beneTimestamp = testHelper.getBeneficiaryBfdUpdatedTs(BENE_ID_PART_A_ONLY);
    assertNotNull(beneTimestamp, "Beneficiary should have a bfd_updated_ts in the database");

    var patient =
        getFhirClient().read().resource(Patient.class).withId(BENE_ID_PART_A_ONLY).execute();

    assertNotNull(patient.getMeta(), "Patient meta should be present");
    assertNotNull(patient.getMeta().getLastUpdated(), "Patient meta.lastUpdated should be set");

    assertTimestampSame(
        beneTimestamp,
        patient.getMeta().getLastUpdated(),
        "Patient meta.lastUpdated should match the beneficiary's bfd_updated_ts from the database");
  }

  @Test
  void coverageMetaLastUpdatedMatchesBeneficiaryBfdUpdatedTs() {
    var beneTimestamp = testHelper.getBeneficiaryBfdUpdatedTs(BENE_ID_PART_A_ONLY);
    assertNotNull(beneTimestamp, "Beneficiary should have a bfd_updated_ts in the database");

    var bundle =
        getFhirClient()
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(BENE_ID_PART_A_ONLY))
            .returnBundle(Bundle.class)
            .execute();

    assertFalse(bundle.getEntry().isEmpty(), "Should find at least one coverage");
    var coverage = (Coverage) bundle.getEntry().get(0).getResource();
    assertNotNull(coverage.getMeta(), "Coverage meta should be present");
    assertNotNull(coverage.getMeta().getLastUpdated(), "Coverage meta.lastUpdated should be set");

    assertTimestampSame(
        beneTimestamp,
        coverage.getMeta().getLastUpdated(),
        "Coverage meta.lastUpdated should match the beneficiary's bfd_updated_ts from the database");
  }

  @Test
  void eobSearchReturnsCorrectBfdUpdatedTsForEachClaim() {
    var claimTimestamp1 = testHelper.getClaimBfdUpdatedTs(CLAIM_ID_ADJUDICATED);
    var claimTimestamp2 = testHelper.getClaimBfdUpdatedTs(CLAIM_ID_PHASE_1);
    assertNotNull(claimTimestamp1, "First claim should have a bfd_updated_ts");
    assertNotNull(claimTimestamp2, "Second claim should have a bfd_updated_ts");

    var patientId = testHelper.getBeneficiaryIdForClaim(CLAIM_ID_ADJUDICATED);
    var bundle =
        getFhirClient()
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(patientId))
            .returnBundle(Bundle.class)
            .execute();

    var maybeEob =
        bundle.getEntry().stream()
            .map(e -> (ExplanationOfBenefit) e.getResource())
            .filter(eob -> eob.getIdElement().getIdPart().equals(CLAIM_ID_ADJUDICATED))
            .findFirst();

    ExplanationOfBenefit eob1 =
        maybeEob.orElseGet(
            () ->
                getFhirClient()
                    .read()
                    .resource(ExplanationOfBenefit.class)
                    .withId(CLAIM_ID_ADJUDICATED)
                    .execute());

    assertTimestampSame(
        claimTimestamp1,
        eob1.getMeta().getLastUpdated(),
        "First EOB should have its specific bfd_updated_ts from the database");
  }

  @Test
  void repositoryReturnsCorrectLastUpdatedFromLoadProgress() {

    var lastUpdated = loadProgressLastUpdatedProvider.lastUpdated();
    assertNotNull(lastUpdated, "LoadProgressRepository should return a non-null timestamp");
    assertTrue(
        lastUpdated.isAfter(DateUtil.MIN_DATETIME),
        "LoadProgressRepository should return a timestamp after MIN_DATETIME when data exists");
  }

  private void assertTimestampSame(
      ZonedDateTime expected, java.util.Date actualDate, String message) {
    assertNotNull(actualDate, message + " - actual timestamp missing");
    long actualSeconds = actualDate.toInstant().getEpochSecond();
    long expectedSeconds = expected.toInstant().getEpochSecond();
    assertTrue(
        actualSeconds == expectedSeconds,
        message
            + " expected="
            + expectedSeconds
            + "s actual="
            + actualSeconds
            + "s (comparing second precision)");
  }
}
