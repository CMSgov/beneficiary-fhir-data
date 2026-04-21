package gov.cms.bfd.server.ng.util;

import static gov.cms.bfd.server.ng.util.LoggerConstants.LOG_TYPE;

import org.slf4j.LoggerFactory;

/** Utility class used for logging. */
public class LogUtil {

  // Private constructor to prevent instantiation
  private LogUtil() {
    // Intentionally empty
  }

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogUtil.class);

  /**
   * For logging bene_sk.
   *
   * @param beneSk the bene_sk
   */
  public static void logBeneSk(Long beneSk) {
    LOGGER
        .atInfo()
        .setMessage(LoggerConstants.BENE_SK_FOUND)
        .addKeyValue(LOG_TYPE, "beneFound")
        .addKeyValue("beneSk", beneSk)
        .log();
  }
}
