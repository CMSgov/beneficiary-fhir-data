package gov.cms.bfd.pipeline.rif.load;

import java.nio.charset.StandardCharsets;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

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

  /** The value to use for {@link LoadAppOptions#isFixupsEnabled()} */
  public static final boolean FIXUPS_ENABLED = false;

  /**
   * @param dataSource a {@link DataSource} for the test DB to connect to
   * @return the {@link LoadAppOptions} that should be used in tests, which specifies how to connect
   *     to the database server that tests should be run against
   */
  public static LoadAppOptions getLoadOptions(DataSource dataSource) {
    return new LoadAppOptions(
        HICN_HASH_ITERATIONS,
        HICN_HASH_PEPPER,
        dataSource,
        LoadAppOptions.DEFAULT_LOADER_THREADS,
        IDEMPOTENCY_REQUIRED,
        FIXUPS_ENABLED);
  }

  /**
   * @param options the {@link LoadAppOptions} specifying the DB to use
   * @return a JPA {@link EntityManagerFactory} for the database server used in tests
   */
  public static EntityManagerFactory createEntityManagerFactory(LoadAppOptions options) {
    if (options.getDatabaseDataSource() == null) {
      throw new IllegalStateException("DB DataSource (not URLs) must be used in tests.");
    }

    DataSource dataSource = options.getDatabaseDataSource();
    return RifLoader.createEntityManagerFactory(dataSource);
  }
}
