package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import java.util.Properties;
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
   * Create a properly configured {@link DataSource} instance.
   *
   * @param properties a collection of properties that will configure the resulting {@link
   *     DataSource}
   * @return the instance
   */
  DataSource createDataSource(Properties properties);

  /**
   * Create a properly configured {@link DataSource} instance.
   *
   * @param metricRegistry the {@link MetricRegistry} that will be used to generate metrics on the
   *     {@link DataSource}
   * @return the instance
   */
  DataSource createDataSource(MetricRegistry metricRegistry);

  /**
   * Create a properly configured {@link DataSource} instance that is configured with the provided
   * {@link MetricRegistry}.
   *
   * @param properties a collection of properties that will configure the resulting {@link
   *     DataSource}
   * @param metricRegistry the {@link MetricRegistry} that will be used to generate metrics on the
   *     {@link DataSource}
   * @return the instance
   */
  DataSource createDataSource(Properties properties, MetricRegistry metricRegistry);
}
