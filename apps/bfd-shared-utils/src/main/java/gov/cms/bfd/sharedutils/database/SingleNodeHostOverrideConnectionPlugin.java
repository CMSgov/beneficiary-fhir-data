package gov.cms.bfd.sharedutils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.JdbcCallable;
import software.amazon.jdbc.PluginService;
import software.amazon.jdbc.hostavailability.HostAvailability;
import software.amazon.jdbc.plugin.AbstractConnectionPlugin;
import software.amazon.jdbc.util.RdsUtils;

/** Test. */
@RequiredArgsConstructor
public class SingleNodeHostOverrideConnectionPlugin extends AbstractConnectionPlugin {
  /** Test. */
  private static final Set<String> subscribedMethods =
      Set.of("initHostProvider", "connect", "forceConnect");

  /** Test. */
  private final RdsUtils rdsUtils = new RdsUtils();

  /** Test. */
  private final PluginService pluginService;

  /** Test. */
  private final Properties properties;

  /** Test. */
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
   * Test.
   *
   * @param hostSpec asdf
   * @param props asdf
   * @param isInitialConnection asf
   * @param connectFunc sdf
   * @return asdf
   */
  private Connection connectInternal(
      HostSpec hostSpec,
      Properties props,
      boolean isInitialConnection,
      JdbcCallable<Connection, SQLException> connectFunc)
      throws SQLException {
    final var rdsUrlType = rdsUtils.identifyRdsType(hostSpec.getHost());
    if (!rdsUrlType.isRdsCluster()) {
      return connectFunc.call();
    }

    final var allValidHosts =
        pluginService.getAllHosts().stream()
            .filter(host -> host.getAvailability() != HostAvailability.NOT_AVAILABLE)
            .toList();
    if (allValidHosts.size() != 1) {
      return connectFunc.call();
    }

    final var singleAvailableHost = allValidHosts.getFirst();
    final var singleHostType = rdsUtils.identifyRdsType(singleAvailableHost.getHost());
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
