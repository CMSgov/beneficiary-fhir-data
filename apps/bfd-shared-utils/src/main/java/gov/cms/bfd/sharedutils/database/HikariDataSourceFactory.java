package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HikariDataSourceFactory implements DataSourceFactory {
  private final DatabaseOptions dbOptions;
  private final Supplier<HikariDataSource> createDataSource;

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
