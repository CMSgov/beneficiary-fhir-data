package gov.cms.bfd.server.war;

import java.io.Serializable;

/** Models the user-configurable application options. */
public class LoadFhirAppOptions implements Serializable {

  private static final long serialVersionUID = 2884121140016566847L;

  private final boolean v2Enabled;

  /**
   * Constructs a new {@link LoadFhirAppOptions} instance.
   *
   * @param v2Enabled the value to use for {@link #isV2Enabled()}
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
