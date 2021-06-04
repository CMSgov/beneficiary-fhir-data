package gov.cms.bfd.pipeline.sharedutils;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import org.hibernate.tool.schema.Action;

/** Database related utility methods used by multiple pipeline jobs. */
public final class DatabaseUtils {

  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd";
  public static final String RDA_PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda-only";

  /*
   * The number of JDBC statements that will be queued/batched within a
   * single transaction. Most recommendations suggest this should be 5-30.
   * Paradoxically, setting it higher seems to actually slow things down.
   * Presumably, it's delaying work that could be done earlier in a batch,
   * and that starts to cost more than the extra network roundtrips.
   */
  public static final int JDBC_BATCH_SIZE = 10;

  /**
   * Create a new DataSource.
   *
   * @param options the {@link DatabaseOptions} to use
   * @param metrics the {@link MetricRegistry} to use
   * @param maximumPoolSize the maximum database bool size to use
   * @return a {@link HikariDataSource} for the BFD database
   */
  public static HikariDataSource createDataSource(
      DatabaseOptions options, MetricRegistry metrics, int maximumPoolSize) {
    HikariDataSource dataSource = new HikariDataSource();

    dataSource.setMaximumPoolSize(maximumPoolSize);

    if (options.getDatabaseDataSource() != null) {
      dataSource.setDataSource(options.getDatabaseDataSource());
    } else {
      dataSource.setJdbcUrl(options.getDatabaseUrl());
      dataSource.setUsername(options.getDatabaseUsername());
      dataSource.setPassword(String.valueOf(options.getDatabasePassword()));
    }

    dataSource.setRegisterMbeans(true);
    dataSource.setMetricRegistry(metrics);

    return dataSource;
  }

  /**
   * Create an EntityManagerFactory using the provided DataSource.
   *
   * @param jdbcDataSource the JDBC {@link DataSource} for the Blue Button API backend database
   * @return a JPA {@link EntityManagerFactory} for the Blue Button API backend database
   */
  public static EntityManagerFactory createEntityManagerFactory(DataSource jdbcDataSource) {
    return createEntityManagerFactory(jdbcDataSource, PERSISTENCE_UNIT_NAME);
  }

  /**
   * Create an EntityManagerFactory using the provided DataSource.
   *
   * @param jdbcDataSource the JDBC {@link DataSource} for the Blue Button API backend database
   * @return a JPA {@link EntityManagerFactory} for the Blue Button API backend database
   */
  public static EntityManagerFactory createEntityManagerFactory(
      DataSource jdbcDataSource, String persistenceUnitName) {
    Map<String, Object> hibernateProperties = new HashMap<>();
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.DATASOURCE, jdbcDataSource);
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.VALIDATE);
    hibernateProperties.put(
        org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, JDBC_BATCH_SIZE);

    EntityManagerFactory entityManagerFactory =
        Persistence.createEntityManagerFactory(persistenceUnitName, hibernateProperties);
    return entityManagerFactory;
  }
}
