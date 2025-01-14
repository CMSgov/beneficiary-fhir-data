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

/** Unit tests for {@link SingleNodeHostOverrideConnectionPlugin}. */
@ExtendWith(MockitoExtension.class)
class SingleNodeHostOverrideConnectionPluginTest {
  /** {@link HostSpec} representing an available Cluster. */
  private static final HostSpec AVAILABLE_CLUSTER_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("cluster", getClusterUrl("cluster"), HostAvailability.AVAILABLE));

  /** {@link HostSpec} representing an available Writer node. */
  private static final HostSpec AVAILABLE_WRITER_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("writer", getInstanceUrl("writer"), HostAvailability.AVAILABLE));

  /** {@link HostSpec} representing an available Reader node #1. */
  private static final HostSpec AVAILABLE_READER1_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("reader1", getInstanceUrl("reader1"), HostAvailability.AVAILABLE));

  /** {@link HostSpec} representing an available Reader node #2. */
  private static final HostSpec AVAILABLE_READER2_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec("reader2", getInstanceUrl("reader2"), HostAvailability.AVAILABLE));

  /** {@link HostSpec} representing an available Reader node #1. */
  private static final HostSpec NOT_AVAILABLE_READER1_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec(
              "reader1", getInstanceUrl("reader1"), HostAvailability.NOT_AVAILABLE));

  /** {@link HostSpec} representing an available Reader node #2. */
  private static final HostSpec NOT_AVAILABLE_READER2_HOST_SPEC =
      PartialHostSpec.toHostSpec(
          new PartialHostSpec(
              "reader2", getInstanceUrl("reader2"), HostAvailability.NOT_AVAILABLE));

  /**
   * Mock used to mock the {@code connectFunc} or {@code forceConnectFunc} in {@link
   * SingleNodeHostOverrideConnectionPlugin#connect(String, HostSpec, Properties, boolean,
   * JdbcCallable)} and {@link SingleNodeHostOverrideConnectionPlugin#forceConnect(String, HostSpec,
   * Properties, boolean, JdbcCallable)}, respectively.
   *
   * @implNote @Mock is used instead of mock() to support generics
   */
  @Mock private JdbcCallable<Connection, SQLException> mockConnectFunc;

  /**
   * Mock used to mock the {@code initHostProviderFunc} for {@link
   * SingleNodeHostOverrideConnectionPlugin#initHostProvider(String, String, Properties,
   * HostListProviderService, JdbcCallable)}.
   *
   * @implNote @Mock is used instead of mock() to support generics
   */
  @Mock private JdbcCallable<Void, SQLException> mockInitHostProviderFunc;

  /**
   * Test case ensuring that the {@link SingleNodeHostOverrideConnectionPlugin} is subscribed to the
   * appropriate plugin methods.
   */
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

  /**
   * Parameterized test case for {@link SingleNodeHostOverrideConnectionPlugin#connect(String,
   * HostSpec, Properties, boolean, JdbcCallable)} testing various combinations of connection
   * specification and host lists, verifying that the plugin does not modify the connection unless
   * there is a single, valid {@link HostSpec} available in the topology and the connection {@code
   * hostSpec} is to a Cluster.
   *
   * @param testCaseName name of the parameterized test case
   * @param connectionHostSpec unmodified {@link HostSpec} that would be used for connecting
   *     otherwise
   * @param mockAllHosts a point-in-time mock {@link List} of {@link HostSpec}s representing the
   *     hosts in the topology
   * @param jdbcCallableCallInvocations number of invocations of {@link #mockConnectFunc}'s {@link
   *     JdbcCallable#call()} method; calls to this method indicate that the connection was
   *     unmodified
   * @param pluginServiceConnectInvocations number of invocations of the {@link
   *     PluginService#connect(HostSpec, Properties)} method; calls to this method indicate that the
   *     connection was overridden to connect directly to the single host in the topology
   * @param expectedPluginServiceConnectHostSpec expected overridden, direct-to-Instance {@link
   *     HostSpec} if {@link PluginService#connect(HostSpec, Properties)} should be called
   * @throws SQLException added to satisfy compiler; never thrown
   * @implNote {@link SingleNodeHostOverrideConnectionPlugin#connect(String, HostSpec, Properties,
   *     boolean, JdbcCallable)} delegates to the {@link
   *     SingleNodeHostOverrideConnectionPlugin#connectInternal(HostSpec, Properties, boolean,
   *     JdbcCallable)} method, so this test is the same as {@link #testForceConnect(String,
   *     HostSpec, List, int, int, HostSpec)}
   */
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

  /**
   * Parameterized test case for {@link SingleNodeHostOverrideConnectionPlugin#forceConnect(String,
   * HostSpec, Properties, boolean, JdbcCallable)} testing various combinations of connection
   * specification and host lists, verifying that the plugin does not modify the connection unless
   * there is a single, valid {@link HostSpec} available in the topology and the connection {@code
   * hostSpec} is to a Cluster.
   *
   * @param testCaseName name of the parameterized test case
   * @param connectionHostSpec unmodified {@link HostSpec} that would be used for connecting
   *     otherwise
   * @param mockAllHosts a point-in-time mock {@link List} of {@link HostSpec}s representing the
   *     hosts in the topology
   * @param jdbcCallableCallInvocations number of invocations of {@link #mockConnectFunc}'s {@link
   *     JdbcCallable#call()} method; calls to this method indicate that the connection was
   *     unmodified
   * @param pluginServiceConnectInvocations number of invocations of the {@link
   *     PluginService#connect(HostSpec, Properties)} method; calls to this method indicate that the
   *     connection was overridden to connect directly to the single host in the topology
   * @param expectedPluginServiceConnectHostSpec expected overridden, direct-to-Instance {@link
   *     HostSpec} if {@link PluginService#connect(HostSpec, Properties)} should be called
   * @throws SQLException added to satisfy compiler; never thrown
   * @implNote {@link SingleNodeHostOverrideConnectionPlugin#connect(String, HostSpec, Properties,
   *     boolean, JdbcCallable)} delegates to the {@link
   *     SingleNodeHostOverrideConnectionPlugin#connectInternal(HostSpec, Properties, boolean,
   *     JdbcCallable)} method, so this test is the same as {@link #testConnect(String, HostSpec,
   *     List, int, int, HostSpec)}
   */
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

  /**
   * Test that ensures that {@link SingleNodeHostOverrideConnectionPlugin#initHostProvider(String,
   * String, Properties, HostListProviderService, JdbcCallable)} calls the {@link
   * #mockInitHostProviderFunc}'s {@link JdbcCallable#call()} method if the provided {@link
   * HostListProviderService} is dynamic.
   *
   * @throws SQLException added to satisfy compiler; never thrown
   */
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

  /**
   * Test that ensures that {@link SingleNodeHostOverrideConnectionPlugin#initHostProvider(String,
   * String, Properties, HostListProviderService, JdbcCallable)} throws a {@link SQLException} if
   * the provided {@link HostListProviderService} is static.
   *
   * @throws SQLException added to satisfy compiler; never thrown
   */
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

  /**
   * {@link Arguments} provider for the {@link #testConnect(String, HostSpec, List, int, int,
   * HostSpec)} and {@link #testForceConnect(String, HostSpec, List, int, int, HostSpec)}
   * parameterized test's test cases.
   *
   * @return a {@link Stream} of {@link Arguments}, each testing a different scenario with {@link
   *     SingleNodeHostOverrideConnectionPlugin#connect(String, HostSpec, Properties, boolean,
   *     JdbcCallable)} or {@link SingleNodeHostOverrideConnectionPlugin#forceConnect(String,
   *     HostSpec, Properties, boolean, JdbcCallable)}.
   */
  private static Stream<Arguments> provideAllTestConnectsParameters() {
    return Stream.of(
        Arguments.of(
            "testConnectOverridesToSingleWriterNodeEndpointWhenOnlyWriterNodeInTopology",
            AVAILABLE_CLUSTER_HOST_SPEC,
            List.of(AVAILABLE_WRITER_HOST_SPEC),
            0,
            1,
            AVAILABLE_WRITER_HOST_SPEC),
        Arguments.of(
            "testConnectOverridesToSingleWriterNodeEndpointWhenOnlyWriterNodeAvailable",
            AVAILABLE_CLUSTER_HOST_SPEC,
            List.of(
                AVAILABLE_WRITER_HOST_SPEC,
                NOT_AVAILABLE_READER1_HOST_SPEC,
                NOT_AVAILABLE_READER2_HOST_SPEC),
            0,
            1,
            AVAILABLE_WRITER_HOST_SPEC),
        Arguments.of(
            "testConnectUsesInstanceEndpointWhenInstanceEndpointIsUsed",
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

  /**
   * Creates a mock RDS Cluster URL from a Cluster ID.
   *
   * @param hostId the Cluster ID
   * @return a mock Cluster URL
   */
  private static String getClusterUrl(String hostId) {
    return String.format("%s.cluster-uniqueid.us-east-1.rds.amazonaws.com", hostId);
  }

  /**
   * Creates a mock RDS Instance URL from an Instance ID.
   *
   * @param hostId the Instance ID
   * @return a mock Instance URL
   */
  private static String getInstanceUrl(String hostId) {
    return String.format("%s.uniqueid.us-east-1.rds.amazonaws.com", hostId);
  }

  /**
   * Record class representing the relevant parts of a {@link HostSpec} to simplify the creation of
   * one.
   *
   * @param hostId the Host ID
   * @param hostUrl the URL of the Host
   * @param hostAvailability the {@link HostAvailability} of the Host
   */
  private record PartialHostSpec(String hostId, String hostUrl, HostAvailability hostAvailability) {
    /**
     * Transforms a {@link PartialHostSpec} into a full {@link HostSpec} with a mock {@link
     * HostAvailabilityStrategy} that always returns {@link #hostAvailability}.
     *
     * @param from the {@link PartialHostSpec} to transform into a {@link HostSpec}
     * @return the {@link HostSpec} from {@code from}
     */
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
