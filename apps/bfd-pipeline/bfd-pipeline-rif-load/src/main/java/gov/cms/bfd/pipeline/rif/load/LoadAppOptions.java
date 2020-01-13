package gov.cms.bfd.pipeline.rif.load;

import java.io.Serializable;
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

  private final int hicnHashIterations;
  private final byte[] hicnHashPepper;
  private final String databaseUrl;
  private final String databaseUsername;
  private final char[] databasePassword;
  private final DataSource databaseDataSource;
  private final int loaderThreads;
  private final boolean idempotencyRequired;
  private final boolean fixupsEnabled;
  private final int fixupThreads;

  /**
   * Constructs a new {@link LoadAppOptions} instance.
   *
   * @param hicnHashIterations the value to use for {@link #getHicnHashIterations()}
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
      int hicnHashIterations,
      byte[] hicnHashPepper,
      String databaseUrl,
      String databaseUsername,
      char[] databasePassword,
      int loaderThreads,
      boolean idempotencyRequired,
      boolean fixupsEnabled,
      int fixupThreads) {
    if (loaderThreads < 1) throw new IllegalArgumentException();

    this.hicnHashIterations = hicnHashIterations;
    this.hicnHashPepper = hicnHashPepper;
    this.databaseUrl = databaseUrl;
    this.databaseUsername = databaseUsername;
    this.databasePassword = databasePassword;
    this.databaseDataSource = null;
    this.loaderThreads = loaderThreads;
    this.idempotencyRequired = idempotencyRequired;
    this.fixupsEnabled = fixupsEnabled;
    this.fixupThreads = fixupThreads;
  }

  /**
   * Constructs a new {@link LoadAppOptions} instance.
   *
   * @param hicnHashIterations the value to use for {@link #getHicnHashIterations()}
   * @param hicnHashPepper the value to use for {@link #getHicnHashPepper()}
   * @param databaseDataSource the value to use for {@link #getDatabaseDataSource()}
   * @param loaderThreads the value to use for {@link #getLoaderThreads()}
   * @param idempotencyRequired the value to use for {@link #isIdempotencyRequired()}
   * @param fixupsEnabled the value to use for {@link #isFixupsEnabled()}
   * @param fixupThreads the value fot use for {@link #getFixupThreads()}
   */
  public LoadAppOptions(
      int hicnHashIterations,
      byte[] hicnHashPepper,
      DataSource databaseDataSource,
      int loaderThreads,
      boolean idempotencyRequired,
      boolean fixupsEnabled,
      int fixupThreads) {
    if (loaderThreads < 1) throw new IllegalArgumentException();

    this.hicnHashIterations = hicnHashIterations;
    this.hicnHashPepper = hicnHashPepper;
    this.databaseUrl = null;
    this.databaseUsername = null;
    this.databasePassword = null;
    this.databaseDataSource = databaseDataSource;
    this.loaderThreads = loaderThreads;
    this.idempotencyRequired = idempotencyRequired;
    this.fixupsEnabled = fixupsEnabled;
    this.fixupThreads = fixupThreads;
  }

  /**
   * @return the number of <code>PBKDF2WithHmacSHA256</code> iterations to use when hashing
   *     beneficiary HICNs
   */
  public int getHicnHashIterations() {
    return hicnHashIterations;
  }

  /**
   * @return the shared secret pepper to use (in lieu of a salt) with <code>PBKDF2WithHmacSHA256
   *     </code> when hashing beneficiary HICNs
   */
  public byte[] getHicnHashPepper() {
    return hicnHashPepper;
  }

  /**
   * @return the JDBC URL of the database to load into, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  public String getDatabaseUrl() {
    return databaseUrl;
  }

  /**
   * @return the database username to connect as when loading data, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  public String getDatabaseUsername() {
    return databaseUsername;
  }

  /**
   * @return the database password to connect with when loading data, or <code>null</code> if {@link
   *     #getDatabaseDataSource()} is used, instead
   */
  public char[] getDatabasePassword() {
    return databasePassword;
  }

  /**
   * @return a {@link DataSource} for the database to connect to when loading data, or <code>null
   *     </code> if {@link #getDatabaseUrl()} is used, instead
   */
  public DataSource getDatabaseDataSource() {
    return databaseDataSource;
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
    builder.append(hicnHashIterations);
    builder.append(", hicnHashPepper=");
    builder.append("***");
    builder.append(", databaseUrl=");
    builder.append(databaseUrl);
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
