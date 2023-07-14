package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.nio.charset.StandardCharsets;

/**
 * Contains utilities that are useful when running the {@link RifLoader}.
 *
 * <p>This is being left in <code>src/main</code> so that it can be used from other modules' tests,
 * without having to delve into classpath dark arts.
 */
public final class CcwRifLoadTestUtils {
  /** The value to use for {@link IdHasher.Config#getHashIterations()} in tests. */
  public static final int HICN_HASH_ITERATIONS = 2;

  /** The value to use for {@link IdHasher.Config#getHashPepper()} in tests. */
  public static final byte[] HICN_HASH_PEPPER = "nottherealpepper".getBytes(StandardCharsets.UTF_8);

  /** The value to use for {@link LoadAppOptions#idempotencyRequired}. */
  public static final boolean IDEMPOTENCY_REQUIRED = true;

  /** The default batch size to use for testing. */
  public static final int DEFAULT_LOAD_BATCH_SIZE = 100;

  /** The default queue size multiple to use for testing. */
  private static final int DEFAULT_QUEUE_SIZE_MULTIPLE = 2;

  /**
   * Gets the load options.
   *
   * @return the {@link LoadAppOptions} that should be used in tests, which specifies how to connect
   *     to the database server that tests should be run against
   */
  public static LoadAppOptions getLoadOptions() {
    return new LoadAppOptions(
        new IdHasher.Config(HICN_HASH_ITERATIONS, HICN_HASH_PEPPER),
        IDEMPOTENCY_REQUIRED,
        false,
        new LoadAppOptions.PerformanceSettings(
            LoadAppOptions.DEFAULT_LOADER_THREADS,
            DEFAULT_LOAD_BATCH_SIZE,
            DEFAULT_QUEUE_SIZE_MULTIPLE),
        new LoadAppOptions.PerformanceSettings(
            LoadAppOptions.DEFAULT_LOADER_THREADS,
            DEFAULT_LOAD_BATCH_SIZE,
            DEFAULT_QUEUE_SIZE_MULTIPLE));
  }

  /**
   * Gets the load options with filtering of non 2023 benes enabled.
   *
   * @param idempotencyRequired if idempotency is required; affects the LoadStrategy that gets used
   *     when loading
   * @return Same as {@link #getLoadOptions()}, but with {@link
   *     LoadAppOptions#filteringNonNullAndNon2023Benes} set to {@code true}. Should only be used in
   *     those test cases looking to test that filtering capability.
   */
  public static LoadAppOptions getLoadOptionsWithFilteringOfNon2023BenesEnabled(
      boolean idempotencyRequired) {
    return getLoadOptions(idempotencyRequired, true);
  }

  /**
   * Gets the load options with filtering of non 2023 benes and idempotency strategy as input.
   *
   * @param idempotencyRequired if idempotency is required; affects the LoadStrategy that gets used
   *     when loading
   * @param filterNon2023benes the filter non 2023 benes turned on if {@code true}
   * @return the {@link LoadAppOptions} that should be used in tests, which specifies how to connect
   *     to the database server that tests should be run against
   */
  public static LoadAppOptions getLoadOptions(
      boolean idempotencyRequired, boolean filterNon2023benes) {
    return new LoadAppOptions(
        new IdHasher.Config(HICN_HASH_ITERATIONS, HICN_HASH_PEPPER),
        idempotencyRequired,
        filterNon2023benes,
        new LoadAppOptions.PerformanceSettings(
            LoadAppOptions.DEFAULT_LOADER_THREADS,
            DEFAULT_LOAD_BATCH_SIZE,
            DEFAULT_QUEUE_SIZE_MULTIPLE),
        new LoadAppOptions.PerformanceSettings(
            LoadAppOptions.DEFAULT_LOADER_THREADS,
            DEFAULT_LOAD_BATCH_SIZE,
            DEFAULT_QUEUE_SIZE_MULTIPLE));
  }

  /**
   * Gets the load options with the specified batch size.
   *
   * @param batchSize the batch size
   * @return the load options with batch size, and other options defaulted to the test defaults
   */
  public static LoadAppOptions getLoadOptionsWithBatchSize(int batchSize) {
    return new LoadAppOptions(
        new IdHasher.Config(HICN_HASH_ITERATIONS, HICN_HASH_PEPPER),
        IDEMPOTENCY_REQUIRED,
        false,
        new LoadAppOptions.PerformanceSettings(
            LoadAppOptions.DEFAULT_LOADER_THREADS, batchSize, DEFAULT_QUEUE_SIZE_MULTIPLE),
        new LoadAppOptions.PerformanceSettings(
            LoadAppOptions.DEFAULT_LOADER_THREADS, batchSize, DEFAULT_QUEUE_SIZE_MULTIPLE));
  }
}
