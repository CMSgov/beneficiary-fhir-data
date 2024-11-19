package gov.cms.bfd.sharedutils.database;

import software.amazon.jdbc.dialect.AuroraPgDialect;
import software.amazon.jdbc.dialect.Dialect;
import software.amazon.jdbc.dialect.HostListProviderSupplier;
import software.amazon.jdbc.hostlistprovider.AuroraHostListProvider;
import software.amazon.jdbc.hostlistprovider.monitoring.MonitoringRdsHostListProvider;
import software.amazon.jdbc.plugin.AuroraInitialConnectionStrategyPlugin;
import software.amazon.jdbc.plugin.failover2.FailoverConnectionPlugin;

/**
 * Custom {@link Dialect} that implements a custom topology query that {@code LEFT JOIN}s on a
 * metadata table, {@link public.rds_instances_state}, that stores the cluster's instances and their
 * state as reported by the AWS API filtering out any instances in the {@code creating} state. This
 * ensures that the {@link AuroraInitialConnectionStrategyPlugin} does not attempt to connect to
 * instances that exist in the cluster topology before they are ready to connect to.
 */
public class StatusAwareAuroraPgDialect extends AuroraPgDialect {

  /**
   * Custom topology query that is an augmented version of the {@link AuroraPgDialect}'s default
   * topology query. This topology query will return instances that are not reported as being in the
   * {@code creating} phase by the RDS API by {@code LEFT JOIN}ing on a metadata table storing those
   * states per-instance. This table is updated by an external AWS Lambda, {@code
   * bfd-ENV-rds-state-updater}, whenever RDS instances in the cluster change state.
   */
  private static final String TOPOLOGY_QUERY =
      """
SELECT
  topology.server_id,
  topology.case,
  topology.cpu,
  topology.coalesce,
  topology.last_update_timestamp
FROM
  (
    SELECT
      SERVER_ID,
      CASE
        WHEN SESSION_ID = 'MASTER_SESSION_ID' THEN TRUE
        ELSE FALSE
      END,
      CPU,
      COALESCE(REPLICA_LAG_IN_MSEC, 0),
      LAST_UPDATE_TIMESTAMP
    FROM
      aurora_replica_status()
    WHERE
      EXTRACT(
        EPOCH
        FROM
          (NOW() - LAST_UPDATE_TIMESTAMP)
      ) <= 300
      OR SESSION_ID = 'MASTER_SESSION_ID'
      OR LAST_UPDATE_TIMESTAMP IS NULL
  ) as topology
  LEFT JOIN public.rds_instances_state as api_detail ON topology.server_id = api_detail.server_id
WHERE
  api_detail.status IS NULL
  OR api_detail.status != 'creating'
""";

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

  @Override
  public HostListProviderSupplier getHostListProvider() {
    // There is no difference in implementation between this override and its super. We must provide
    // our own topology query, and the only means of doing so is to override this method and provide
    // it in the constructor of the host list providers
    return (properties, initialUrl, hostListProviderService, pluginService) -> {
      final FailoverConnectionPlugin failover2Plugin =
          pluginService.getPlugin(FailoverConnectionPlugin.class);

      if (failover2Plugin != null) {
        return new MonitoringRdsHostListProvider(
            properties,
            initialUrl,
            hostListProviderService,
            TOPOLOGY_QUERY,
            NODE_ID_QUERY,
            IS_READER_QUERY,
            IS_WRITER_QUERY,
            pluginService);
      }
      return new AuroraHostListProvider(
          properties,
          initialUrl,
          hostListProviderService,
          TOPOLOGY_QUERY,
          NODE_ID_QUERY,
          IS_READER_QUERY);
    };
  }
}
