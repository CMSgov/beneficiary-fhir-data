package gov.cms.bfd.sharedutils.database;

import java.util.Properties;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import software.amazon.jdbc.ds.AwsWrapperDataSource;

/** Testing. */
@AllArgsConstructor
public class AwsWrapperDataSourceFactory implements SimpleDataSourceFactory {

  /** Testing. */
  private final DatabaseOptions databaseOptions;

  @Override
  public DataSource createDataSource() {
    AwsWrapperDataSource dataSource = new AwsWrapperDataSource();
    dataSource.setJdbcUrl(databaseOptions.getDatabaseUrl());
    dataSource.setUser(databaseOptions.getDatabaseUsername());
    dataSource.setPassword(databaseOptions.getDatabasePassword());
    Properties targetDataSourceProps = new Properties();
    targetDataSourceProps.setProperty("wrapperProfileName", "E");
    //        targetDataSourceProps.setProperty("readerHostSelectorStrategy", "leastConnections");
    dataSource.setTargetDataSourceProperties(targetDataSourceProps);
    return dataSource;
  }
}
