package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;

import com.codahale.metrics.servlets.AdminServlet;
import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.Test;

/** Verifies that the metrics {@link AdminServlet} works as expected (as configured in web.xml). */
public final class MetricsAdminServletE2E extends ServerRequiredTest {
  /**
   * Verifies that the <code>/metrics/ping</code> endpoint works as expected.
   *
   * @throws IOException (indicates test failure)
   * @throws ClientProtocolException (indicates test failure)
   */
  @Test
  public void ping() throws ClientProtocolException, IOException {
    given().spec(requestAuth).expect().statusCode(200).when().get(baseServerUrl + "/metrics/ping");
  }

  /**
   * Verifies that the <code>/metrics/metrics</code> endpoint works as expected.
   *
   * @throws IOException (indicates test failure)
   * @throws ClientProtocolException (indicates test failure)
   */
  @Test
  public void metrics() throws ClientProtocolException, IOException {
    given()
        .spec(requestAuth)
        .expect()
        .statusCode(200)
        .when()
        .get(baseServerUrl + "/metrics/metrics");
  }
}
