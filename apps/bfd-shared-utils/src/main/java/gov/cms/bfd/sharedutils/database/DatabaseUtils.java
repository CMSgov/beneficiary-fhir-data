package gov.cms.bfd.sharedutils.database;

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
}
