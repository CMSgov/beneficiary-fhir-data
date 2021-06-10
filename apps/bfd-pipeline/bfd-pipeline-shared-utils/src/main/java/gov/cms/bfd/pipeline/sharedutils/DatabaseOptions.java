package gov.cms.bfd.pipeline.sharedutils;

import com.zaxxer.hikari.HikariDataSource;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.sql.DataSource;

/**
 * Immutable object to hold database configuration information. Can be initialized either using
 * connection parameters (url, uid, password) or an already configured DataSource object. Its
 * primary function is to initialize an HikariDataSource object so there is no need for it to
 * publish the value of its properties.
 */
public class DatabaseOptions implements Serializable {
  private final String databaseUrl;
  private final String databaseUsername;
  private final String databasePassword;
  private final DataSource databaseDataSource;

  public DatabaseOptions(DataSource dataSource) {
    this(null, null, null, dataSource);
  }

  public DatabaseOptions(String databaseUrl, String databaseUsername, String databasePassword) {
    this(databaseUrl, databaseUsername, databasePassword, null);
  }

  private DatabaseOptions(
      String databaseUrl, String databaseUsername, String databasePassword, DataSource dataSource) {
    this.databaseUrl = databaseUrl;
    this.databaseUsername = databaseUsername;
    this.databasePassword = databasePassword;
    this.databaseDataSource = dataSource;
  }

  public void initializeHikariDataSource(HikariDataSource dataSource) {
    if (this.databaseDataSource != null) {
      dataSource.setDataSource(this.databaseDataSource);
    } else {
      dataSource.setJdbcUrl(databaseUrl);
      dataSource.setUsername(databaseUsername);
      dataSource.setPassword(databasePassword);
    }
  }

  @Nullable
  public DataSource getDatabaseDataSource() {
    return databaseDataSource;
  }

  @Nullable
  public String getDatabaseUrl() {
    return databaseUrl;
  }

  @Nullable
  public String getDatabaseUsername() {
    return databaseUsername;
  }

  @Nullable
  public String getDatabasePassword() {
    return databasePassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DatabaseOptions)) {
      return false;
    }
    DatabaseOptions that = (DatabaseOptions) o;
    return Objects.equals(databaseUrl, that.databaseUrl)
        && Objects.equals(databaseUsername, that.databaseUsername)
        && Objects.equals(databasePassword, that.databasePassword)
        && Objects.equals(databaseDataSource, that.databaseDataSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(databaseUrl, databaseUsername, databasePassword, databaseDataSource);
  }

  /**
   * Includes just enough information to tell how the object was configured but not enough to
   * accidentally log sensitive information.
   *
   * @return a string representation of how the object was configured
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("[");
    if (databaseDataSource != null) {
      sb.append("dataSource");
    } else {
      sb.append("databaseUrl=");
      sb.append(databaseUrl);
      sb.append(",databaseUsername=***,databasePassword=***");
    }
    sb.append("]");
    return sb.toString();
  }
}
