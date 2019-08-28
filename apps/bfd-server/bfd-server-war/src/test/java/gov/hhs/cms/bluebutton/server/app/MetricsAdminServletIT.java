package gov.hhs.cms.bluebutton.server.app;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import com.codahale.metrics.servlets.AdminServlet;

/**
 * Verifies that the metrics {@link AdminServlet} works as expected (as
 * configured in web.xml).
 */
public final class MetricsAdminServletIT {
	/**
	 * Verifies that the <code>/metrics/ping</code> endpoint works as expected.
	 * 
	 * @throws IOException
	 *             (indicates test failure)
	 * @throws ClientProtocolException
	 *             (indicates test failure)
	 */
	@Test
	public void ping() throws ClientProtocolException, IOException {
		try (CloseableHttpClient httpClient = HttpClients.custom()
				.setSSLContext(ServerTestUtils.createSslContext(Optional.of(ClientSslIdentity.TRUSTED))).build();) {
			HttpGet pingGet = new HttpGet(String.format("%s/metrics/ping", ServerTestUtils.getServerBaseUrl()));
			try (CloseableHttpResponse pingResponse = httpClient.execute(pingGet);) {
				Assert.assertEquals(200, pingResponse.getStatusLine().getStatusCode());
			}
		}
	}

	/**
	 * Verifies that the <code>/metrics/metrics</code> endpoint works as expected.
	 * 
	 * @throws IOException
	 *             (indicates test failure)
	 * @throws ClientProtocolException
	 *             (indicates test failure)
	 */
	@Test
	public void metrics() throws ClientProtocolException, IOException {
		try (CloseableHttpClient httpClient = HttpClients.custom()
				.setSSLContext(ServerTestUtils.createSslContext(Optional.of(ClientSslIdentity.TRUSTED))).build();) {
			HttpGet metricsGet = new HttpGet(String.format("%s/metrics/metrics", ServerTestUtils.getServerBaseUrl()));
			try (CloseableHttpResponse metricsResponse = httpClient.execute(metricsGet);) {
				Assert.assertEquals(200, metricsResponse.getStatusLine().getStatusCode());
			}
		}
	}
}
