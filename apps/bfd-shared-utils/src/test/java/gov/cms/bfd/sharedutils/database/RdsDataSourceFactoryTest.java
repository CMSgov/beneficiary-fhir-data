package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;

/** Unit tests for {@link RdsDataSourceFactory}. */
@ExtendWith(MockitoExtension.class)
public class RdsDataSourceFactoryTest {
  /** Fixed options for use in tests. */
  private final DatabaseOptions databaseOptions =
      DatabaseOptions.builder()
          .authenticationType(DatabaseOptions.AuthenticationType.RDS)
          .databaseUrl("jdbc:postgres://host-name:111/")
          .databaseUsername("user")
          .databasePassword("pass")
          .maxPoolSize(10)
          .build();

  /** Mock {@link AwsClientConfig}. */
  @Mock private AwsClientConfig awsClientConfig;

  /** Mock {@link RdsClientBuilder}. */
  @Mock private RdsClientBuilder rdsClientBuilder;

  /** Mock {@link RdsClient}. */
  @Mock private RdsClient rdsClient;

  /** Verifies default settings are used for clock and tokenTtlMillis. */
  @Test
  void constructorShouldUseDefaultConfigSettings() {
    var dataSourceFactory =
        RdsDataSourceFactory.builder()
            .awsClientConfig(awsClientConfig)
            .databaseOptions(databaseOptions)
            .build();

    var expectedDataSourceConfig =
        RdsHikariDataSource.Config.builder()
            .databaseUser("user")
            .databaseHost("host-name")
            .databasePort(111)
            .clock(Clock.systemUTC())
            .tokenTtlMillis(RdsDataSourceFactory.DEFAULT_TOKEN_TTL_MILLIS)
            .build();

    assertEquals(expectedDataSourceConfig, dataSourceFactory.getDataSourceConfig());
  }

  /**
   * Verify that {@link RdsClientBuilder} and {@link RdsHikariDataSource} are configured correctly.
   */
  @Test
  void shouldConfigureDataSource() {
    // Using a spy lets us override and verify method calls.
    var dataSourceFactory =
        spy(
            RdsDataSourceFactory.builder()
                .awsClientConfig(awsClientConfig)
                .databaseOptions(databaseOptions)
                .build());

    // wire up the mocks we need
    var dataSource = mock(RdsHikariDataSource.class);
    doReturn(rdsClientBuilder).when(dataSourceFactory).createRdsClientBuilder();
    doReturn(rdsClient).when(rdsClientBuilder).build();
    doReturn(dataSource).when(dataSourceFactory).createRdsHikariDataSource(rdsClient);

    // We're just verifying these are called.  We don't want them to do anything.
    doNothing().when(awsClientConfig).configureAwsService(rdsClientBuilder);
    doNothing().when(dataSourceFactory).configureDataSource(dataSource, null);

    // now create the data source and verify the calls were made as expected
    assertSame(dataSource, dataSourceFactory.createDataSource());
    verify(awsClientConfig).configureAwsService(rdsClientBuilder);
    verify(rdsClientBuilder).credentialsProvider(any(DefaultCredentialsProvider.class));
    verify(dataSourceFactory).configureDataSource(dataSource, null);
  }
}
