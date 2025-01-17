package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.collect.ImmutableList;

/** Helper Utils class for Functions shared across. multiple classes. */
public class StringUtils {

  /**
   * Custom Split Function to replace.
   *
   * @param input a String that needs to remove whitespace and commas.
   * @return Split Array.
   */
  public static String[] splitOnCommas(String input) {
    final ImmutableList.Builder<String> builder = ImmutableList.builder();
    StringBuilder curr = new StringBuilder();

    for (char c : input.toCharArray()) {
      if (c == ',') {
        if (curr.length() > 0) {
          builder.add(curr.toString().trim());
          curr = new StringBuilder();
        }
      } else {
        curr.append(c);
      }
    }

    if (curr.length() > 0) {
      builder.add(curr.toString().trim());
    }
    return builder.build().toArray(new String[0]);
  }

  /**
   * Attempts to parse the input as a Long, throwing a bad request error if it fails.
   *
   * @param input numeric input
   * @param fieldName field name for error message
   * @return parsed value
   */
  public static Long parseLongOrBadRequest(String input, String fieldName) {
    try {
      return Long.parseLong(input);
    } catch (NumberFormatException ex) {
      // Intentionally omitting the supplied input from the message here because it could be
      // sensitive
      throw new InvalidRequestException(
          String.format("Failed to parse value for %s as a number.", fieldName));
    }
  }

  /**
   * Attempts to parse the input as an int, throwing a bad request error if it fails.
   *
   * @param input numeric input
   * @param fieldName field name for error message
   * @return parsed value
   */
  public static int parseIntOrBadRequest(String input, String fieldName) {
    try {
      return Integer.parseInt(input);
    } catch (NumberFormatException ex) {
      // Intentionally omitting the supplied input from the message here because it could be
      // sensitive
      throw new InvalidRequestException(
          String.format("Failed to parse value for %s as a number.", fieldName));
    }
  }
}
