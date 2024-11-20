package gov.cms.bfd.sharedutils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import software.amazon.awssdk.services.rds.RdsClient;
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

  /** Used for {@link RdsClient#describeDBInstances()} calls to filter the topology/host list. */
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
  }

  @Override
  protected List<HostSpec> queryForTopology(Connection conn) throws SQLException {
    final var dbTopology = super.queryForTopology(conn);

    // Taken from logic to determine if the monitor is in high refresh rate mode in delay()
    boolean isInHighRefreshRateMode =
        highRefreshRateEndTimeNano > 0 && System.nanoTime() < highRefreshRateEndTimeNano;
    if (isInHighRefreshRateMode) {
      // We don't want to make API calls to check status every 100 ms (which is the default high
      // refresh rate). Just return the topology as it is from the database and move on. High
      // refresh rate occurs only after failover, so the worst case has already happened and there
      // is no reason to restrict the hosts that can be connected to based on status
      return dbTopology;
    }

    return StateAwareHostListProviderUtils.filterUnreadyHostsByApiState(dbTopology, rdsClient);
  }
}
