package gov.cms.bfd.pipeline.rda.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This helper class abstracts the source of configuration values and handles insertion of default
 * values when none are provided by the environment. Currently implemented to pull values from
 * environment variables.
 */
public class ConfigUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

  /**
   * Retrieve configuration value with the given name. Returns the specified default value when
   * there is no configuration value matching the key.
   *
   * @param key key that identifies the particular configuration value.
   * @param defaultValue default value if there is no configuration value matching the key
   * @return either the configuration value or the default
   */
  public static String getString(String key, String defaultValue) {
    String value = System.getenv(key);
    return value != null ? value : defaultValue;
  }

  /**
   * Retrieve integer configuration value with the given name. Returns the specified default value
   * when there is no configuration value matching the key.
   *
   * @param key key that identifies the particular configuration value.
   * @param defaultValue default value if there is no configuration value matching the key
   * @return either the configuration value or the default
   */
  public static int getInt(String key, int defaultValue) {
    try {
      String strValue = System.getenv(key);
      return strValue != null ? Integer.parseInt(strValue) : defaultValue;
    } catch (NumberFormatException ex) {
      LOGGER.error(
          "Invalid environment variable value, expected a valid integer: envvar={} error='{}'",
          key,
          ex.getMessage(),
          ex);
      throw ex;
    }
  }

  /**
   * Retrieve long configuration value with the given name. Returns the specified default value when
   * there is no configuration value matching the key.
   *
   * @param key key that identifies the particular configuration value.
   * @param defaultValue default value if there is no configuration value matching the key
   * @return either the configuration value or the default
   */
  public static long getLong(String key, long defaultValue) {
    try {
      String strValue = System.getenv(key);
      return strValue != null ? Long.parseLong(strValue) : defaultValue;
    } catch (NumberFormatException ex) {
      LOGGER.error(
          "Invalid environment variable value, expected a valid long: envvar={} error='{}'",
          key,
          ex.getMessage(),
          ex);
      throw ex;
    }
  }
}
