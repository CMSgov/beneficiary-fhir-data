package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Simple implementation of {@link DataSourceFactory} that creates a {@link HikariDataSource}
 * instance using settings defined in a {@link DatabaseOptions} instance.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class HikariDataSourceFactory implements DataSourceFactory {
  /** Used to configure constructed instances. */
  private final DatabaseOptions dbOptions;

  /** Used to create a new uninitialized instance. */
  private final Supplier<HikariDataSource> createDataSource;

  /**
   * Primary constructor that creates a normal {@link HikariDataSource}.
   *
   * @param dbOptions used to configure constructed instances
   */
  public HikariDataSourceFactory(DatabaseOptions dbOptions) {
    this(dbOptions, HikariDataSource::new);
  }

  @Override
  public HikariDataSource createDataSource() {
    HikariDataSource pooledDataSource = createDataSource.get();
    pooledDataSource.setJdbcUrl(dbOptions.getDatabaseUrl());
    pooledDataSource.setUsername(dbOptions.getDatabaseUsername());
    pooledDataSource.setPassword(dbOptions.getDatabasePassword());
    pooledDataSource.setMaximumPoolSize(Math.max(2, dbOptions.getMaxPoolSize()));
    pooledDataSource.setRegisterMbeans(true);
    return pooledDataSource;
  }
}
