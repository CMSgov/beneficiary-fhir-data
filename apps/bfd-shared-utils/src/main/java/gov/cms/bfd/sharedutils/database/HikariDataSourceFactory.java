package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import lombok.AllArgsConstructor;
import software.amazon.jdbc.ds.AwsWrapperDataSource;

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
    dataSource.setMaximumPoolSize(Math.max(2, dbOptions.getMaxPoolSize()));
    dataSource.setRegisterMbeans(true);
    dataSource.setDataSourceClassName(AwsWrapperDataSource.class.getName());
    dataSource.setExceptionOverrideClassName("software.amazon.jdbc.util.HikariCPSQLException");
    Properties targetDataSourceProps = new Properties();
    targetDataSourceProps.setProperty("wrapperPlugins", "failover,efm2");
    dataSource.addDataSourceProperty("targetDataSourceProperties", targetDataSourceProps);
  }
}
