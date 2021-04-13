package gov.cms.bfd.server.launcher;

import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugEnableMode;
import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugOptions;
import java.io.IOException;
import java.util.Optional;
import javax.net.ssl.SSLException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

/** Verifies that authentication works as expected. */
public final class AuthenticationIT {
  /**
   * Verifies that authentication works for an SSL client certificate in the server's trust store.
   *
   * @throws IOException (this exception indicates a test failure)
   */
  @Test
  public void authenticationWorksForTrustedClient() throws IOException {
    try (ServerProcess serverProcess =
            new ServerProcess(
                ServerTestUtils.getSampleWar(), new JvmDebugOptions(JvmDebugEnableMode.DISABLED));
        CloseableHttpClient httpClient =
            ServerTestUtils.createHttpClient(Optional.of(ClientSslIdentity.TRUSTED));
        CloseableHttpResponse httpResponse =
            httpClient.execute(new HttpGet(serverProcess.getServerUri())); ) {
      Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
    }
  }

  /**
   * Verifies that clients that don't present a client certificate receive an access denied error.
   *
   * @throws IOException (this exception indicates a test failure)
   */
  @Test(expected = SSLException.class)
  public void accessDeniedForNoClientCert() throws IOException {
    try (ServerProcess serverProcess =
            new ServerProcess(
                ServerTestUtils.getSampleWar(), new JvmDebugOptions(JvmDebugEnableMode.DISABLED));
        CloseableHttpClient httpClient = ServerTestUtils.createHttpClient(Optional.empty());
        CloseableHttpResponse httpResponse =
            httpClient.execute(new HttpGet(serverProcess.getServerUri())); ) {
      /*
       * FIXME This won't work as long as we're calling setNeedClientAuth(true) on Jetty's
       * SslContextFactory: we'll get SSL handshake exceptions, instead of HTTP error codes.
       */
      // Assert.assertEquals(401, httpResponse.getStatusLine().getStatusCode());
    }
  }

  /**
   * Verifies that clients that present a client certificate that is not in the server's trust store
   * receive an access denied error.
   *
   * @throws IOException (this exception indicates a test failure)
   */
  @Test(expected = SSLException.class)
  public void accessDeniedForClientCertThatIsNotTrusted() throws IOException {
    try (ServerProcess serverProcess =
            new ServerProcess(
                ServerTestUtils.getSampleWar(), new JvmDebugOptions(JvmDebugEnableMode.DISABLED));
        CloseableHttpClient httpClient =
            ServerTestUtils.createHttpClient(Optional.of(ClientSslIdentity.UNTRUSTED));
        CloseableHttpResponse httpResponse =
            httpClient.execute(new HttpGet(serverProcess.getServerUri())); ) {
      /*
       * FIXME This won't work as long as we're calling setNeedClientAuth(true) on Jetty's
       * SslContextFactory: we'll get SSL handshake exceptions, instead of HTTP error codes.
       */
      // Assert.assertEquals(403, httpResponse.getStatusLine().getStatusCode());
    }
  }
}
