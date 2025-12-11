package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.coverage.CoverageRepository;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.DateUtil;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests verifying the bfd_updated_ts values from claim and beneficiary records are
 * correctly reflected in FHIR resource meta.lastUpdated timestamps.
 *
 * <p>These tests verify that the existing bfd_updated_ts values in the database are properly
 * reflected in the API responses, without mutating the test data.
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BfdUpdatedTimestampIT extends IntegrationTestBase {

  private static final String EOB_META_LAST_UPDATED_MSG = "EOB meta.lastUpdated should be set";
  private final LoadProgressRepository loadProgressRepository;
  private final ClaimRepository claimRepository;
  private final BeneficiaryRepository beneficiaryRepository;
  private final CoverageRepository coverageRepository;

  @Test
  void eobMetaLastUpdatedMatchesClaimBfdUpdatedTs() {
    // Fetch a claim and get its bfd_updated_ts from the database
    var claimTimestamp =
        getClaimBfdUpdatedTs(CLAIM_ID_ADJUDICATED)
            .orElseThrow(
                () -> new AssertionError("Claim should have a bfd_updated_ts in the database"));

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
    var beneTimestamp =
        getBeneficiaryBfdUpdatedTs(BENE_ID_PART_A_ONLY)
            .orElseThrow(
                () ->
                    new AssertionError("Beneficiary should have a bfd_updated_ts in the database"));

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
    var beneTimestamp =
        getCoverageBfdUpdatedTs(
                BENE_ID_PART_A_ONLY, CoverageCompositeId.parse("part-a-" + BENE_ID_PART_A_ONLY))
            .orElseThrow(
                () ->
                    new AssertionError("Beneficiary should have a bfd_updated_ts in the database"));

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
    var claimTimestamp1 =
        getClaimBfdUpdatedTs(CLAIM_ID_ADJUDICATED)
            .orElseThrow(() -> new AssertionError("First claim should have a bfd_updated_ts"));

    var patientId =
        getBeneficiaryIdForClaim(CLAIM_ID_ADJUDICATED)
            .orElseThrow(() -> new AssertionError("Could not find beneficiary for claim"));
    var bundle =
        getFhirClient()
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.PATIENT.hasId(patientId))
            .returnBundle(Bundle.class)
            .execute();

    var eob1 =
        bundle.getEntry().stream()
            .map(e -> (ExplanationOfBenefit) e.getResource())
            .filter(eob -> eob.getIdElement().getIdPart().equals(CLAIM_ID_ADJUDICATED))
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "EOB " + CLAIM_ID_ADJUDICATED + " not found in search results"));

    assertTimestampSame(
        claimTimestamp1,
        eob1.getMeta().getLastUpdated(),
        "First EOB should have its specific bfd_updated_ts from the database");
  }

  @Test
  void repositoryReturnsCorrectLastUpdatedFromLoadProgress() {

    var lastUpdated = loadProgressRepository.lastUpdated();
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
    assertEquals(expectedSeconds, actualSeconds, message + " (comparing second precision)");
  }

  private java.util.Optional<ZonedDateTime> getClaimBfdUpdatedTs(String claimId) {
    var id = Long.parseLong(claimId);
    var claimOpt = claimRepository.findById(id, new DateTimeRange(), new DateTimeRange());
    return claimOpt.map(
        claim -> {
          var eob = claim.toFhir(ClaimSecurityStatus.NONE);
          return eob.getMeta().getLastUpdated().toInstant().atZone(ZoneId.of("UTC"));
        });
  }

  private java.util.Optional<ZonedDateTime> getBeneficiaryBfdUpdatedTs(String beneSk) {
    var beneId = Long.parseLong(beneSk);
    var beneOpt = beneficiaryRepository.findById(beneId, new DateTimeRange());
    return beneOpt.map(
        bene -> {
          var patient = bene.toFhir(ProfileType.C4BB);
          return patient.getMeta().getLastUpdated().toInstant().atZone(ZoneId.of("UTC"));
        });
  }

  private java.util.Optional<ZonedDateTime> getCoverageBfdUpdatedTs(
      String beneSk, CoverageCompositeId coverageCompositeId) {
    var beneId = Long.parseLong(beneSk);
    var beneOpt = coverageRepository.searchBeneficiaryWithCoverage(beneId, new DateTimeRange());
    return beneOpt.map(
        bene -> {
          var coverage =
              bene.toFhirCoverageIfPresent(coverageCompositeId, ProfileType.C4BB).stream()
                  .findFirst()
                  .get();
          return coverage.getMeta().getLastUpdated().toInstant().atZone(ZoneId.of("UTC"));
        });
  }

  private java.util.Optional<String> getBeneficiaryIdForClaim(String claimId) {
    var id = Long.parseLong(claimId);
    var claimOpt = claimRepository.findById(id, new DateTimeRange(), new DateTimeRange());
    return claimOpt.map(claim -> String.valueOf(claim.getBeneficiary().getXrefSk()));
  }
}
