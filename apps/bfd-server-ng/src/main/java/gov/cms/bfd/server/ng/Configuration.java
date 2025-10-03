package gov.cms.bfd.server.ng;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.database.AwsWrapperDataSourceFactory;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/** Root configuration class. */
@Data
@ConfigurationProperties(prefix = "bfd")
public class Configuration implements Serializable {
  // Unfortunately, constructor injection doesn't work with @ConfigurationProperties

  /** Identifies which Spring profiles indicate that the server is being run on a local machine. */
  private static final List<String> ALLOWED_LOCAL_PROFILES = List.of("local", "sqlprofile");

  // Getters should only be generated for configuration properties, not dependencies
  @Getter(value = AccessLevel.NONE)
  @Autowired
  private AwsCredentialsProvider credentialsProvider;

  @Getter(value = AccessLevel.NONE)
  @Autowired
  private AwsRegionProvider regionProvider;

  // This can be injected by tests (using TestContainers, most likely) to override the database
  // connection info
  @Getter(value = AccessLevel.NONE)
  @Autowired(required = false)
  private JdbcConnectionDetails jdbcConnectionDetails;

  private static final String BFD_ENV_LOCAL = "local";

  // Default to local configuration, this should be overridden on deployment with the correct
  // environment.
  private String env = BFD_ENV_LOCAL;
  private Local local = new Local();
  private Sensitive sensitive = new Sensitive();
  private Nonsensitive nonsensitive = new Nonsensitive();

  @Getter(lazy = true)
  private final Map<String, String> clientCertsToAliases = getClientCertsToAliasesInternal();

  /**
   * Determines if the profile requires auth.
   *
   * @param profile Spring profile
   * @return boolean
   */
  public static boolean canProfileBypassAuth(String profile) {
    return ALLOWED_LOCAL_PROFILES.contains(profile.toLowerCase());
  }

  /**
   * Creates a new {@link AwsWrapperDataSourceFactory}.
   *
   * @return wrapper factory.
   */
  public DataSourceFactory getDataSourceFactory() {
    if (useRds()) {
      var awsConfig = getRdsClientConfig();
      return new AwsWrapperDataSourceFactory(getDatabaseOptions(), awsConfig);
    } else {
      return new HikariDataSourceFactory(getDatabaseOptions());
    }
  }

  boolean useRds() {
    return !env.equalsIgnoreCase(BFD_ENV_LOCAL);
  }

  private Map<String, String> getClientCertsToAliasesInternal() {

    return nonsensitive.clientCertificates.entrySet().stream()
        .collect(
            Collectors.toMap(e -> StringUtils.deleteWhitespace(e.getValue()), Map.Entry::getKey));
  }

  private JdbcConnectionDetails getJdbcConfiguration() {
    return Objects.requireNonNullElseGet(jdbcConnectionDetails, () -> new JdbcConfiguration(this));
  }

  private DatabaseOptions.DataSourceType getDataSourceType() {
    return useRds()
        ? DatabaseOptions.DataSourceType.AWS_WRAPPER
        : DatabaseOptions.DataSourceType.HIKARI;
  }

  private AwsClientConfig getRdsClientConfig() {
    var credentials = credentialsProvider.resolveCredentials();
    var region = regionProvider.getRegion();
    return AwsClientConfig.awsBuilder()
        .accessKey(credentials.accessKeyId())
        .secretKey(credentials.secretAccessKey())
        .region(region)
        .build();
  }

  private DatabaseOptions.HikariOptions getHikariOptions() {
    final var hikari = nonsensitive.db.hikari;
    return DatabaseOptions.HikariOptions.builder()
        .maximumPoolSize(hikari.maxPoolSize)
        .minimumIdleConnections(hikari.minIdleConnections)
        .idleTimeoutMs(hikari.idleTimeoutMs)
        .initializationFailTimeoutMs(hikari.initFailTimeoutMs)
        .connectionTimeoutMs(hikari.connectionTimeoutMs)
        .keepaliveTimeMs(hikari.keepaliveTimeoutMs)
        .validationTimeoutMs(hikari.validationTimeoutMs)
        .maxConnectionLifetimeMs(hikari.maxConnectionLifetimeMs)
        .build();
  }

  private DatabaseOptions.AwsJdbcWrapperOptions getJdbcWrapperOptions() {
    final var wrapper = nonsensitive.db.wrapper;
    return DatabaseOptions.AwsJdbcWrapperOptions.builder()
        .basePresetCode(wrapper.basePreset)
        .pluginsCsv(wrapper.pluginsCsv)
        .clusterTopologyRefreshRateMs(wrapper.clusterTopologyRefreshRateMs)
        .instanceStateMonitorRefreshRateMs(wrapper.instanceStateMonitorRefreshRateMs)
        .build();
  }

  private DatabaseOptions getDatabaseOptions() {
    final var hikariOptions = getHikariOptions();
    final var jdbcWrapperOptions = getJdbcWrapperOptions();
    var jdbcOptions = getJdbcConfiguration();
    return DatabaseOptions.builder()
        .databaseUrl(jdbcOptions.getJdbcUrl())
        .databaseUsername(jdbcOptions.getUsername())
        .databasePassword(jdbcOptions.getPassword())
        .authenticationType(DatabaseOptions.AuthenticationType.JDBC)
        .dataSourceType(getDataSourceType())
        .awsJdbcWrapperOptions(jdbcWrapperOptions)
        .hikariOptions(hikariOptions)
        .build();
  }

  /** Configuration for local-only properties. */
  @Data
  @ConfigurationProperties
  public static class Local {
    // in AWS, this is loaded dynamically using the RDS API.
    private String dbHost = "localhost";
  }

  /** Sensitive configuration. */
  @Data
  @ConfigurationProperties
  public static class Sensitive {
    private Db db = new Db();

    /** Sensitive Database configuration. */
    @Data
    @ConfigurationProperties
    public static class Db {
      private String username;
      private String password;
    }
  }

  /** Nonsensitive configuration. */
  @Data
  @ConfigurationProperties
  public static class Nonsensitive {
    private Db db = new Db();
    private boolean eobEnabled = true;
    private boolean patientEnabled = true;
    private boolean coverageEnabled = true;

    /** Nonsensitive database configuration. */
    @Data
    @ConfigurationProperties
    public static class Db {
      private Hikari hikari = new Hikari();
      private Wrapper wrapper = new Wrapper();
      private String clusterIdentifierTemplate = "bfd-%s-aurora-cluster";
      private String name = "fhirdb";
      private String port = "5432";
      private String connectionStringTemplate = "jdbc:postgresql://%s:%s/%s";

      /** Hikari configuration. */
      @Data
      @ConfigurationProperties
      public static class Hikari {
        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        private int maxPoolSize = Runtime.getRuntime().availableProcessors();
        private int minIdleConnections = Runtime.getRuntime().availableProcessors();
        private long idleTimeoutMs = Duration.ofMinutes(10).toMillis();
        private long initFailTimeoutMs = 1;
        private long connectionTimeoutMs = Duration.ofSeconds(30).toMillis();
        private long keepaliveTimeoutMs = 0;
        private long validationTimeoutMs = Duration.ofSeconds(5).toMillis();
        private long maxConnectionLifetimeMs = Duration.ofMinutes(30).toMillis();
      }

      /** AWS JDBC wrapper configuration. */
      @Data
      @ConfigurationProperties
      public static class Wrapper {
        private String pluginsCsv = "auroraConnectionTracker,failover,efm2";
        private String hostSelectorStrategy = "roundRobin";
        private String basePreset = "E";
        private long clusterTopologyRefreshRateMs = Duration.ofSeconds(30).toMillis();
        private long instanceStateMonitorRefreshRateMs = Duration.ofSeconds(5).toMillis();
      }
    }

    private final Map<String, String> clientCertificates = new HashMap<>();
  }
}
