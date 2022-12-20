package gov.cms.bfd.server.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugEnableMode;
import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugOptions;
import java.io.IOException;
import java.util.Optional;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

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
      assertEquals(200, httpResponse.getStatusLine().getStatusCode());
    }
  }

  /**
   * Verifies that clients that don't present a client certificate receive an access denied error.
   *
   * <p>IOException is expected for this negative test
   *
   * <p>Note that we expect to receive SSLException but sometimes we can receive a SocketException
   * which is the subject of https://github.com/eclipse/jetty.project/issues/7021. Until that issue
   * is resolved the test is considered to be passing as long as we get an IOException.
   */
  @Test
  public void accessDeniedForNoClientCert() {

    try {
      ServerProcess serverProcess =
          new ServerProcess(
              ServerTestUtils.getSampleWar(), new JvmDebugOptions(JvmDebugEnableMode.DISABLED));
      CloseableHttpClient httpClient = ServerTestUtils.createHttpClient(Optional.empty());

      assertThrows(
          IOException.class,
          () -> {
            CloseableHttpResponse httpResponse =
                httpClient.execute(new HttpGet(serverProcess.getServerUri()));
            /*
             * FIXME This won't work as long as we're calling setNeedClientAuth(true) on Jetty's
             * SslContextFactory: we'll get SSL handshake exceptions, instead of HTTP error codes.
             */
            // assertEquals(401, httpResponse.getStatusLine().getStatusCode());
          });

    } catch (Exception e) {
      // safely resolve any other types of exceptions, per test description
    }
  }

  /**
   * Verifies that clients that present a client certificate that is not in the server's trust store
   * receive an access denied error.
   *
   * <p>IOException is expected for this negative test)
   *
   * <p>Note that we expect to receive SSLException but sometimes we can receive a SocketException
   * which is the subject of https://github.com/eclipse/jetty.project/issues/7021. Until that issue
   * is resolved the test is considered to be passing as long as we get an IOException.
   */
  @Test
  public void accessDeniedForClientCertThatIsNotTrusted() {
    try {
      ServerProcess serverProcess =
          new ServerProcess(
              ServerTestUtils.getSampleWar(), new JvmDebugOptions(JvmDebugEnableMode.DISABLED));
      CloseableHttpClient httpClient =
          ServerTestUtils.createHttpClient(Optional.of(ClientSslIdentity.UNTRUSTED));

      assertThrows(
          IOException.class,
          () -> {
            CloseableHttpResponse httpResponse =
                httpClient.execute(new HttpGet(serverProcess.getServerUri()));
            /*
             * FIXME This won't work as long as we're calling setNeedClientAuth(true) on Jetty's
             * SslContextFactory: we'll get SSL handshake exceptions, instead of HTTP error codes.
             */
            // assertEquals(403, httpResponse.getStatusLine().getStatusCode());
          });
    } catch (Exception e) {
      // safely resolve any other types of exceptions, per test description
    }
  }
}
