package gov.cms.bfd.pipeline.sharedutils;

import java.io.Serializable;

/** The user-configurable options that specify how to access the application's database. */
public final class DatabaseOptions implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String databaseUrl;
  private final String databaseUsername;
  private final String databasePassword;
  private final int maxPoolSize;

  /**
   * Constructs a new {@link DatabaseOptions} instance.
   *
   * @param databaseUrl the value to use for {@link #getDatabaseUrl()}
   * @param databaseUsername the value to use for {@link #getDatabaseUsername()}
   * @param databasePassword the value to use for {@link #getDatabasePassword()}
   * @param maxPoolSize the value to use for {@link #getMaxPoolSize()}
   */
  public DatabaseOptions(
      String databaseUrl, String databaseUsername, String databasePassword, int maxPoolSize) {
    this.databaseUrl = databaseUrl;
    this.databaseUsername = databaseUsername;
    this.databasePassword = databasePassword;
    this.maxPoolSize = maxPoolSize;
  }

  /** @return the JDBC URL of the database to load into */
  public String getDatabaseUrl() {
    return databaseUrl;
  }

  /** @return the database username to connect as when loading data */
  public String getDatabaseUsername() {
    return databaseUsername;
  }

  /** @return the database password to connect with when loading data */
  public String getDatabasePassword() {
    return databasePassword;
  }

  /** @return the maximum size of the DB connection pool that the application will create */
  public int getMaxPoolSize() {
    return maxPoolSize;
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
    builder.append(", maxPoolSize=");
    builder.append(maxPoolSize);
    builder.append("]");
    return builder.toString();
  }
}
