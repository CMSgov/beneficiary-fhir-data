package gov.cms.bfd.sharedutils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
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

  /** Map of cluster ID to the {@link List} of {@link HostSpec}s within its topology. */
  private final ConcurrentHashMap<String, List<HostSpec>> clusterToUnfilteredTopologyMap =
      new ConcurrentHashMap<>();

  /**
   * Map of instance ID to the status of the instance returned by the RDS API. Periodically updated
   * by the {@link InstanceStateMonitoringThread} every {@link #instanceStateMonitorRefreshRateMs}
   * milliseconds.
   */
  private final ConcurrentHashMap<String, Map<String, String>> clusterToHostsStateMap =
      new ConcurrentHashMap<>();

  /**
   * Rate at which the {@link InstanceStateMonitoringThread} will update the {@link
   * #clusterToHostsStateMap} with the status of each RDS instance in a given cluster as reported by
   * the RDS API.
   */
  private final long instanceStateMonitorRefreshRateMs;

  /**
   * Used by the {@link InstanceStateMonitoringThread} to retrieve the status of RDS instances in a
   * given cluster.
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
   *     InstanceStateMonitoringThread} queries the RDS API for instance status
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
    this.instanceStateMonitorRefreshRateMs = instanceStateMonitorRefreshRateMs;
    this.rdsClient = rdsClient;
  }

  @Override
  public void run() {
    final var instanceStateMonitor = new InstanceStateMonitoringThread(this);
    instanceStateMonitor.start();

    super.run();
  }

  @Override
  protected List<HostSpec> queryForTopology(Connection conn) throws SQLException {
    final var dbTopology = super.queryForTopology(conn);
    // Store the unfiltered topology for the instance state monitoring thread to use as filtering
    // criteria in the RDS API request
    clusterToUnfilteredTopologyMap.put(clusterId, dbTopology);

    // Taken from logic to determine if the monitor is in high refresh rate mode in delay()
    boolean isInHighRefreshRateMode =
        highRefreshRateEndTimeNano > 0 && System.nanoTime() < highRefreshRateEndTimeNano;
    if (isInHighRefreshRateMode || !clusterToHostsStateMap.containsKey(clusterId)) {
      // High refresh rate occurs only after failover, so the worst case has already happened and
      // there is no reason to restrict the hosts that can be connected to based on status. Or, the
      // hosts map is empty, which indicates that this may be the first query for topology.
      return dbTopology;
    }

    // This ensures that unready instances are not connected to early during scale-out. Other states
    // (like "deleting") are not considered as failover should handle them
    final var currentClusterInstanceStateMap = clusterToHostsStateMap.get(clusterId);
    return dbTopology.stream()
        .filter(hostSpec -> currentClusterInstanceStateMap.containsKey(hostSpec.getHostId()))
        .filter(
            hostSpec ->
                !currentClusterInstanceStateMap
                    .get(hostSpec.getHostId())
                    .equalsIgnoreCase("creating"))
        .toList();
  }

  /**
   * {@link Thread} spawned on initialization of a {@link StateAwareClusterTopologyMonitor} that
   * will independently monitor the RDS API status of RDS instances in the current {@link
   * #clusterId} cluster and concurrently update the {@link #clusterToHostsStateMap} every {@link
   * #instanceStateMonitorRefreshRateMs} millisecond(s).
   */
  @AllArgsConstructor
  private static class InstanceStateMonitoringThread extends Thread {

    /** Logger for this class. */
    static final Logger LOGGER = LoggerFactory.getLogger(InstanceStateMonitoringThread.class);

    /** {@link StateAwareClusterTopologyMonitor} that spawned this thread. */
    private final StateAwareClusterTopologyMonitor monitor;

    @Override
    public void run() {
      try {
        while (!monitor.stop.get()) {
          try {
            List<HostSpec> unfilteredHosts =
                monitor.clusterToUnfilteredTopologyMap.getOrDefault(monitor.clusterId, List.of());
            final var dbInstancesDetails =
                monitor.rdsClient.describeDBInstances(
                    request -> {
                      // If the topology hasn't been retrieved yet (likely the case on startup),
                      // then return the status of all instances in RDS. This should be fine,
                      // assuming that the IAM Policy is not overly restrictive
                      if (!unfilteredHosts.isEmpty()) {
                        final var dbInstanceIdFilter =
                            Filter.builder()
                                .name("db-instance-id")
                                .values(
                                    unfilteredHosts.stream()
                                        .map(HostSpec::getHostId)
                                        .collect(Collectors.toSet()))
                                .build();
                        request.filters(dbInstanceIdFilter);
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
            monitor.clusterToHostsStateMap.put(monitor.clusterId, dbInstancesStateMap);
          } catch (AwsServiceException e) {
            LOGGER.error(
                "Unable to retrieve instance state of current RDS cluster from RDS API", e);
          }

          TimeUnit.MILLISECONDS.sleep(monitor.instanceStateMonitorRefreshRateMs);
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
