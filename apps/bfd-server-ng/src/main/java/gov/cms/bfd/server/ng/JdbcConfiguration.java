package gov.cms.bfd.server.ng;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;

/**
 * {@link JdbcConnectionDetails} implementation for connecting to a live database. This
 * implementation is meant to be replaced in tests that connect to an ephemeral database container.
 */
public class JdbcConfiguration implements JdbcConnectionDetails {
  private final Configuration.Nonsensitive.Db nonsensitiveDb;
  private final Configuration.Sensitive.Db sensitiveDb;
  private final String localDbHost;
  private final String env;
  private final boolean useRds;

  JdbcConfiguration(Configuration configuration) {
    nonsensitiveDb = configuration.getNonsensitive().getDb();
    sensitiveDb = configuration.getSensitive().getDb();
    localDbHost = configuration.getLocal().getDbHost();
    env = configuration.getEnv();
    useRds = configuration.useRds();
  }

  @Override
  public String getUsername() {
    return sensitiveDb.getUsername();
  }

  @Override
  public String getPassword() {
    return sensitiveDb.getPassword();
  }

  @Override
  public String getJdbcUrl() {
    if (useRds) {
      try (var rdsClient = RdsClient.create()) {
        var clusterIdentifier = String.format(nonsensitiveDb.getClusterIdentifierTemplate(), env);
        var clusters =
            rdsClient.describeDBClusters(
                DescribeDbClustersRequest.builder().dbClusterIdentifier(clusterIdentifier).build());
        return getConnectionString(clusters.dbClusters().getFirst().readerEndpoint());
      }
    } else {
      return getConnectionString(localDbHost);
    }
  }

  private String getConnectionString(String dbHost) {
    return String.format(
        nonsensitiveDb.getConnectionStringTemplate(),
        dbHost,
        nonsensitiveDb.getPort(),
        nonsensitiveDb.getName());
  }
}
