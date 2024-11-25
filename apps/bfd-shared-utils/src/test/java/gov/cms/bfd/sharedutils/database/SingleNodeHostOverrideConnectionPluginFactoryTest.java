package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import software.amazon.jdbc.PluginService;

class SingleNodeHostOverrideConnectionPluginFactoryTest {

  @Test
  void testGetInstanceShouldReturnCorrectPluginTypeWhenCalled() {
    // Arrange
    final var mockPluginService = mock(PluginService.class);
    final var mockProperties = mock(Properties.class);
    final var pluginFactory = new SingleNodeHostOverrideConnectionPluginFactory();

    // Act
    final var plugin = pluginFactory.getInstance(mockPluginService, mockProperties);

    // Assert
    assertInstanceOf(SingleNodeHostOverrideConnectionPlugin.class, plugin);
  }
}
