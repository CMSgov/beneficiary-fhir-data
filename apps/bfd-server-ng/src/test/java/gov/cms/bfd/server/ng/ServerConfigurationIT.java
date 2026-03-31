package gov.cms.bfd.server.ng;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import java.util.List;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class ServerConfigurationIT extends IntegrationTestBase {

  static final String RESOURCE_NOT_ALLOWED_MESSAGE_FRAGMENT = "Resource not allowed.";

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    // These properties will apply to all tests in this class.
    registry.add("bfd.nonsensitive.disabled_uris", () -> List.of("/v3/fhir/ExplanationOfBenefit"));
  }

  @Test
  void disallowedEndpointReturns401() {
    given()
        .when()
        .get(getServerUrl() + "/ExplanationOfBenefit")
        .then()
        .statusCode(401)
        .body(containsString(RESOURCE_NOT_ALLOWED_MESSAGE_FRAGMENT));
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
            AuthenticationException.class,
            readRequest::execute,
            "Expected AuthenticationException because ExplanationOfBenefit endpoint should be"
                + " disabled.");

    assertTrue(thrown.getResponseBody().contains(RESOURCE_NOT_ALLOWED_MESSAGE_FRAGMENT));

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
