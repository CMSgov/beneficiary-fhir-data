package gov.cms.bfd.sharedutils.database;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.HostListProvider;
import software.amazon.jdbc.dialect.AuroraPgDialect;
import software.amazon.jdbc.dialect.Dialect;
import software.amazon.jdbc.dialect.HostListProviderSupplier;
import software.amazon.jdbc.plugin.failover2.FailoverConnectionPlugin;

/**
 * Custom {@link Dialect} that is exactly the same as the {@link AuroraPgDialect} in all but the
 * {@link HostListProvider}s it supplies. The custom "state aware" {@link HostListProvider}s
 * provided by this {@link Dialect} query the RDS API and filter any host that is in the {@code
 * creating} status so that the Wrapper does not attempt to make connections to unready hosts.
 */
public class StateAwareAuroraPgDialect extends AuroraPgDialect {
  /**
   * Query that returns the cluster topology, or the instances in the cluster, from the database
   * that is being queried. Taken directly from {@link AuroraPgDialect}.
   */
  private static final String TOPOLOGY_QUERY =
      "SELECT SERVER_ID, CASE WHEN SESSION_ID = 'MASTER_SESSION_ID' THEN TRUE ELSE FALSE END, "
          + "CPU, COALESCE(REPLICA_LAG_IN_MSEC, 0), LAST_UPDATE_TIMESTAMP "
          + "FROM aurora_replica_status() "
          // filter out nodes that haven't been updated in the last 5 minutes
          + "WHERE EXTRACT(EPOCH FROM(NOW() - LAST_UPDATE_TIMESTAMP)) <= 300 OR SESSION_ID = 'MASTER_SESSION_ID' "
          + "OR LAST_UPDATE_TIMESTAMP IS NULL";

  /**
   * Query that returns whether the database instances that is being queried is a writer node. Taken
   * directly from {@link AuroraPgDialect}.
   */
  private static final String IS_WRITER_QUERY =
      "SELECT SERVER_ID FROM aurora_replica_status() "
          + "WHERE SESSION_ID = 'MASTER_SESSION_ID' AND SERVER_ID = aurora_db_instance_identifier()";

  /**
   * Query that returns the instance identifier (name of instance) of the database instance that is
   * being queried. Taken directly from {@link AuroraPgDialect}.
   */
  private static final String NODE_ID_QUERY = "SELECT aurora_db_instance_identifier()";

  /**
   * Query that returns whether the database instance that is being queried is a reader node. Taken
   * directly from {@link AuroraPgDialect}.
   */
  private static final String IS_READER_QUERY = "SELECT pg_is_in_recovery()";

  /**
   * Used for {@link RdsClient#describeDBInstances()} calls by the "state aware" {@link
   * HostListProvider}s.
   */
  private final RdsClient rdsClient;

  /**
   * Creates an instance of {@link StateAwareAuroraPgDialect}.
   *
   * @param awsClientConfig client configuration for the {@link RdsClient}
   */
  public StateAwareAuroraPgDialect(AwsClientConfig awsClientConfig) {
    this.rdsClient = getRdsClient(awsClientConfig);
  }

  @Override
  public HostListProviderSupplier getHostListProvider() {
    return (properties, initialUrl, hostListProviderService, pluginService) -> {
      final var failover2Plugin = pluginService.getPlugin(FailoverConnectionPlugin.class);
      if (failover2Plugin != null) {
        return new StateAwareMonitoringRdsHostListProvider(
            properties,
            initialUrl,
            hostListProviderService,
            TOPOLOGY_QUERY,
            NODE_ID_QUERY,
            IS_READER_QUERY,
            IS_WRITER_QUERY,
            pluginService,
            rdsClient);
      }

      return new StateAwareAuroraHostListProvider(
          properties,
          initialUrl,
          hostListProviderService,
          TOPOLOGY_QUERY,
          NODE_ID_QUERY,
          IS_READER_QUERY,
          rdsClient);
    };
  }

  /**
   * Creates a {@link RdsClient}.
   *
   * @param awsClientConfig the AWS client configuration to use for the {@link RdsClient}
   * @return the RDS client
   */
  private RdsClient getRdsClient(AwsClientConfig awsClientConfig) {
    final var rdsClientBuilder = RdsClient.builder();
    awsClientConfig.configureAwsService(rdsClientBuilder);
    rdsClientBuilder.credentialsProvider(DefaultCredentialsProvider.create());
    return rdsClientBuilder.build();
  }
}
