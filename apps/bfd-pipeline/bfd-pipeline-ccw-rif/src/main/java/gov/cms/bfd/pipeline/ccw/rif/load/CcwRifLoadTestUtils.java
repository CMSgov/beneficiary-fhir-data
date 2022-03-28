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

  /** The value to use for {@link LoadAppOptions#isIdempotencyRequired()}. */
  public static final boolean IDEMPOTENCY_REQUIRED = true;

  /**
   * @return the {@link LoadAppOptions} that should be used in tests, which specifies how to connect
   *     to the database server that tests should be run against
   */
  public static LoadAppOptions getLoadOptions() {
    return new LoadAppOptions(
        new IdHasher.Config(HICN_HASH_ITERATIONS, HICN_HASH_PEPPER),
        LoadAppOptions.DEFAULT_LOADER_THREADS,
        IDEMPOTENCY_REQUIRED,
        false);
  }

  /**
   * Gets the load options with filtering of non 2022 benes enabled.
   *
   * @param idempotencyRequired if idempotency is required; affects the LoadStrategy that gets used
   *     when loading
   * @return Same as {@link #getLoadOptions()}, but with {@link
   *     LoadAppOptions#isFilteringNonNullAndNon2022Benes()} set to <code>true</code>. Should only
   *     be used in those test cases looking to test that filtering capability.
   */
  public static LoadAppOptions getLoadOptionsWithFilteringofNon2022BenesEnabled(
      boolean idempotencyRequired) {
    return getLoadOptions(idempotencyRequired, true);
  }

  /**
   * Gets the load options with filtering of non 2022 benes and idempotency strategy as input.
   *
   * @param idempotencyRequired if idempotency is required; affects the LoadStrategy that gets used
   *     when loading
   * @param filterNon2022benes the filter non 2022 benes turned on if {@code true}
   * @return the {@link LoadAppOptions} that should be used in tests, which specifies how to connect
   *     to the database server that tests should be run against
   */
  public static LoadAppOptions getLoadOptions(
      boolean idempotencyRequired, boolean filterNon2022benes) {
    return new LoadAppOptions(
        new IdHasher.Config(HICN_HASH_ITERATIONS, HICN_HASH_PEPPER),
        LoadAppOptions.DEFAULT_LOADER_THREADS,
        idempotencyRequired,
        filterNon2022benes);
  }
}
