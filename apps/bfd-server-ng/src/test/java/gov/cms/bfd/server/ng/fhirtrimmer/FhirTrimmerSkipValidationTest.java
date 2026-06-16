package gov.cms.bfd.server.ng.fhirtrimmer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class FhirTrimmerSkipValidationTest {

  private FhirTrimmer_SkipValidation skipValidationTrimmer;
  private Claim baseClaim;

  @BeforeEach
  void setUp() {
    FhirContext ctx = FhirContext.forR4Cached();

    // Simulating a highly restricted profile configuration containing many rule paths
    var profileCache =
        Map.of(
            "http://example.com/StructureDefinition/StrictCMSClaim",
            List.of(
                "Claim.extension",
                "Claim.supportingInfo[1]", // Target specific index
                "Claim.supportingInfo[2]",
                "Claim.careTeam", // Wipe entire arrays
                "Claim.diagnosis"));

    skipValidationTrimmer = new FhirTrimmer_SkipValidation(profileCache);

    // Baseline minimal valid claim setup
    var validClaimJson =
        """
        {
          "resourceType": "Claim",
          "status": "active",
          "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/claim-type", "code": "institutional" } ] },
          "use": "claim",
          "patient": { "reference": "Patient/1" },
          "created": "2026-06-05T00:00:00Z",
          "provider": { "reference": "Organization/1" },
          "priority": { "coding": [ { "code": "normal" } ] },
          "insurance": [ { "sequence": 1, "focal": true, "coverage": { "reference": "Coverage/1" } } ]
        }
        """;
    baseClaim = ctx.newJsonParser().parseResource(Claim.class, validClaimJson);
    baseClaim.getMeta().addProfile("http://example.com/StructureDefinition/StrictCMSClaim");
  }

  @Test
  void testSkipValidationTrimmer_MultipleRules() {
    Bundle testBundle = createBloatedBundle(baseClaim);
    Claim claim = (Claim) testBundle.getEntry().getFirst().getResource();

    // Verify properties are populated prior to trimming
    assertEquals(2, claim.getExtension().size());
    assertEquals(3, claim.getSupportingInfo().size());
    assertFalse(claim.getCareTeam().isEmpty());
    assertFalse(claim.getDiagnosis().isEmpty());

    skipValidationTrimmer.trim(testBundle);

    Claim trimmedClaim = (Claim) testBundle.getEntry().getFirst().getResource();
    // Assert that all targeted structural paths were successfully stripped
    assertTrue(trimmedClaim.getExtension().isEmpty(), "Extensions should be wiped");
    assertTrue(trimmedClaim.getCareTeam().isEmpty(), "CareTeam array should be wiped");
    assertTrue(trimmedClaim.getDiagnosis().isEmpty(), "Diagnosis array should be wiped");

    // Index specific pruning check: index [1] and [2] removed, leaving exactly index [0] intact
    assertEquals(
        1, trimmedClaim.getSupportingInfo().size(), "Only supportingInfo[0] should remain");
    assertEquals(
        "allowed-info",
        trimmedClaim.getSupportingInfo().getFirst().getCategory().getCodingFirstRep().getCode());
  }

  @Test
  @Disabled("Manual benchmark load test")
  void reallyTestFhirTrimmers() {
    var totalIterations = 1_000_000;
    var warmupIterations = 10_000;

    System.out.println("Warming up Proactive Trimmer JIT compiler with multi-rule data...");
    for (int i = 0; i < warmupIterations; i++) {
      skipValidationTrimmer.trim(createBloatedBundle(baseClaim));
    }
    System.gc();

    System.out.println("Starting heavy load test of " + totalIterations + " complex Bundles...");
    var proactiveWatch = StopWatch.createStarted();

    for (int i = 0; i < totalIterations; i++) {
      proactiveWatch.suspend();
      var targetBundle = createBloatedBundle(baseClaim);
      proactiveWatch.resume();

      skipValidationTrimmer.trim(targetBundle);

      if (i > 0 && i % (totalIterations / 5) == 0) {
        double currentThroughput = (i * 1000.0) / proactiveWatch.getTime();
        System.out.printf("Processed %d bundles... Current rate: %.2f/sec%n", i, currentThroughput);
      }
    }
    proactiveWatch.stop();

    printResults(totalIterations, proactiveWatch.getTime());
  }

  private void printResults(int iterations, long totalMillis) {
    double throughput = (iterations * 1000.0) / totalMillis;
    System.out.println(
        "\n=================== PROACTIVE (SKIP-VALIDATION) BENCHMARK ===================");
    System.out.println("Total Time:  " + totalMillis + " ms");
    System.out.printf("Throughput:  %.2f Bundles per second%n", throughput);
    System.out.printf("Avg Latency: %.4f ms per Bundle%n", ((double) totalMillis / iterations));
    System.out.println(
        "=============================================================================");
  }

  /** Generates highly varied, deep data trees matching the structural targets of the cache. */
  private Bundle createBloatedBundle(Claim templateClaim) {
    var bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);

    var claimCopy = templateClaim.copy();

    // 1. Add illegal extensions
    claimCopy.addExtension(
        new Extension("http://example.com/ext/bad-1", new StringType("value-1")));
    claimCopy.addExtension(
        new Extension("http://example.com/ext/bad-2", new StringType("value-2")));

    // 2. Add an item array that should remain untouched
    claimCopy.addItem().setSequence(1).setFactor(1.5);

    // 3. Build a multi-item array for testing precise index removals
    claimCopy.addSupportingInfo().setSequence(1).getCategory().addCoding().setCode("allowed-info");
    claimCopy
        .addSupportingInfo()
        .setSequence(2)
        .getCategory()
        .addCoding()
        .setCode("forbidden-info-1");
    claimCopy
        .addSupportingInfo()
        .setSequence(3)
        .getCategory()
        .addCoding()
        .setCode("forbidden-info-2");

    // 4. Populate complex arrays slated for complete deletion
    claimCopy.addCareTeam().setSequence(1).setProvider(new Reference("Practitioner/99"));
    claimCopy.addDiagnosis().setSequence(1).getDiagnosisReference().setReference("Condition/101");

    bundle.addEntry().setResource(claimCopy);
    return bundle;
  }
}
