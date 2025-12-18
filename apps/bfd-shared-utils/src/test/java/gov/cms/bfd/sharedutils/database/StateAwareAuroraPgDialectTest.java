package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.dialect.HostListProviderSupplier;
import software.amazon.jdbc.util.FullServicesContainer;

/** Unit tests for {@link StateAwareAuroraPgDialect}. */
class StateAwareAuroraPgDialectTest {

  /**
   * Test ensuring that {@link StateAwareAuroraPgDialect#getHostListProvider()} returns a {@link
   * HostListProviderSupplier} that returns a {@link StateAwareMonitoringRdsHostListProvider} when
   * its {@link HostListProviderSupplier#getProvider(Properties, String, FullServicesContainer)}
   * method is called.
   */
  @Test
  void testGetHostListProviderReturnsCorrectHostListSupplierWhenCalled() {
    // Arrange
    final var mockFullServicesContainer = mock(FullServicesContainer.class);
    final var properties = new Properties();
    final var dialect = new StateAwareAuroraPgDialect(mock(RdsClient.class));
    final var supplier = dialect.getHostListProvider();

    // Act
    final var provider = supplier.getProvider(properties, "", mockFullServicesContainer);

    // Assert
    assertInstanceOf(StateAwareMonitoringRdsHostListProvider.class, provider);
  }
}
