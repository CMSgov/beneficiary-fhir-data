package gov.cms.bfd;

import javax.sql.DataSource;
import lombok.Getter;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * Represents the components required to construct a {@link DataSource} for our test DBs.
 *
 * <p>This is wildly insufficient for more complicated {@link DataSource}s; we're leaning heavily on
 * the very constrained set of simple {@link DataSource}s that are supported for our tests.
 */
@Getter
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
    if (dataSource instanceof PGSimpleDataSource pgDataSource) {
      this.url = pgDataSource.getUrl();
      this.username = pgDataSource.getUser();
      this.password = pgDataSource.getPassword();
    } else {
      throw new RuntimeException();
    }
  }
}
