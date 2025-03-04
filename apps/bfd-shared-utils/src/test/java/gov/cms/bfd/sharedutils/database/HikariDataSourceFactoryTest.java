package gov.cms.bfd.sharedutils.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link HikariDataSourceFactory}. */
public class HikariDataSourceFactoryTest {
  /** Verify that options are applied when data source is configured. */
  @Test
  void shouldConfigureDataSource() {
    final var databaseOptions =
        DatabaseOptions.builder()
            .authenticationType(DatabaseOptions.AuthenticationType.RDS)
            .databaseUrl("jdbc:postgres://host-name:111/")
            .databaseUsername("user")
            .databasePassword("pass")
            .hikariOptions(DatabaseOptions.HikariOptions.builder().maximumPoolSize(10).build())
            .build();
    var dataSource = mock(HikariDataSource.class);
    var factory = new HikariDataSourceFactory(databaseOptions);
    factory.configureDataSource(dataSource, null, null);
    verify(dataSource).setJdbcUrl(databaseOptions.getDatabaseUrl());
    verify(dataSource).setUsername(databaseOptions.getDatabaseUsername());
    verify(dataSource).setPassword(databaseOptions.getDatabasePassword());
    verify(dataSource).setMaximumPoolSize(databaseOptions.getHikariOptions().getMaximumPoolSize());
    verify(dataSource).setRegisterMbeans(true);
  }
}
