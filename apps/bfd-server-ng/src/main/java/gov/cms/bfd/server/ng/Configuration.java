package gov.cms.bfd.server.ng;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.database.AwsWrapperDataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import java.time.Duration;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;

/** Root configuration class. */
@Data
@ConfigurationProperties(prefix = "bfd")
public class Configuration {
  @Autowired private AwsCredentialsProvider credentialsProvider;
  @Autowired private AwsRegionProvider regionProvider;

  private static final String BFD_ENV_LOCAL = "local";

  // Default to local configuration, this should be overridden on deployment with the correct
  // environment.
  private String env = BFD_ENV_LOCAL;
  private Local local = new Local();
  private Sensitive sensitive = new Sensitive();
  private Nonsensitive nonsensitive = new Nonsensitive();

  /**
   * Creates a new {@link AwsWrapperDataSourceFactory}.
   *
   * @return wrapper factory.
   */
  public AwsWrapperDataSourceFactory getAwsWrapperDataSourceFactory() {
    var awsConfig = getRdsClientConfig();
    return new AwsWrapperDataSourceFactory(getDatabaseOptions(), awsConfig);
  }

  private boolean useRds() {
    return !env.equalsIgnoreCase(BFD_ENV_LOCAL);
  }

  private String getConnectionString(String dbHost) {
    var db = nonsensitive.db;
    return String.format(db.connectionStringTemplate, dbHost, db.port, db.name);
  }

  private String resolveDatabaseReaderEndpoint() {
    if (useRds()) {
      try (var rdsClient = RdsClient.create()) {
        var clusterIdentifier = String.format(nonsensitive.db.clusterIdentifierTemplate, env);
        var clusters =
            rdsClient.describeDBClusters(
                DescribeDbClustersRequest.builder().dbClusterIdentifier(clusterIdentifier).build());
        return getConnectionString(clusters.dbClusters().getFirst().readerEndpoint());
      }
    } else {
      return getConnectionString(local.dbHost);
    }
  }

  private DatabaseOptions.AuthenticationType getAuthenticationType() {
    return useRds()
        ? DatabaseOptions.AuthenticationType.JDBC
        : DatabaseOptions.AuthenticationType.RDS;
  }

  private DatabaseOptions.DataSourceType getDataSourceType() {
    return useRds()
        ? DatabaseOptions.DataSourceType.AWS_WRAPPER
        : DatabaseOptions.DataSourceType.HIKARI;
  }

  private AwsClientConfig getRdsClientConfig() {
    if (useRds()) {
      var credentials = credentialsProvider.resolveCredentials();
      var region = regionProvider.getRegion();
      return AwsClientConfig.awsBuilder()
          .accessKey(credentials.accessKeyId())
          .secretKey(credentials.secretAccessKey())
          .region(region)
          .build();
    } else {
      return AwsClientConfig.awsBuilder().build();
    }
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
    final var jdbcOptions = getJdbcWrapperOptions();

    return DatabaseOptions.builder()
        .databaseUrl(resolveDatabaseReaderEndpoint())
        .databaseUsername(sensitive.db.username)
        .databasePassword(sensitive.db.password)
        .authenticationType(getAuthenticationType())
        .dataSourceType(getDataSourceType())
        .awsJdbcWrapperOptions(jdbcOptions)
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

    /** Nonsensitive database configuration. */
    @Data
    @ConfigurationProperties
    public static class Db {
      private Hikari hikari = new Hikari();
      private Wrapper wrapper = new Wrapper();
      private String clusterIdentifierTemplate = "bfd-%s-aurora-cluster";
      private String name = "idr";
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
        private long validationTimeoutMs = 0;
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
  }
}
