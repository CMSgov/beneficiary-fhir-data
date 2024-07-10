package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;

/** Implementations of this interface create {@link HikariDataSource} instances on demand. */
public interface HikariDataSourceFactory extends DataSourceFactory {
  /**
   * Create a properly configured {@link HikariDataSource} instance.
   *
   * @return the instance
   */
  HikariDataSource createDataSource();

  /**
   * Create a properly configured {@link HikariDataSource} instance that is configured with the
   * provided {@link MetricRegistry}.
   *
   * @param metricRegistry the {@link MetricRegistry} that will be used to generate metrics on the
   *     {@link HikariDataSource}
   * @return the instance
   */
  HikariDataSource createDataSource(MetricRegistry metricRegistry);
}
