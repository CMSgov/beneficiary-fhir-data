package gov.cms.bfd.sharedutils.database;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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
import software.amazon.jdbc.util.CacheMap;

/**
 * "State aware" {@link ClusterTopologyMonitor} that extends the default {@link
 * ClusterTopologyMonitorImpl} modifying {@link #queryForTopology(Connection)} to return a list of
 * hosts filtered by their API status.
 */
public class StateAwareClusterTopologyMonitor extends ClusterTopologyMonitorImpl {
  /**
   * Map of instance ID to the status of the instance returned by the RDS API. Periodically updated
   * by the {@link InstanceStateMonitor} every {@link
   * StateAwareMonitoringRdsHostListProvider#instanceStateMonitorRefreshRateMs} millisecond(s).
   */
  private final ConcurrentHashMap<String, Map<String, String>> clusterToHostsStateMap =
      new ConcurrentHashMap<>();

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
    this.rdsClient = rdsClient;

    instanceStateMonitorExecutor.scheduleAtFixedRate(
        new InstanceStateMonitor(this),
        0,
        instanceStateMonitorRefreshRateMs,
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void close() throws Exception {
    instanceStateMonitorExecutor.shutdown();
    try {
      if (!instanceStateMonitorExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
        instanceStateMonitorExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      instanceStateMonitorExecutor.shutdownNow();
    }

    super.close();
  }

  @Override
  protected List<HostSpec> queryForTopology(Connection conn) throws SQLException {
    final var unfilteredTopology = getUnfilteredTopology(conn);

    // Taken from logic to determine if the topologyMonitor is in high refresh rate mode in delay()
    boolean isInHighRefreshRateMode =
        highRefreshRateEndTimeNano > 0 && System.nanoTime() < highRefreshRateEndTimeNano;
    if (isInHighRefreshRateMode || !clusterToHostsStateMap.containsKey(clusterId)) {
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
   * Queries the database for the RDS Cluster topology.
   *
   * @param conn {@link Connection} to the current database
   * @return an unfiltered {@link List} of {@link HostSpec}s representing the instances in the RDS
   *     Cluster as reported by the database topology query
   * @throws SQLException if there is an issue querying the database
   */
  @VisibleForTesting
  private List<HostSpec> getUnfilteredTopology(Connection conn) throws SQLException {
    // The call to the super's queryForTopology has been extracted to this method so that it is
    // possible to mock the super's queryForTopology() while being able to independently test the
    // filtering logic in the overridden variant
    return super.queryForTopology(conn);
  }

  /**
   * {@link Runnable} task started in a scheduled {@link Thread} upon initialization of a {@link
   * StateAwareClusterTopologyMonitor} that will independently topologyMonitor the RDS API status of
   * RDS instances in the current {@link #clusterId} cluster and concurrently update the {@link
   * #clusterToHostsStateMap} every {@link
   * StateAwareMonitoringRdsHostListProvider#instanceStateMonitorRefreshRateMs} millisecond(s).
   *
   * @param topologyMonitor {@link StateAwareClusterTopologyMonitor} that spawned this thread.
   */
  @VisibleForTesting
  private record InstanceStateMonitor(StateAwareClusterTopologyMonitor topologyMonitor)
      implements Runnable {

    /** Logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceStateMonitor.class);

    /**
     * {@link Pattern} that matches the cluster ID part of a typical writer or reader cluster
     * endpoint hostname.
     *
     * @implNote This is used to extract the RDS Cluster ID from {@link
     *     StateAwareClusterTopologyMonitor#clusterId} to properly filter {@link
     *     RdsClient#describeDBInstances()} calls to the current cluster. Although the field's name
     *     implies that it is the ID of the Cluster, it is actually the host provided in the
     *     connection string.
     */
    private static final Pattern HOST_CLUSTER_ID_PATTERN =
        Pattern.compile("^(.*)\\.cluster-(?:ro-)?\\w+\\.[\\w-]+\\.rds\\.amazonaws.com");

    @Override
    public void run() {
      try {
        final var clusterIdHostnameMatcher =
            HOST_CLUSTER_ID_PATTERN.matcher(topologyMonitor.clusterId);
        final var dbInstancesDetails =
            topologyMonitor.rdsClient.describeDBInstances(
                request -> {
                  if (clusterIdHostnameMatcher.find()) {
                    final var trueClusterId = clusterIdHostnameMatcher.group(1);
                    final var dbClusterIdFilter =
                        Filter.builder().name("db-cluster-id").values(trueClusterId).build();
                    request.filters(dbClusterIdFilter);
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
        topologyMonitor.clusterToHostsStateMap.put(topologyMonitor.clusterId, dbInstancesStateMap);
      } catch (AwsServiceException e) {
        LOGGER.error("Unable to retrieve instance state of current RDS cluster from RDS API", e);
      }
    }
  }
}
