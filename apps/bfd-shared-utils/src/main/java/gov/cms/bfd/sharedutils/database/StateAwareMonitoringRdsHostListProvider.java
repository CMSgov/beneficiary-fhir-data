package gov.cms.bfd.sharedutils.database;

import java.util.Properties;
import software.amazon.awssdk.services.rds.RdsClient;
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
  }

  @Override
  protected ClusterTopologyMonitor initMonitor() {
    return monitors.computeIfAbsent(
        this.clusterId,
        (key) ->
            new StateAwareClusterTopologyMonitor(
                key,
                topologyCache,
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
                rdsClient),
        MONITOR_EXPIRATION_NANO);
  }
}
