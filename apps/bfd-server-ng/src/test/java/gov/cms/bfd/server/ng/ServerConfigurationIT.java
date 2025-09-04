package gov.cms.bfd.server.ng;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public class ServerConfigurationIT extends IntegrationTestBase {

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
    given().when().get(getServerUrl() + "/ExplanationOfBenefit/178083966").then().statusCode(404);
    given().when().get(getServerUrl() + "/Patient/178083966").then().statusCode(200);
    given().when().get(getServerUrl() + "/Coverage/part-b-365359727").then().statusCode(200);
  }
}
