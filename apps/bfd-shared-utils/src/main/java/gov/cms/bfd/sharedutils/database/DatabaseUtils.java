package gov.cms.bfd.sharedutils.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Utility class for database functions */
public final class DatabaseUtils {

  private DatabaseUtils() {
    // No construction. Utility class only.
  }

  /** Returns true if the connection is to an HSQL database */
  public static boolean isHsqlConnection(Connection connection) throws SQLException {
    return extractVendorName(connection).equals("HSQL Database Engine");
  }

  /** Returns true if the connection is to a Postgres database */
  public static boolean isPostgresConnection(Connection connection) throws SQLException {
    return extractVendorName(connection).equals("PostgreSQL");
  }

  /** Returns the vendor name for the connection */
  public static String extractVendorName(Connection connection) throws SQLException {
    return connection.getMetaData().getDatabaseProductName();
  }
}
