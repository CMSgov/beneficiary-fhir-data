package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.model.rif.schema.DatabaseTestHelper;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utilities that are useful when running the {@link RifLoader}.
 *
 * <p>This is being left in <code>src/main</code> so that it can be used from other modules' tests,
 * without having to delve into classpath dark arts.
 */
public final class RifLoaderTestUtils {
  /** The value to use for {@link LoadAppOptions#getHicnHashIterations()} in tests. */
  public static final int HICN_HASH_ITERATIONS = 2;

  /** The value to use for {@link LoadAppOptions#getHicnHashPepper()} in tests. */
  public static final byte[] HICN_HASH_PEPPER = "nottherealpepper".getBytes(StandardCharsets.UTF_8);

  /** The value to use for {@link LoadAppOptions#isIdempotencyRequired()}. */
  public static final boolean IDEMPOTENCY_REQUIRED = true;

  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderTestUtils.class);

  /**
   * A wrapper for the entity manager logic and action. The consumer is called within a transaction
   * to which is rolled back.
   *
   * @param consumer to call with an entity manager.
   */
  public static void doTestWithDb(BiConsumer<DataSource, EntityManager> consumer) {
    final DataSource jdbcDataSource = DatabaseTestHelper.getTestDatabaseAfterClean();
    DatabaseSchemaManager.createOrUpdateSchema(jdbcDataSource);
    final EntityManagerFactory entityManagerFactory =
        RifLoader.createEntityManagerFactory(jdbcDataSource);
    EntityManager entityManager = null;
    try {
      entityManager = entityManagerFactory.createEntityManager();
      consumer.accept(jdbcDataSource, entityManager);
    } finally {
      if (entityManager != null && entityManager.isOpen()) {
        entityManager.close();
      }
    }
  }

  /**
   * Get the list of loaded files from the passed in db, latest first
   *
   * @param entityManager to use
   * @return the list of loaded files in the db
   */
  public static List<LoadedFile> findLoadedFiles(EntityManager entityManager) {
    entityManager.clear();
    return entityManager
        .createQuery("select f from LoadedFile f order by f.created desc", LoadedFile.class)
        .getResultList();
  }

  /**
   * Return a Files Event with a single dummy file
   *
   * @return a new RifFilesEvent
   */
  public static RifFilesEvent createDummyFilesEvent() {
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

    return new RifFilesEvent(Instant.now(), Arrays.asList(dummyFile));
  }

  /**
   * @param dataSource a {@link DataSource} for the test DB to connect to
   * @return the {@link LoadAppOptions} that should be used in tests, which specifies how to connect
   *     to the database server that tests should be run against
   */
  public static LoadAppOptions getLoadOptions(DataSource dataSource) {
    return new LoadAppOptions(
        HICN_HASH_ITERATIONS,
        HICN_HASH_PEPPER,
        LoadAppOptions.DEFAULT_LOADER_THREADS,
        IDEMPOTENCY_REQUIRED);
  }

  /**
   * @param dataSource the {@link DataSource} specifying the DB to use
   * @return a JPA {@link EntityManagerFactory} for the database server used in tests
   */
  public static EntityManagerFactory createEntityManagerFactory(DataSource dataSource) {
    if (dataSource == null) {
      throw new IllegalStateException("DB DataSource (not URLs) must be used in tests.");
    }

    return RifLoader.createEntityManagerFactory(dataSource);
  }

  /**
   * Pause of a number milliseconds.
   *
   * @param millis to sleap
   */
  public static void pauseMillis(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
    }
  }
}
