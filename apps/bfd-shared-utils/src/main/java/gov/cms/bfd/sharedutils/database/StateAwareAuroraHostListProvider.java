package gov.cms.bfd.sharedutils.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.HostListProviderService;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.dialect.Dialect;
import software.amazon.jdbc.ds.AwsWrapperDataSource;
import software.amazon.jdbc.hostlistprovider.AuroraHostListProvider;

/**
 * "State aware" {@link AuroraHostListProvider}. Hosts that are returned from the cluster topology
 * are excluded from the topology/host list cache based on their status as reported by the AWS RDS
 * API.
 */
public class StateAwareAuroraHostListProvider extends AuroraHostListProvider {

  /** Used for {@link RdsClient#describeDBInstances()} calls to filter the topology/host list. */
  private final RdsClient rdsClient;

  /**
   * Creates an instance of {@link StateAwareAuroraHostListProvider}.
   *
   * @param properties the {@link AwsWrapperDataSource} properties
   * @param originalUrl the original JDBC URL
   * @param hostListProviderService the {@link HostListProviderService} as determined by the {@link
   *     Dialect}
   * @param topologyQuery the SQL query that returns the cluster topology
   * @param nodeIdQuery the SQL query that returns the instance identifier of the database that is
   *     queried
   * @param isReaderQuery the SQL query that returns whether the current database is a reader
   * @param rdsClient {@link RdsClient} used to check instance status
   */
  public StateAwareAuroraHostListProvider(
      Properties properties,
      String originalUrl,
      HostListProviderService hostListProviderService,
      String topologyQuery,
      String nodeIdQuery,
      String isReaderQuery,
      RdsClient rdsClient) {
    super(
        properties,
        originalUrl,
        hostListProviderService,
        topologyQuery,
        nodeIdQuery,
        isReaderQuery);

    this.rdsClient = rdsClient;
  }

  @Override
  protected List<HostSpec> queryForTopology(Connection conn) throws SQLException {
    final var dbTopology = super.queryForTopology(conn);
    return StateAwareHostListProviderUtils.filterUnreadyHostsByApiState(dbTopology, rdsClient);
  }
}
