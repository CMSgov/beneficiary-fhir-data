package gov.cms.bfd.pipeline.ccw.rif.load;

import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.Serializable;
import javax.annotation.Nullable;
import javax.sql.DataSource;

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

  private final DatabaseOptions databaseOptions;
  private final IdHasher.Config idHasherConfig;
  private final int loaderThreads;
  private final boolean idempotencyRequired;
  private final boolean fixupsEnabled;
  private final int fixupThreads;

  /**
   * Constructs a new {@link LoadAppOptions} instance.
   *
   * @param idHasherConfig the value to use for {@link #getIdHasherConfig()}
   * @param hicnHashPepper the value to use for {@link #getHicnHashPepper()}
   * @param databaseUrl the value to use for {@link #getDatabaseUrl()}
   * @param databaseUsername the value to use for {@link #getDatabaseUsername()}
   * @param databasePassword the value to use for {@link #getDatabasePassword()}
   * @param loaderThreads the value to use for {@link #getLoaderThreads()}
   * @param idempotencyRequired the value to use for {@link #isIdempotencyRequired()}
   * @param fixupsEnabled the value to use for {@link #isFixupsEnabled()}
   * @param fixupThreads the value fot use for {@link #getFixupThreads()}
   */
  public LoadAppOptions(
      DatabaseOptions databaseOptions,
      IdHasher.Config idHasherConfig,
      int loaderThreads,
      boolean idempotencyRequired,
      boolean fixupsEnabled,
      int fixupThreads) {
    if (loaderThreads < 1) throw new IllegalArgumentException();

    this.databaseOptions = databaseOptions;
    this.idHasherConfig = idHasherConfig;
    this.loaderThreads = loaderThreads;
    this.idempotencyRequired = idempotencyRequired;
    this.fixupsEnabled = fixupsEnabled;
    this.fixupThreads = fixupThreads;
  }

  /** @return the configuration settings used when hashing beneficiary HICNs */
  public IdHasher.Config getIdHasherConfig() {
    return idHasherConfig;
  }

  public DatabaseOptions getDatabaseOptions() {
    return databaseOptions;
  }

  /**
   * @return the JDBC URL of the database to load into, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  @Nullable
  public String getDatabaseUrl() {
    return databaseOptions.getDatabaseUrl();
  }

  /**
   * @return the database username to connect as when loading data, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  @Nullable
  public String getDatabaseUsername() {
    return databaseOptions.getDatabaseUsername();
  }

  /**
   * @return the database password to connect with when loading data, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  public char[] getDatabasePassword() {
    return Strings.nullToEmpty(databaseOptions.getDatabasePassword()).toCharArray();
  }

  /**
   * @return a {@link DataSource} for the database to connect to when loading data, or <code>null
   *     </code> if {@link #getDatabaseUrl()} is used, instead
   */
  @Nullable
  public DataSource getDatabaseDataSource() {
    return databaseOptions.getDatabaseDataSource();
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

  /**
   * Feature flag for fixups processing
   *
   * @return is enabled
   */
  public boolean isFixupsEnabled() {
    return fixupsEnabled;
  }

  /**
   * Feature flag for fixups processing
   *
   * @return is enabled
   */
  public int getFixupThreads() {
    return fixupThreads;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LoadAppOptions [hicnHashIterations=");
    builder.append(idHasherConfig.getHashIterations());
    builder.append(", hicnHashPepper=");
    builder.append("***");
    builder.append(", databaseUrl=");
    builder.append(databaseOptions.getDatabaseUrl());
    builder.append(", databaseUsername=");
    builder.append("***");
    builder.append(", databasePassword=");
    builder.append("***");
    builder.append(", databaseDataSource=");
    builder.append("***");
    builder.append(", loaderThreads=");
    builder.append(loaderThreads);
    builder.append(", idempotencyRequired=");
    builder.append(idempotencyRequired);
    builder.append(", fixupEnabled=");
    builder.append(fixupsEnabled);
    builder.append(", fixupThreads=");
    builder.append(fixupThreads);
    builder.append("]");
    return builder.toString();
  }
}
