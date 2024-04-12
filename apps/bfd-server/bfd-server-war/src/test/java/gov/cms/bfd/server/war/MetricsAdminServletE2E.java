package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;

import io.dropwizard.metrics.servlets.AdminServlet;
import org.junit.jupiter.api.Test;

/** Verifies that the metrics {@link AdminServlet} works as expected (as configured in web.xml). */
public final class MetricsAdminServletE2E extends ServerRequiredTest {
  /** Verifies that the <code>/metrics/ping</code> endpoint works as expected. */
  @Test
  public void ping() {
    given().spec(requestAuth).expect().statusCode(200).when().get(baseServerUrl + "/metrics/ping");
  }

  /** Verifies that the <code>/metrics/metrics</code> endpoint works as expected. */
  @Test
  public void metrics() {
    given()
        .spec(requestAuth)
        .expect()
        .statusCode(200)
        .when()
        .get(baseServerUrl + "/metrics/metrics");
  }
}
