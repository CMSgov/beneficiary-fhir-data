package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A set of methods for various logging purposes i.e. MDC */
public class LoggingUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUtils.class);

  /**
   * Log a list of beneficiary IDs to the BfdMDC under the 'bene_id' key.
   *
   * @param beneIds the {@link Long} of beneficiary IDs top log
   */
  public static void logBeneIdToMdc(Long... beneIds) {
    if (beneIds.length > 0) {
      String beneIdEntry =
          Arrays.stream(beneIds).map(String::valueOf).collect(Collectors.joining(", "));
      BfdMDC.put("bene_id", beneIdEntry);
    }
  }

  /**
   * Log a beneficiary ID to the BfdMDC under the 'bene_id' key if the ID supplied can be parsed as
   * a long.
   *
   * @param beneId the {@link String} of beneficiary IDs top log
   */
  public static void logBeneIdToMdc(String beneId) {
    try {
      logBeneIdToMdc(Long.parseLong(beneId));
    } catch (NumberFormatException e) {
      LOGGER.warn("Could not parse long from bene_id: " + beneId);
    }
  }
}
