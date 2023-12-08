package gov.cms.bfd.server.launcher;

import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.nio.file.Path;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Models the configuration options for the launcher. */
@AllArgsConstructor
@Getter
public final class AppConfiguration {

  /** The name of the environment variable that should be used to provide the server's host name. */
  public static final String ENV_VAR_KEY_HOST = "host";

  /** The name of the environment variable that should be used to provide the server's port. */
  public static final String ENV_VAR_KEY_PORT = "port";

  /**
   * The name of the environment variable that should be used to provide the path to the keystore
   * file.
   */
  public static final String ENV_VAR_KEY_KEYSTORE = "paths.files.keystore";

  /**
   * The name of the environment variable that should be used to provide the path to the truststore
   * file.
   */
  public static final String ENV_VAR_KEY_TRUSTSTORE = "paths.files.truststore";

  /**
   * The name of the environment variable that should be used to provide the web application's war
   * file or directory.
   */
  public static final String ENV_VAR_KEY_WAR = "paths.files.war";

  /**
   * The host/address that the server will bind to and listen for HTTPS connections on.
   *
   * <p>If {@link Optional#empty()} or {@code "0.0.0.0"}, then it will try to bind to all interfaces
   * (though note that the port may not be available on all of them, and Jetty just kinda' silently
   * ignores that).
   */
  private final Optional<String> host;

  /** The port that the server will listen for HTTPS connections on. * */
  private final int port;

  /**
   * The {@link Path} of the Java keystore ({@code .pfx} file) containing the private key and
   * certificate to use for this server. *
   */
  private final Path keystore;

  /**
   * The {@link Path} of the Java trust store ({@code .pfx} file) containing the client certificates
   * to use (i.e. trust/authenticate) for this server. *
   */
  private final Path truststore;

  /** The {@link Path} of the WAR file to run. * */
  private final Path war;

  @Override
  public String toString() {
    return "AppConfiguration [port="
        + port
        + ", keystore="
        + keystore
        + ", truststore="
        + truststore
        + ", war="
        + war
        + "]";
  }

  /**
   * Read configuration variables from the provided {@link ConfigLoader} and build an {@link
   * AppConfiguration} instance from them.
   *
   * @param config the {@link ConfigLoader} to use
   * @return the created instance
   * @throws ConfigException if the configuration passed to the application are incomplete or
   *     incorrect.
   */
  public static AppConfiguration loadConfig(ConfigLoader config) {
    Optional<String> host = config.stringOption(ENV_VAR_KEY_HOST);
    int port = config.positiveIntValueZeroOK(ENV_VAR_KEY_PORT);
    Path war = Path.of(config.stringValue(ENV_VAR_KEY_WAR));
    Path keystore = config.readableFile(ENV_VAR_KEY_KEYSTORE).toPath();
    Path truststore = config.readableFile(ENV_VAR_KEY_TRUSTSTORE).toPath();
    return new AppConfiguration(host, port, keystore, truststore, war);
  }
}
