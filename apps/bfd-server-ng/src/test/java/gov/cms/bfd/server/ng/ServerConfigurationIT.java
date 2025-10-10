package gov.cms.bfd.server.ng;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class ServerConfigurationIT extends IntegrationTestBase {

  static final String EOB_UNKNOWN_RESOURCE_TYPE_ERROR_MESSAGE_FRAGMENT =
      "HAPI-0302: Unknown resource type 'ExplanationOfBenefit'";

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    // These properties will apply to all tests in this class.
    // For this test, we want to ensure Patient and Coverage are ON by default, and EOB is OFF by
    // default.
    // This setup mirrors what we expect in 'prod-like' environments initially.
    registry.add("bfd.nonsensitive.eob_enabled", () -> "false");
    registry.add("bfd.nonsensitive.patient_enabled", () -> "true");
    registry.add("bfd.nonsensitive.coverage_enabled", () -> "true");
  }

  @Test
  void disallowedMethodReturns405() {
    given().when().delete(getServerUrl() + "/Coverage/" + 1).then().statusCode(405);
  }

  @Test
  void nonExistentResourceReturnsDefaultError() {
    given()
        .when()
        .get(getServerBaseUrl() + "/fakeUrl")
        .then()
        .statusCode(404)
        .body(containsString("No static resource"));
  }

  @Test
  void eobEndpointIsDisabledWhenConfigured() {
    var fhirClient = getFhirClient();
    var readRequest =
        fhirClient.read().resource("ExplanationOfBenefit").withId(BENE_ID_PART_A_ONLY);

    var thrown =
        assertThrows(
            ResourceNotFoundException.class,
            readRequest::execute,
            "Expected ResourceNotFoundException because ExplanationOfBenefit endpoint should be disabled.");

    assertTrue(thrown.getMessage().contains(EOB_UNKNOWN_RESOURCE_TYPE_ERROR_MESSAGE_FRAGMENT));

    assertDoesNotThrow(
        () -> {
          fhirClient.read().resource(Patient.class).withId(BENE_ID_PART_A_ONLY).execute();
        },
        "Expected Patient endpoint to be accessible and not throw an exception.");

    assertDoesNotThrow(
        () -> {
          fhirClient
              .read()
              .resource(Coverage.class)
              .withId(String.format("part-b-" + BENE_ID_PART_A_ONLY))
              .execute();
        },
        "Expected Coverage endpoint to be accessible and not throw an exception.");
  }
}
