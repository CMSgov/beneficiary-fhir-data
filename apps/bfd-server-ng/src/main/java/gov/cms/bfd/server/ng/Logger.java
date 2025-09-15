package gov.cms.bfd.server.ng;

import org.slf4j.LoggerFactory;

/** Class used for logging. */
public class Logger {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);

  /**
   * For logging bene_sk.
   *
   * @param beneSk the bene_sk
   */
  public static void logBeneSkIfPresent(Long beneSk) {
    LOGGER
        .atInfo()
        .setMessage(LoggerConstants.BENE_SK_REQUESTED)
        .addKeyValue("bene_sk", beneSk)
        .log();
  }
}
