package gov.cms.bfd.sharedutils.database;

import software.amazon.jdbc.dialect.AuroraPgDialect;
import software.amazon.jdbc.dialect.HostListProviderSupplier;
import software.amazon.jdbc.hostlistprovider.AuroraHostListProvider;
import software.amazon.jdbc.hostlistprovider.monitoring.MonitoringRdsHostListProvider;
import software.amazon.jdbc.plugin.failover2.FailoverConnectionPlugin;

/** Test. */
public class CustomAuroraPgDialect extends AuroraPgDialect {

  /** Test. */
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
          (NOW () - LAST_UPDATE_TIMESTAMP)
      ) <= 300
      OR SESSION_ID = 'MASTER_SESSION_ID'
      OR LAST_UPDATE_TIMESTAMP IS NULL
  ) as topology
  INNER JOIN public.cluster_instance_statuses as statuses ON topology.server_id = statuses.server_id
where
  statuses.server_status = 'available'
""";

  /** Test. */
  private static final String IS_WRITER_QUERY =
      "SELECT SERVER_ID FROM aurora_replica_status() "
          + "WHERE SESSION_ID = 'MASTER_SESSION_ID' AND SERVER_ID = aurora_db_instance_identifier()";

  /** Test. */
  private static final String NODE_ID_QUERY = "SELECT aurora_db_instance_identifier()";

  /** Test. */
  private static final String IS_READER_QUERY = "SELECT pg_is_in_recovery()";

  @Override
  public HostListProviderSupplier getHostListProvider() {
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
