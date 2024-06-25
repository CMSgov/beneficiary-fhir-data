package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
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
    HikariDataSource pooledDataSource = new HikariDataSource();
    configureDataSource(pooledDataSource);
    return pooledDataSource;
  }

  /**
   * Applies settings from {@link HikariDataSourceFactory#dbOptions} to the provided data source.
   *
   * @param dataSource data source to configure
   */
  protected void configureDataSource(HikariDataSource dataSource) {
    dataSource.setJdbcUrl(dbOptions.getDatabaseUrl());
    dataSource.setUsername(dbOptions.getDatabaseUsername());
    dataSource.setPassword(dbOptions.getDatabasePassword());
    // Setting the pool size to 1 is the dumbest possible change to "disable" Hikari connection pooling so that we
    // rely entirely on RDS Proxy
    dataSource.setMaximumPoolSize(1);
    dataSource.setRegisterMbeans(true);
  }
}
