package gov.cms.bfd.server.war;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This Spring {@link Component} launches a local instance of the Plaid application, listening for
 * HTTP requests on a random port, connected to the same PostgreSQL database as this application.
 */
@Component
public class PlaidApplicationComponent implements DisposableBean {
  private final int plaidHttpPort;
  private final Process plaidProcess;

  /**
   * Construct a {@link PlaidApplicationComponent}, launching a local Plaid application instance.
   *
   * @param plaidPathText the path to the Plaid application's binary
   * @param plaidLogPath the path to log the Plaid application's output to
   * @param plaidTlsServerCert the path to the Plaid application's TLS server certficate file
   * @param plaidTlsServerKey the path to the Plaid application's TLS server key file
   * @param plaidTlsClientCerts the path to the Plaid application's TLS client certificates file
   * @param databaseUrlFromProperties the PostgreSQL JDBC database URL provided as a system property
   * @param databaseUsernameFromProperties the PostgreSQL database username provided as a system
   *     property, if any
   * @param databasePasswordFromProperties the PostgreSQL database password provided as a system
   *     property, if any
   */
  @Inject
  public PlaidApplicationComponent(
      @Value("${bfdServer.plaid.path}") String plaidPathText,
      @Value("${bfdServer.plaid.log}") String plaidLogPath,
      @Value("${bfdServer.plaid.tls.server.cert}") String plaidTlsServerCert,
      @Value("${bfdServer.plaid.tls.server.key}") String plaidTlsServerKey,
      @Value("${bfdServer.plaid.tls.client.certs}") String plaidTlsClientCerts,
      @Value("${" + SpringConfiguration.PROP_DB_URL + "}") String databaseUrlFromProperties,
      @Value("${" + SpringConfiguration.PROP_DB_USERNAME + "}")
          String databaseUsernameFromProperties,
      @Value("${" + SpringConfiguration.PROP_DB_PASSWORD + "}")
          String databasePasswordFromProperties) {
    // Verify the Path to the Plaid application binary.
    Path plaidPath = Paths.get(plaidPathText);
    if (!Files.isExecutable(plaidPath)) {
      throw new IllegalArgumentException("Unable to find Plaid binary.");
    }

    // Select the port to have Plaid use for HTTP.
    // FIXME Select random HTTP port once we're done comparing HTTP/HTTPS performance.
    // this.plaidHttpPort = findFreePort();
    this.plaidHttpPort = 3000;

    // Calculate the Diesel database URL.
    String databaseUrl =
        computeDieselDatabaseUrl(
            databaseUrlFromProperties,
            databaseUsernameFromProperties,
            databasePasswordFromProperties);

    // Create a ProcessBuild that can be used to launch the Plaid application.
    ProcessBuilder plaidProcessBuilder = new ProcessBuilder(plaidPath.toString());
    plaidProcessBuilder.environment().put("BFD_PLAID_HTTP_PORT", "" + plaidHttpPort);
    plaidProcessBuilder.environment().put("DATABASE_URL", databaseUrl);
    plaidProcessBuilder.environment().put("BFD_TLS_SERVER_CERT", plaidTlsServerCert);
    plaidProcessBuilder.environment().put("BFD_TLS_SERVER_KEY", plaidTlsServerKey);
    plaidProcessBuilder.environment().put("BFD_TLS_CLIENT_CERTS", plaidTlsClientCerts);
    plaidProcessBuilder.redirectErrorStream(true);
    plaidProcessBuilder.redirectOutput(new File(plaidLogPath));

    // Launch the Plaid application.
    try {
      this.plaidProcess = plaidProcessBuilder.start();
    } catch (IOException e) {
      throw new UncheckedIOException("Error launching Plaid application.", e);
    }
  }

  /** @return the port that the Plaid application is listening to for HTTP requests */
  public int getPlaidHttpPort() {
    return plaidHttpPort;
  }

  @Override
  public void destroy() throws Exception {
    if (plaidProcess != null) {
      plaidProcess.destroy();
      plaidProcess.wait(60 * 1000);
      if (plaidProcess.isAlive()) {
        plaidProcess.destroyForcibly();
      }
    }
  }

  /**
   * @param databaseUrlFromProperties the PostgreSQL JDBC database URL provided as a system property
   * @param databaseUsernameFromProperties the PostgreSQL database username provided as a system
   *     property, if any
   * @param databasePasswordFromProperties the PostgreSQL database password provided as a system
   *     property, if any
   * @return the Diesel database URL to use
   */
  private String computeDieselDatabaseUrl(
      String databaseUrlFromProperties,
      String databaseUsernameFromProperties,
      String databasePasswordFromProperties) {
    // Parse info out of the JDBC URL.
    Pattern jdbcUrlPattern =
        Pattern.compile(
            "jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^\\?]+)(?:\\?user=(.+)&password=(.+))?");
    Matcher jdbcUrlMatcher = jdbcUrlPattern.matcher(databaseUrlFromProperties);
    if (!jdbcUrlMatcher.matches()) {
      throw new IllegalArgumentException("Invalid DB URL for Plaid.");
    }
    String databaseAddress = jdbcUrlMatcher.group(1);
    Optional<String> databasePort = Optional.ofNullable(jdbcUrlMatcher.group(2));
    String databaseName = jdbcUrlMatcher.group(3);
    Optional<String> databaseUsernameFromUrl = Optional.ofNullable(jdbcUrlMatcher.group(4));
    Optional<String> databasePasswordFromUrl = Optional.ofNullable(jdbcUrlMatcher.group(5));

    // Determine which DB username to use.
    String databaseUsername;
    if (databaseUsernameFromProperties != null && !databaseUsernameFromProperties.isEmpty()) {
      databaseUsername = databaseUsernameFromProperties;
    } else if (databaseUsernameFromUrl.isPresent()) {
      databaseUsername = databaseUsernameFromUrl.get();
    } else {
      throw new IllegalArgumentException("No DB username specified.");
    }

    // Determine which DB password to use.
    String databasePassword;
    if (databasePasswordFromProperties != null && !databasePasswordFromProperties.isEmpty()) {
      databasePassword = databasePasswordFromProperties;
    } else if (databasePasswordFromUrl.isPresent()) {
      databasePassword = databasePasswordFromUrl.get();
    } else {
      throw new IllegalArgumentException("No DB username specified.");
    }

    // Put it all together into a Diesel DB URL.
    StringBuilder dieselDatabaseUrl = new StringBuilder("postgres://");
    dieselDatabaseUrl.append(databaseUsername);
    dieselDatabaseUrl.append(":");
    dieselDatabaseUrl.append(databasePassword);
    dieselDatabaseUrl.append("@");
    dieselDatabaseUrl.append(databaseAddress);
    if (databasePort.isPresent()) {
      dieselDatabaseUrl.append(":");
      dieselDatabaseUrl.append(databasePort.get());
    }
    dieselDatabaseUrl.append("/");
    dieselDatabaseUrl.append(databaseName);

    return dieselDatabaseUrl.toString();
  }

  /**
   * Note: It's possible for this to result in race conditions, if the random port selected enters
   * use after this method returns and before whatever called this method gets a chance to grab it.
   * It's pretty unlikely, though, and there's not much we can do about it, either. So.
   *
   * @return a free local port number
   */
  private int findFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
