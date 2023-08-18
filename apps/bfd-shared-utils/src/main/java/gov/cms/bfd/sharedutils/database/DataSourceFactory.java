package gov.cms.bfd.sharedutils.database;

import com.zaxxer.hikari.HikariDataSource;

public interface DataSourceFactory {
  HikariDataSource createDataSource();
}
