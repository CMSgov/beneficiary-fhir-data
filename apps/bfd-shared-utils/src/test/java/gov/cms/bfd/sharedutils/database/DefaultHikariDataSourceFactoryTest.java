package gov.cms.bfd.sharedutils.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultHikariDataSourceFactory}. */
public class DefaultHikariDataSourceFactoryTest {
  /** Verify that options are applied when data source is configured. */
  @Test
  void shouldConfigureDataSource() {
    final var databaseOptions =
        DatabaseOptions.builder()
            .authenticationType(DatabaseOptions.AuthenticationType.RDS)
            .databaseUrl("jdbc:postgres://host-name:111/")
            .databaseUsername("user")
            .databasePassword("pass")
            .maxPoolSize(10)
            .build();
    var dataSource = mock(HikariDataSource.class);
    var factory = new DefaultHikariDataSourceFactory(databaseOptions);
    factory.configureDataSource(dataSource, null);
    verify(dataSource).setJdbcUrl(databaseOptions.getDatabaseUrl());
    verify(dataSource).setUsername(databaseOptions.getDatabaseUsername());
    verify(dataSource).setPassword(databaseOptions.getDatabasePassword());
    verify(dataSource).setMaximumPoolSize(databaseOptions.getMaxPoolSize());
    verify(dataSource).setRegisterMbeans(true);
  }
}
