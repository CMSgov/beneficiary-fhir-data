package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
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
    HikariDataSource pooledDataSource = new HikariDataSource();
    configureDataSource(pooledDataSource);
    return pooledDataSource;
  }

  /**
   * Applies settings from {@link DefaultHikariDataSourceFactory#dbOptions} to the provided data
   * source.
   *
   * @param dataSource data source to configure
   */
  protected void configureDataSource(HikariDataSource dataSource) {
    dataSource.setJdbcUrl(dbOptions.getDatabaseUrl());
    dataSource.setUsername(dbOptions.getDatabaseUsername());
    dataSource.setPassword(dbOptions.getDatabasePassword());
    dataSource.setMaximumPoolSize(Math.max(2, dbOptions.getMaxPoolSize()));
    dataSource.setRegisterMbeans(true);
  }
}
