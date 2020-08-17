package gov.cms.bfd.server.war;

import java.io.Serializable;

/** Models the user-configurable application options. */
public class LoadFhirAppOptions implements Serializable {

  /*
   * This class is marked Serializable purely to help keep AppConfigurationTest simple.
   * Unfortunately, Path implementations aren't also Serializable, so we have to store Strings here,
   * instead.
   */

  private static final long serialVersionUID = 2884121140016566847L;

  /**
   * A reasonable (though not terribly performant) suggested default value for
   * {@link #getLoaderThreads()}.
   */
  public static final int DEFAULT_LOADER_THREADS =
      Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2;


  private final boolean v2Enabled;

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
  public LoadFhirAppOptions(boolean v2Enabled) {


    this.v2Enabled = v2Enabled;
  }

  /**
   * Feature flag for BFD v2
   *
   * @return is V2 enabled
   */
  public boolean isV2Enabled() {
    return v2Enabled;
  }



  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LoadFhirAppOptions =");

    builder.append(", v2Enabled=");
    builder.append(v2Enabled);

    builder.append("]");
    return builder.toString();
  }
}
