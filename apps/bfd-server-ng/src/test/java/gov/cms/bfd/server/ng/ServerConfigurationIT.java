package gov.cms.bfd.server.ng;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

public class ServerConfigurationIT extends IntegrationTestBase {

  @Test
  void disallowedMethodReturns405() {
    RestAssured.given().when().delete(getServerUrl() + "/Coverage/" + 1).then().statusCode(405);
  }
}
