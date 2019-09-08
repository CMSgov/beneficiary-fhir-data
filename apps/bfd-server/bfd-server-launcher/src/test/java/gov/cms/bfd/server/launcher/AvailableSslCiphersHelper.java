package gov.cms.bfd.server.launcher;

import java.util.Arrays;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Run this little app to print out a list of SSL cipher suites supported by your JRE.
 *
 * <p>This is useful when trying to determine how to properly configure the cipher suites to be
 * included/excluded by {@link gov.cms.bfd.server.launcher.DataServerLauncherApp}.
 */
public final class AvailableSslCiphersHelper {
  /**
   * App entry point.
   *
   * @param args (unused)
   * @throws Exception (shouldn't happen, but will cause app to error out)
   */
  public static void main(String[] args) throws Exception {
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.start();
    System.out.println(
        Arrays.toString(
            sslContextFactory.getSslContext().createSSLEngine().getSupportedCipherSuites()));
  }
}
