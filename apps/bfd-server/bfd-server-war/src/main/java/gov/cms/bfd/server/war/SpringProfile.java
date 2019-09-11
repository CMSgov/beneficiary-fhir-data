package gov.cms.bfd.server.war;

import org.springframework.context.annotation.Profile;

/**
 * Enumerates the various Spring {@link Profile}s supported in the application's Spring
 * configuration. (Not an actual enum, as the values need to be referenced within annotations.)
 */
public final class SpringProfile {
  /**
   * The default {@link Profile}. Contains the configuration for use when the application is
   * deployed in production.
   */
  public static final String PRODUCTION = "production";

  /** Private constructor; class not intended to be instantiated. */
  private SpringProfile() {}
}
