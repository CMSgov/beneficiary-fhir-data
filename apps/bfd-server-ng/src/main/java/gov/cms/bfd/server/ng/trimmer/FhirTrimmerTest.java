package gov.cms.bfd.server.ng.trimmer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.converter.*;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FhirTrimmerTest {

    private FhirContext ctx;
    private FhirValidator validator;
    private FhirTrimmer trimmer;

    @BeforeEach
    void setUp() {
        // Initialize context
        ctx = FhirContext.forR4Cached();

        // 1. Build the Validation Support Chain
        // This is required to resolve base profiles and terminologies
        var supportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx),
                new CommonCodeSystemsTerminologyService(ctx)
        );

        // 2. Load the Custom StructureDefinition Fixture
        // We define a differential that forces Claim.extension to have a max of 0.
        String sdJson = """
            {
              "resourceType": "StructureDefinition",
              "url": "http://example.com/StructureDefinition/StrictClaim",
              "name": "StrictClaim",
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

        StructureDefinition strictClaimDef = ctx.newJsonParser().parseResource(StructureDefinition.class, sdJson);

        // Add our custom profile to a PrePopulated support module
        var prePopulatedSupport = new PrePopulatedValidationSupport(ctx);
        prePopulatedSupport.addStructureDefinition(strictClaimDef);

        // Append it to the main chain
        supportChain.addValidationSupport(prePopulatedSupport);

        // 3. Configure and Inject the Validator
        var instanceValidator = new FhirInstanceValidator(supportChain);
        // Turn off any external terminology server calls to keep unit tests fast/offline
        instanceValidator.setAnyExtensionsAllowed(false);

        validator = ctx.newValidator().registerValidatorModule(instanceValidator);

        // Initialize the trimmer Proof of Concept
        trimmer = new FhirTrimmer(ctx, validator);
    }

    @Test
    void testTrim_RemovesForbiddenExtensions() {
        // Arrange: Create a bloated Claim fixture
        Claim bloatedClaim = new Claim();
        // CRITICAL: Bind the resource to our custom profile so the validator checks it
        bloatedClaim.getMeta().addProfile("http://example.com/StructureDefinition/StrictClaim");

        bloatedClaim.addExtension(new Extension("http://example.com/ext/forbidden", new StringType("illegal value")));
        bloatedClaim.addExtension(new Extension("http://example.com/ext/also-forbidden", new StringType("illegal value 2")));

        assertEquals(2, bloatedClaim.getExtension().size(), "Resource should start with 2 extensions");

        // Act: Run the trimmer
        Claim trimmedClaim = (Claim) trimmer.trim(bloatedClaim);

        // Assert: The extensions should be wiped out because of the max=0 constraint in the SD
        assertTrue(trimmedClaim.getExtension().isEmpty(), "Extensions should be completely trimmed");
    }
}
