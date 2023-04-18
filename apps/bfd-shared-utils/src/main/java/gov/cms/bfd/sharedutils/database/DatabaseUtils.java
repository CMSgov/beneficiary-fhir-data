package gov.cms.bfd.sharedutils.database;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for database functions. */
public final class DatabaseUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUtils.class);

  /** Private constructor to prevent instantiation of utility class. */
  private DatabaseUtils() {}

  /**
   * Returns true if the connection is to an HSQL database @param connection the connection.
   *
   * @param connection the connection
   * @return if the connection is to an HSQL database
   * @throws SQLException any sql exception
   */
  public static boolean isHsqlConnection(Connection connection) throws SQLException {
    return extractVendorName(connection).equals("HSQL Database Engine");
  }

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
   * Configures a data source.
   *
   * @param poolingDataSource the {@link HikariDataSource} to be configured, which must already have
   *     its basic connection properties (URL, username, password) configured
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   */
  public static void configureDataSource(
      HikariDataSource poolingDataSource,
      String connectionsMaxText,
      MetricRegistry metricRegistry) {
    int connectionsMax;
    try {
      connectionsMax = Integer.parseInt(connectionsMaxText);
      LOGGER.info("Setting up datasource from setting with {} max connections.", connectionsMax);
    } catch (NumberFormatException e) {
      connectionsMax = -1;
    }
    if (connectionsMax < 1) {
      // Assign a reasonable default value, if none was specified.
      connectionsMax = Runtime.getRuntime().availableProcessors() * 2;
      LOGGER.info("Setting up datasource from default with {} max connections.", connectionsMax);
    }

    poolingDataSource.setMaximumPoolSize(connectionsMax);

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
