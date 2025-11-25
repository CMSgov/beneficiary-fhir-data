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
import jakarta.annotation.Nullable;
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
import software.amazon.jdbc.util.RdsUrlType;
import software.amazon.jdbc.util.storage.CacheMap;

/** Unit tests for {@link StateAwareClusterTopologyMonitor}. */
class StateAwareClusterTopologyMonitorTest {

  /** String that is returned by the RDS API for an Instance that is available to query. */
  private static final String RDS_INSTANCE_STATE_AVAILABLE = "available";

  /**
   * String that is returned by the RDS API for an Instance that is not yet ready and is still being
   * initialized.
   */
  private static final String RDS_INSTANCE_STATE_CREATING = "creating";

  /**
   * String that is returned by the RDS API for an Instance that is ready but has not finished
   * configuring its CloudWatch log exporting.
   */
  private static final String RDS_INSTANCE_STATE_CONFIGURING_LOG_EXPORTS =
      "configuring-log-exports";

  /**
   * String that is returned by the RDS API for an Instance that is ready but has not finished
   * configuring its Enhanced Monitoring.
   */
  private static final String RDS_INSTANCE_STATE_CONFIGURING_ENHANCED_MONITORING =
      "configuring-enhanced-monitoring";

  /** Default name for the mock RDS Cluster used in tests. */
  private static final String DEFAULT_MOCK_CLUSTER_ID = "test";

  /** Static mock used for {@link Executors} methods. */
  private MockedStatic<Executors> mockedStaticExecutors;

  /** Mock for {@link ExecutorService} used by {@link #mockedStaticExecutors}. */
  private final ExecutorService mockTopologyMonitorExecutor = mock(ExecutorService.class);

  /** Mock for {@link ScheduledExecutorService} used by {@link #mockedStaticExecutors}. */
  private final ScheduledExecutorService mockInstanceStateMonitorExecutor =
      mock(ScheduledExecutorService.class);

  /** Sets up the {@link Executors} mocks before each test. */
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

  /** Closes the static {@link #mockedStaticExecutors} {@link Executors} mock after each test. */
  @AfterEach
  void tearDown() {
    mockedStaticExecutors.close();
  }

  /**
   * Test ensuring that the {@link StateAwareClusterTopologyMonitor} will start the {@link
   * InstanceStateMonitor} as a scheduled {@link ScheduledExecutorService} task when the monitor is
   * created.
   *
   * @throws Exception necessary to satisfy compiler, will never throw
   */
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

  /**
   * Test ensuring that {@link StateAwareClusterTopologyMonitor#close()} will also shut down the
   * {@link InstanceStateMonitor} {@link ScheduledExecutorService}.
   *
   * @throws Exception necessary to satisfy compiler, will never throw
   */
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

  /**
   * Parameterized test for {@link StateAwareClusterTopologyMonitor#queryForTopology(Connection)}
   * testing various combinations of topologies and instance states to ensure that the {@link
   * StateAwareClusterTopologyMonitor} updates the topology map according to Instance state
   * appropriately.
   *
   * @param testCaseName the name of the parameterized test case
   * @param inHighRefreshRateMode flag indicating whether the {@link
   *     StateAwareClusterTopologyMonitor} should be in "high refresh rate mode" or not
   * @param instanceStateMap point-in-time snapshot of map of Instances to their state that will be
   *     used as the filter for the topology
   * @param mockUnfilteredTopology point-in-time snapshot of an unfiltered topology map that will be
   *     filtered based on {@code instanceStateMap}
   * @param expectedTopology expected, filtered topology assuming that {@link
   *     StateAwareClusterTopologyMonitor#queryForTopology(Connection)} properly filters the {@code
   *     mockUnfilteredTopology} using {@code instanceStateMap}
   * @throws Exception necessary to satisfy compiler, will never throw
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("provideTestQueryForTopologyArguments")
  void testQueryForTopology(
      String testCaseName,
      boolean inHighRefreshRateMode,
      Map<String, String> instanceStateMap,
      List<HostSpec> mockUnfilteredTopology,
      List<HostSpec> expectedTopology)
      throws Exception {
    // Arrange
    final var mockInstancesInCluster = createClusterInstanceStateMap(instanceStateMap);
    try (final var monitor = spy(createMonitor(mockInstancesInCluster))) {
      doReturn(mockUnfilteredTopology).when(monitor).getUnfilteredTopology(any(Connection.class));
      doReturn(inHighRefreshRateMode).when(monitor).isInHighRefreshRateMode();

      // Act
      final var actualTopology = monitor.queryForTopology(mock(Connection.class));

      // Assert
      assertEquals(expectedTopology, actualTopology);
    }
  }

  /**
   * {@link Arguments} provider for the {@link #testQueryForTopology(String, boolean, Map, List,
   * List)} parameterized test's test cases.
   *
   * @return a {@link Stream} of {@link Arguments}, each testing a different scenario with {@link
   *     StateAwareClusterTopologyMonitor#queryForTopology(Connection)}
   */
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

  /**
   * Creates a {@link StateAwareClusterTopologyMonitor} with various mocked parameters.
   *
   * @param instanceStateMap point-in-time snapshot of map of Instances to their state that will be
   *     used as the filter for the topology
   * @return a stubbed {@link StateAwareClusterTopologyMonitor}
   */
  private static StateAwareClusterTopologyMonitor createMonitor(
      ConcurrentHashMap<String, Map<String, String>> instanceStateMap) {
    return createMonitor(instanceStateMap, 0L);
  }

  /**
   * Creates a {@link StateAwareClusterTopologyMonitor} with various mocked parameters.
   *
   * @param instanceStateMap point-in-time snapshot of map of Instances to their state that will be
   *     used as the filter for the topology
   * @param instanceStateRefreshRateMs refresh rate of the {@link InstanceStateMonitor} {@link
   *     ScheduledExecutorService}
   * @return a stubbed {@link StateAwareClusterTopologyMonitor}
   */
  private static StateAwareClusterTopologyMonitor createMonitor(
      ConcurrentHashMap<String, Map<String, String>> instanceStateMap,
      long instanceStateRefreshRateMs) {
    return createMonitor(instanceStateMap, instanceStateRefreshRateMs, mock(RdsClient.class));
  }

  /**
   * Creates a {@link StateAwareClusterTopologyMonitor} with various mocked parameters.
   *
   * @param instanceStateMap point-in-time snapshot of map of Instances to their state that will be
   *     used as the filter for the topology
   * @param mockRdsClient mocked {@link RdsClient}
   * @return a stubbed {@link StateAwareClusterTopologyMonitor}
   */
  private static StateAwareClusterTopologyMonitor createMonitor(
      ConcurrentHashMap<String, Map<String, String>> instanceStateMap, RdsClient mockRdsClient) {
    return createMonitor(instanceStateMap, 0L, mockRdsClient);
  }

  /**
   * Creates a {@link StateAwareClusterTopologyMonitor} with various mocked parameters.
   *
   * @param instanceStateMap point-in-time snapshot of map of Instances to their state that will be
   *     used as the filter for the topology
   * @param instanceStateRefreshRateMs refresh rate of the {@link InstanceStateMonitor} {@link
   *     ScheduledExecutorService}
   * @param mockRdsClient mocked {@link RdsClient}
   * @return a stubbed {@link StateAwareClusterTopologyMonitor}
   */
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

  /**
   * Creates an empty Cluster instance state map.
   *
   * @return an empty map
   */
  private static ConcurrentHashMap<String, Map<String, String>> createClusterInstanceStateMap() {
    return createClusterInstanceStateMap(null);
  }

  /**
   * Creates a Cluster instance state map with a single Cluster entry with {@link
   * #DEFAULT_MOCK_CLUSTER_ID} as the key and {@code instanceStateMap} as the value, if {@code
   * instanceStateMap} is not {@code null}.
   *
   * @param instanceStateMap point-in-time snapshot of map of Instances to their state that will be
   *     used as the filter for the topology
   * @return map with a single Cluster entry, or an empty map if {@code instanceStateMap} is {@code
   *     null}
   */
  private static ConcurrentHashMap<String, Map<String, String>> createClusterInstanceStateMap(
      @Nullable Map<String, String> instanceStateMap) {
    final ConcurrentHashMap<String, Map<String, String>> mockMap = new ConcurrentHashMap<>();
    if (instanceStateMap != null) {
      mockMap.put(DEFAULT_MOCK_CLUSTER_ID, instanceStateMap);
    }
    return mockMap;
  }

  /**
   * Creates a {@link List} of {@link HostSpec}s given a {@link List} of Instance IDs.
   *
   * @param instanceIds {@code List} of Instance IDs to transform
   * @return a {@link List} of {@link HostSpec}s generated from the {@link List} of Instance IDs
   */
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

  /** Unit tests for {@link InstanceStateMonitor}. */
  @Nested
  class InstanceStateMonitorTest {
    /**
     * Parameterized test for {@link InstanceStateMonitor#run()} testing various RDS Instance state
     * responses ensuring that only valid responses are put into the {@link
     * StateAwareClusterTopologyMonitor}'s instance state map.
     *
     * @param testCaseName name of the parameterized test case
     * @param mockRdsClientInstanceStateResponseMap mock response from {@link
     *     RdsClient#describeDBInstances()}
     * @param expectedInstanceStateMap expected instance state map for the {@link
     *     #DEFAULT_MOCK_CLUSTER_ID} Cluster
     * @throws Exception necessary to satisfy compiler, will never throw
     */
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

    /**
     * Tests that {@link InstanceStateMonitor#run()} does not modify the {@link
     * StateAwareClusterTopologyMonitor} instance state map when an {@link AwsServiceException} is
     * thrown by {@link RdsClient#describeDBInstances()}.
     *
     * @throws Exception necessary to satisfy compiler, will never throw
     */
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

    /**
     * Parameterized test for {@link
     * InstanceStateMonitor#getDescribeDbInstancesFilterFromUrl(String)} testing that the
     * appropriate {@link Filter} is generated depending on the {@link RdsUrlType} of the given URL.
     *
     * @param testCaseName name of the parameterized test case
     * @param rdsUrl URL to generate a {@link Filter} from
     * @param expectedFilter the {@link Filter} that is expected to be created from the {@code
     *     rdsUrl}
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetDescribeDbInstancesFilterFromUrlArguments")
    void testGetDescribeDbInstancesFilterFromUrl(
        String testCaseName, String rdsUrl, Filter expectedFilter) {
      // Act
      final var actualFilter = InstanceStateMonitor.getDescribeDbInstancesFilterFromUrl(rdsUrl);

      // Assert
      assertEquals(expectedFilter, actualFilter);
    }

    /**
     * {@link Arguments} provider for the {@link #testRun(String, List, Map)} parameterized test's
     * test cases.
     *
     * @return a {@link Stream} of {@link Arguments}, each testing a different scenario with {@link
     *     InstanceStateMonitor#run()}
     */
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

    /**
     * {@link Arguments} provider for the {@link #testGetDescribeDbInstancesFilterFromUrl(String,
     * String, Filter)} parameterized test's test cases.
     *
     * @return a {@link Stream} of {@link Arguments}, each testing a different scenario with {@link
     *     InstanceStateMonitor#getDescribeDbInstancesFilterFromUrl(String)}
     */
    private static Stream<Arguments> provideGetDescribeDbInstancesFilterFromUrlArguments() {
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

    /**
     * Creates a mocked {@link RdsClient} that returns a {@link DescribeDbInstancesResponse} based
     * on the provided {@link InstanceWithState} {@link List}.
     *
     * @param expectedInstancesWithStateResult {@link List} of {@link InstanceWithState}(s) that
     *     will be used to create a corresponding {@link DescribeDbInstancesResponse}
     * @return a {@link DescribeDbInstancesResponse} corresponding to {@code
     *     expectedInstancesWithStateResult}
     */
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

    /**
     * Record class used to generate a {@link DescribeDbInstancesResponse}.
     *
     * @param instanceId Instance ID
     * @param instanceState state of the Instance
     * @implNote this is necessary as using something like {@link Map#of()} would not allow for
     *     setting {@code null} values and keys
     */
    private record InstanceWithState(@Nullable String instanceId, @Nullable String instanceState) {}
  }
}
