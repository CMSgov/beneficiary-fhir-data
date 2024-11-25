package gov.cms.bfd.sharedutils.database;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.AwsWrapperProperty;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.dialect.Dialect;
import software.amazon.jdbc.ds.AwsWrapperDataSource;
import software.amazon.jdbc.hostlistprovider.monitoring.ClusterTopologyMonitor;
import software.amazon.jdbc.hostlistprovider.monitoring.MonitoringRdsHostListProvider;

/**
 * "State aware" {@link MonitoringRdsHostListProvider}. This variant creates the {@link
 * StateAwareClusterTopologyMonitor} when it initializes a {@link ClusterTopologyMonitor} in {@link
 * #initMonitor()}.
 */
public class StateAwareMonitoringRdsHostListProvider extends MonitoringRdsHostListProvider {

  /**
   * AWS Wrapper property that is read from the {@link AwsWrapperDataSource} {@link Properties} that
   * sets {@link #instanceStateMonitorRefreshRateMs}. Controls the refresh rate of the thread that
   * monitors the RDS API state of RDS instances retrieved from the cluster topology.
   */
  public static final AwsWrapperProperty INSTANCE_STATE_MONITOR_REFRESH_RATE_MS =
      new AwsWrapperProperty(
          "instanceStateMonitorRefreshRateMs",
          "5000",
          "Refresh rate of the thread that monitors the RDS API state of RDS instances retrieved from the cluster topology.");

  /**
   * Map {@link #clusterId}s to immutable map of instance ID to the status of the instance returned
   * by the RDS API. Periodically updated by {@link StateAwareClusterTopologyMonitor}(s) every
   * {@link #instanceStateMonitorRefreshRateMs} millisecond(s).
   */
  private final ConcurrentHashMap<String, Map<String, String>> clusterToHostsStateMap =
      new ConcurrentHashMap<>();

  /**
   * The rate, in milliseconds, at which the {@link StateAwareMonitoringRdsHostListProvider} will
   * monitor the state of RDS instances in the current cluster as reported by the RDS API.
   */
  private final long instanceStateMonitorRefreshRateMs;

  /** Used for {@link RdsClient#describeDBInstances()} calls to filter the topology/host list. */
  private final RdsClient rdsClient;

  /**
   * Creates an instance of {@link StateAwareMonitoringRdsHostListProvider}.
   *
   * @param properties the {@link AwsWrapperDataSource} properties
   * @param originalUrl the original JDBC URL
   * @param hostListProviderService the {@link HostListProviderService} as determined by the {@link
   *     Dialect}
   * @param topologyQuery the SQL query that returns the cluster topology
   * @param nodeIdQuery the SQL query that returns the instance identifier of the database that is
   *     queried
   * @param isReaderQuery the SQL query that returns whether the current database is a reader
   * @param writerTopologyQuery the SQL query that returns whether the current database is a writer
   * @param pluginService the {@link PluginService} as provided by the {@link Dialect}
   * @param rdsClient {@link RdsClient} used to check instance status
   */
  public StateAwareMonitoringRdsHostListProvider(
      Properties properties,
      String originalUrl,
      HostListProviderService hostListProviderService,
      String topologyQuery,
      String nodeIdQuery,
      String isReaderQuery,
      String writerTopologyQuery,
      PluginService pluginService,
      RdsClient rdsClient) {
    super(
        properties,
        originalUrl,
        hostListProviderService,
        topologyQuery,
        nodeIdQuery,
        isReaderQuery,
        writerTopologyQuery,
        pluginService);
    this.rdsClient = rdsClient;
    this.instanceStateMonitorRefreshRateMs =
        INSTANCE_STATE_MONITOR_REFRESH_RATE_MS.getLong(properties);
  }

  @Override
  protected ClusterTopologyMonitor initMonitor() {
    return monitors.computeIfAbsent(
        this.clusterId,
        (key) ->
            new StateAwareClusterTopologyMonitor(
                key,
                topologyCache,
                clusterToHostsStateMap,
                initialHostSpec,
                properties,
                pluginService,
                hostListProviderService,
                clusterInstanceTemplate,
                refreshRateNano,
                highRefreshRateNano,
                TOPOLOGY_CACHE_EXPIRATION_NANO,
                topologyQuery,
                writerTopologyQuery,
                nodeIdQuery,
                instanceStateMonitorRefreshRateMs,
                rdsClient),
        MONITOR_EXPIRATION_NANO);
  }
}
