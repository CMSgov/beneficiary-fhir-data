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

  /** MDC key for the mdc prefix http access. */
  private static final String MDC_PREFIX = "http_access";

  /** MDC key for the http response prefix. */
  private static final String RESPONSE_PREFIX = "response";

  /** MDC key for the http request_Start_Key. */
  public static final String REQUEST_START_KEY =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "start_milliseconds");

  /** MDC key for the http output size in bytes. */
  public static final String HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "output_size_in_bytes");

  /** MDC key for the http response duration per kb. */
  public static final String HTTP_ACCESS_RESPONSE_DURATION_PER_KB =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "duration_per_kb");

  /** MDC key for the http response duration milliseconds. */
  public static final String HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "duration_milliseconds");

  /** MDC key for the http response start in milliseconds. */
  public static final String HTTP_ACCESS_RESPONSE_START_MILLISECONDS =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "tart_milliseconds");

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
   * Reformat an identifier for an {@link MDC} key, replacing "." with "_".
   *
   * @param key Key to reformat
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
