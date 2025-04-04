package gov.cms.bfd.server.ng;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.database.AwsWrapperDataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;

/** Root configuration class. */
@Getter
@Setter
@ConfigurationProperties(prefix = "bfd")
public class Configuration {
  @Autowired private AwsCredentialsProvider credentialsProvider;
  @Autowired private AwsRegionProvider regionProvider;

  private String env;
  private Local local = new Local();
  private Sensitive sensitive = new Sensitive();
  private Nonsensitive nonsensitive = new Nonsensitive();

  private static final String BFD_ENV_LOCAL = "local";

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

  private String resolveDatabaseReaderEndpoint() {
    if (useRds()) {
      try (var rdsClient = RdsClient.create()) {
        var clusterIdentifier = String.format(nonsensitive.db.clusterIdentifierTemplate, env);
        var clusters =
            rdsClient.describeDBClusters(
                DescribeDbClustersRequest.builder().dbClusterIdentifier(clusterIdentifier).build());
        return clusters.dbClusters().getFirst().readerEndpoint();
      }
    } else {
      return local.dbUrl;
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
  @Getter
  @Setter
  @ConfigurationProperties
  public static class Local {
    private final String dbUrl = "jdbc:postgresql://localhost:5432/idr";
  }

  /** Sensitive configuration. */
  @Getter
  @Setter
  @ConfigurationProperties
  public static class Sensitive {
    private final Db db = new Db();

    /** Sensitive Database configuration. */
    @Getter
    @Setter
    @ConfigurationProperties
    public static class Db {
      private String username;
      private String password;
    }
  }

  /** Nonsensitive configuration. */
  @Getter
  @Setter
  @ConfigurationProperties
  public static class Nonsensitive {
    private final Db db = new Db();

    /** Nonsensitive database configuration. */
    @Getter
    @Setter
    @ConfigurationProperties
    public static class Db {
      private final Hikari hikari = new Hikari();
      private final Wrapper wrapper = new Wrapper();
      private final String clusterIdentifierTemplate = "bfd-%s-aurora-cluster";

      /** Hikari configuration. */
      @Getter
      @Setter
      @ConfigurationProperties
      public static class Hikari {
        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        private final int maxPoolSize = Runtime.getRuntime().availableProcessors();
        private final int minIdleConnections = Runtime.getRuntime().availableProcessors();
        private final long idleTimeoutMs = Duration.ofMinutes(10).toMillis();
        private final long initFailTimeoutMs = 1;
        private final long connectionTimeoutMs = Duration.ofSeconds(30).toMillis();
        private final long keepaliveTimeoutMs = 0;
        private final long validationTimeoutMs = 0;
        private final long maxConnectionLifetimeMs = Duration.ofMinutes(30).toMillis();
      }

      /** AWS JDBC wrapper configuration. */
      @Getter
      @Setter
      @ConfigurationProperties
      public static class Wrapper {
        private final String pluginsCsv = "auroraConnectionTracker,failover,efm2";
        private final String hostSelectorStrategy = "roundRobin";
        private final String basePreset = "E";
        private final long clusterTopologyRefreshRateMs = Duration.ofSeconds(30).toMillis();
        private final long instanceStateMonitorRefreshRateMs = Duration.ofSeconds(5).toMillis();
      }
    }
  }
}
