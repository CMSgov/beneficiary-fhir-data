package gov.cms.bfd.sharedutils.database;

import javax.sql.DataSource;

/** Implementations of this interface create {@link DataSource} instances on demand. */
public interface DataSourceFactory {

  /**
   * Create a properly configured {@link DataSource} instance.
   *
   * @return the instance
   */
  DataSource createDataSource();
}
