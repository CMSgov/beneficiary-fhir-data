package gov.cms.bfd.sharedutils.database;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.jdbc.HikariPooledConnectionProvider;
import software.amazon.jdbc.HostSpec;
import software.amazon.jdbc.ds.AwsWrapperDataSource;
import software.amazon.jdbc.profile.ConfigurationProfileBuilder;
import software.amazon.jdbc.profile.ConfigurationProfilePresetCodes;

/**
 * Implementation of {@link DataSourceFactory} that creates instances of {@link HikariDataSource}
 * objects that use temporary auth tokens requested from RDS instead of a fixed password. {@see
 * https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html}.
 */
public class RdsAwsWrapperDataSourceFactory implements SimpleDataSourceFactory {
    /**
     * A short delay between token requests so that constructing a new thread pool doesn't need to
     * trigger hundreds of token requests at the same time. According to the AWS documentation a token
     * is valid for 15 minutes but we can fetch a new one more frequently just to be safe.
     */
    static final long DEFAULT_TOKEN_TTL_MILLIS = Duration.ofMinutes(5).toMillis();

    /**
     * Configuration settings for {@link RdsClient}.
     */
    @Getter
    private final AwsClientConfig awsClientConfig;

    /**
     * Used to configure {@link RdsHikariDataSource} instances.
     */
    @Getter(AccessLevel.PACKAGE)
    @VisibleForTesting
    private final RdsHikariDataSource.Config dataSourceConfig;

    /**
     * Testing.
     */
    @Getter
    private final DatabaseOptions databaseOptions;

    /**
     * Initializes an instance. The {@link DatabaseOptions#authenticationType} must be set to {@link
     * DatabaseOptions.AuthenticationType#RDS}.
     *
     * @param clock           optional, used to get current time
     * @param tokenTtlMillis  optional, minimum millis between token requests
     * @param awsClientConfig used to config {@link RdsClient} instances
     * @param databaseOptions used to configure {@link RdsHikariDataSource} instances
     */
    @Builder
    private RdsAwsWrapperDataSourceFactory(
            @Nullable Clock clock,
            @Nullable Long tokenTtlMillis,
            @Nonnull AwsClientConfig awsClientConfig,
            @Nonnull DatabaseOptions databaseOptions) {
        //        super(databaseOptions);
        var configBuilder = RdsHikariDataSource.Config.builder();
        configBuilder.clock(clock == null ? Clock.systemUTC() : clock);
        configBuilder.tokenTtlMillis(
                tokenTtlMillis == null ? DEFAULT_TOKEN_TTL_MILLIS : tokenTtlMillis);
        if (databaseOptions.getAuthenticationType() != DatabaseOptions.AuthenticationType.RDS) {
            throw reportInvalidDatabaseOptions();
        }
        configBuilder.databaseUser(databaseOptions.getDatabaseUsername());
        configBuilder.databaseHost(
                databaseOptions
                        .getDatabaseHost()
                        .orElseThrow(RdsDataSourceFactory::reportInvalidDatabaseOptions));
        configBuilder.databasePort(
                databaseOptions
                        .getDatabasePort()
                        .orElseThrow(RdsDataSourceFactory::reportInvalidDatabaseOptions));
        this.awsClientConfig = awsClientConfig;
        this.databaseOptions = databaseOptions;
        dataSourceConfig = configBuilder.build();
    }

    /**
     * Constructs a {@link RdsHikariDataSource} instance.
     *
     * @return the data source
     */
    public AwsWrapperDataSource createDataSource() {
        RdsClientBuilder rdsClientBuilder = createRdsClientBuilder();
        awsClientConfig.configureAwsService(rdsClientBuilder);
        rdsClientBuilder.credentialsProvider(DefaultCredentialsProvider.create());
        var rdsClient = rdsClientBuilder.build();

        ConfigurationProfileBuilder.get()
                .from(ConfigurationProfilePresetCodes.E)
                .withName("datasource-with-internal-connection-pool")
                .withConnectionProvider(
                        new HikariPooledConnectionProvider(
                                (HostSpec hostSpec, Properties originalProps) -> {
                                    final HikariConfig config = new HikariConfig();
                                    config.setMaximumPoolSize(databaseOptions.getMaxPoolSize());
                                    // holds few extra connections in case of sudden traffic peak
                                    config.setMinimumIdle(Math.max(2, Runtime.getRuntime().availableProcessors()));
                                    //                        // close idle connection in 15min; helps to get back to
                                    // normal pool size after load peak
                                    //                        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(15));
                                    //                        // verify pool configuration and creates no connections
                                    // during initialization phase
                                    //                        config.setInitializationFailTimeout(-1);
                                    //
                                    // config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
                                    //                        // validate idle connections at least every 3 min
                                    //                        config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(3));
                                    //                        // allows to quickly validate connection in the pool and
                                    // move on to another connection if needed
                                    //
                                    // config.setValidationTimeout(TimeUnit.SECONDS.toMillis(1));
                                    //                        config.setMaxLifetime(TimeUnit.DAYS.toMillis(1));
                                    return config;
                                },
                                null))
                .buildAndSet();

        // the dataSource will take care of closing this when its close method is called
        AwsWrapperDataSource dataSource = createRdsAwsWrapperDataSource(rdsClient);
        dataSource.setJdbcUrl(databaseOptions.getDatabaseUrl());
        dataSource.setUser(databaseOptions.getDatabaseUsername());
        dataSource.setPassword(databaseOptions.getDatabasePassword());
        Properties targetDataSourceProps = new Properties();
        targetDataSourceProps.setProperty(
                "wrapperProfileName", "datasource-with-internal-connection-pool");
        targetDataSourceProps.setProperty("readerHostSelectorStrategy", "leastConnections");
        dataSource.setTargetDataSourceProperties(targetDataSourceProps);
        return dataSource;
    }

    /**
     * Creates a {@link RdsClientBuilder}.
     *
     * @return the builder
     */
    @VisibleForTesting
    RdsClientBuilder createRdsClientBuilder() {
        return RdsClient.builder();
    }

    /**
     * Creates a {@link RdsHikariDataSource} using the provided {@link RdsClient}.
     *
     * @param rdsClient used to call RDS API
     * @return the data source
     */
    @VisibleForTesting
    RdsAwsWrapperDataSource createRdsAwsWrapperDataSource(RdsClient rdsClient) {
        return new RdsAwsWrapperDataSource(dataSourceConfig, rdsClient);
    }

    /**
     * Creates an exception with appropriate message to indicate our {@link DatabaseOptions} was
     * invalid. Defined as a method since we need to generate a message in multiple places and to make
     * it easy to use with {@link Optional#orElse}.
     *
     * @return the exception
     */
    static IllegalArgumentException reportInvalidDatabaseOptions() {
        return new IllegalArgumentException(
                "RDS Authentication must be enabled and a valid JDBC URL must include host and port");
    }
}
