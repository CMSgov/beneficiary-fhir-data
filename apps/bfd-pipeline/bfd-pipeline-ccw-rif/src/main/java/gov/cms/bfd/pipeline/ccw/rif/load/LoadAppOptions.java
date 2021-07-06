package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.Serializable;

/** Models the user-configurable application options. */
public final class LoadAppOptions implements Serializable {
  /*
   * This class is marked Serializable purely to help keep
   * AppConfigurationTest simple. Unfortunately, Path implementations aren't
   * also Serializable, so we have to store Strings here, instead.
   */

  private static final long serialVersionUID = 2884121140016566847L;

  /**
   * A reasonable (though not terribly performant) suggested default value for {@link
   * #getLoaderThreads()}.
   */
  public static final int DEFAULT_LOADER_THREADS =
      Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2;

  private final IdHasher.Config idHasherConfig;
  private final int loaderThreads;
  private final boolean idempotencyRequired;

  /**
   * Constructs a new {@link LoadAppOptions} instance.
   *
   * @param idHasherConfig the value to use for {@link #getIdHasherConfig()}
   * @param loaderThreads the value to use for {@link #getLoaderThreads()}
   * @param idempotencyRequired the value to use for {@link #isIdempotencyRequired()}
   */
  public LoadAppOptions(
      IdHasher.Config idHasherConfig, int loaderThreads, boolean idempotencyRequired) {
    if (loaderThreads < 1) throw new IllegalArgumentException();

    this.idHasherConfig = idHasherConfig;
    this.loaderThreads = loaderThreads;
    this.idempotencyRequired = idempotencyRequired;
  }

  /** @return the configuration settings used when hashing beneficiary HICNs */
  public IdHasher.Config getIdHasherConfig() {
    return idHasherConfig;
  }

  /**
   * @return the number of threads that will be used to simultaneously process {@link RifLoader}
   *     operations
   */
  public int getLoaderThreads() {
    return loaderThreads;
  }

  /**
   * @return
   *     <p><code>true</code> if {@link RifLoader} should check to see if each record has already
   *     been processed, <code>false</code> if it should blindly assume that it hasn't
   *     <p>This is sometimes a reasonable speed vs. safety tradeoff to make, as that checking is
   *     slow, particularly if indexes have been dropped in an attempt to speed up initial loads.
   *     Aside from that, though, this value is best left set to <code>true</code>.
   */
  public boolean isIdempotencyRequired() {
    return idempotencyRequired;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LoadAppOptions [hicnHashIterations=");
    builder.append(idHasherConfig.getHashIterations());
    builder.append(", hicnHashPepper=");
    builder.append("***");
    builder.append(", loaderThreads=");
    builder.append(loaderThreads);
    builder.append(", idempotencyRequired=");
    builder.append(idempotencyRequired);
    builder.append("]");
    return builder.toString();
  }
}
