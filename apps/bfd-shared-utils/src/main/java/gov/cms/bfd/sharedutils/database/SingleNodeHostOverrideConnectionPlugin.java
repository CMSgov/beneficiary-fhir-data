package gov.cms.bfd.sharedutils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import software.amazon.jdbc.ConnectionPlugin;
import software.amazon.jdbc.ConnectionProvider;
import software.amazon.jdbc.DriverConnectionProvider;
import software.amazon.jdbc.HikariPooledConnectionProvider;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.JdbcCallable;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.hostavailability.HostAvailability;
import software.amazon.jdbc.plugin.AbstractConnectionPlugin;
import software.amazon.jdbc.util.RdsUtils;

/**
 * AWS JDBC Wrapper {@link ConnectionPlugin} that will redirect/override a connection to the RDS
 * Cluster URL to a direct RDS Instance URL if there is a single, valid Instance available to
 * connect to in the topology.
 *
 * @implNote This plugin is uniquely useful for the BFD Server as the Server typically uses the RDS
 *     Cluster Reader Endpoint in its connection string configuration along with Hikari connection
 *     pooling. The {@link HikariPooledConnectionProvider} has a clause in it preventing it from
 *     being used as the {@link ConnectionProvider} if the connection is to an RDS Cluster URL; that
 *     is, no connection pools are established against RDS Cluster URLs (probably to avoid DNS
 *     round-robin conflicts), instead the {@link DriverConnectionProvider} is used which will
 *     establish new {@link Connection}s for every query. If there is only a writer node/host in the
 *     Cluster, no built-in plugin will override the connection string host to connect directly to
 *     the writer, and so no connection pool is established. By overriding the connection URL from
 *     the Cluster to the single Instance, this plugin enables connection pooling against Clusters
 *     with a single writer node, greatly increasing performance under high-load scenarios.
 */
@RequiredArgsConstructor
public class SingleNodeHostOverrideConnectionPlugin extends AbstractConnectionPlugin {
  /** RDS utilities used to check if the host is a cluster endpoint or not. */
  private static final RdsUtils RDS_UTILS = new RdsUtils();

  /**
   * Methods that are subscribed to by this plugin so that this plugin can modify their behavior.
   */
  private static final Set<String> subscribedMethods =
      Set.of("initHostProvider", "connect", "forceConnect");

  /** Service providing various helper functions for plugins. */
  private final PluginService pluginService;

  /**
   * Service that provides the list of hosts from the topology. Initialized in {@link
   * #initHostProvider(String, String, Properties, HostListProviderService, JdbcCallable)}.
   */
  private HostListProviderService hostListProviderService;

  @Override
  public Set<String> getSubscribedMethods() {
    return subscribedMethods;
  }

  @Override
  public Connection connect(
      String driverProtocol,
      HostSpec hostSpec,
      Properties props,
      boolean isInitialConnection,
      JdbcCallable<Connection, SQLException> connectFunc)
      throws SQLException {
    return connectInternal(hostSpec, props, isInitialConnection, connectFunc);
  }

  @Override
  public Connection forceConnect(
      String driverProtocol,
      HostSpec hostSpec,
      Properties props,
      boolean isInitialConnection,
      JdbcCallable<Connection, SQLException> forceConnectFunc)
      throws SQLException {
    return connectInternal(hostSpec, props, isInitialConnection, forceConnectFunc);
  }

  @Override
  public void initHostProvider(
      String driverProtocol,
      String initialUrl,
      Properties props,
      HostListProviderService hostListProviderService,
      JdbcCallable<Void, SQLException> initHostProviderFunc)
      throws SQLException {
    this.hostListProviderService = hostListProviderService;
    if (hostListProviderService.isStaticHostListProvider()) {
      throw new SQLException("HostListProviderService must be dynamic");
    } else {
      initHostProviderFunc.call();
    }
  }

  /**
   * Forces a connection directly to the last remaining, valid RDS Instance in the topology if
   * attempting to connect to a RDS Cluster endpoint URL.
   *
   * @param hostSpec the original {@link HostSpec} that is being connected to
   * @param props properties of the connection
   * @param isInitialConnection {@code true} if this is the first connection, {@code false}
   *     otherwise
   * @param connectFunc {@link JdbcCallable} function that may or may not call other {@link
   *     ConnectionPlugin}s before actually establishing a connection using a {@link
   *     ConnectionProvider}
   * @return a {@link Connection} directly to the single RDS Instance in the topology if {@code
   *     hostSpec} is a RDS Cluster URL and the topology contains only a single Instance; otherwise,
   *     the {@link Connection} established by {@code connectFunc}
   */
  private Connection connectInternal(
      HostSpec hostSpec,
      Properties props,
      boolean isInitialConnection,
      JdbcCallable<Connection, SQLException> connectFunc)
      throws SQLException {
    final var rdsUrlType = RDS_UTILS.identifyRdsType(hostSpec.getHost());
    if (!rdsUrlType.isRdsCluster()) {
      // If the host we're trying to connect to isn't a cluster, we can assume that we're trying to
      // connect to a node directly
      return connectFunc.call();
    }

    final var allValidHosts =
        pluginService.getAllHosts().stream()
            .filter(host -> host.getAvailability() != HostAvailability.NOT_AVAILABLE)
            .toList();
    if (allValidHosts.size() != 1) {
      // If the list of valid hosts is not 1 that means that there is more than one node available.
      // This also implies that there is a reader available. Let the initial connection plugin (if
      // it's enabled) handle establishing direct connections further down the connect chain
      return connectFunc.call();
    }

    final var singleAvailableHost = allValidHosts.getFirst();
    final var singleHostType = RDS_UTILS.identifyRdsType(singleAvailableHost.getHost());
    if (singleHostType.isRdsCluster()) {
      // We haven't yet retrieved the cluster topology so the host is still the Cluster endpoint;
      // continue to connect
      return connectFunc.call();
    }

    final var singleNodeHostSpec = allValidHosts.getFirst();
    if (isInitialConnection) {
      hostListProviderService.setInitialConnectionHostSpec(singleNodeHostSpec);
    }
    return pluginService.connect(singleNodeHostSpec, props);
  }
}
