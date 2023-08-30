package gov.cms.bfd.server.war.commons;

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
}
