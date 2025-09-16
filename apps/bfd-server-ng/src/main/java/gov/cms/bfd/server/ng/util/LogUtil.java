package gov.cms.bfd.server.ng.util;

import org.slf4j.LoggerFactory;

/** Utility class used for logging. */
public class LogUtil {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogUtil.class);

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
