package gov.cms.bfd.server.sharedutils;

import org.slf4j.spi.MDCAdapter;

/** Wrapper for to the {@link org.slf4j.MDC} class (used in logging). */
public class MDC {

  /**
   * Delimiter that used to be used to separate the parts of MDC field names; we want to replace
   * this with TO_DELIMITER.
   */
  public static final String FROM_DELIMITER = ".";

  /**
   * Delimiter that we want to be used to separate the parts of MDC field names; we want to replace
   * FROM_DELIMITER with this.
   */
  public static final String TO_DELIMITER = "_";

  /** MDC Adapter being used. Unless this is set, use the MDC class's adapter. */
  private static MDCAdapter mdcAdapter = null;

  /**
   * Set the MDC Adapter. Normally this is not needed, but it can be useful in testing.
   *
   * @param adapter The MDC Adapter to use.
   */
  public static void setMdcAdapter(MDCAdapter adapter) {
    mdcAdapter = adapter;
  }

  /**
   * Get the MDC Adapter, generally via {@link org.slf4j.MDC#getMDCAdapter()}.
   *
   * @return The MDC Adapter being used.
   */
  public static MDCAdapter getMDCAdapter() {
    if (mdcAdapter == null) {
      return org.slf4j.MDC.getMDCAdapter();
    }
    return mdcAdapter;
  }

  /**
   * Format an identifier for an {@link org.slf4j.MDC} key. Historically, we have used "." to
   * delimit parts of the MDC keys, such as "http_access.request.header". For AWS CloudWatch
   * Metrics, though, the "." character is not supported, so we need to change these to "_".
   *
   * @param key Fields to concatenate into the final key that we'll put into MDC
   * @return Text of the key that we'll put into MDC
   */
  public static String formatMdcKey(String key) {
    return key.replace(FROM_DELIMITER, TO_DELIMITER);
  }

  /**
   * Wrapper for {@link org.slf4j.MDC#put(String,String)}, except that we format the key.
   *
   * @param key Key for later finding the value
   * @param value Value put into MDC
   */
  public static void put(String key, String value) {
    getMDCAdapter().put(formatMdcKey(key), value);
  }

  /** Wrapper for {@link org.slf4j.MDC#clear()}. */
  public static void clear() {
    getMDCAdapter().clear();
  }
}
