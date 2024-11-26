package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.HostSpecBuilder;
import software.amazon.jdbc.JdbcCallable;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.hostavailability.HostAvailability;
import software.amazon.jdbc.hostavailability.HostAvailabilityStrategy;

@ExtendWith(MockitoExtension.class)
class SingleNodeHostOverrideConnectionPluginTest {
  private static final HostSpec AVAILABLE_CLUSTER_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("cluster", getClusterUrl("cluster"), HostAvailability.AVAILABLE));
  public static final HostSpec AVAILABLE_WRITER_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("writer", getInstanceUrl("writer"), HostAvailability.AVAILABLE));
  public static final HostSpec NOT_AVAILABLE_READER1_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec(
              "reader1", getInstanceUrl("reader1"), HostAvailability.NOT_AVAILABLE));
  public static final HostSpec NOT_AVAILABLE_READER2_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec(
              "reader2", getInstanceUrl("reader2"), HostAvailability.NOT_AVAILABLE));
  public static final HostSpec AVAILABLE_READER1_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("reader1", getInstanceUrl("reader1"), HostAvailability.AVAILABLE));
  public static final HostSpec AVAILABLE_READER2_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("reader2", getInstanceUrl("reader2"), HostAvailability.AVAILABLE));
  // @Mock is used instead of mock() to support generics
  @Mock private JdbcCallable<Connection, SQLException> mockConnectFunc;
  @Mock private JdbcCallable<Void, SQLException> mockInitHostProviderFunc;

  @Test
  void testGetSubscribedMethodsReturnsExpectedMethodsWhenCalled() {
    // Arrange
    final var mockPluginService = mock(PluginService.class);
    final var plugin = new SingleNodeHostOverrideConnectionPlugin(mockPluginService);

    // Act
    final var actualMethods = plugin.getSubscribedMethods();

    // Assert
    assertEquals(Set.of("initHostProvider", "connect", "forceConnect"), actualMethods);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAllTestConnectsParameters")
  void testConnect(
      String testCaseName,
      HostSpec connectionHostSpec,
      List<HostSpec> mockAllHosts,
      int jdbcCallableCallInvocations,
      int pluginServiceConnectInvocations,
      HostSpec expectedPluginServiceConnectHostSpec)
      throws SQLException {
    // Arrange
    final var mockPluginService = mock(PluginService.class);
    final var mockProperties = mock(Properties.class);
    lenient().when(mockPluginService.getAllHosts()).thenReturn(mockAllHosts);
    final var plugin = new SingleNodeHostOverrideConnectionPlugin(mockPluginService);

    // Act
    try (final var ignored =
        plugin.connect("", connectionHostSpec, mockProperties, false, mockConnectFunc)) {

      // Assert
      verify(mockConnectFunc, times(jdbcCallableCallInvocations)).call();
      verify(mockPluginService, times(pluginServiceConnectInvocations))
          .connect(expectedPluginServiceConnectHostSpec, mockProperties);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAllTestConnectsParameters")
  void testForceConnect(
      String testCaseName,
      HostSpec connectionHostSpec,
      List<HostSpec> mockAllHosts,
      int jdbcCallableCallInvocations,
      int pluginServiceConnectInvocations,
      HostSpec expectedPluginServiceConnectHostSpec)
      throws SQLException {
    // Arrange
    final var mockPluginService = mock(PluginService.class);
    final var mockProperties = mock(Properties.class);
    lenient().when(mockPluginService.getAllHosts()).thenReturn(mockAllHosts);
    final var plugin = new SingleNodeHostOverrideConnectionPlugin(mockPluginService);

    // Act
    try (final var ignored =
        plugin.forceConnect("", connectionHostSpec, mockProperties, false, mockConnectFunc)) {

      // Assert
      verify(mockConnectFunc, times(jdbcCallableCallInvocations)).call();
      verify(mockPluginService, times(pluginServiceConnectInvocations))
          .connect(expectedPluginServiceConnectHostSpec, mockProperties);
    }
  }

  @Test
  void testInitHostProviderShouldCallInitFuncWhenHostProviderIsDynamic() throws SQLException {
    // Arrange
    final var mockPluginService = mock(PluginService.class);
    final var mockProperties = mock(Properties.class);
    final var mockHostListProviderService = mock(HostListProviderService.class);
    when(mockHostListProviderService.isStaticHostListProvider()).thenReturn(false);
    final var plugin = new SingleNodeHostOverrideConnectionPlugin(mockPluginService);

    // Act
    plugin.initHostProvider(
        "", "", mockProperties, mockHostListProviderService, mockInitHostProviderFunc);

    // Assert
    verify(mockInitHostProviderFunc, times(1)).call();
  }

  @Test
  void testInitHostProviderShouldThrowSQLExceptionWhenHostProviderIsStatic() throws SQLException {
    // Arrange
    final var mockPluginService = mock(PluginService.class);
    final var mockProperties = mock(Properties.class);
    final var mockHostListProviderService = mock(HostListProviderService.class);
    when(mockHostListProviderService.isStaticHostListProvider()).thenReturn(true);
    final var plugin = new SingleNodeHostOverrideConnectionPlugin(mockPluginService);

    // Act & Assert
    assertThrows(
        SQLException.class,
        () ->
            plugin.initHostProvider(
                "", "", mockProperties, mockHostListProviderService, mockInitHostProviderFunc));
    verify(mockInitHostProviderFunc, times(0)).call();
  }

  private static Stream<Arguments> provideAllTestConnectsParameters() {
    return Stream.of(
        Arguments.of(
            "testConnectUsesSingleWriterNodeEndpointWhenOnlyWriterNodeInTopology",
            AVAILABLE_CLUSTER_HOST_SPEC,
            List.of(AVAILABLE_WRITER_HOST_SPEC),
            0,
            1,
            AVAILABLE_WRITER_HOST_SPEC),
        Arguments.of(
            "testConnectUsesSingleWriterNodeEndpointWhenOnlyWriterNodeAvailable",
            AVAILABLE_CLUSTER_HOST_SPEC,
            List.of(
                AVAILABLE_WRITER_HOST_SPEC,
                NOT_AVAILABLE_READER1_HOST_SPEC,
                NOT_AVAILABLE_READER2_HOST_SPEC),
            0,
            1,
            AVAILABLE_WRITER_HOST_SPEC),
        Arguments.of(
            "testConnectUsesClusterEndpointWhenInstanceEndpointIsUsed",
            AVAILABLE_READER1_HOST_SPEC,
            List.of(
                AVAILABLE_WRITER_HOST_SPEC,
                AVAILABLE_READER1_HOST_SPEC,
                AVAILABLE_READER2_HOST_SPEC),
            1,
            0,
            null),
        Arguments.of(
            "testConnectUsesClusterEndpointWhenMultipleHostsAreAvailable",
            AVAILABLE_CLUSTER_HOST_SPEC,
            List.of(
                AVAILABLE_WRITER_HOST_SPEC,
                AVAILABLE_READER1_HOST_SPEC,
                AVAILABLE_READER2_HOST_SPEC),
            1,
            0,
            null),
        Arguments.of(
            "testConnectUsesClusterEndpointWhenOnlyClusterIsInTopology",
            AVAILABLE_CLUSTER_HOST_SPEC,
            List.of(AVAILABLE_CLUSTER_HOST_SPEC),
            1,
            0,
            null),
        Arguments.of(
            "testConnectUsesClusterEndpointWhenNoHostsAreInTopology",
            AVAILABLE_CLUSTER_HOST_SPEC,
            List.of(),
            1,
            0,
            null));
  }

  private static String getClusterUrl(String hostId) {
    return String.format("%s.cluster-uniqueid.us-east-1.rds.amazonaws.com", hostId);
  }

  private static String getInstanceUrl(String hostId) {
    return String.format("%s.uniqueid.us-east-1.rds.amazonaws.com", hostId);
  }

  private record PartialHostSpec(String hostId, String hostUrl, HostAvailability hostAvailability) {
    static HostSpec toHostSpec(PartialHostSpec from) {
      final var mockHostAvailabilityStrategy = mock(HostAvailabilityStrategy.class);
      lenient()
          .when(mockHostAvailabilityStrategy.getHostAvailability(any(HostAvailability.class)))
          .thenAnswer(ans -> ans.getArguments()[0]);
      return new HostSpecBuilder(mockHostAvailabilityStrategy)
          .hostId(from.hostId())
          .host(from.hostUrl())
          .availability(from.hostAvailability())
          .build();
    }
  }
}
