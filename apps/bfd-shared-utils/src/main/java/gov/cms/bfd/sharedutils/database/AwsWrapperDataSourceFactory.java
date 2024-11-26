package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.database.DatabaseOptions.AwsJdbcWrapperOptions;
import gov.cms.bfd.sharedutils.database.DatabaseOptions.HikariOptions;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.HikariPooledConnectionProvider;
import software.amazon.jdbc.HostListProvider;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.PropertyDefinition;
import software.amazon.jdbc.dialect.DialectManager;
import software.amazon.jdbc.ds.AwsWrapperDataSource;
import software.amazon.jdbc.hostlistprovider.RdsHostListProvider;
import software.amazon.jdbc.plugin.AuroraInitialConnectionStrategyPlugin;
import software.amazon.jdbc.plugin.failover2.FailoverConnectionPlugin;
import software.amazon.jdbc.profile.ConfigurationProfile;
import software.amazon.jdbc.profile.ConfigurationProfileBuilder;
import software.amazon.jdbc.profile.DriverConfigurationProfiles;
import software.amazon.jdbc.targetdriverdialect.TargetDriverDialectCodes;
import software.amazon.jdbc.targetdriverdialect.TargetDriverDialectManager;

/**
 * This class implements a {@link DataSourceFactory} that creates {@link AwsWrapperDataSource}
 * instances on demand.
 */
@AllArgsConstructor
public class AwsWrapperDataSourceFactory implements DataSourceFactory {

  /**
   * Name of custom preset created when calling {@link #getCustomPresetProfile(MetricRegistry,
   * AwsJdbcWrapperOptions, HikariOptions)}.
   */
  @VisibleForTesting static final String CUSTOM_PRESET_NAME = "custom-preset";

  /** Used to configure constructed instances. */
  private final DatabaseOptions databaseOptions;

  /**
   * Configuration settings for {@link RdsClient}s used in the "state aware" {@link
   * HostListProvider}s.
   */
  private final AwsClientConfig awsClientConfig;

  @Override
  public AwsWrapperDataSource createDataSource() {
    return createDataSource(null, null);
  }

  @Override
  public AwsWrapperDataSource createDataSource(Properties properties) {
    return createDataSource(properties, null);
  }

  @Override
  public AwsWrapperDataSource createDataSource(MetricRegistry metricRegistry) {
    return createDataSource(null, metricRegistry);
  }

  @Override
  public AwsWrapperDataSource createDataSource(
      Properties properties, MetricRegistry metricRegistry) {
    Preconditions.checkNotNull(
        databaseOptions.getAwsJdbcWrapperOptions(),
        "AWS JDCB Wrapper options must not be null when creating an AwsWrapperDataSource");

    final var dataSource = new AwsWrapperDataSource();
    dataSource.setJdbcUrl(databaseOptions.getDatabaseUrl());
    dataSource.setUser(databaseOptions.getDatabaseUsername());
    dataSource.setPassword(databaseOptions.getDatabasePassword());

    final var hikariOptions = databaseOptions.getHikariOptions();
    final var wrapperOptions = databaseOptions.getAwsJdbcWrapperOptions();
    if (wrapperOptions.useCustomPreset()) {
      final var customPresetProfile =
          getCustomPresetProfile(metricRegistry, wrapperOptions, hikariOptions);
      DriverConfigurationProfiles.addOrReplaceProfile(
          customPresetProfile.getName(), customPresetProfile);
    }

    DialectManager.setCustomDialect(new StateAwareAuroraPgDialect(awsClientConfig));

    final var targetDataSourceProps = getProperties(wrapperOptions);
    dataSource.setTargetDataSourceProperties(mergeProperties(properties, targetDataSourceProps));

    return dataSource;
  }

  /**
   * Builds and returns a custom {@link ConfigurationProfile} used by the AWS JDBC Wrapper to
   * configure its behavior based upon user-configured options.
   *
   * @param metricRegistry the {@link MetricRegistry} that metrics will be sent to
   * @param wrapperOptions the user-configured options for the AWS JDBC Wrapper
   * @param hikariOptions the user-configured options for the AWS JDBC Wrapper's internal HikariCP
   *     connection pool
   * @return a custom {@link ConfigurationProfile} configured from the various user-provided options
   */
  private ConfigurationProfile getCustomPresetProfile(
      MetricRegistry metricRegistry,
      AwsJdbcWrapperOptions wrapperOptions,
      HikariOptions hikariOptions) {
    return ConfigurationProfileBuilder.get()
        .from(wrapperOptions.getBasePresetCode())
        .withName(CUSTOM_PRESET_NAME)
        .withPluginFactories(wrapperOptions.getPlugins())
        .withConnectionProvider(
            new HikariPooledConnectionProvider(
                (HostSpec hostSpec, Properties originalProps) -> {
                  final var config = new HikariConfig();
                  config.setRegisterMbeans(true);
                  config.setMaximumPoolSize(hikariOptions.getMaximumPoolSize());
                  config.setMinimumIdle(hikariOptions.getMinimumIdleConnections());
                  config.setIdleTimeout(hikariOptions.getIdleTimeoutMs());
                  config.setInitializationFailTimeout(
                      hikariOptions.getInitializationFailTimeoutMs());
                  config.setConnectionTimeout(hikariOptions.getConnectionTimeoutMs());
                  config.setKeepaliveTime(hikariOptions.getKeepaliveTimeMs());
                  config.setValidationTimeout(hikariOptions.getValidationTimeoutMs());
                  config.setMaxLifetime(hikariOptions.getMaxConnectionLifetimeMs());

                  if (metricRegistry != null) {
                    config.setMetricRegistry(metricRegistry);
                  }

                  return config;
                }))
        .build();
  }

  /**
   * Configures a {@link Properties} collection with various AWS JDBC Wrapper options and returns
   * it.
   *
   * @param wrapperOptions the user-configured options for the AWS JDBC Wrapper
   * @return a {@link Properties} object configured with the given {@link AwsJdbcWrapperOptions}
   */
  private static Properties getProperties(AwsJdbcWrapperOptions wrapperOptions) {
    final var targetDataSourceProps = new Properties();
    targetDataSourceProps.setProperty(
        TargetDriverDialectManager.TARGET_DRIVER_DIALECT.name, TargetDriverDialectCodes.PG_JDBC);
    targetDataSourceProps.setProperty(
        PropertyDefinition.PROFILE_NAME.name,
        wrapperOptions.useCustomPreset() ? CUSTOM_PRESET_NAME : wrapperOptions.getBasePresetCode());
    targetDataSourceProps.setProperty(PropertyDefinition.AUTO_SORT_PLUGIN_ORDER.name, "false");
    targetDataSourceProps.setProperty(
        AuroraInitialConnectionStrategyPlugin.READER_HOST_SELECTOR_STRATEGY.name,
        wrapperOptions.getHostSelectorStrategy());
    targetDataSourceProps.setProperty(
        FailoverConnectionPlugin.FAILOVER_READER_HOST_SELECTOR_STRATEGY.name,
        wrapperOptions.getHostSelectorStrategy());
    targetDataSourceProps.setProperty(
        FailoverConnectionPlugin.ENABLE_CONNECT_FAILOVER.name, "true");
    targetDataSourceProps.setProperty(
        RdsHostListProvider.CLUSTER_TOPOLOGY_REFRESH_RATE_MS.name,
        Long.toString(wrapperOptions.getClusterTopologyRefreshRateMs()));
    targetDataSourceProps.setProperty(
        StateAwareMonitoringRdsHostListProvider.INSTANCE_STATE_MONITOR_REFRESH_RATE_MS.name,
        Long.toString(wrapperOptions.getInstanceStateMonitorRefreshRateMs()));
    return targetDataSourceProps;
  }

  /**
   * Merges 2 or more {@link Properties} collections together into a single {@link Properties}
   * object. {@literal null} is ignored
   *
   * @param properties the {@link Properties} objects to merge
   * @return a merged {@link Properties}
   */
  private static Properties mergeProperties(Properties... properties) {
    return Stream.of(properties)
        .filter(Objects::nonNull)
        .collect(Properties::new, Map::putAll, Map::putAll);
  }
}
