package gov.cms.bfd.sharedutils.database;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import lombok.AllArgsConstructor;
import software.amazon.jdbc.HikariPooledConnectionProvider;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.ds.AwsWrapperDataSource;
import software.amazon.jdbc.plugin.AuroraConnectionTrackerPlugin;
import software.amazon.jdbc.plugin.AuroraConnectionTrackerPluginFactory;
import software.amazon.jdbc.plugin.AuroraInitialConnectionStrategyPlugin;
import software.amazon.jdbc.plugin.AuroraInitialConnectionStrategyPluginFactory;
import software.amazon.jdbc.plugin.failover.FailoverConnectionPluginFactory;
import software.amazon.jdbc.plugin.readwritesplitting.ReadWriteSplittingPluginFactory;
import software.amazon.jdbc.profile.ConfigurationProfileBuilder;
import software.amazon.jdbc.profile.ConfigurationProfilePresetCodes;

/**
 * Testing.
 */
public class AwsWrapperDataSourceFactory implements SimpleDataSourceFactory {

    /**
     * Testing.
     */
    private static final String CUSTOM_PRESET_NAME = "datasource-with-internal-connection-pool";
    /**
     * Testing.
     */
    private final DatabaseOptions databaseOptions;

    /**
     * Testing.
     *
     * @param databaseOptions testing.
     */
    public AwsWrapperDataSourceFactory(DatabaseOptions databaseOptions) {
        this.databaseOptions = databaseOptions;

        ConfigurationProfileBuilder.get().from(ConfigurationProfilePresetCodes.E)
                .withName(CUSTOM_PRESET_NAME)
                .withPluginFactories(Arrays.asList(
                        AuroraInitialConnectionStrategyPluginFactory.class,
                        AuroraConnectionTrackerPluginFactory.class,
                        FailoverConnectionPluginFactory.class,
                        ReadWriteSplittingPluginFactory.class
                ))
                .withConnectionProvider(new HikariPooledConnectionProvider(
                        (HostSpec hostSpec, Properties originalProps) -> {
                            final HikariConfig config = new HikariConfig();
                            config.setMaximumPoolSize(Math.max(Runtime.getRuntime().availableProcessors(), databaseOptions.getMaxPoolSize()));

                            // The following lines are copied verbatim from the default configuration of profile E:

                            // holds few extra connections in case of sudden traffic peak
                            config.setMinimumIdle(2);
                            // close idle connection in 15min; helps to get back to normal pool size after load peak
                            config.setIdleTimeout(TimeUnit.MINUTES.toMillis(15));
                            // verify pool configuration and creates no connections during initialization phase
                            config.setInitializationFailTimeout(-1);
                            config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
                            // validate idle connections at least every 3 min
                            config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(3));
                            // allows to quickly validate connection in the pool and move on to another connection if needed
                            config.setValidationTimeout(TimeUnit.SECONDS.toMillis(1));
                            config.setMaxLifetime(TimeUnit.DAYS.toMillis(1));
                            return config;
                        }
                ))
                .buildAndSet();
    }

    @Override
    public DataSource createDataSource() {
        AwsWrapperDataSource dataSource = new AwsWrapperDataSource();
        dataSource.setJdbcUrl(databaseOptions.getDatabaseUrl());
        dataSource.setUser(databaseOptions.getDatabaseUsername());
        dataSource.setPassword(databaseOptions.getDatabasePassword());
        Properties targetDataSourceProps = new Properties();
        targetDataSourceProps.setProperty("wrapperProfileName", CUSTOM_PRESET_NAME);
        targetDataSourceProps.setProperty(AuroraInitialConnectionStrategyPlugin.READER_HOST_SELECTOR_STRATEGY.name, "roundRobin");
        dataSource.setTargetDataSourceProperties(targetDataSourceProps);
        return dataSource;
    }

}
