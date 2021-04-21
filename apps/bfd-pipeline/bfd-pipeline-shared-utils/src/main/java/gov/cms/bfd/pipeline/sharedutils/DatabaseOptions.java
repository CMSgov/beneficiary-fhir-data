package gov.cms.bfd.pipeline.sharedutils;

import java.io.Serializable;
import javax.sql.DataSource;

/** The user-configurable options that specify how to access the application's database. */
public final class DatabaseOptions implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String databaseUrl;
  private final String databaseUsername;
  private final char[] databasePassword;
  private final DataSource databaseDataSource;

  /**
   * Constructs a new {@link DatabaseOptions} instance.
   *
   * @param databaseUrl the value to use for {@link #getDatabaseUrl()}
   * @param databaseUsername the value to use for {@link #getDatabaseUsername()}
   * @param databasePassword the value to use for {@link #getDatabasePassword()}
   */
  public DatabaseOptions(String databaseUrl, String databaseUsername, char[] databasePassword) {
    this.databaseUrl = databaseUrl;
    this.databaseUsername = databaseUsername;
    this.databasePassword = databasePassword;
    this.databaseDataSource = null;
  }

  /**
   * Constructs a new {@link DatabaseOptions} instance.
   *
   * @param databaseDataSource the value to use for {@link #getDatabaseDataSource()}
   */
  public DatabaseOptions(DataSource databaseDataSource) {
    this.databaseUrl = null;
    this.databaseUsername = null;
    this.databasePassword = null;
    this.databaseDataSource = databaseDataSource;
  }

  /**
   * @return the JDBC URL of the database to load into, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  public String getDatabaseUrl() {
    return databaseUrl;
  }

  /**
   * @return the database username to connect as when loading data, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  public String getDatabaseUsername() {
    return databaseUsername;
  }

  /**
   * @return the database password to connect with when loading data, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  public char[] getDatabasePassword() {
    return databasePassword;
  }

  /**
   * @return a {@link DataSource} for the database to connect to when loading data, or <code>null
   *     </code> if {@link #getDatabaseUrl()} is used, instead
   */
  public DataSource getDatabaseDataSource() {
    return databaseDataSource;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DatabaseOptions [databaseUrl=");
    builder.append(databaseUrl);
    builder.append(", databaseUsername=");
    builder.append("***");
    builder.append(", databasePassword=");
    builder.append("***");
    builder.append(", databaseDataSource=");
    builder.append("***");
    builder.append("]");
    return builder.toString();
  }
}
