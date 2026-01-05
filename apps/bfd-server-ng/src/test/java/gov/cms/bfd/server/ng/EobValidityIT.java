package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests that enforce data integrity and invariants for ExplanationOfBenefit resources.
 * This covers both direct Read operations and Search results to ensure all returned EOBs meet the
 * required profile and field population standards.
 */
class EobValidityIT extends IntegrationTestBase {

  private void validateEob(ExplanationOfBenefit eob) {
    assertFalse(eob.isEmpty(), "EOB should not be empty.");
    assertTrue(eob.hasInsurance(), "All EOBs should have insurance");
    assertTrue(eob.hasMeta(), "EOB should have meta.");
    assertTrue(eob.hasOutcome(), "EOB should have outcome");
    assertTrue(eob.hasPatient(), "EOB should have patient");
    assertTrue(eob.hasType(), "EOB should have type");
    assertTrue(eob.hasStatus(), "EOB should have status");
    assertTrue(eob.hasCreated(), "EOB should have created");
    assertTrue(eob.hasUse(), "EOB should have use");
    assertFalse(
        eob.getMeta().getProfile().isEmpty(), "EOB Meta must have at least one Profile defined");

    var hasCarinProfile =
        eob.getMeta().getProfile().stream()
            .anyMatch(
                p ->
                    p.getValueAsString()
                        .startsWith(
                            "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-ExplanationOfBenefit"));

    assertTrue(
        hasCarinProfile,
        String.format(
            "EOB (ID: %s) must have a valid CARIN BB Profile. Found: %s",
            eob.getIdElement().getIdPart(),
            eob.getMeta().getProfile().stream()
                .map(p -> p.getValueAsString())
                .collect(Collectors.joining(", "))));

    if (eob.getMeta().getProfile().stream().anyMatch(p -> p.getValue().contains("Pharmacy"))) {
      // TODO: REMOVE IN BFD-4419 -> Pharmacy EOBs should pass after 4419
    } else {
      validateCodings(eob);
      validateFinancialPrecision(eob);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        CLAIM_ID_ADJUDICATED,
        CLAIM_ID_ADJUDICATED_ICD_9,
        CLAIM_ID_PHASE_1,
        CLAIM_ID_PHASE_2,
        CLAIM_ID_PROFESSIONAL,
        CLAIM_ID_RX,
        CLAIM_ID_RX_ORGANIZATION,
        CLAIM_ID_PROFESSIONAL_MCS
      })
  void testEobReadValidity(String claimId) {
    var eob = getFhirClient().read().resource(ExplanationOfBenefit.class).withId(claimId).execute();

    validateEob(eob);
  }

  @Test
  void testEobSearchValidity() {
    var bundle =
        getFhirClient()
            .search()
            .forResource(ExplanationOfBenefit.class)
            .where(
                new ca.uhn.fhir.rest.gclient.TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_PART_A_ONLY))
            .returnBundle(Bundle.class)
            .execute();

    assertFalse(bundle.getEntry().isEmpty(), "Search should return results for validation");

    for (var entry : bundle.getEntry()) {
      validateEob((ExplanationOfBenefit) entry.getResource());
    }
  }
}
