package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Preconditions;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import lombok.Getter;

/**
 * Models the common configuration options for BFD applications, should be extended by a specific
 * application.
 */
public abstract class BaseAppConfiguration extends BaseConfiguration {

  /** Holds the configured options for the database connection. */
  @Getter private final DatabaseOptions databaseOptions;

  /** Common configuration settings for all AWS clients. * */
  @Getter private final AwsClientConfig awsClientConfig;

  /**
   * Initializes an instance.
   *
   * @param databaseOptions the value to use for {@link #databaseOptions} flyway looks for migration
   *     scripts
   * @param awsClientConfig common configuration settings for all AWS clients
   */
  protected BaseAppConfiguration(DatabaseOptions databaseOptions, AwsClientConfig awsClientConfig) {
    this.databaseOptions = Preconditions.checkNotNull(databaseOptions);
    this.awsClientConfig = Preconditions.checkNotNull(awsClientConfig);
  }

  /**
   * Creates appropriate {@link DataSourceFactory} based on our {@link DatabaseOptions}.
   *
   * @return factory for creating data sources
   */
  public DataSourceFactory createDataSourceFactory() {
    return createDataSourceFactory(databaseOptions, awsClientConfig);
  }

  /**
   * Creates appropriate {@link HikariDataSourceFactory} based on our {@link DatabaseOptions}.
   *
   * @return factory for creating data sources
   */
  public HikariDataSourceFactory createHikariDataSourceFactory() {
    // TODO: Introduced as an escape hatch for BFD applications relying on being provided a
    // HikariDataSource or subclass thereof. This method should be removed when those
    // applications more properly support generic DataSources
    return createHikariDataSourceFactory(databaseOptions, awsClientConfig);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(", databaseOptions=");
    builder.append(databaseOptions);
    builder.append(", awsClientConfig=");
    builder.append(awsClientConfig);
    return builder.toString();
  }
}
