package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import software.amazon.jdbc.HikariPooledConnectionProvider;
import software.amazon.jdbc.PropertyDefinition;
import software.amazon.jdbc.plugin.AuroraInitialConnectionStrategyPlugin;
import software.amazon.jdbc.plugin.readwritesplitting.ReadWriteSplittingPlugin;
import software.amazon.jdbc.profile.DriverConfigurationProfiles;

/** Unit tests for {@link AwsWrapperDataSourceFactory}. */
public class AwsWrapperDataSourceFactoryTest {

  /**
   * Verify that a valid {@link software.amazon.jdbc.ds.AwsWrapperDataSource} is created when valid
   * {@link DatabaseOptions} are provided.
   */
  @Test
  void verifyCreatesValidDataSourceGivenValidOptions() {
    // Arrange
    var dbOptions =
        DatabaseOptions.builder()
            .authenticationType(DatabaseOptions.AuthenticationType.JDBC)
            .databaseUrl("jdbc:postgres://host-name:111/")
            .databaseUsername("user")
            .databasePassword("pass")
            .hikariOptions(DatabaseOptions.HikariOptions.builder().maximumPoolSize(10).build())
            .awsJdbcWrapperOptions(
                DatabaseOptions.AwsJdbcWrapperOptions.builder()
                    .basePresetCode("E")
                    .useCustomPreset(true)
                    .pluginsCsv("efm2,failover")
                    .build())
            .build();
    var factory = new AwsWrapperDataSourceFactory(dbOptions);

    // Act
    var dataSource = factory.createDataSource(null, null);

    // Assert
    var dataSourceProps = dataSource.getTargetDataSourceProperties();
    var customProfile =
        DriverConfigurationProfiles.getProfileConfiguration(
            AwsWrapperDataSourceFactory.CUSTOM_PRESET_NAME);
    assertEquals(dataSource.getJdbcUrl(), dbOptions.getDatabaseUrl());
    assertEquals(dataSource.getUser(), dbOptions.getDatabaseUsername());
    assertEquals(dataSource.getPassword(), dbOptions.getDatabasePassword());
    assertEquals(
        dataSourceProps.get(PropertyDefinition.PROFILE_NAME.name),
        AwsWrapperDataSourceFactory.CUSTOM_PRESET_NAME);
    assertEquals(
        dataSourceProps.get(
            AuroraInitialConnectionStrategyPlugin.READER_HOST_SELECTOR_STRATEGY.name),
        dbOptions.getAwsJdbcWrapperOptions().getHostSelectionStrategy());
    assertEquals(
        dataSourceProps.get(ReadWriteSplittingPlugin.READER_HOST_SELECTOR_STRATEGY.name),
        dbOptions.getAwsJdbcWrapperOptions().getInitialConnectionStrategy());
    assertInstanceOf(HikariPooledConnectionProvider.class, customProfile.getConnectionProvider());
  }
}
