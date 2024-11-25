package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.HostSpecBuilder;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.hostavailability.HostAvailability;
import software.amazon.jdbc.hostavailability.HostAvailabilityStrategy;
import software.amazon.jdbc.util.CacheMap;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StateAwareClusterTopologyMonitorTest {

  public static final String RDS_INSTANCE_STATE_AVAILABLE = "available";
  public static final String RDS_INSTANCE_STATE_CREATING = "creating";
  public static final String RDS_INSTANCE_STATE_CONFIGURING_LOG_EXPORTS = "configuring-log-exports";
  public static final String RDS_INSTANCE_STATE_CONFIGURING_ENHANCED_MONITORING =
      "configuring-enhanced-monitoring";
  public static final String MOCK_CLUSTER_ID = "test";
  private MockedStatic<Executors> mockedStaticExecutors;
  private final ExecutorService mockTopologyMonitorExecutor = mock(ExecutorService.class);
  private final ScheduledExecutorService mockInstanceStateMonitorExecutor =
      mock(ScheduledExecutorService.class);

  @BeforeEach
  void setUp() {
    mockedStaticExecutors = mockStatic(Executors.class);
    mockedStaticExecutors
        .when(() -> Executors.newSingleThreadExecutor(any()))
        .thenReturn(mockTopologyMonitorExecutor);
    mockedStaticExecutors
        .when(Executors::newSingleThreadScheduledExecutor)
        .thenReturn(mockInstanceStateMonitorExecutor);
  }

  @AfterEach
  void tearDown() {
    mockedStaticExecutors.close();
  }

  @Test
  void testItStartsInstanceStateMonitorWhenInitialized() throws Exception {
    // Arrange
    final var refreshRateMs = 1000L;

    // Act
    try (var ignored = createMonitor(new ConcurrentHashMap<>(), refreshRateMs)) {
      // Assert
      verify(mockInstanceStateMonitorExecutor, times(1))
          .scheduleAtFixedRate(
              any(StateAwareClusterTopologyMonitor.InstanceStateMonitor.class),
              eq(0L),
              eq(refreshRateMs),
              eq(TimeUnit.MILLISECONDS));
    }
  }

  @Test
  void testItShutsDownInstanceStateMonitorWhenClosed() throws Exception {
    // Arrange
    final var monitor = spy(createMonitor(new ConcurrentHashMap<>()));
    doNothing().when(monitor).shutdownInstanceMonitorExecutor();

    // Act
    monitor.close();

    // Assert
    verify(monitor, times(1)).shutdownInstanceMonitorExecutor();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideQueryForTopologyTestArguments")
  void testQueryForTopology(
      String testCaseName,
      boolean inHighRefreshRateMode,
      Map<String, String> instanceStateMap,
      List<HostSpec> mockFilteredTopology,
      List<HostSpec> expectedTopology)
      throws Exception {
    // Arrange
    final var mockInstancesInCluster = createClusterInstanceStateMap(instanceStateMap);
    try (final var monitor = spy(createMonitor(mockInstancesInCluster))) {
      doReturn(mockFilteredTopology).when(monitor).getUnfilteredTopology(any(Connection.class));
      doReturn(inHighRefreshRateMode).when(monitor).isInHighRefreshRateMode();

      // Act
      final var actualTopology = monitor.queryForTopology(mock(Connection.class));

      // Assert
      assertEquals(expectedTopology, actualTopology);
    }
  }

  private static Stream<Arguments> provideQueryForTopologyTestArguments() {
    return Stream.of(
        Arguments.of(
            "testQueryForTopologyReturnsAllNodesWhenAllNodesAreAvailable",
            false,
            Map.of(
                "writer",
                RDS_INSTANCE_STATE_AVAILABLE,
                "reader1",
                RDS_INSTANCE_STATE_AVAILABLE,
                "reader2",
                RDS_INSTANCE_STATE_AVAILABLE),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2"))),
        Arguments.of(
            "testQueryForTopologyReturnsAllNodesWhenAllNodesAreReady",
            false,
            Map.of(
                "writer",
                RDS_INSTANCE_STATE_AVAILABLE,
                "reader1",
                RDS_INSTANCE_STATE_CONFIGURING_LOG_EXPORTS,
                "reader2",
                RDS_INSTANCE_STATE_CONFIGURING_ENHANCED_MONITORING),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2"))),
        Arguments.of(
            "testQueryForTopologyReturnsOnlyAvailableNodesWhenOneNodeIsCreating",
            false,
            Map.of(
                "writer",
                RDS_INSTANCE_STATE_AVAILABLE,
                "reader1",
                RDS_INSTANCE_STATE_CREATING,
                "reader2",
                RDS_INSTANCE_STATE_AVAILABLE),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer", "reader2"))),
        Arguments.of(
            "testQueryForTopologyReturnsOnlyReadyWriterNodeWhenAllReadersAreCreating",
            false,
            Map.of(
                "writer",
                RDS_INSTANCE_STATE_AVAILABLE,
                "reader1",
                RDS_INSTANCE_STATE_CREATING,
                "reader2",
                RDS_INSTANCE_STATE_CREATING),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer"))),
        Arguments.of(
            "testQueryForTopologyReturnsOnlyNodesWithStateWhenTopologyDiffersFromInstanceStateMap",
            false,
            Map.of("writer", RDS_INSTANCE_STATE_AVAILABLE, "reader2", RDS_INSTANCE_STATE_AVAILABLE),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer", "reader2"))),
        Arguments.of(
            "testQueryForTopologyReturnsAllNodesWhenClusterHasNoInstanceStateMap",
            false,
            null,
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2"))),
        Arguments.of(
            "testQueryForTopologyReturnsAllNodesWhenInstanceStateMapIsEmpty",
            false,
            Map.of(),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2"))),
        Arguments.of(
            "testQueryForTopologyReturnsAllNodesWhenInHighRefreshRateMode",
            true,
            Map.of("writer", RDS_INSTANCE_STATE_AVAILABLE, "reader1", RDS_INSTANCE_STATE_CREATING),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2")),
            getHostSpecsFromInstanceIds(List.of("writer", "reader1", "reader2"))));
  }

  private static StateAwareClusterTopologyMonitor createMonitor(
      ConcurrentHashMap<String, Map<String, String>> instanceStateMap) {
    return createMonitor(instanceStateMap, 0L);
  }

  private static StateAwareClusterTopologyMonitor createMonitor(
      ConcurrentHashMap<String, Map<String, String>> instanceStateMap,
      long instanceStateRefreshRateMs) {
    return new StateAwareClusterTopologyMonitor(
        MOCK_CLUSTER_ID,
        new CacheMap<>(),
        instanceStateMap,
        mock(HostSpec.class),
        mock(Properties.class),
        mock(PluginService.class),
        mock(HostListProviderService.class),
        mock(HostSpec.class),
        0L,
        0L,
        0L,
        "",
        "",
        "",
        instanceStateRefreshRateMs,
        mock(RdsClient.class));
  }

  private static ConcurrentHashMap<String, Map<String, String>> createClusterInstanceStateMap(
      @Nullable Map<String, String> instanceStateMap) {
    final ConcurrentHashMap<String, Map<String, String>> mockMap = new ConcurrentHashMap<>();
    if (instanceStateMap != null) {
      mockMap.put(MOCK_CLUSTER_ID, instanceStateMap);
    }
    return mockMap;
  }

  private static List<HostSpec> getHostSpecsFromInstanceIds(Collection<String> instanceIds) {
    return instanceIds.stream()
        .map(
            instanceId ->
                new HostSpecBuilder(mock(HostAvailabilityStrategy.class))
                    .hostId(instanceId)
                    .host(instanceId)
                    .availability(HostAvailability.AVAILABLE)
                    .build())
        .toList();
  }

  private static List<String> getInstanceIdsFromHostSpecs(Collection<HostSpec> hostSpecs) {
    return hostSpecs.stream().map(HostSpec::getHostId).toList();
  }
}
