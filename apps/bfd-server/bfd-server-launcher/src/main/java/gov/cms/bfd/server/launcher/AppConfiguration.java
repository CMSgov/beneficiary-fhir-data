package gov.cms.bfd.server.launcher;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/** Models the configuration options for the launcher. */
public final class AppConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

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

  private final String host;
  private final int port;
  private final String keystore;
  private final String truststore;
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
   * @return the host/address that the server will bind to and listen for HTTPS connections on, if
   *     {@link Optional#empty()} or <code>"0.0.0.0"</code>, then it will try to bind to all
   *     interfaces (though note that the port may not be available on all of them, and Jetty just
   *     kinda' silently ignores that)
   */
  public Optional<String> getHost() {
    return Optional.ofNullable(host);
  }

  /** @return the port that the server will listen for HTTPS connections on */
  public int getPort() {
    return port;
  }

  /**
   * @return the {@link Path} of the Java keystore (<code>.jks</code> file) containing the private
   *     key and certificate to use for this server
   */
  public Path getKeystore() {
    return Paths.get(keystore);
  }

  /**
   * @return the {@link Path} of the Java keystore (<code>.jks</code> file) containing the client
   *     certificates to use (i.e. trust/authenticate) for this server
   */
  public Path getTruststore() {
    return Paths.get(truststore);
  }

  /** @return the {@link Path} of the WAR file to run */
  public Path getWar() {
    return Paths.get(war);
  }

  /** @see java.lang.Object#toString() */
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
   * This application accepts its configuration via environment variables. Read those in, and build
   * an {@link AppConfiguration} instance from them.
   *
   * @return the {@link AppConfiguration} instance represented by the configuration provided to this
   *     application via the environment variables
   * @throws AppConfigurationException An {@link AppConfigurationException} will be thrown if the
   *     configuration passed to the application are incomplete or incorrect.
   */
  static AppConfiguration readConfigFromEnvironmentVariables() {
    Optional<String> host = readEnvVarAsString(ENV_VAR_KEY_HOST);
    if (host.isPresent() && host.get().trim().isEmpty())
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s'.", ENV_VAR_KEY_HOST));

    int port = readEnvVarAsInt(ENV_VAR_KEY_PORT);
    if (port < 0)
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s'.", ENV_VAR_KEY_PORT));

    Path war = readEnvVarAsPath(ENV_VAR_KEY_WAR);
    if (!Files.isReadable(war))
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s'.", ENV_VAR_KEY_WAR));

    Path keystore = readEnvVarAsPath(ENV_VAR_KEY_KEYSTORE);
    if (!Files.isReadable(keystore))
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s'.", ENV_VAR_KEY_KEYSTORE));

    Path truststore = readEnvVarAsPath(ENV_VAR_KEY_TRUSTSTORE);
    if (!Files.isReadable(truststore))
      throw new AppConfigurationException(
          String.format(
              "Invalid value for configuration environment variable '%s'.",
              ENV_VAR_KEY_TRUSTSTORE));

    return new AppConfiguration(host, port, keystore, truststore, war);
  }

  /**
   * @param envVarKey the name of the environment variable to read
   * @return the specified environment variable's value, as a {@link String}, or {@link
   *     Optional#empty()} if it's not defined
   */
  private static Optional<String> readEnvVarAsString(String envVarKey) {
    String value = System.getenv(envVarKey);
    return Optional.ofNullable(value);
  }

  /**
   * @param envVarKey the name of the environment variable to read
   * @return the specified environment variable's value, as an <code>int</code>
   */
  private static int readEnvVarAsInt(String envVarKey) {
    String intText = System.getenv(envVarKey);
    return Optional.ofNullable(intText)
        .map(
            v -> {
              try {
                return Integer.parseInt(v);
              } catch (Throwable t) {
                return null;
              }
            })
        .orElseThrow(
            () ->
                new AppConfigurationException(
                    String.format(
                        "Missing or invalid value for configuration environment variable '%s': '%s'.",
                        envVarKey, intText)));
  }

  /**
   * @param envVarKey the name of the environment variable to read
   * @return the specified environment variable's value, as a {@link Path}
   */
  private static Path readEnvVarAsPath(String envVarKey) {
    String pathText = System.getenv(envVarKey);
    return Optional.ofNullable(pathText)
        .filter(v -> !v.isEmpty())
        .map(
            v -> {
              return Paths.get(pathText);
            })
        .orElseThrow(
            () ->
                new AppConfigurationException(
                    String.format(
                        "Missing or invalid value for configuration environment variable '%s': '%s'.",
                        envVarKey, pathText)));
  }
}
