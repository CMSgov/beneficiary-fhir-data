package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.dialect.HostListProviderSupplier;

/** Unit tests for {@link StateAwareAuroraPgDialect}. */
class StateAwareAuroraPgDialectTest {

  /**
   * Test ensuring that {@link StateAwareAuroraPgDialect#getHostListProvider()} returns a {@link
   * HostListProviderSupplier} that returns a {@link StateAwareMonitoringRdsHostListProvider} when
   * its {@link HostListProviderSupplier#getProvider(Properties, String, HostListProviderService,
   * PluginService)} method is called.
   */
  @Test
  void testGetHostListProviderReturnsCorrectHostListSupplierWhenCalled() {
    // Arrange
    final var mockHostListProviderService = mock(HostListProviderService.class);
    final var mockPluginService = mock(PluginService.class);
    final var mockAwsClientConfig = mock(AwsClientConfig.class);
    final var properties = new Properties();
    final var dialect = spy(new StateAwareAuroraPgDialect(mockAwsClientConfig));
    doReturn(mock(RdsClient.class)).when(dialect).getRdsClient(any(AwsClientConfig.class));
    final var supplier = dialect.getHostListProvider();

    // Act
    final var provider =
        supplier.getProvider(properties, "", mockHostListProviderService, mockPluginService);

    // Assert
    assertInstanceOf(StateAwareMonitoringRdsHostListProvider.class, provider);
  }
}
