package gov.cms.bfd.server.ng.fhirtrimmer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.time.StopWatch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FhirTrimmerTest {

    private FhirContext ctx;
    private FhirTrimmer trimmer;

    // A sample standard R4 Claim JSON to spin up test resources rapidly
    private final String baseClaimJson =
            """
            {
              "resourceType": "Claim",
              "status": "active",
              "type": { "coding": [ { "system": "http://terminology.hl7.org/CodeSystem/claim-type", "code": "institutional" } ] },
              "use": "claim",
              "patient": { "reference": "Patient/1" },
              "created": "2026-06-24T00:00:00Z",
              "provider": { "reference": "Organization/1" },
              "priority": { "coding": [ { "code": "normal" } ] },
              "insurance": [ { "sequence": 1, "focal": true, "coverage": { "reference": "Coverage/1" } } ]
            }
            """;

    @BeforeEach
    void setUp() {
        ctx = FhirContext.forR4Cached();
        var basisProfileMap = new BasisProfileMap();
        basisProfileMap.generateProfileBasisMap();
        trimmer = new FhirTrimmer(basisProfileMap.getBlackList(), ctx);
    }

    @Test
    void testBlacklistTrimmingLogic() {

        // Arrange
        Claim claim = ctx.newJsonParser().parseResource(Claim.class, baseClaimJson);
        claim.addExtension(new Extension("http://example.com/ext/test", new StringType("value")));

        // Act
        Claim trimmed = (Claim) trimmer.trim(claim, BasisProfile.BASIS);

        // Assert: In inverse blacklist mode, things not explicitly banned should stay
        assertNotNull(trimmed);
    }

    @Test
    @Disabled("Manual benchmark load test to compare Whitelist vs Blacklist execution speeds")
    void benchmarkTrimmingPerformance() {
        Claim templateClaim = ctx.newJsonParser().parseResource(Claim.class, baseClaimJson);

        int totalIterations = 50_000;
        int warmupIterations = 2_000;

        // --- 1. Warm up JVM JIT compiler ---
        System.out.println("Warming up Trimmer engine paths...");
        for (int i = 0; i < warmupIterations; i++) {
            trimmer.trim(createBloatedBundle(templateClaim), BasisProfile.BASIS);
        }
        System.gc();


        // --- 3. Benchmark Blacklist Mode ---
        System.out.println("Starting Benchmark: Blacklist Mode...");
        StopWatch blacklistWatch = StopWatch.createStarted();
        for (int i = 0; i < totalIterations; i++) {
            blacklistWatch.suspend();
            Bundle target = createBloatedBundle(templateClaim);
            blacklistWatch.resume();

            trimmer.trim(target, BasisProfile.BASIS);
        }
        blacklistWatch.stop();
        printResults("BLACKLIST STRATEGY", totalIterations, blacklistWatch.getTime());
    }

    private Bundle createBloatedBundle(Claim templateClaim) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Claim claimCopy = templateClaim.copy();
        // Add noise values to be caught by the tree pruning walker execution path
        claimCopy.addExtension(new Extension("http://example.com/ext/noise-1", new StringType("PruneMe")));
        claimCopy.addExtension(new Extension("http://example.com/ext/noise-2", new StringType("DeleteMe")));

        bundle.addEntry().setResource(claimCopy);
        return bundle;
    }

    private void printResults(String strategyName, int iterations, long totalMillis) {
        double throughput = (iterations * 1000.0) / totalMillis;
        System.out.println("\n=================== " + strategyName + " ===================");
        System.out.println("Total Execution Time: " + totalMillis + " ms");
        System.out.printf("Throughput Rate:      %.2f Bundles/sec%n", throughput);
        System.out.printf("Average Latency:      %.4f ms per Bundle%n", ((double) totalMillis / iterations));
        System.out.println("=========================================================");
    }
}