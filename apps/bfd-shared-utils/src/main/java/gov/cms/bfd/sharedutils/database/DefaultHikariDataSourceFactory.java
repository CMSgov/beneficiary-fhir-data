package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;

/**
 * Simple implementation of {@link HikariDataSourceFactory} that creates a {@link HikariDataSource}
 * instance using settings defined in a {@link DatabaseOptions} instance.
 */
@AllArgsConstructor
public class DefaultHikariDataSourceFactory implements HikariDataSourceFactory {
  /** Used to configure constructed instances. */
  private final DatabaseOptions dbOptions;

  @Override
  public HikariDataSource createDataSource() {
    return createDataSource(null);
  }

  @Override
  public HikariDataSource createDataSource(MetricRegistry metricRegistry) {
    HikariDataSource pooledDataSource = new HikariDataSource();
    configureDataSource(pooledDataSource, metricRegistry);
    return pooledDataSource;
  }

  /**
   * Applies settings from {@link DefaultHikariDataSourceFactory#dbOptions} to the provided data
   * source.
   *
   * @param dataSource data source to configure
   * @param metricRegistry the {@link MetricRegistry} that will be used to generate metrics on the
   *     {@link HikariDataSource}
   */
  protected void configureDataSource(
      HikariDataSource dataSource, @Nullable MetricRegistry metricRegistry) {
    dataSource.setJdbcUrl(dbOptions.getDatabaseUrl());
    dataSource.setUsername(dbOptions.getDatabaseUsername());
    dataSource.setPassword(dbOptions.getDatabasePassword());
    dataSource.setRegisterMbeans(true);

    final var hikariOptions = dbOptions.getHikariOptions();
    dataSource.setMaximumPoolSize(Math.max(2, hikariOptions.getMaximumPoolSize()));
    dataSource.setMinimumIdle(hikariOptions.getMinimumIdleConnections());
    dataSource.setIdleTimeout(hikariOptions.getIdleTimeoutMs());
    dataSource.setInitializationFailTimeout(
            hikariOptions.getInitializationFailTimeoutMs());
    dataSource.setConnectionTimeout(hikariOptions.getConnectionTimeoutMs());
    dataSource.setKeepaliveTime(hikariOptions.getKeepaliveTimeMs());
    dataSource.setValidationTimeout(hikariOptions.getValidationTimeoutMs());
    dataSource.setMaxLifetime(hikariOptions.getMaxConnectionLifetimeMs());

    /*
     * FIXME Temporary workaround for CBBI-357: send Postgres' query planner a
     * strongly worded letter instructing it to avoid sequential scans whenever
     * possible.
     */
    if (dataSource.getJdbcUrl() != null && dataSource.getJdbcUrl().contains("postgre"))
      dataSource.setConnectionInitSql(
          "set application_name = 'bfd-server'; set enable_seqscan = false;");

    if (metricRegistry != null) {
      dataSource.setMetricRegistry(metricRegistry);
    }

    /*
     * FIXME Temporary setting for BB-1233 to find the source of any possible leaks
     * (see: https://github.com/brettwooldridge/HikariCP/issues/1111)
     */
    dataSource.setLeakDetectionThreshold(60 * 1000);
  }
}
