package gov.cms.bfd.server.launcher;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.opentest4j.TestAbortedException;

/** Contains test utilities. */
public final class ServerTestUtils {
  /**
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the server
   * @return a new {@link HttpClient} for use
   */
  public static CloseableHttpClient createHttpClient(
      Optional<ClientSslIdentity> clientSslIdentity) {
    SSLContext sslContext = createSslContext(clientSslIdentity);

    CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLContext(sslContext).build();
    return httpClient;
  }

  /**
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the server
   * @return a new {@link SSLContext} for HTTP clients connecting to the server to use
   */
  private static SSLContext createSslContext(Optional<ClientSslIdentity> clientSslIdentity) {
    SSLContext sslContext;
    try {
      SSLContextBuilder sslContextBuilder = SSLContexts.custom();

      // If a client key is desired, configure the key store with it.
      if (clientSslIdentity.isPresent())
        sslContextBuilder.loadKeyMaterial(
            clientSslIdentity.get().getKeyStore(),
            clientSslIdentity.get().getStorePassword(),
            clientSslIdentity.get().getKeyPass());

      // Configure the trust store.
      sslContextBuilder.loadTrustMaterial(
          getClientTrustStorePath().toFile(), "changeit".toCharArray());

      sslContext = sslContextBuilder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (KeyManagementException
        | UnrecoverableKeyException
        | NoSuchAlgorithmException
        | KeyStoreException
        | CertificateException e) {
      throw new IllegalStateException(e);
    }
    return sslContext;
  }

  /** @return the local {@link Path} that the project can be found in */
  public static Path getLauncherProjectDirectory() {
    try {
      /*
       * The working directory for tests will either be the module directory or their parent
       * directory. With that knowledge, we're searching for the project directory.
       */
      Path projectDir = Paths.get(".");
      if (!Files.isDirectory(projectDir) && projectDir.toRealPath().endsWith("bfd-server-launcher"))
        throw new IllegalStateException();
      return projectDir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** @return the local {@link Path} that development/test key and trust stores can be found in */
  static Path getSslStoresDirectory() {
    /*
     * The working directory for tests will either be the module directory
     * or their parent directory. With that knowledge, we're searching for
     * the ssl-stores directory.
     */
    Path sslStoresDir = Paths.get("..", "dev", "ssl-stores");
    if (!Files.isDirectory(sslStoresDir)) sslStoresDir = Paths.get("dev", "ssl-stores");
    if (!Files.isDirectory(sslStoresDir)) throw new IllegalStateException();
    return sslStoresDir;
  }

  /** @return the local {@link Path} to the trust store that FHIR clients should use */
  private static Path getClientTrustStorePath() {
    Path trustStorePath = getSslStoresDirectory().resolve("client-truststore.jks");
    return trustStorePath;
  }

  /**
   * Throws an {@link TestAbortedException} if the OS doesn't support <strong>graceful</strong>
   * shutdowns via {@link Process#destroy()}.
   */
  static void skipOnUnsupportedOs() {
    /*
     * The only OS I know for sure that handles this correctly is Linux, because I've verified that
     * there. However, the following project seems to indicate that Linux really might be it:
     * https://github.com/zeroturnaround/zt-process-killer. Some further research indicates that
     * this could be supported on Windows for GUI apps, but not console apps. If this lack of OS
     * support ever proves to be a problem, the best thing to do would be to enhance our application
     * such that it listens on a particular port for shutdown requests, and handles them gracefully.
     */

    assumeTrue(
        Arrays.asList("Linux", "Mac OS X").contains(System.getProperty("os.name")),
        "Unsupported OS for this test case.");
  }

  /** @return the {@link Path} to the <code>bfd-server-launcher-sample</code> WAR */
  static Path getSampleWar() {
    return AppConfigurationIT.getProjectDirectory()
        .resolve(Paths.get("target", "sample", "bfd-server-launcher-sample-1.0.0-SNAPSHOT.war"));
  }
}
