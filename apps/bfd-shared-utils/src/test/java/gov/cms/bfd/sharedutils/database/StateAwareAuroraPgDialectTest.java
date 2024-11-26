package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.PluginService;

class StateAwareAuroraPgDialectTest {

  @Test
  void testGetHostListProviderReturnsCorrectHostListSupplierWhenCalled() {
    // Arrange
    final var mockHostListProviderService = mock(HostListProviderService.class);
    final var mockPluginService = mock(PluginService.class);
    final var mockAwsClientConfig = mock(AwsClientConfig.class);
    final var properties = new Properties();
    final var dialect = new StateAwareAuroraPgDialect(mockAwsClientConfig);
    final var supplier = dialect.getHostListProvider();

    // Act
    final var provider =
        supplier.getProvider(properties, "", mockHostListProviderService, mockPluginService);

    // Assert
    assertInstanceOf(StateAwareMonitoringRdsHostListProvider.class, provider);
  }
}
