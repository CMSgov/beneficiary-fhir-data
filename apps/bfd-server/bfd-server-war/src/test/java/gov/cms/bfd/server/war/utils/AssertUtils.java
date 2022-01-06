package gov.cms.bfd.server.war.utils;

import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.junit.Assert;

public class AssertUtils {

  @FunctionalInterface
  public interface Executor {
    void execute();
  }

  public static Exception catchExceptions(Executor executor) {
    Exception ex = null;

    try {
      executor.execute();
    } catch (Exception e) {
      ex = e;
    }

    return ex;
  }

  public static void assertThrowEquals(Exception expected, Exception actual) {
    if (expected.getClass() == actual.getClass()) {
      if (!Objects.equals(expected.getMessage(), actual.getMessage())) {
        fail(
            "expected: "
                + expected.getClass().getCanonicalName()
                + "<"
                + expected.getClass().getCanonicalName()
                + ": "
                + expected.getMessage()
                + "> but was: "
                + actual.getClass().getCanonicalName()
                + "<"
                + actual.getClass().getCanonicalName()
                + ": "
                + actual.getMessage()
                + ">");
      }
    } else {
      fail(
          "expected: "
              + expected.getClass().getCanonicalName()
              + " but was: "
              + actual.getClass().getCanonicalName());
    }
  }

  /**
   * Checks if two json strings are equivalent through node path based comparisons.
   *
   * @param expected The expected json string
   * @param actual The actual json string
   * @param ignorePaths Set of Node paths that should be ignored during comparison.
   */
  public static void assertJsonEquals(String expected, String actual, Set<String> ignorePaths) {
    Pattern pattern = Pattern.compile(String.join("|", ignorePaths));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode beforeNode;

    try {
      beforeNode = mapper.readTree(expected);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + expected, e);
    }

    JsonNode afterNode;

    try {
      afterNode = mapper.readTree(actual);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + actual, e);
    }

    JsonNode diff = JsonDiff.asJson(beforeNode, afterNode);

    // Filter out diffs that we don't care about (due to changing with each call)
    // such as "lastUpdated" fields, the port on URLs, etc. ...
    NodeFilteringConsumer consumer =
        new NodeFilteringConsumer(node -> pattern.matcher(node.get("path").toString()).matches());

    diff.forEach(consumer);

    List<JsonNode> actualDiffs = new ArrayList<>();

    // Looping through the diffs, checking what actually contains a non-empty object, and adding
    // them to actualDiffs, which will be printed out as the real list of differences.
    if (diff.size() > 0) {
      for (int i = 0; i < diff.size(); i++) {
        if (!diff.get(i).toString().equals("{}")) {
          actualDiffs.add(diff.get(i));
        }
      }
    }

    Assert.assertEquals(Collections.emptyList(), actualDiffs);
  }

  /**
   * NodeFilter is a simple interface with one method that takes a single argument, {@link
   * JsonNode}, and returns true if the JsonNode satisfies the filter.
   */
  private interface NodeFilter {
    boolean apply(JsonNode node);
  }

  /**
   * NodeFilteringConsumer implements the {@link Consumer} interface, and is used to filter out
   * fields in a JsonNode that meet requirements as specified by a given {@link NodeFilter}.
   */
  private static class NodeFilteringConsumer implements Consumer<JsonNode> {

    private final NodeFilter f;

    public NodeFilteringConsumer(NodeFilter f) {
      this.f = f;
    }

    @Override
    public void accept(JsonNode t) {
      if (f.apply(t)) {
        ObjectNode node = (ObjectNode) t;
        node.removeAll();
      }
    }
  }
}
