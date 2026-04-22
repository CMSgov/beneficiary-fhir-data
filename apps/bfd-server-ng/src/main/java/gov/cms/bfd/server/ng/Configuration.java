package gov.cms.bfd.server.ng;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import gov.cms.bfd.server.ng.log.AuditLogger;
import gov.cms.bfd.server.ng.log.DynamoDbAuditLogger;
import gov.cms.bfd.server.ng.log.LogStreamAuditLogger;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.database.AwsWrapperDataSourceFactory;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import java.io.Serializable;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Root configuration class. */
@Data
@ConfigurationProperties(prefix = "bfd")
public class Configuration implements Serializable {
  // Unfortunately, constructor injection doesn't work with @ConfigurationProperties

  /** Identifies which Spring profiles indicate that the server is being run on a local machine. */
  private static final List<String> ALLOWED_LOCAL_PROFILES =
      List.of("local", "sql-profile", "structured-log");

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
  private String dynamoLocalUrl = "http://localhost:8000";

  // Default to local configuration, this should be overridden on deployment with the correct
  // environment.
  private String env = BFD_ENV_LOCAL;
  private String dbIdentifier = "";
  private Local local = new Local();
  private Sensitive sensitive = new Sensitive();
  private Nonsensitive nonsensitive = new Nonsensitive();

  @Getter(lazy = true)
  private final Map<String, String> clientCertsToAliases = getClientCertsToAliasesInternal();

  @Getter(lazy = true)
  private final Set<String> samhsaAllowedCertificateAliases =
      getValuesFromJson(nonsensitive.samhsaAllowedCertificateAliasesJson);

  @Getter(lazy = true)
  private final Set<String> disabledUris = getValuesFromJson(nonsensitive.disabledUrisJson);

  @Getter(lazy = true)
  private final Set<String> internalCertificateAliases =
      getValuesFromJson(nonsensitive.internalCertificateAliasesJson);

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
    if (isLocal()) {
      return new HikariDataSourceFactory(getDatabaseOptions());
    } else {
      var awsConfig = getRdsClientConfig();
      return new AwsWrapperDataSourceFactory(getDatabaseOptions(), awsConfig);
    }
  }

  /**
   * Creates a new {@link AuditLogger}. If {@link AuditLoggerType} is DYNAMO_DB both loggers will be
   * used.
   *
   * @param objectMapper used for serializing patient audit records
   * @return audit logger
   */
  public AuditLogger getAuditLogger(ObjectMapper objectMapper) {
    var logStreamLogger = new LogStreamAuditLogger(objectMapper);
    if (getAuditLoggerType() == AuditLoggerType.DYNAMO_DB) {
      var dynamoLogger =
          new DynamoDbAuditLogger(
              getDynamoDbClient(), objectMapper, getPatientMatchAuditTableName());

      return auditRecord -> {
        logStreamLogger.log(auditRecord);
        dynamoLogger.log(auditRecord);
      };
    }
    return logStreamLogger;
  }

  boolean isLocal() {
    return env.equalsIgnoreCase(BFD_ENV_LOCAL);
  }

  /** Represents possible types of audit logging. */
  public enum AuditLoggerType {
    /** Use DYNAMO_DB for audit logging. */
    DYNAMO_DB,
    /** Use standard log stream for audit logging. */
    LOG_STREAM
  }

  /**
   * Returns the {@link AuditLoggerType} to use based on the current environment.
   *
   * @return the audit logger type
   */
  public AuditLoggerType getAuditLoggerType() {
    return isLocal() ? AuditLoggerType.LOG_STREAM : AuditLoggerType.DYNAMO_DB;
  }

  /**
   * Creates a new {@link DynamoDbClient}.
   *
   * @return a configured {@link DynamoDbClient} instance
   */
  public DynamoDbClient getDynamoDbClient() {
    var region = regionProvider.getRegion();

    if (isLocal()) {
      return DynamoDbClient.builder()
          .endpointOverride(URI.create(dynamoLocalUrl))
          .region(region)
          .credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
          .build();
    }

    return DynamoDbClient.builder().region(region).credentialsProvider(credentialsProvider).build();
  }

  protected String getPatientMatchAuditTableName() {
    return String.format("bfd-%s-patient-match-audit", env);
  }

  private Map<String, String> getClientCertsToAliasesInternal() {
    return nonsensitive.clientCertificates.entrySet().stream()
        .collect(
            Collectors.toMap(e -> StringUtils.deleteWhitespace(e.getValue()), Map.Entry::getKey));
  }

  private Set<String> getValuesFromJson(String jsonStr) {
    final var setType = new TypeToken<Set<String>>() {}.getType();
    return new Gson().fromJson(jsonStr, setType);
  }

  private JdbcConnectionDetails getJdbcConfiguration() {
    return Objects.requireNonNullElseGet(jdbcConnectionDetails, () -> new JdbcConfiguration(this));
  }

  private DatabaseOptions.DataSourceType getDataSourceType() {
    return isLocal()
        ? DatabaseOptions.DataSourceType.HIKARI
        : DatabaseOptions.DataSourceType.AWS_WRAPPER;
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
    private String disabledUrisJson = "[]";
    private String internalCertificateAliasesJson = "[]";
    private String samhsaAllowedCertificateAliasesJson = "[]";

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
