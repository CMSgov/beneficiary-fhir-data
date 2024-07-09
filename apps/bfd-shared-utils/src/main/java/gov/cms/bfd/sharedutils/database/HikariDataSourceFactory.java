package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;

/** Implementations of this interface create {@link HikariDataSource} instances on demand. */
public interface HikariDataSourceFactory extends DataSourceFactory {
  /**
   * Create a properly configured {@link HikariDataSource} instance.
   *
   * @return the instance
   */
  HikariDataSource createDataSource();
}
