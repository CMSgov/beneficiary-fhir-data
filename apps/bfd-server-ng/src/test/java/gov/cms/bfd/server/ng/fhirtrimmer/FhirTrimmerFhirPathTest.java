package gov.cms.bfd.server.ng.fhirtrimmer;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.time.StopWatch;

class FhirTrimmerFhirPathTest {

  private FhirContext fhirContext;
  private Claim testClaim;

  @BeforeEach
  void setUp() {
    fhirContext = FhirContext.forR4Cached();

    // Create a base Claim setup with extensions and nested attributes for testing
    testClaim = new Claim();
    testClaim.setStatus(Claim.ClaimStatus.ACTIVE);

    // Set a complex object field
    Period period = new Period();
    period.setStartElement(new org.hl7.fhir.r4.model.DateTimeType("2026-01-01"));
    period.setEndElement(new org.hl7.fhir.r4.model.DateTimeType("2026-12-31"));
    testClaim.setBillablePeriod(period);

    // Add extensions (one "good", one "bad" to test conditional filtering)
    testClaim.addExtension(new Extension("http://example.com/ext/good", new StringType("Keep Me")));
    testClaim.addExtension(
        new Extension("http://example.com/ext/bad", new StringType("Remove Me")));
  }

  @Test
  void testTrim_NullResource_ReturnsNull() {
    Map<String, List<String>> profileMap = Map.of("Basis", List.of("Claim.status"));
    FhirTrimmerFhirPath trimmer = new FhirTrimmerFhirPath(profileMap, fhirContext);

    assertNull(trimmer.trim(null, "Basis"));
  }

  @Test
  void testTrim_UnmappedProfile_ReturnsUnmodifiedResource() {
    Map<String, List<String>> profileMap = Map.of("Basis", List.of("Claim.status"));
    FhirTrimmerFhirPath trimmer = new FhirTrimmerFhirPath(profileMap, fhirContext);

    // Attempting to trim with a profile name not present in the map configuration
    IBaseResource result = trimmer.trim(testClaim, "UnknownProfile");

    assertNotNull(result);
    assertEquals(Claim.ClaimStatus.ACTIVE, ((Claim) result).getStatus());
    assertTrue(((Claim) result).hasBillablePeriod());
  }

  @Test
  void testTrim_SimpleFieldRemoval() {
    // Arrange: Blacklist the status field and the billablePeriod object field
    Map<String, List<String>> profileMap =
        Map.of("Strict", List.of("Claim.status", "Claim.billablePeriod"));
    FhirTrimmerFhirPath trimmer = new FhirTrimmerFhirPath(profileMap, fhirContext);

    // Act
    Claim trimmedClaim = (Claim) trimmer.trim(testClaim, "Strict");

    // Assert
    assertNull(trimmedClaim.getStatus());
    assertFalse(trimmedClaim.hasBillablePeriod());
    // Ensure untouched fields remain intact
    assertEquals(2, trimmedClaim.getExtension().size());
  }

  @Test
  void testTrim_CompleteCollectionRemoval() {
    // Arrange: Blacklist all extensions entirely
    Map<String, List<String>> profileMap = Map.of("NoExtensions", List.of("Claim.extension"));
    FhirTrimmerFhirPath trimmer = new FhirTrimmerFhirPath(profileMap, fhirContext);

    // Act
    Claim trimmedClaim = (Claim) trimmer.trim(testClaim, "NoExtensions");

    // Assert
    assertTrue(
        trimmedClaim.getExtension().isEmpty(), "All extensions should be completely stripped");
    assertEquals(Claim.ClaimStatus.ACTIVE, trimmedClaim.getStatus());
  }

  @Test
  void testTrim_ConditionalPathRemovalWithWhereClause() {
    // Arrange: Target ONLY the extension matching the "bad" URL
    // Note: Due to findParentPathSplit tracking parentheses depth correctly,
    // the parent calculation correctly falls back to "Claim".
    Map<String, List<String>> profileMap =
        Map.of("Filtered", List.of("Claim.extension.where(url = 'http://example.com/ext/bad')"));
    FhirTrimmerFhirPath trimmer = new FhirTrimmerFhirPath(profileMap, fhirContext);

    // Act
    Claim trimmedClaim = (Claim) trimmer.trim(testClaim, "Filtered");

    // Assert
    assertEquals(1, trimmedClaim.getExtension().size(), "Only one extension should remain");
    assertEquals("http://example.com/ext/good", trimmedClaim.getExtension().get(0).getUrl());
  }

  @Test
  void testTrim_NoMatchesFound_LeavesResourceIntact() {
    // Arrange: Path does not exist inside the current resource payload
    Map<String, List<String>> profileMap = Map.of("Basis", List.of("Claim.accident"));
    FhirTrimmerFhirPath trimmer = new FhirTrimmerFhirPath(profileMap, fhirContext);

    // Act
    Claim trimmedClaim = (Claim) trimmer.trim(testClaim, "Basis");

    // Assert
    assertNotNull(trimmedClaim);
    assertEquals(Claim.ClaimStatus.ACTIVE, trimmedClaim.getStatus());
    assertEquals(2, trimmedClaim.getExtension().size());
  }

  @Test
  @Disabled("Manual benchmark load test for 100,000 iterations")
  void benchmarkFhirTrimmerFhirPathPerformance() {
    // 1. Arrange the trimmer with a realistic blacklist profile
    var profilePathMap = Map.of("Basis", List.of("Claim.extension"));
    var performanceTrimmer = new FhirTrimmerFhirPath(profilePathMap, fhirContext);

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

    var baseClaim = fhirContext.newJsonParser().parseResource(Claim.class, validClaimJson);

    int totalIterations = 100_000;
    int warmupIterations = 5_000;

    // 2. Warm up the JVM JIT compiler to ensure accurate baseline metrics
    System.out.println("Warming up FhirTrimmerFhirPath JIT compiler...");
    for (int i = 0; i < warmupIterations; i++) {
      performanceTrimmer.trim(createBloatedClaim(baseClaim), "Basis");
    }
    System.gc();

    System.out.println(
        "Starting load test of " + totalIterations + " Claims (FhirTrimmerFhirPath)...");
    var watch = StopWatch.createStarted();

    // 3. Execution loop
    for (int i = 0; i < totalIterations; i++) {
      // Pause the timer so object copying/generation doesn't skew trimming results
      watch.suspend();
      var targetClaim = createBloatedClaim(baseClaim);
      watch.resume();

      // Execute the actual trim operation under test
      var returned = (Claim) performanceTrimmer.trim(targetClaim, "Basis");

      // Pause the timer to evaluate assertions safely
      watch.suspend();
      assertTrue(
          returned.getExtension().isEmpty(), "FhirTrimmerFhirPath failed to strip extensions");
      watch.resume();

      // Report throughput snapshots every 20% of the way through
      if (i > 0 && i % (totalIterations / 5) == 0) {
        double currentThroughput = (i * 1000.0) / watch.getTime();
        System.out.printf("Processed %d claims... Current rate: %.2f/sec%n", i, currentThroughput);
      }
    }
    watch.stop();

    printBenchmarkResults("FHIRTRIMMER FHIRPATH", totalIterations, watch.getTime());
  }

  /** Helper method to generate distinct, modified claims for the workload array loop. */
  private Claim createBloatedClaim(Claim templateClaim) {
    Claim claimCopy = templateClaim.copy();
    // Injects explicit payload clutter for the engine to find and delete
    claimCopy.addExtension(
        new Extension("http://example.com/ext/extra-illegal", new StringType("42")));
    claimCopy.addExtension(
        new Extension("http://example.com/ext/literal-crime", new StringType("crime 2.0")));
    return claimCopy;
  }

  private void printBenchmarkResults(String strategyName, int iterations, long totalMillis) {
    double throughput = (iterations * 1000.0) / totalMillis;
    System.out.println("\n=================== " + strategyName + " ===================");
    System.out.println("Total Time:  " + totalMillis + " ms");
    System.out.printf("Throughput:  %.2f Claims per second%n", throughput);
    System.out.printf("Avg Latency: %.4f ms per Claim%n", ((double) totalMillis / iterations));
    System.out.println("=========================================================");
  }
}
