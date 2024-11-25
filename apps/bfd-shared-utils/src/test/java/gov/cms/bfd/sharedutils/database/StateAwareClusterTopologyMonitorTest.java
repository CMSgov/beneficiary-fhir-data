package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.cms.bfd.sharedutils.database.StateAwareClusterTopologyMonitor.InstanceStateMonitor;
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
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.Filter;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.HostSpecBuilder;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.hostavailability.HostAvailability;
import software.amazon.jdbc.hostavailability.HostAvailabilityStrategy;
import software.amazon.jdbc.util.CacheMap;

class StateAwareClusterTopologyMonitorTest {

  public static final String RDS_INSTANCE_STATE_AVAILABLE = "available";
  public static final String RDS_INSTANCE_STATE_CREATING = "creating";
  public static final String RDS_INSTANCE_STATE_CONFIGURING_LOG_EXPORTS = "configuring-log-exports";
  public static final String RDS_INSTANCE_STATE_CONFIGURING_ENHANCED_MONITORING =
      "configuring-enhanced-monitoring";
  public static final String DEFAULT_MOCK_CLUSTER_ID = "test";
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
              any(InstanceStateMonitor.class),
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
  @MethodSource("provideTestQueryForTopologyArguments")
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

  private static Stream<Arguments> provideTestQueryForTopologyArguments() {
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
    return createMonitor(instanceStateMap, instanceStateRefreshRateMs, mock(RdsClient.class));
  }

  private static StateAwareClusterTopologyMonitor createMonitor(
      ConcurrentHashMap<String, Map<String, String>> instanceStateMap, RdsClient mockRdsClient) {
    return createMonitor(instanceStateMap, 0L, mockRdsClient);
  }

  private static StateAwareClusterTopologyMonitor createMonitor(
      ConcurrentHashMap<String, Map<String, String>> instanceStateMap,
      long instanceStateRefreshRateMs,
      RdsClient mockRdsClient) {
    return new StateAwareClusterTopologyMonitor(
        DEFAULT_MOCK_CLUSTER_ID,
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
        mockRdsClient);
  }

  private static ConcurrentHashMap<String, Map<String, String>> createClusterInstanceStateMap() {
    return createClusterInstanceStateMap(null);
  }

  private static ConcurrentHashMap<String, Map<String, String>> createClusterInstanceStateMap(
      @Nullable Map<String, String> instanceStateMap) {
    final ConcurrentHashMap<String, Map<String, String>> mockMap = new ConcurrentHashMap<>();
    if (instanceStateMap != null) {
      mockMap.put(DEFAULT_MOCK_CLUSTER_ID, instanceStateMap);
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

  @Nested
  class InstanceStateMonitorTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestRunArguments")
    void testRun(
        String testCaseName,
        List<InstanceWithState> mockRdsClientInstanceStateResponseMap,
        Map<String, String> expectedInstanceStateMap)
        throws Exception {
      // Arrange
      final var clusterInstanceStateMap = createClusterInstanceStateMap();
      final var mockRdsClient =
          createMockRdsClientWithMockedDescribeDBInstances(mockRdsClientInstanceStateResponseMap);
      try (var topologyMonitor = createMonitor(clusterInstanceStateMap, mockRdsClient)) {
        final var instanceMonitor = new InstanceStateMonitor(topologyMonitor);

        // Act
        instanceMonitor.run();

        // Assert
        assertEquals(
            expectedInstanceStateMap, clusterInstanceStateMap.get(DEFAULT_MOCK_CLUSTER_ID));
      }
    }

    @Test
    void testRunShouldPutNothingInClusterStateMapWhenAwsServiceExceptionIsThrown()
        throws Exception {
      // Arrange
      final var clusterInstanceStateMap = createClusterInstanceStateMap();
      final var mockRdsClient = mock(RdsClient.class);
      doThrow(AwsServiceException.class)
          .when(mockRdsClient)
          .describeDBInstances(any(Consumer.class));
      try (var topologyMonitor = createMonitor(clusterInstanceStateMap, mockRdsClient)) {
        final var instanceMonitor = new InstanceStateMonitor(topologyMonitor);

        // Act
        instanceMonitor.run();

        // Assert
        assertFalse(clusterInstanceStateMap.containsKey(DEFAULT_MOCK_CLUSTER_ID));
      }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestApplyFilterToRequestArguments")
    void testApplyFilterToRequest(String testCaseName, String rdsUrl, Filter expectedFilter) {
      // Act
      final var actualFilter = InstanceStateMonitor.getDescribeDbInstancesFilterFromUrl(rdsUrl);

      // Assert
      assertEquals(expectedFilter, actualFilter);
    }

    private static Stream<Arguments> provideTestRunArguments() {
      return Stream.of(
          Arguments.of(
              "testRunShouldPutAllInstanceStatesInClusterStateMapWhenAllInstancesAreAvailable",
              List.of(
                  new InstanceWithState("writer", RDS_INSTANCE_STATE_AVAILABLE),
                  new InstanceWithState("reader1", RDS_INSTANCE_STATE_AVAILABLE),
                  new InstanceWithState("reader2", RDS_INSTANCE_STATE_AVAILABLE)),
              Map.of(
                  "writer",
                  RDS_INSTANCE_STATE_AVAILABLE,
                  "reader1",
                  RDS_INSTANCE_STATE_AVAILABLE,
                  "reader2",
                  RDS_INSTANCE_STATE_AVAILABLE)),
          Arguments.of(
              "testRunShouldPutWriterInstanceStateInClusterStateMapWhenResponseHasInvalidValues",
              List.of(
                  new InstanceWithState("writer", RDS_INSTANCE_STATE_AVAILABLE),
                  new InstanceWithState("reader1", null),
                  new InstanceWithState(null, RDS_INSTANCE_STATE_AVAILABLE),
                  new InstanceWithState(null, null)),
              Map.of("writer", RDS_INSTANCE_STATE_AVAILABLE)),
          Arguments.of(
              "testRunShouldPutNothingInClusterStateMapWhenNoResponseIsReturned", List.of(), null));
    }

    private static Stream<Arguments> provideTestApplyFilterToRequestArguments() {
      return Stream.of(
          Arguments.of(
              "testApplyFilterToRequestReturnsClusterIdFilterWhenClusterWriterEndpointIsGiven",
              String.format(
                  "%s.cluster-uniqueid.us-east-1.rds.amazonaws.com", DEFAULT_MOCK_CLUSTER_ID),
              Filter.builder().name("db-cluster-id").values(DEFAULT_MOCK_CLUSTER_ID).build()),
          Arguments.of(
              "testApplyFilterToRequestReturnsClusterIdFilterWhenClusterReaderEndpointIsGiven",
              String.format(
                  "%s.cluster-ro-uniqueid.us-east-1.rds.amazonaws.com", DEFAULT_MOCK_CLUSTER_ID),
              Filter.builder().name("db-cluster-id").values(DEFAULT_MOCK_CLUSTER_ID).build()),
          Arguments.of(
              "testApplyFilterToRequestReturnsInstanceIdFilterWhenInstanceEndpointIsGiven",
              String.format("%s.uniqueid.us-east-1.rds.amazonaws.com", "writer-node"),
              Filter.builder().name("db-instance-id").values("writer-node").build()),
          Arguments.of(
              "testApplyFilterToRequestReturnsNullWhenInvalidEndpointIsGiven",
              "invalid_endpoint",
              null));
    }

    private static RdsClient createMockRdsClientWithMockedDescribeDBInstances(
        List<InstanceWithState> expectedInstancesWithStateResult) {
      final var mockRdsClient = mock(RdsClient.class);
      when(mockRdsClient.describeDBInstances(any(Consumer.class)))
          .thenReturn(
              DescribeDbInstancesResponse.builder()
                  .dbInstances(
                      expectedInstancesWithStateResult.stream()
                          .map(
                              instanceWithState ->
                                  DBInstance.builder()
                                      .dbInstanceIdentifier(instanceWithState.instanceId())
                                      .dbInstanceStatus(instanceWithState.instanceState())
                                      .build())
                          .toList())
                  .build());
      return mockRdsClient;
    }

    // Necessary as Map.of() disallows null values and keys
    private record InstanceWithState(String instanceId, String instanceState) {}
  }
}
