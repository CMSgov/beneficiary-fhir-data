package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;

/**
 * Simple implementation of {@link DataSourceFactory} that creates a {@link HikariDataSource}
 * instance using settings defined in a {@link DatabaseOptions} instance.
 */
@AllArgsConstructor
public class HikariDataSourceFactory implements DataSourceFactory {
  /** Used to configure constructed instances. */
  private final DatabaseOptions dbOptions;

  @Override
  public HikariDataSource createDataSource() {
    return createDataSource(null, null);
  }

  @Override
  public HikariDataSource createDataSource(Properties properties) {
    return createDataSource(properties, null);
  }

  @Override
  public HikariDataSource createDataSource(MetricRegistry metricRegistry) {
    return createDataSource(null, metricRegistry);
  }

  @Override
  public HikariDataSource createDataSource(Properties properties, MetricRegistry metricRegistry) {
    HikariDataSource pooledDataSource = new HikariDataSource();
    configureDataSource(pooledDataSource, properties, metricRegistry);
    return pooledDataSource;
  }

  /**
   * Applies settings from {@link HikariDataSourceFactory#dbOptions} to the provided data source.
   *
   * @param dataSource data source to configure
   * @param properties a collection of properties that will configure the resulting {@link
   *     DataSource}
   * @param metricRegistry the {@link MetricRegistry} that will be used to generate metrics on the
   *     {@link HikariDataSource}
   */
  protected void configureDataSource(
      HikariDataSource dataSource,
      @Nullable Properties properties,
      @Nullable MetricRegistry metricRegistry) {
    dataSource.setJdbcUrl(dbOptions.getDatabaseUrl());
    dataSource.setUsername(dbOptions.getDatabaseUsername());
    dataSource.setPassword(dbOptions.getDatabasePassword());
    dataSource.setRegisterMbeans(true);

    final var hikariOptions = dbOptions.getHikariOptions();
    dataSource.setMaximumPoolSize(Math.max(2, hikariOptions.getMaximumPoolSize()));
    dataSource.setMinimumIdle(hikariOptions.getMinimumIdleConnections());
    dataSource.setIdleTimeout(hikariOptions.getIdleTimeoutMs());
    dataSource.setInitializationFailTimeout(hikariOptions.getInitializationFailTimeoutMs());
    dataSource.setConnectionTimeout(hikariOptions.getConnectionTimeoutMs());
    dataSource.setKeepaliveTime(hikariOptions.getKeepaliveTimeMs());
    dataSource.setValidationTimeout(hikariOptions.getValidationTimeoutMs());
    dataSource.setMaxLifetime(hikariOptions.getMaxConnectionLifetimeMs());

    if (metricRegistry != null) {
      dataSource.setMetricRegistry(metricRegistry);
    }

    if (properties != null) {
      dataSource.setDataSourceProperties(properties);
    }
  }
}
