package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

/** Unit tests for {@link RdsHikariDataSource}. */
@ExtendWith(MockitoExtension.class)
public class RdsHikariDataSourceTest {
  /** Mock {@link RdsClient}. */
  @Mock private RdsClient rdsClient;

  /** Mock {@link RdsUtilities}. */
  @Mock private RdsUtilities rdsUtilities;

  /** Verify that {@link RdsHikariDataSource#close} closes the client. */
  @Test
  void shouldCloseClient() {
    var dataSourceConfig = RdsHikariDataSource.Config.builder().build();
    var dataSource = new RdsHikariDataSource(dataSourceConfig, rdsClient);
    dataSource.close();
    verify(rdsClient).close();
  }

  /** Verify that tokens are reused once received. */
  @Test
  void shouldReuseTokens() {
    // First two times are in same 7 ms TTL window, second two in another.
    Clock clock = mock(Clock.class);
    doReturn(5L, 10L, 30L, 35L).when(clock).millis();

    // We expect to get 2 token requests and want a different token for each.
    doReturn(rdsUtilities).when(rdsClient).utilities();
    doReturn("token1", "token2", "bad-token")
        .when(rdsUtilities)
        .generateAuthenticationToken(any(GenerateAuthenticationTokenRequest.class));

    // Create a data source with dummy config
    var dataSourceConfig =
        RdsHikariDataSource.Config.builder()
            .clock(clock)
            .databaseUser("user")
            .databaseHost("host")
            .databasePort(5432)
            .tokenTtlMillis(7L)
            .build();

    // The request object we expect based on our config.
    final var expectedRequest =
        GenerateAuthenticationTokenRequest.builder()
            .username("user")
            .hostname("host")
            .port(5432)
            .build();

    // Data source using our client and config.
    final var dataSource = new RdsHikariDataSource(dataSourceConfig, rdsClient);

    // First call time of 5 sets the expires to 12.
    assertEquals("token1", dataSource.getPassword());
    assertEquals(12L, dataSource.getExpires());

    // Second call time of 10 is before expires.
    assertEquals("token1", dataSource.getPassword());
    assertEquals(12L, dataSource.getExpires());

    // Third call time of 30 sets the expires to 37.
    assertEquals("token2", dataSource.getPassword());
    assertEquals(37L, dataSource.getExpires());

    // Fourth call time of 35 is before expires.
    assertEquals("token2", dataSource.getPassword());
    assertEquals(37L, dataSource.getExpires());

    verify(rdsUtilities, times(2)).generateAuthenticationToken(refEq(expectedRequest));
  }
}
