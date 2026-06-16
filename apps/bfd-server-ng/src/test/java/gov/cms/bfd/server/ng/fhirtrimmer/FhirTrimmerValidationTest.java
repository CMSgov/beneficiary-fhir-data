package gov.cms.bfd.server.ng.fhirtrimmer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FhirTrimmerValidationTest {

  private FhirContext ctx;
  private FhirTrimmer_Validation validationTrimmer;
  private FhirTrimmer_SkipValidation skipValidationTrimmer;

  @BeforeEach
  void setUp() {
    ctx = FhirContext.forR4Cached();

    var supportChain =
        new ValidationSupportChain(
            new DefaultProfileValidationSupport(ctx),
            new InMemoryTerminologyServerValidationSupport(ctx),
            new CommonCodeSystemsTerminologyService(ctx));

    var structureDefinition =
        """
                {
                  "resourceType": "StructureDefinition",
                  "url": "http://example.com/StructureDefinition/TestStructureDefinition",
                  "name": "TestStructureDefinition",
                  "status": "active",
                  "fhirVersion": "4.0.1",
                  "kind": "resource",
                  "abstract": false,
                  "type": "Claim",
                  "baseDefinition": "http://hl7.org/fhir/StructureDefinition/Claim",
                  "derivation": "constraint",
                  "differential": {
                    "element": [
                      {
                        "id": "Claim.extension",
                        "path": "Claim.extension",
                        "max": "0"
                      }
                    ]
                  }
                }
                """;

    var strictClaimDef =
        ctx.newJsonParser().parseResource(StructureDefinition.class, structureDefinition);
    var prePopulatedSupport = new PrePopulatedValidationSupport(ctx);
    prePopulatedSupport.addStructureDefinition(strictClaimDef);
    supportChain.addValidationSupport(prePopulatedSupport);

    var instanceValidator = new FhirInstanceValidator(supportChain);
    var validator = ctx.newValidator().registerValidatorModule(instanceValidator);

    validationTrimmer = new FhirTrimmer_Validation(validator);

    var profileCache =
        Map.of("http://example.com/StructureDefinition/StrictClaim", List.of("Claim.extension"));
    skipValidationTrimmer = new FhirTrimmer_SkipValidation(profileCache);
  }

  @Test
  void testFhirTrimmers() {
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

    var claimForValidation = ctx.newJsonParser().parseResource(Claim.class, validClaimJson);
    claimForValidation.getMeta().addProfile("http://example.com/StructureDefinition/StrictClaim");
    claimForValidation.addExtension(
        new Extension("http://example.com/ext/bad", new StringType("very illegal")));

    var trimmedValidation = (Claim) validationTrimmer.trim(claimForValidation);
    assertTrue(trimmedValidation.getExtension().isEmpty(), "Validation trimmer failed");

    var claimForSkipValidation = ctx.newJsonParser().parseResource(Claim.class, validClaimJson);
    claimForSkipValidation
        .getMeta()
        .addProfile("http://example.com/StructureDefinition/StrictClaim");
    claimForSkipValidation.addExtension(
        new Extension("http://example.com/ext/bad", new StringType("very illegal")));

    var trimmedSkip = (Claim) skipValidationTrimmer.trim(claimForSkipValidation);
    assertTrue(trimmedSkip.getExtension().isEmpty(), "Skip-validation trimmer failed");
  }

  @Test
  @Disabled("Manual benchmark load test")
  void reallyTestFhirTrimmers() {
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

    var baseClaim = ctx.newJsonParser().parseResource(Claim.class, validClaimJson);
    baseClaim.getMeta().addProfile("http://example.com/StructureDefinition/StrictClaim");

    var totalIterations = 10000;
    var warmupIterations = 1000;

    // =========================================================================
    // PHASE 1: BENCHMARK VALIDATION TRIMMER
    // =========================================================================
    //    System.out.println("Warming up Validation Trimmer JIT compiler...");
    //    for (int i = 0; i < warmupIterations; i++) {
    //      validationTrimmer.trim(createBloatedBundle(baseClaim));
    //    }
    //    System.gc();
    //
    //    System.out.println("Starting load test of " + totalIterations + " Bundles (Validation
    // Trimmer)...");
    //    var validationWatch = StopWatch.createStarted();
    //    for (int i = 0; i < totalIterations; i++) {
    //      validationWatch.suspend();
    //      var targetBundle = createBloatedBundle(baseClaim);
    //      validationWatch.resume();
    //
    //      validationTrimmer.trim(targetBundle);
    //    }
    //    validationWatch.stop();

    // =========================================================================
    // PHASE 2: BENCHMARK SKIP VALIDATION (PROACTIVE) TRIMMER
    // =========================================================================
    System.out.println("\nWarming up Proactive Trimmer JIT compiler...");
    for (int i = 0; i < warmupIterations; i++) {
      skipValidationTrimmer.trim(createBloatedBundle(baseClaim));
    }
    System.gc();

    totalIterations = 1000000;

    System.out.println(
        "Starting load test of " + totalIterations + " Bundles (Proactive Trimmer)...");
    var proactiveWatch = StopWatch.createStarted();
    for (int i = 0; i < totalIterations; i++) {
      proactiveWatch.suspend();
      var targetBundle = createBloatedBundle(baseClaim);
      proactiveWatch.resume();

      var returned = (Bundle) skipValidationTrimmer.trim(targetBundle);

      proactiveWatch.suspend();
      assertTrue(
          returned.getEntry().getFirst().getExtension().isEmpty(),
          "Skip-validation trimmer failed");
      proactiveWatch.resume();
    }
    proactiveWatch.stop();

    // =========================================================================
    // PRINT COMPARISON REPORT
    // =========================================================================
    //    printResults("VALIDATION-BASED TRIMMER", totalIterations, validationWatch.getTime());
    printResults(totalIterations, proactiveWatch.getTime());
  }

  private void printResults(int iterations, long totalMillis) {
    double throughput = (iterations * 1000.0) / totalMillis;
    System.out.println(
        "\n=================== " + "PROACTIVE (SKIP-VALIDATION) TRIMMER" + " ===================");
    System.out.println("Total Time:  " + totalMillis + " ms");
    System.out.printf("Throughput:  %.2f Bundles per second%n", throughput);
    System.out.printf("Avg Latency: %.4f ms per Bundle%n", ((double) totalMillis / iterations));
    System.out.println("=========================================================");
  }

  private Bundle createBloatedBundle(Claim templateClaim) {
    var bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);

    var claimCopy = templateClaim.copy();
    claimCopy.addExtension(
        new Extension("http://example.com/ext/extra-illegal", new StringType("42")));
    claimCopy.addExtension(
        new Extension("http://example.com/ext/literal-crime", new StringType("crime 2.0")));

    bundle.addEntry().setResource(claimCopy);
    return bundle;
  }
}
