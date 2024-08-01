package gov.cms.bfd.pipeline;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.entities.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseSchemaManager;
import gov.cms.bfd.sharedutils.database.DatabaseUtils;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utilities that are useful when testing with or against the BFD Pipeline application.
 *
 * <p>This is being left in <code>src/main</code> so that it can be used from other modules' tests,
 * without having to delve into classpath dark arts.
 */
public final class PipelineTestUtils {
  /**
   * A reasonable (though not terribly performant) suggested default value for {@link
   * DatabaseOptions.HikariOptions#maximumPoolSize}. Effectively, it's double the default value that
   * will be used in tests for <code>LoadAppOptions#getLoaderThreads()</code>.
   */
  private static final int DEFAULT_MAX_POOL_SIZE =
      Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2 * 2;

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineTestUtils.class);

  /** The singleton {@link PipelineTestUtils} instance to use everywhere. */
  private static PipelineTestUtils SINGLETON;

  /**
   * The {@link PipelineApplicationState} that should be used across all of the tests, which most
   * notably contains the {@link HikariDataSource} and {@link EntityManagerFactory} to use.
   */
  private final PipelineApplicationState pipelineApplicationState;

  /**
   * Constructs a new {@link PipelineTestUtils} instance. Marked <code>private</code>; use {@link
   * #get()}, instead.
   */
  private PipelineTestUtils() {
    MetricRegistry testMetrics = new MetricRegistry();
    DatabaseSchemaManager.createOrUpdateSchema(DatabaseTestUtils.get().getUnpooledDataSource());

    // Create a testing specific pooled data source from the existing unpooled data source
    HikariDataSource pooledDataSource = new HikariDataSource();

    pooledDataSource.setDataSource(DatabaseTestUtils.get().getUnpooledDataSource());
    pooledDataSource.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
    pooledDataSource.setRegisterMbeans(true);
    pooledDataSource.setMetricRegistry(testMetrics);
    // By default, the pool would immediately open all allowed connections. This is excessive for
    // unit/IT testing, so we lower it to avoid exceeding max connections in postgresql.
    pooledDataSource.setMinimumIdle(3);
    pooledDataSource.setIdleTimeout(30_000);

    this.pipelineApplicationState =
        new PipelineApplicationState(
            new SimpleMeterRegistry(),
            testMetrics,
            pooledDataSource,
            PipelineApplicationState.PERSISTENCE_UNIT_NAME,
            Clock.systemUTC());
  }

  /**
   * Gets the singleton {@link PipelineTestUtils} instance to use everywhere.
   *
   * @return the instance
   */
  public static synchronized PipelineTestUtils get() {
    /*
     * Why are we using a singleton and caching all of these fields? Because creating some of the
     * fields stored in the PipelineApplicationState is EXPENSIVE (it maintains a DB connection
     * pool), so we don't want to have to re-create it for every test.
     */

    if (SINGLETON == null) {
      SINGLETON = new PipelineTestUtils();
    }

    return SINGLETON;
  }

  /**
   * Gets the {@link PipelineApplicationState} that should be used across all of the tests, which
   * most notably contains the {@link HikariDataSource} and {@link EntityManagerFactory} to use.
   *
   * @return the application state
   */
  public PipelineApplicationState getPipelineApplicationState() {
    return pipelineApplicationState;
  }

  /**
   * Runs a <code>TRUNCATE</code> for all tables in the {@link
   * DatabaseTestUtils#getUnpooledDataSource()} database.
   */
  public void truncateTablesInDataSource() {
    List<Class<?>> entityTypes =
        Arrays.asList(
            PartDEvent.class,
            SNFClaimLine.class,
            SNFClaim.class,
            OutpatientClaimLine.class,
            OutpatientClaim.class,
            InpatientClaimLine.class,
            InpatientClaim.class,
            HospiceClaimLine.class,
            HospiceClaim.class,
            HHAClaimLine.class,
            HHAClaim.class,
            DMEClaimLine.class,
            DMEClaim.class,
            CarrierClaimLine.class,
            CarrierClaim.class,
            BeneficiaryHistory.class,
            BeneficiaryMonthly.class,
            Beneficiary.class,
            LoadedBatch.class,
            LoadedFile.class,
            S3ManifestFile.class,
            S3DataFile.class,
            RdaFissClaim.class,
            RdaFissProcCode.class);

    try (Connection connection = pipelineApplicationState.getPooledDataSource().getConnection()) {
      // Disable auto-commit and remember the default schema name.
      connection.setAutoCommit(false);
      Optional<String> defaultSchemaName = Optional.ofNullable(connection.getSchema());
      if (defaultSchemaName.isEmpty()) {
        throw new BadCodeMonkeyException("Unable to determine default schema name.");
      }

      // Loop over every @Entity type.
      for (Class<?> entityType : entityTypes) {
        Optional<Table> entityTableAnnotation =
            Optional.ofNullable(entityType.getAnnotation(Table.class));

        // First, make sure we found an @Table annotation.
        if (entityTableAnnotation.isEmpty()) {
          throw new BadCodeMonkeyException(
              "Unable to determine table metadata for entity: " + entityType.getCanonicalName());
        }

        // Then, make sure we have a table name specified.
        if (entityTableAnnotation.get().name() == null
            || entityTableAnnotation.get().name().isEmpty()) {
          throw new BadCodeMonkeyException(
              "Unable to determine table name for entity: " + entityType.getCanonicalName());
        }
        String tableNameSpecifier = normalizeTableName(entityTableAnnotation.get().name());

        // Then, switch to the appropriate schema.
        if (entityTableAnnotation.get().schema() != null
            && !entityTableAnnotation.get().schema().isEmpty()) {
          String schemaNameSpecifier = normalizeSchemaName(entityTableAnnotation.get().schema());
          connection.setSchema(schemaNameSpecifier);
        } else {
          connection.setSchema(defaultSchemaName.get());
        }

        /*
         * Finally, run the TRUNCATE. On Postgres the cascade option is required due to the
         * presence of FK constraints.
         */
        String truncateTableSql = String.format("TRUNCATE TABLE %s", tableNameSpecifier);
        if (DatabaseUtils.isPostgresConnection(connection)) {
          truncateTableSql = truncateTableSql + " CASCADE";
        }
        try (Statement statement = connection.createStatement()) {
          statement.execute(truncateTableSql);
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
        connection.setSchema(defaultSchemaName.get());
      }
      connection.commit();
      LOGGER.info("Removed all application data from database.");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Normalize the schema names by removing any quotes.
   *
   * @param schemaNameSpecifier name of a schema from a hibernate annotation
   * @return value compatible with call to {@link Connection#setSchema(String)}
   * @throws SQLException the sql exception
   */
  private String normalizeSchemaName(String schemaNameSpecifier) throws SQLException {
    return schemaNameSpecifier.replaceAll("`", "");
  }

  /**
   * Normalize table names by removing quotes and uppercasing them.
   *
   * @param tableNameSpecifier name of a table from a hibernate annotation
   * @return value compatible with call to {@link java.sql.Statement#execute(String)}
   */
  private String normalizeTableName(String tableNameSpecifier) {
    if (tableNameSpecifier.startsWith("`")) {
      tableNameSpecifier = tableNameSpecifier.replaceAll("`", "");
    } else {
      tableNameSpecifier = tableNameSpecifier.toUpperCase();
    }
    return tableNameSpecifier;
  }

  /**
   * A wrapper for the entity manager logic and action. The consumer is called within a transaction
   * to which is rolled back.
   *
   * @param consumer to call with an entity manager.
   */
  public void doTestWithDb(BiConsumer<DataSource, EntityManager> consumer) {
    EntityManager entityManager = null;
    try {
      entityManager = pipelineApplicationState.getEntityManagerFactory().createEntityManager();
      consumer.accept(pipelineApplicationState.getPooledDataSource(), entityManager);
    } finally {
      if (entityManager != null && entityManager.isOpen()) {
        entityManager.close();
      }
    }
  }

  /**
   * Get the list of loaded files from the passed in db, latest first.
   *
   * @param entityManager to use
   * @return the list of loaded files in the db
   */
  public List<LoadedFile> findLoadedFiles(EntityManager entityManager) {
    entityManager.clear();
    return entityManager
        .createQuery("select f from LoadedFile f order by f.created desc", LoadedFile.class)
        .getResultList();
  }

  /**
   * Return a Files Event with a single dummy file.
   *
   * @return a new RifFilesEvent
   */
  public RifFilesEvent createDummyFilesEvent() {
    RifFile dummyFile =
        new RifFile() {

          @Override
          public InputStream open() {
            return null;
          }

          @Override
          public RifFileType getFileType() {
            return RifFileType.BENEFICIARY;
          }

          @Override
          public String getDisplayName() {
            return "Dummy.txt";
          }

          @Override
          public Charset getCharset() {
            return StandardCharsets.UTF_8;
          }
        };

    return new RifFilesEvent(Instant.now(), false, Arrays.asList(dummyFile));
  }

  /**
   * Pause of a number milliseconds.
   *
   * @param millis to sleap
   */
  public void pauseMillis(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
    }
  }
}
