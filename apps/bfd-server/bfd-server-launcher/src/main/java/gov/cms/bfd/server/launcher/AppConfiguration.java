package gov.cms.bfd.server.launcher;

import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/** Models the configuration options for the launcher. */
public final class AppConfiguration {

  /**
   * The name of the environment variable that should be used to provide the {@link #getHost()}
   * value.
   */
  public static final String ENV_VAR_KEY_HOST = "BFD_HOST";

  /**
   * The name of the environment variable that should be used to provide the {@link #getPort()}
   * value.
   */
  public static final String ENV_VAR_KEY_PORT = "BFD_PORT";

  /**
   * The name of the environment variable that should be used to provide the {@link #getKeystore()}
   * value.
   */
  public static final String ENV_VAR_KEY_KEYSTORE = "BFD_KEYSTORE";

  /**
   * The name of the environment variable that should be used to provide the {@link
   * #getTruststore()} value.
   */
  public static final String ENV_VAR_KEY_TRUSTSTORE = "BFD_TRUSTSTORE";

  /**
   * The name of the environment variable that should be used to provide the {@link #getWar()}
   * value.
   */
  public static final String ENV_VAR_KEY_WAR = "BFD_WAR";

  /** The host/address that the server will bind to and listen for HTTPS connections on. * */
  private final String host;
  /** The port that the server will listen for HTTPS connections on. * */
  private final int port;
  /**
   * The {@link Path} of the Java keystore ({@code .pfx} file) containing the private key and
   * certificate to use for this server. *
   */
  private final String keystore;
  /**
   * The {@link Path} of the Java trust store ({@code .pfx} file) containing the client certificates
   * to use (i.e. trust/authenticate) for this server. *
   */
  private final String truststore;
  /** The {@link Path} of the WAR file to run. * */
  private final String war;

  /**
   * Constructs a new {@link AppConfiguration} instance.
   *
   * @param host the value to use for {@link #getHost()}
   * @param port the value to use for {@link #getPort()}
   * @param keystore the value to use for {@link #getKeystore()}
   * @param truststore the value to use for {@link #getTruststore()}
   * @param war the value to use for {@link #getWar()}
   */
  public AppConfiguration(
      Optional<String> host, int port, Path keystore, Path truststore, Path war) {
    this.host = host.orElse(null);
    this.port = port;
    this.keystore = keystore.toString();
    this.truststore = truststore.toString();
    this.war = war.toString();
  }

  /**
   * Gets the {@link #host}.
   *
   * <p>If {@link Optional#empty()} or <code>"0.0.0.0"</code>, then it will try to bind to all
   * interfaces (though note that the port may not be available on all of them, and Jetty just
   * kinda' silently ignores that).
   *
   * @return the host/address that the server will bind to and listen for HTTPS connections on
   */
  public Optional<String> getHost() {
    return Optional.ofNullable(host);
  }

  /**
   * Gets the {@link #port}.
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the {@link #keystore}.
   *
   * @return the {@link Path} of the Java keystore
   */
  public Path getKeystore() {
    return Paths.get(keystore);
  }

  /**
   * Gets the {@link #truststore}.
   *
   * @return the {@link Path} of the Java trust store
   */
  public Path getTruststore() {
    return Paths.get(truststore);
  }

  /**
   * Gets the {@link #war}.
   *
   * @return the {@link Path} of the WAR file
   */
  public Path getWar() {
    return Paths.get(war);
  }

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
  static AppConfiguration loadConfig(ConfigLoader config) {
    Optional<String> host = config.stringOption(ENV_VAR_KEY_HOST);
    int port = config.positiveIntValueZeroOK(ENV_VAR_KEY_PORT);
    Path war = config.readableFile(ENV_VAR_KEY_WAR).toPath();
    Path keystore = config.readableFile(ENV_VAR_KEY_KEYSTORE).toPath();
    Path truststore = config.readableFile(ENV_VAR_KEY_TRUSTSTORE).toPath();
    return new AppConfiguration(host, port, keystore, truststore, war);
  }
}
