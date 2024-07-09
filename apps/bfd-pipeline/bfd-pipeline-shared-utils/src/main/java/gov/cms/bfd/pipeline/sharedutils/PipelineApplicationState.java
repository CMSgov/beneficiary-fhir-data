package gov.cms.bfd.pipeline.sharedutils;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.Getter;
import org.hibernate.tool.schema.Action;

/**
 * Stores the shared state needed by the Pipeline application. Note that it implements {@link
 * AutoCloseable} as it owns expensive resources that need to be closed, e.g. {@link
 * #pooledDataSource}.
 */
@Getter
public final class PipelineApplicationState implements AutoCloseable {
  /** The persistence unit name for the adjudicated pipeline. */
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd";

  /** The persistence unit name for the RDA (pre-adjudicated) pipeline. */
  public static final String RDA_PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  /** Registry for metering. */
  private final MeterRegistry meters;

  /** Registry for metrics. */
  private final MetricRegistry metrics;

  /** The pooled data source for communicating with the database. */
  private final HikariDataSource pooledDataSource;

  /** Factory for the entity manager, used in persistence. */
  private final EntityManagerFactory entityManagerFactory;

  /** Clock instance for timekeeping. */
  private final Clock clock;

  /**
   * Constructs a new {@link PipelineApplicationState} instance using a pre-existing pooled data
   * DataSource. This is the standard constructor used by PipelineApplication.
   *
   * @param meters the meters
   * @param metrics the value to use for {@link #metrics}
   * @param pooledDataSource the value to use for {@link #pooledDataSource}
   * @param persistenceUnitName the persistence unit name
   * @param clock the clock
   */
  public PipelineApplicationState(
      MeterRegistry meters,
      MetricRegistry metrics,
      HikariDataSource pooledDataSource,
      String persistenceUnitName,
      Clock clock) {
    this(
        meters,
        metrics,
        pooledDataSource,
        createEntityManagerFactory(pooledDataSource, persistenceUnitName),
        clock);
  }

  /**
   * Constructs a new {@link PipelineApplicationState} instance using pre-existing DataSource and
   * EntityManagerFactory. This constructor is intended for use by other constructors and specific
   * unit tests.
   *
   * @param meters the meters
   * @param metrics the value to use for {@link #metrics}
   * @param pooledDataSource the value to use for {@link #pooledDataSource}
   * @param entityManagerFactory the value to use for {@link #entityManagerFactory}
   * @param clock the clock
   */
  @VisibleForTesting
  public PipelineApplicationState(
      MeterRegistry meters,
      MetricRegistry metrics,
      HikariDataSource pooledDataSource,
      EntityManagerFactory entityManagerFactory,
      Clock clock) {
    this.meters = meters;
    this.metrics = metrics;
    this.pooledDataSource = pooledDataSource;
    this.entityManagerFactory = entityManagerFactory;
    this.clock = clock;
  }

  /**
   * Create pooled data source used to communicate with the database.
   *
   * @param dataSourceFactory the {@link HikariDataSourceFactory} to use for the application's DB
   *     (which this will use to create {@link #pooledDataSource})
   * @param metrics the {@link MetricRegistry} to use
   * @return a {@link HikariDataSource} for the BFD database
   */
  public static HikariDataSource createPooledDataSource(
      HikariDataSourceFactory dataSourceFactory, MetricRegistry metrics) {
    HikariDataSource pooledDataSource = dataSourceFactory.createDataSource();
    pooledDataSource.setMetricRegistry(metrics);

    // In order to store and retrieve JSON in postgresql without adding any additional maven
    // dependencies  we can set this property to allow String values to be transparently
    // converted to/from jsonb values.
    Properties dataSourceProperties = new Properties();
    dataSourceProperties.setProperty("stringtype", "unspecified");
    pooledDataSource.setDataSourceProperties(dataSourceProperties);

    return pooledDataSource;
  }

  /**
   * Creates an entity manager factory.
   *
   * @param pooledDataSource the JDBC {@link DataSource} for the application's database
   * @param persistenceUnitName the persistence unit name
   * @return a JPA {@link EntityManagerFactory} for the application's database
   */
  private static EntityManagerFactory createEntityManagerFactory(
      DataSource pooledDataSource, String persistenceUnitName) {
    /*
     * The number of JDBC statements that will be queued/batched within a single transaction. Most
     * recommendations suggest this should be 5-30. Paradoxically, setting it higher seems to
     * actually slow things down. Presumably, it's delaying work that could be done earlier in a
     * batch, and that starts to cost more than the extra network roundtrips.
     */
    final int jdbcBatchSize = 10;

    Map<String, Object> hibernateProperties = new HashMap<>();
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.DATASOURCE, pooledDataSource);
    /*
     * Hibernate validation is done in the validator app, so leave HBM2DDL_AUTO disabled here.
     * This defaults to NONE, but just explicitly noting this.
     */
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.NONE);
    hibernateProperties.put(
        org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, jdbcBatchSize);

    EntityManagerFactory entityManagerFactory =
        Persistence.createEntityManagerFactory(persistenceUnitName, hibernateProperties);
    return entityManagerFactory;
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    if (entityManagerFactory.isOpen()) {
      entityManagerFactory.close();
    }
    if (!pooledDataSource.isClosed()) {
      pooledDataSource.close();
    }
  }
}
