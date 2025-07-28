package gov.cms.bfd.server.ng;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

public class ServerConfigurationIT extends IntegrationTestBase {

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
}
