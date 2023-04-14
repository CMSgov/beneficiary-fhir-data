package gov.cms.bfd;

import static gov.cms.bfd.DatabaseTestUtils.HSQL_SERVER_PASSWORD;

import javax.sql.DataSource;
import org.hsqldb.jdbc.JDBCDataSource;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * Represents the components required to construct a {@link DataSource} for our test DBs.
 *
 * <p>This is wildly insufficient for more complicated {@link DataSource}s; we're leaning heavily on
 * the very constrained set of simple {@link DataSource}s that are supported for our tests.
 */
public final class DataSourceComponents {
  /** The JDBC URL that should be used to connect to the test DB. */
  private final String url;
  /** The username that should be used to connect to the test DB. */
  private final String username;
  /** The password that should be used to connect to the test DB. */
  private final String password;

  /**
   * Constructs a {@link DataSourceComponents} instance for the specified test {@link DataSource}
   * (does not support more complicated {@link DataSource}s, as discussed in the class' JavaDoc).
   *
   * @param dataSource the data source
   */
  public DataSourceComponents(DataSource dataSource) {
    if (dataSource instanceof JDBCDataSource) {
      JDBCDataSource hsqlDataSource = (JDBCDataSource) dataSource;
      this.url = DatabaseTestApplicationUrl.includeApplicationNameInDbUrl(hsqlDataSource.getUrl());
      this.username = hsqlDataSource.getUser();
      /*
       * HSQL's implementation doesn't expose the DataSource's password, which is dumb. Because
       * I'm lazy, I just hardcode it here. If you need this to NOT be hardcoded, simplest fix
       * would be to write a helper method that pulls the field's value via reflection.
       */
      this.password = HSQL_SERVER_PASSWORD; // no getter available; hardcoded
    } else if (dataSource instanceof PGSimpleDataSource) {
      PGSimpleDataSource pgDataSource = (PGSimpleDataSource) dataSource;
      this.url = DatabaseTestApplicationUrl.includeApplicationNameInDbUrl(pgDataSource.getUrl());
      this.username = pgDataSource.getUser();
      this.password = pgDataSource.getPassword();
    } else {
      throw new RuntimeException();
    }
  }

  /**
   * Gets the {@link #url}.
   *
   * @return the JDBC URL that should be used to connect to the test DB
   */
  public String getUrl() {
    return url;
  }

  /**
   * Gets the {@link #username}.
   *
   * @return the username that should be used to connect to the test DB
   */
  public String getUsername() {
    return username;
  }

  /**
   * Gets the {@link #password}.
   *
   * @return the password that should be used to connect to the test DB
   */
  public String getPassword() {
    return password;
  }
}
