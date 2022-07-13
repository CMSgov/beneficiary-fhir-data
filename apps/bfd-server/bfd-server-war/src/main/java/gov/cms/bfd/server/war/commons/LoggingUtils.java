package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.util.Arrays;
import java.util.stream.Collectors;

/** A set of methods for various logging purposes i.e. MDC */
public class LoggingUtils {
  /**
   * Output list of benefificiary IDs to MDC logging
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
}
