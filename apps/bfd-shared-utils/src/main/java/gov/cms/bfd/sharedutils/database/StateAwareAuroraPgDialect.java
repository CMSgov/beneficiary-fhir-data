package gov.cms.bfd.sharedutils.database;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.HostListProvider;
import software.amazon.jdbc.dialect.AuroraPgDialect;
import software.amazon.jdbc.dialect.Dialect;
import software.amazon.jdbc.dialect.HostListProviderSupplier;

/**
 * Custom {@link Dialect} that is exactly the same as the {@link AuroraPgDialect} in all but the
 * {@link HostListProvider}s it supplies. The custom {@link StateAwareMonitoringRdsHostListProvider}
 * provided by this {@link Dialect} spawns a thread that periodically queries the RDS API for the
 * status of RDS instances. This result is used to filter any host from the cluster topology that is
 * in the {@code creating} status so that the Wrapper does not attempt to make connections to
 * unready hosts.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED) // Implies @VisibleForTesting
public class StateAwareAuroraPgDialect extends AuroraPgDialect {
  /**
   * Query that returns the cluster topology, or the instances in the cluster, from the database
   * that is being queried.
   *
   * @implNote Taken directly from {@link AuroraPgDialect}.
   *     <p>See <a
   *     href="https://github.com/aws/aws-advanced-jdbc-wrapper/blob/0385fecbc9edd097a79c5a3edd066062281a47a0/wrapper/src/main/java/software/amazon/jdbc/dialect/AuroraPgDialect.java#L44-L52">...</a>
   */
  private static final String TOPOLOGY_QUERY =
      "SELECT SERVER_ID, CASE WHEN SESSION_ID OPERATOR(pg_catalog.=) 'MASTER_SESSION_ID' THEN TRUE ELSE FALSE END, "
          + "CPU, COALESCE(REPLICA_LAG_IN_MSEC, 0), LAST_UPDATE_TIMESTAMP "
          + "FROM pg_catalog.aurora_replica_status() "
          // filter out nodes that haven't been updated in the last 5 minutes
          + "WHERE EXTRACT("
          + "EPOCH FROM(pg_catalog.NOW() OPERATOR(pg_catalog.-) LAST_UPDATE_TIMESTAMP)) OPERATOR(pg_catalog.<=) 300 "
          + "OR SESSION_ID OPERATOR(pg_catalog.=) 'MASTER_SESSION_ID' "
          + "OR LAST_UPDATE_TIMESTAMP IS NULL";

  /**
   * Query that returns whether the database instances that is being queried is a writer node. Taken
   * directly from {@link AuroraPgDialect}.
   *
   * @implNote Taken directly from {@link AuroraPgDialect}.
   *     <p>See <a
   *     href="https://github.com/aws/aws-advanced-jdbc-wrapper/blob/0385fecbc9edd097a79c5a3edd066062281a47a0/wrapper/src/main/java/software/amazon/jdbc/dialect/AuroraPgDialect.java#L54-L57">...</a>
   */
  private static final String IS_WRITER_QUERY =
      "SELECT SERVER_ID FROM pg_catalog.aurora_replica_status() "
          + "WHERE SESSION_ID OPERATOR(pg_catalog.=) 'MASTER_SESSION_ID' "
          + "AND SERVER_ID OPERATOR(pg_catalog.=) pg_catalog.aurora_db_instance_identifier()";

  /**
   * Query that returns the instance identifier (name of instance) of the database instance that is
   * being queried. Taken directly from {@link AuroraPgDialect}.
   *
   * @implNote Taken directly from {@link AuroraPgDialect}.
   *     <p>See <a
   *     href="https://github.com/aws/aws-advanced-jdbc-wrapper/blob/0385fecbc9edd097a79c5a3edd066062281a47a0/wrapper/src/main/java/software/amazon/jdbc/dialect/AuroraPgDialect.java#L59">...</a>
   */
  private static final String NODE_ID_QUERY = "SELECT pg_catalog.aurora_db_instance_identifier()";

  /**
   * Query that returns whether the database instance that is being queried is a reader node. Taken
   * directly from {@link AuroraPgDialect}.
   *
   * @implNote Taken directly from {@link AuroraPgDialect}.
   *     <p>See <a
   *     href="https://github.com/aws/aws-advanced-jdbc-wrapper/blob/0385fecbc9edd097a79c5a3edd066062281a47a0/wrapper/src/main/java/software/amazon/jdbc/dialect/AuroraPgDialect.java#L60">...</a>
   */
  private static final String IS_READER_QUERY = "SELECT pg_catalog.pg_is_in_recovery()";

  /** Used for RDS API calls by the {@link StateAwareMonitoringRdsHostListProvider}. */
  private final RdsClient rdsClient;

  @Override
  public HostListProviderSupplier getHostListProvider() {
    return (properties, initialUrl, servicesContainer) ->
        new StateAwareMonitoringRdsHostListProvider(
            properties,
            initialUrl,
            servicesContainer,
            TOPOLOGY_QUERY,
            NODE_ID_QUERY,
            IS_READER_QUERY,
            IS_WRITER_QUERY,
            rdsClient);
  }

  /**
   * Creates an instance of {@link StateAwareAuroraPgDialect} with the given {@link AwsClientConfig}
   * used for creating a {@link RdsClient}.
   *
   * @param awsClientConfig AWS config used to create a {@link RdsClient}
   * @return a new {@link StateAwareAuroraPgDialect}
   */
  public static StateAwareAuroraPgDialect createWithAwsConfig(AwsClientConfig awsClientConfig) {
    return new StateAwareAuroraPgDialect(createRdsClient(awsClientConfig));
  }

  /**
   * Creates a {@link RdsClient}.
   *
   * @param awsClientConfig the AWS client configuration to use for the {@link RdsClient}
   * @return the RDS client
   */
  private static RdsClient createRdsClient(AwsClientConfig awsClientConfig) {
    final var rdsClientBuilder = RdsClient.builder();
    awsClientConfig.configureAwsService(rdsClientBuilder);
    rdsClientBuilder.credentialsProvider(DefaultCredentialsProvider.create());
    return rdsClientBuilder.build();
  }
}
