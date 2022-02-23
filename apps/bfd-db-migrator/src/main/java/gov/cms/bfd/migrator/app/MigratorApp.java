package gov.cms.bfd.migrator.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main application/entry point for the Database Migration system. See {@link
 * MigratorApp#main(String[])}.
 */
public final class MigratorApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigratorApp.class);

  /**
   * This method is called when the application is launched from the command line. Currently, this
   * is a notional placeholder application that produces NDJSON-formatted log output.
   *
   * @param args (should be empty. Application <strong>will</strong> accept configuration via
   *     environment variables)
   */
  public static void main(String[] args) {
    LOGGER.info("Successfully started");
    LOGGER.info("Hello, World!");
    LOGGER.info("Shutting down");
    System.exit(0);
  }
}
