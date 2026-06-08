package gov.cms.bfd.server.ng.trimmer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.commons.lang3.time.StopWatch;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Disabled;

class FhirTrimmerValidationTest {

  private FhirContext ctx;
  private FhirTrimmer_Validation trimmer;

  @BeforeEach
  void setUp() {

    ctx = FhirContext.forR4Cached();

    var supportChain =
        new ValidationSupportChain(
            new DefaultProfileValidationSupport(ctx),
            new InMemoryTerminologyServerValidationSupport(ctx),
            new CommonCodeSystemsTerminologyService(ctx));

    var sdJson =
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

    var strictClaimDef = ctx.newJsonParser().parseResource(StructureDefinition.class, sdJson);

    var prePopulatedSupport = new PrePopulatedValidationSupport(ctx);
    prePopulatedSupport.addStructureDefinition(strictClaimDef);

    supportChain.addValidationSupport(prePopulatedSupport);

    var instanceValidator = new FhirInstanceValidator(supportChain);
    instanceValidator.setAnyExtensionsAllowed(false);

    var validator = ctx.newValidator().registerValidatorModule(instanceValidator);

    trimmer = new FhirTrimmer_Validation(validator);
  }

  @Test
  void testFhirTrimmer() {

    // Valid claim with all required elements filled out to quiet validation
    var validClaimJson = """
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

    var fullClaim = ctx.newJsonParser().parseResource(Claim.class, validClaimJson);

    fullClaim.getMeta().addProfile("http://example.com/StructureDefinition/StrictClaim");

    // Add illegal extensions
    fullClaim.addExtension(new Extension("http://example.com/ext/bad", new StringType("very illegal")));
    fullClaim.addExtension(new Extension("http://example.com/ext/not-allowed", new StringType("so illegal")));
    assertEquals(2, fullClaim.getExtension().size(), "Resource should have 2 extensions");

    var trimmedClaim = (Claim) trimmer.trim(fullClaim);

    assertTrue(trimmedClaim.getExtension().isEmpty(), "Extensions should be empty");
  }

  @Test
  @Disabled
  void reallyTestFhirTrimmer() {
    var validClaimJson = """
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

    var totalIterations = 100000;
    var warmupIterations = 5000;

    System.out.println("Warming up JIT compiler with " + warmupIterations + " runs...");
    for (int i = 0; i < warmupIterations; i++) {
      var warmupBundle = createBloatedBundle(baseClaim);
      trimmer.trim(warmupBundle);
    }
    System.gc();

    System.out.println("Starting load test of " + totalIterations + " Bundles...");
    var watch = StopWatch.createStarted();

    for (int i = 0; i < totalIterations; i++) {
      watch.suspend();
      var targetBundle = createBloatedBundle(baseClaim);
      watch.resume();

      trimmer.trim(targetBundle);

      if (i > 0 && i % (totalIterations/5) == 0) {
        double currentThroughput = (i * 1000.0) / watch.getTime();
        System.out.printf("Processed %d bundles... Current rate: %.2f/sec%n", i, currentThroughput);
      }
    }

    var totalMillis = watch.getTime();
    var finalThroughput = (totalIterations * 1000.0) / totalMillis;

    System.out.println("=================== LOAD TEST RESULTS ===================");
    System.out.println("Total Time: " + watch);
    System.out.printf("Throughput: %.2f Bundles per second%n", finalThroughput);
    System.out.printf("Avg Latency: %.2f ms per Bundle%n", ((double) totalMillis / totalIterations));
    System.out.println("=========================================================");
  }

  /**
   * Use copy to save time.
   */
  private Bundle createBloatedBundle(Claim templateClaim) {
    var bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);

    var claimCopy = templateClaim.copy();

    claimCopy.addExtension(new Extension("http://example.com/ext/forbidden", new StringType("illegal 1")));
    claimCopy.addExtension(new Extension("http://example.com/ext/also-forbidden", new StringType("illegal 2")));

    bundle.addEntry().setResource(claimCopy);
    return bundle;
  }
}
