package gov.cms.bfd.server.sharedutils;

import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * Wrapper for to the {@link MDC} class (used in logging). Historically, we have used "." to delimit
 * parts of the MDC keys, such as "http_access.request.header". For AWS CloudWatch Metrics, though,
 * the "." character is not supported, so we need to change these to "_".
 */
public class BfdMDC {

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

  /**
   * MDC Adapter explicitly set in {@link #setMDCAdapter(MDCAdapter)} (if any). Unless this is set,
   * use the MDC class's adapter. You normally wouldn't set this explicitly, but it can be useful
   * for mocked test MDC adapters, for example.
   */
  private static MDCAdapter mdcAdapter = null;

  /**
   * Set the MDC Adapter. Normally this is not needed, but it can be useful in testing.
   *
   * @param adapter The MDC Adapter to use.
   */
  public static void setMDCAdapter(MDCAdapter adapter) {
    mdcAdapter = adapter;
  }

  /**
   * Get the MDC Adapter, generally via {@link MDC#getMDCAdapter()}.
   *
   * @return The MDC Adapter being used.
   */
  public static MDCAdapter getMDCAdapter() {
    if (mdcAdapter == null) {
      return MDC.getMDCAdapter();
    }
    return mdcAdapter;
  }

  /**
   * Compute a key for {@link MDC} from several parts.
   *
   * @param keys Pieces of the key to be joined together
   * @return The final key
   */
  public static String computeMDCKey(String... keys) {
    return String.join("_", keys);
  }

  /**
   * Format an identifier for an {@link MDC} key.
   *
   * @param key Fields to concatenate into the final key that we'll put into MDC
   * @return Text of the key that we'll put into MDC
   */
  public static String formatMDCKey(String key) {
    return key.replace(FROM_DELIMITER, TO_DELIMITER);
  }

  /**
   * Wrapper for {@link MDC#put(String,String)}, except that we format the key.
   *
   * @param key Key for later finding the value
   * @param value Value put into MDC
   */
  public static void put(String key, String value) {
    getMDCAdapter().put(formatMDCKey(key), value);
  }

  /** Wrapper for {@link org.slf4j.MDC#clear()}. */
  public static void clear() {
    getMDCAdapter().clear();
  }
}
