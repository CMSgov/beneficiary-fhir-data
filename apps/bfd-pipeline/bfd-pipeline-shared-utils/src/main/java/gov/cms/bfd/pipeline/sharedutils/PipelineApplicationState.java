package gov.cms.bfd.pipeline.sharedutils;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import org.hibernate.tool.schema.Action;

/**
 * Stores the shared state needed by the Pipeline application. Note that it implements {@link
 * AutoCloseable} as it owns expensive resources that need to be closed, e.g. {@link
 * #getPooledDataSource()}.
 */
public final class PipelineApplicationState implements AutoCloseable {
  private final MetricRegistry metrics;
  private final HikariDataSource pooledDataSource;
  private final EntityManagerFactory entityManagerFactory;

  /**
   * Constructs a new {@link PipelineApplicationState} instance.
   *
   * @param metrics the value to use for {@link #getMetrics()}
   * @param dbOptions the {@link DatabaseOptions} for the application's DB (which this will use to
   *     create {@link #getPooledDataSource()})
   */
  public PipelineApplicationState(MetricRegistry metrics, DatabaseOptions dbOptions) {
    this.metrics = metrics;
    this.pooledDataSource = createPooledDataSource(dbOptions, metrics);
    this.entityManagerFactory = createEntityManagerFactory(pooledDataSource);
  }

  /**
   * Constructs a new {@link PipelineApplicationState} instance.
   *
   * @param metrics the value to use for {@link #getMetrics()}
   * @param dataSource the {@link DatabaseOptions} for the application's DB (which this will use to
   *     create {@link #getPooledDataSource()})
   * @param maxPoolSize the {@link DatabaseOptions#getMaxPoolSize()} value to use
   */
  public PipelineApplicationState(MetricRegistry metrics, DataSource dataSource, int maxPoolSize) {
    this.metrics = metrics;
    this.pooledDataSource = createPooledDataSource(dataSource, maxPoolSize, metrics);
    this.entityManagerFactory = createEntityManagerFactory(pooledDataSource);
  }

  /**
   * Constructs a new {@link PipelineApplicationState} instance.
   *
   * @param metrics the value to use for {@link #getMetrics()}
   * @param pooledDataSource the value to use for {@link #getPooledDataSource()}
   * @param entityManagerFactory the value to use for {@link #getEntityManagerFactory()}
   */
  public PipelineApplicationState(
      MetricRegistry metrics,
      HikariDataSource pooledDataSource,
      EntityManagerFactory entityManagerFactory) {
    this.metrics = metrics;
    this.pooledDataSource = pooledDataSource;
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * @param dbOptions the {@link DatabaseOptions} to use for the application's DB (which this will
   *     use to create {@link #getPooledDataSource()})
   * @param metrics the {@link MetricRegistry} to use
   * @return a {@link HikariDataSource} for the BFD database
   */
  private static HikariDataSource createPooledDataSource(
      DatabaseOptions dbOptions, MetricRegistry metrics) {
    HikariDataSource pooledDataSource = new HikariDataSource();

    pooledDataSource.setJdbcUrl(dbOptions.getDatabaseUrl());
    pooledDataSource.setUsername(dbOptions.getDatabaseUsername());
    pooledDataSource.setPassword(dbOptions.getDatabasePassword());
    pooledDataSource.setMaximumPoolSize(dbOptions.getMaxPoolSize());
    pooledDataSource.setRegisterMbeans(true);
    pooledDataSource.setMetricRegistry(metrics);

    return pooledDataSource;
  }

  /**
   * @param unpooledDataSource a non-pooled {@link DataSource} for the application's DB (which this
   *     will use to create {@link #getPooledDataSource()})
   * @param maxPoolSize the {@link DatabaseOptions#getMaxPoolSize()} value to use
   * @param metrics the {@link MetricRegistry} to use
   * @return a {@link HikariDataSource} for the BFD database
   */
  private static HikariDataSource createPooledDataSource(
      DataSource unpooledDataSource, int maxPoolSize, MetricRegistry metrics) {
    HikariDataSource pooledDataSource = new HikariDataSource();

    pooledDataSource.setDataSource(unpooledDataSource);
    pooledDataSource.setMaximumPoolSize(maxPoolSize);
    pooledDataSource.setRegisterMbeans(true);
    pooledDataSource.setMetricRegistry(metrics);

    return pooledDataSource;
  }

  /**
   * @param pooledDataSource the JDBC {@link DataSource} for the application's database
   * @return a JPA {@link EntityManagerFactory} for the application's database
   */
  private static EntityManagerFactory createEntityManagerFactory(DataSource pooledDataSource) {
    /*
     * The number of JDBC statements that will be queued/batched within a single transaction. Most
     * recommendations suggest this should be 5-30. Paradoxically, setting it higher seems to
     * actually slow things down. Presumably, it's delaying work that could be done earlier in a
     * batch, and that starts to cost more than the extra network roundtrips.
     */
    final int jdbcBatchSize = 10;

    Map<String, Object> hibernateProperties = new HashMap<>();
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.DATASOURCE, pooledDataSource);
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.VALIDATE);
    hibernateProperties.put(
        org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, jdbcBatchSize);

    EntityManagerFactory entityManagerFactory =
        Persistence.createEntityManagerFactory("gov.cms.bfd", hibernateProperties);
    return entityManagerFactory;
  }

  /** @return the {@link MetricRegistry} for the application */
  public MetricRegistry getMetrics() {
    return metrics;
  }

  /** @return the {@link HikariDataSource} that holds the application's database connection pool */
  public HikariDataSource getPooledDataSource() {
    return pooledDataSource;
  }

  /** @return the {@link EntityManagerFactory} for the application */
  public EntityManagerFactory getEntityManagerFactory() {
    return entityManagerFactory;
  }

  /** @see java.lang.AutoCloseable#close() */
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
