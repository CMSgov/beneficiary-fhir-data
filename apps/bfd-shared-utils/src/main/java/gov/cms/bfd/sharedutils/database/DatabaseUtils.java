package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Utility class for database functions. */
public final class DatabaseUtils {

  /** Private constructor to prevent instantiation of utility class. */
  private DatabaseUtils() {}

  /**
   * Returns true if the connection is to a Postgres database @param connection the connection.
   *
   * @param connection the connection
   * @return if the connection is to a Postgres database
   * @throws SQLException any sql exception
   */
  public static boolean isPostgresConnection(Connection connection) throws SQLException {
    return extractVendorName(connection).equals("PostgreSQL");
  }

  /**
   * Returns the vendor name for the connection @param connection the connection.
   *
   * @param connection the connection
   * @return the vendor name for the connection
   * @throws SQLException any sql exception
   */
  public static String extractVendorName(Connection connection) throws SQLException {
    return connection.getMetaData().getDatabaseProductName();
  }

  /**
   * Computes an appropriate maximum number of connections to use by parsing the provided string.
   * Falls back to a computed value if the string cannot be parsed or is non-positive.
   *
   * @param connectionsMaxText the maximum number of database connections to use
   * @return computed maximum number of database connections to use
   */
  public static int computeMaximumPoolSize(String connectionsMaxText) {
    int connectionsMax;
    try {
      connectionsMax = Integer.parseInt(connectionsMaxText);
    } catch (NumberFormatException e) {
      connectionsMax = -1;
    }
    if (connectionsMax < 1) {
      // Assign a reasonable default value, if none was specified.
      connectionsMax = Runtime.getRuntime().availableProcessors() * 5;
    }
    return connectionsMax;
  }

  /**
   * Configures a data source.
   *
   * @param poolingDataSource the {@link HikariDataSource} to be configured, which must already have
   *     its basic connection properties (URL, username, password) configured
   * @param metricRegistry the {@link MetricRegistry} for the application
   */
  public static void configureDataSource(
      HikariDataSource poolingDataSource, MetricRegistry metricRegistry) {
    /*
     * FIXME Temporary workaround for CBBI-357: send Postgres' query planner a
     * strongly worded letter instructing it to avoid sequential scans whenever
     * possible.
     */
    if (poolingDataSource.getJdbcUrl() != null
        && poolingDataSource.getJdbcUrl().contains("postgre"))
      poolingDataSource.setConnectionInitSql(
          "set application_name = 'bfd-server'; set enable_seqscan = false;");

    poolingDataSource.setRegisterMbeans(true);
    poolingDataSource.setMetricRegistry(metricRegistry);

    /*
     * FIXME Temporary setting for BB-1233 to find the source of any possible leaks
     * (see: https://github.com/brettwooldridge/HikariCP/issues/1111)
     */
    poolingDataSource.setLeakDetectionThreshold(60 * 1000);
  }
}
