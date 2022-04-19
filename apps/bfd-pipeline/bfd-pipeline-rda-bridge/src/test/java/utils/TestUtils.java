package utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Testing utility for making more succinct tests */
public class TestUtils {

  /**
   * Compares two lists of json objects (ndjson) for equality
   *
   * @param expectedJson The expected json values
   * @param actualJson The actual json values
   * @param ignorePaths The paths to ignore in comparisons
   */
  public static void assertJsonEquals(
      List<String> expectedJson, List<String> actualJson, Set<String> ignorePaths) {
    int i;

    List<String> diffList = new ArrayList<>();

    for (i = 0; i < expectedJson.size() && i < actualJson.size(); ++i) {
      String diffMessage = createDiff(expectedJson.get(i), actualJson.get(i), ignorePaths);

      if (!diffMessage.isEmpty()) {
        diffList.add("Entry " + (i + 1) + ": " + diffMessage);
      }
    }

    if (i < actualJson.size()) {
      for (int j = i; j < actualJson.size(); ++j) {
        String diffMessage = createDiff("{}", actualJson.get(j), ignorePaths);

        if (!diffMessage.isEmpty()) {
          diffList.add("Entry " + (j + 1) + ": " + diffMessage);
        }
      }
    } else if (i < expectedJson.size()) {
      for (int j = i; j < expectedJson.size(); ++j) {
        String diffMessage = createDiff(expectedJson.get(j), "{}", ignorePaths);

        if (!diffMessage.isEmpty()) {
          diffList.add("Entry " + (j + 1) + ": " + diffMessage);
        }
      }
    }

    assertTrue(diffList.isEmpty(), String.join("\n", diffList));
  }

  /**
   * Creates a single diff string of the differences fond between the json strings
   *
   * @param expectedJson The expected json string
   * @param actualJson The actual json string
   * @param ignorePaths Paths to ignore during comparison
   * @return A string containing all the diff messages when comparing the json strings.
   */
  private static String createDiff(
      String expectedJson, String actualJson, Set<String> ignorePaths) {
    String message;

    try {
      List<String> diffs = diffJson(expectedJson, actualJson, ignorePaths);
      message = String.join(",", diffs);
    } catch (IOException e) {
      message = "Failed to be read";
    }

    return message;
  }

  /**
   * Compares two json strings and creates a list of diff strings showing how they differ.
   *
   * @param expectedJson The expected JSON string value
   * @param actualJson The actual JSON string value
   * @param ignorePaths The paths to ignore while comparing the JSON string values
   * @return A list of diff strings showing each difference between the two JSON string values.
   * @throws IOException If there was an issue reading the JSON strings
   */
  private static List<String> diffJson(
      String expectedJson, String actualJson, Set<String> ignorePaths) throws IOException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode expected = mapper.readTree(expectedJson);
    JsonNode actual = mapper.readTree(actualJson);

    JsonNode diff = JsonDiff.asJson(expected, actual);

    List<String> diffs = new ArrayList<>();

    for (int i = 0; i < diff.size(); ++i) {
      if (!ignorePaths.contains(diff.get(i).get("path").asText())) {
        diffs.add(diff.get(i).toString());
      }
    }

    return diffs;
  }

  /**
   * Compares two {@link MessageOrBuilder} objects to check for equality, ignoring the given paths
   * during comparison.
   *
   * @param expected The expected {@link MessageOrBuilder}
   * @param actual The actual {@link MessageOrBuilder}
   * @param ignorePaths The paths to ignore during comparison.
   */
  public static void assertMessagesEqual(
      MessageOrBuilder expected, MessageOrBuilder actual, Set<String> ignorePaths) {
    try {
      String expectedJson = JsonFormat.printer().omittingInsignificantWhitespace().print(expected);
      String actualJson = JsonFormat.printer().omittingInsignificantWhitespace().print(actual);

      TestUtils.assertJsonEquals(List.of(expectedJson), List.of(actualJson), ignorePaths);
    } catch (InvalidProtocolBufferException e) {
      fail("Encountered error running test", e);
    }
  }
}
