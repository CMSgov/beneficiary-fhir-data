package gov.cms.bfd.sharedutils.database;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.Filter;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.dialect.Dialect;
import software.amazon.jdbc.ds.AwsWrapperDataSource;
import software.amazon.jdbc.hostlistprovider.monitoring.ClusterTopologyMonitor;
import software.amazon.jdbc.hostlistprovider.monitoring.ClusterTopologyMonitorImpl;
import software.amazon.jdbc.util.RdsUtils;
import software.amazon.jdbc.util.storage.CacheMap;

/**
 * "State aware" {@link ClusterTopologyMonitor} that extends the default {@link
 * ClusterTopologyMonitorImpl} modifying {@link #queryForTopology(Connection)} to return a list of
 * hosts filtered by their API status.
 */
public class StateAwareClusterTopologyMonitor extends ClusterTopologyMonitorImpl {
  /**
   * Map {@link #clusterId}s to immutable map of instance ID to the status of the instance returned
   * by the RDS API. Periodically updated by the {@link InstanceStateMonitor} every {@link
   * StateAwareMonitoringRdsHostListProvider#instanceStateMonitorRefreshRateMs} millisecond(s).
   */
  private final ConcurrentMap<String, Map<String, String>> clusterToHostsStateMap;

  /** Executor for the periodic {@link InstanceStateMonitor} task. */
  private final ScheduledExecutorService instanceStateMonitorExecutor =
      Executors.newSingleThreadScheduledExecutor();

  /**
   * Used by the {@link InstanceStateMonitor} to retrieve the status of RDS instances in a given
   * cluster.
   */
  private final RdsClient rdsClient;

  /**
   * Creates an instance of {@link StateAwareClusterTopologyMonitor}.
   *
   * @param clusterId the cluster identifier of the current RDS cluster
   * @param topologyMap the map of cluster to its topology
   * @param clusterToHostsStateMap the map of cluster IDs to an immutable {@link Map} of instance
   *     IDs to their status
   * @param initialHostSpec the initial host to connect to (cluster URL)
   * @param properties the {@link AwsWrapperDataSource} properties
   * @param pluginService the {@link PluginService} as provided by the {@link Dialect}
   * @param hostListProviderService the {@link HostListProviderService} as determined by the {@link
   *     Dialect}
   * @param clusterInstanceTemplate host template used to generate {@link HostSpec}s from the
   *     topology
   * @param refreshRateNano the normal refresh rate at which to update the topology cache, in
   *     nanoseconds
   * @param highRefreshRateNano the high refresh rate, after failover, at which to update the
   *     topology cache, in nanoseconds
   * @param topologyCacheExpirationNano the duration of time, in nanoseconds, before the topology
   *     cache expires
   * @param topologyQuery the SQL query that returns the cluster topology
   * @param writerTopologyQuery the SQL query that returns whether the current database is a writer
   * @param nodeIdQuery the SQL query that returns the instance identifier of the database that is
   *     queried
   * @param instanceStateMonitorRefreshRateMs the rate, in milliseconds, at which the {@link
   *     InstanceStateMonitor} queries the RDS API for instance status
   * @param rdsClient {@link RdsClient} used to check instance status
   */
  public StateAwareClusterTopologyMonitor(
      String clusterId,
      CacheMap<String, List<HostSpec>> topologyMap,
      ConcurrentMap<String, Map<String, String>> clusterToHostsStateMap,
      HostSpec initialHostSpec,
      Properties properties,
      PluginService pluginService,
      HostListProviderService hostListProviderService,
      HostSpec clusterInstanceTemplate,
      long refreshRateNano,
      long highRefreshRateNano,
      long topologyCacheExpirationNano,
      String topologyQuery,
      String writerTopologyQuery,
      String nodeIdQuery,
      long instanceStateMonitorRefreshRateMs,
      RdsClient rdsClient) {
    super(
        clusterId,
        topologyMap,
        initialHostSpec,
        properties,
        pluginService,
        hostListProviderService,
        clusterInstanceTemplate,
        refreshRateNano,
        highRefreshRateNano,
        topologyCacheExpirationNano,
        topologyQuery,
        writerTopologyQuery,
        nodeIdQuery);
    this.clusterToHostsStateMap = clusterToHostsStateMap;
    this.rdsClient = rdsClient;

    instanceStateMonitorExecutor.scheduleAtFixedRate(
        new InstanceStateMonitor(this),
        0,
        instanceStateMonitorRefreshRateMs,
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void close() throws Exception {
    shutdownInstanceMonitorExecutor();

    super.close();
  }

  @Override
  @VisibleForTesting
  protected List<HostSpec> queryForTopology(Connection conn) throws SQLException {
    final var unfilteredTopology = getUnfilteredTopology(conn);

    if (isInHighRefreshRateMode()
        || !clusterToHostsStateMap.containsKey(clusterId)
        || clusterToHostsStateMap.get(clusterId).isEmpty()) {
      // High refresh rate occurs only after failover, so the worst case has already happened and
      // there is no reason to restrict the hosts that can be connected to based on status. Or, the
      // hosts map is empty, which indicates that this may be the first query for topology.
      return unfilteredTopology;
    }

    // This ensures that unready instances are not connected to early during scale-out. Other states
    // (like "deleting") are not considered as failover should handle them
    final var currentClusterInstanceStateMap = clusterToHostsStateMap.get(clusterId);
    return unfilteredTopology.stream()
        .filter(hostSpec -> currentClusterInstanceStateMap.containsKey(hostSpec.getHostId()))
        .filter(
            hostSpec ->
                !currentClusterInstanceStateMap
                    .get(hostSpec.getHostId())
                    .equalsIgnoreCase("creating"))
        .toList();
  }

  /**
   * Attempt to shut down the {@link #instanceStateMonitorExecutor}.
   *
   * @implNote This method was extracted out of {@link #close()} to enable mocking in unit tests
   */
  @VisibleForTesting
  protected void shutdownInstanceMonitorExecutor() throws InterruptedException {
    instanceStateMonitorExecutor.shutdown();
    try {
      if (!instanceStateMonitorExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
        instanceStateMonitorExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      instanceStateMonitorExecutor.shutdownNow();
      throw e;
    }
  }

  /**
   * Returns whether the monitor is in "high refresh rate" mode where the topology is queried from
   * the database at a much faster rate (by default, every 100ms).
   *
   * @return {@code true} if in "high refresh rate" mode, {@code false} otherwise
   */
  @VisibleForTesting
  protected boolean isInHighRefreshRateMode() {
    // Taken from logic to determine if the topologyMonitor is in high refresh rate mode in delay()
    return highRefreshRateEndTimeNano > 0 && System.nanoTime() < highRefreshRateEndTimeNano;
  }

  /**
   * Queries the database for the RDS Cluster topology.
   *
   * @param conn {@link Connection} to the current database
   * @return an unfiltered {@link List} of {@link HostSpec}s representing the instances in the RDS
   *     Cluster as reported by the database topology query
   * @throws SQLException if there is an issue querying the database
   * @implNote The call to the super's {@link #queryForTopology(Connection)} has been extracted to
   *     this method so that it is possible to mock the super's {@link
   *     #queryForTopology(Connection)} while being able to independently test the filtering logic
   *     in the overridden variant
   */
  @VisibleForTesting
  protected List<HostSpec> getUnfilteredTopology(Connection conn) throws SQLException {
    return super.queryForTopology(conn);
  }

  /**
   * {@link Runnable} task started in a scheduled {@link Thread} upon initialization of a {@link
   * StateAwareClusterTopologyMonitor} that will independently monitor the RDS API status of RDS
   * instances in the current {@link #clusterId} cluster and concurrently update the {@link
   * StateAwareClusterTopologyMonitor#clusterToHostsStateMap} every {@link
   * StateAwareMonitoringRdsHostListProvider#instanceStateMonitorRefreshRateMs} millisecond(s).
   *
   * @param topologyMonitor {@link StateAwareClusterTopologyMonitor} that spawned this task.
   */
  @VisibleForTesting
  record InstanceStateMonitor(StateAwareClusterTopologyMonitor topologyMonitor)
      implements Runnable {

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceStateMonitor.class);

    /**
     * Used to extract RDS details like Cluster or Instance ID from the hostname given by {@link
     * StateAwareClusterTopologyMonitor#clusterId}.
     *
     * @implNote Although the name of the field {@link StateAwareClusterTopologyMonitor#clusterId}
     *     implies that it is the ID of the Cluster, it is actually the host provided in the
     *     original Server database connection string.
     */
    private static final RdsUtils RDS_UTILS = new RdsUtils();

    @Override
    public void run() {
      try {
        final var dbInstancesDetails =
            topologyMonitor.rdsClient.describeDBInstances(
                request -> {
                  final var instanceOrClusterFilter =
                      getDescribeDbInstancesFilterFromUrl(topologyMonitor.clusterId);
                  if (instanceOrClusterFilter != null) {
                    request.filters(instanceOrClusterFilter);
                  }
                });

        final var dbInstancesStateMap =
            dbInstancesDetails.dbInstances().stream()
                .filter(
                    dbInstance ->
                        dbInstance.dbInstanceIdentifier() != null
                            && dbInstance.dbInstanceStatus() != null)
                .collect(
                    Collectors.toUnmodifiableMap(
                        DBInstance::dbInstanceIdentifier, DBInstance::dbInstanceStatus));
        if (dbInstancesStateMap.isEmpty()) {
          LOGGER.error(
              "DescribeDBInstances returned no valid results when monitoring instance state");
          return;
        }

        topologyMonitor.clusterToHostsStateMap.put(topologyMonitor.clusterId, dbInstancesStateMap);
      } catch (AwsServiceException e) {
        LOGGER.error("Unable to retrieve instance state of current RDS cluster from RDS API", e);
      }
    }

    /**
     * Returns a {@code db-cluster-id} or {@code db-instance-id} {@link Filter} depending on what
     * type of host URL the {@code rdsUrl} is. If the {@code rdsUrl} does not match an Instance or
     * Cluster URL, then no {@link Filter} is returned.
     *
     * @param rdsUrl the RDS URL to parse for the Cluster or Instance ID for the {@link Filter}
     * @return a {@link Filter} filtering for {@code db-cluster-id} or {@code db-instance-id} if
     *     applicable, {@code null} otherwise
     */
    @VisibleForTesting
    static Filter getDescribeDbInstancesFilterFromUrl(String rdsUrl) {
      final var rdsUrlType = RDS_UTILS.identifyRdsType(rdsUrl);
      switch (rdsUrlType) {
        case RDS_READER_CLUSTER, RDS_WRITER_CLUSTER -> {
          final var trueClusterId = RDS_UTILS.getRdsClusterId(rdsUrl);
          return Filter.builder().name("db-cluster-id").values(trueClusterId).build();
        }
        case RDS_INSTANCE -> {
          final var trueInstanceId = RDS_UTILS.getRdsInstanceId(rdsUrl);
          return Filter.builder().name("db-instance-id").values(trueInstanceId).build();
        }
        default -> {
          return null;
        }
      }
    }
  }
}
