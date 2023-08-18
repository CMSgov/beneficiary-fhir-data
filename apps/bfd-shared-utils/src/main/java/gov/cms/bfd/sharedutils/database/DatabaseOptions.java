package gov.cms.bfd.sharedutils.database;

import java.util.Optional;

/** The user-configurable options that specify how to access the application's database. */
public final class DatabaseOptions {

  /** The JDBC URL of the database. */
  private final String databaseUrl;
  /** The username for the database. */
  private final String databaseUsername;
  /** The password for the database. */
  private final String databasePassword;
  /** The maximum size of the database connection pool. */
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

  /**
   * Gets the {@link #databaseUrl}.
   *
   * @return the JDBC URL of the database to load into
   */
  public String getDatabaseUrl() {
    return databaseUrl;
  }

  /**
   * Gets the {@link #databaseUsername}.
   *
   * @return the database username to connect as when loading data
   */
  public String getDatabaseUsername() {
    return databaseUsername;
  }

  /**
   * Gets the {@link #databasePassword}.
   *
   * @return the database password to connect with when loading data
   */
  public String getDatabasePassword() {
    return databasePassword;
  }

  /**
   * Gets the {@link #maxPoolSize}.
   *
   * @return the maximum size of the DB connection pool that the application will create
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public Optional<String> getDatabaseHost() {
    return getHostPortFromUrl()
        .map(
            hostAndPort -> {
              int separatorOffset = hostAndPort.indexOf(':');
              return separatorOffset < 0 ? hostAndPort : hostAndPort.substring(0, separatorOffset);
            });
  }

  public Optional<Integer> getDatabasePort() {
    return getHostPortFromUrl()
        .flatMap(
            hostAndPort -> {
              int separatorOffset = hostAndPort.indexOf(':');
              return separatorOffset > 0
                  ? Optional.of(Integer.parseInt(hostAndPort.substring(separatorOffset + 1)))
                  : Optional.empty();
            });
  }

  private Optional<String> getHostPortFromUrl() {
    int hostStartOffset = databaseUrl.indexOf("//");
    if (hostStartOffset < 0) {
      return Optional.empty();
    }
    hostStartOffset += 2;

    int hostEndOffset = databaseUrl.indexOf("/", hostStartOffset);
    if (hostEndOffset < 0) {
      return Optional.empty();
    }

    return Optional.of(databaseUrl.substring(hostStartOffset, hostEndOffset));
  }

  /** {@inheritDoc} */
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
