package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import javax.sql.DataSource;

/** Implementations of this interface create {@link DataSource} instances on demand. */
public interface DataSourceFactory {

  /**
   * Create a properly configured {@link DataSource} instance.
   *
   * @return the instance
   */
  DataSource createDataSource();

  /**
   * Create a properly configured {@link DataSource} instance that is configured with the provided
   * {@link MetricRegistry}.
   *
   * @param metricRegistry the {@link MetricRegistry} that will be used to generate metrics on the
   *     {@link DataSource}
   * @return the instance
   */
  DataSource createDataSource(MetricRegistry metricRegistry);
}
