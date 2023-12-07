package gov.cms.bfd.server.war.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Utility class for helping with assertions. */
public class AssertUtils {

  /** Interface for executing. TODO: What is this? */
  @FunctionalInterface
  public interface Executor {
    /** Executes something. */
    void execute();
  }

  /**
   * Catch exceptions using an {@link Executor}.
   *
   * <p>TODO: Unused code, delete this and the above. exceptions can be thrown by tests without this
   *
   * @param executor the executor
   * @return the exception
   */
  public static Exception catchExceptions(Executor executor) {
    Exception ex = null;

    try {
      executor.execute();
    } catch (Exception e) {
      ex = e;
    }

    return ex;
  }

  /**
   * Checks if two exception messages are equal, and fails the test if not.
   *
   * <p>TODO: Unused, remove
   *
   * @param expected the expected exception
   * @param actual the actual exception
   */
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
   * Checks if two json strings are equivalent by converting both to pretty printed strings and
   * comparing those strings. If the two are not equivalent, the assertion will include the
   * differences between the two strings. Individual elements within the JSON can be ignored for
   * purposes of comparison by providing their paths in the provided set.
   *
   * @param expected The expected json string
   * @param actual The actual json string
   * @param ignorePaths Set of Node paths that should be ignored during comparison.
   */
  public static void assertJsonEquals(String expected, String actual, Set<String> ignorePaths) {
    // Or together all of the paths, using parens to ensure there is no ambiguity.
    final String ignoredPathRegex = String.format("(%s)", String.join(")|(", ignorePaths));

    final Pattern ignoredPathPattern = Pattern.compile(ignoredPathRegex);
    final JsonFilter filter = new JsonFilter(ignoredPathPattern);

    final String expectedJson;
    try {
      expectedJson = filter.filterJson(expected);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + expected, e);
    }

    final String actualJson;
    try {
      actualJson = filter.filterJson(actual);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + actual, e);
    }

    assertEquals(expectedJson, actualJson);
  }

  /**
   * Used internally to format and filter JSON into strings suitable for comparison in unit test
   * assertions.
   *
   * @param ignoredPathPattern Regex used to identify nodes that need to be ignored.
   */
  private record JsonFilter(Pattern ignoredPathPattern) {
    /** Constant value used to replace real value of any nodes we need to ignore in the JSON. */
    private static final String IGNORED_VALUE = "--ignored-value--";

    /**
     * Shared mapper used serialize and deserialize JSON. Uses indenting and field sorting to ensure
     * string differences are easy to understand in assertion failure messages.
     */
    private static final ObjectMapper mapper =
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    /**
     * Convert the provided JSON string into one that has fields filtered, sorted, and pretty
     * printed in preparation for use in string comparison.
     *
     * @param jsonString JSON to be processed
     * @return filtered, sorted, and pretty printed JSON
     * @throws JsonProcessingException indicates some problem with the JSON string
     */
    private String filterJson(String jsonString) throws JsonProcessingException {
      Map<String, Object> jsonMap = stringToMap(jsonString);
      filterMapElements(new ArrayList<>(), jsonMap);
      return mapToString(jsonMap);
    }

    /**
     * Parses a JSON string into a map.
     *
     * @param jsonString JSON to be processed
     * @return parsed json represented as maps, lists, and values
     * @throws JsonProcessingException indicates some problem with the JSON string
     */
    private Map<String, Object> stringToMap(String jsonString) throws JsonProcessingException {
      return mapper.readValue(jsonString, new TypeReference<HashMap<String, Object>>() {});
    }

    /**
     * Serializes a map back into equivalent JSON.
     *
     * @param jsonMap map to serialize
     * @return equivalent JSON string
     * @throws JsonProcessingException indicates some problem with the JSON string
     */
    private String mapToString(Map<String, Object> jsonMap) throws JsonProcessingException {
      return mapper.writeValueAsString(jsonMap);
    }

    /**
     * Combines the path elements into a node path string.
     *
     * @param path elements of the path
     * @return elements combined into a single path string
     */
    private String pathToString(List<String> path) {
      return "/" + String.join("/", path);
    }

    /**
     * Compares the path to our regex to determine if the node with this path should be ignored.
     *
     * @param path elements of the path
     * @return true if the node with this path should be ignored
     */
    private boolean isFilteredPath(List<String> path) {
      final var pathString = pathToString(path);
      return ignoredPathPattern.matcher(pathString).matches();
    }

    /**
     * Recursively applies path filtering to all of the nodes in the given list.
     *
     * @param path base path for the list
     * @param list list of nodes to be filtered
     */
    @SuppressWarnings("unchecked")
    private void filterListElements(List<String> path, List<Object> list) {
      for (int index = 0; index < list.size(); ++index) {
        path.add(String.valueOf(index));
        try {
          Object value = list.get(index);
          if (isFilteredPath(path)) {
            // we have to actually remove this later so indexes remain unchanged
            list.set(index, IGNORED_VALUE);
          } else if (value instanceof Map) {
            Map<String, Object> children = (Map<String, Object>) value;
            filterMapElements(path, children);
          } else if (value instanceof List) {
            List<Object> elements = (List<Object>) value;
            filterListElements(path, elements);
          }
        } finally {
          path.removeLast();
        }
      }
    }

    /**
     * Recursively applies path filtering to all of the nodes in the given map.
     *
     * @param path base path for the map
     * @param map map of nodes to be filtered
     */
    @SuppressWarnings("unchecked")
    private void filterMapElements(List<String> path, Map<String, Object> map) {
      var mapEntries = List.copyOf(map.entrySet());
      for (Map.Entry<String, Object> node : mapEntries) {
        path.add(node.getKey());
        try {
          if (isFilteredPath(path)) {
            map.put(node.getKey(), IGNORED_VALUE);
          } else if (node.getValue() instanceof Map) {
            Map<String, Object> children = (Map<String, Object>) node.getValue();
            filterMapElements(path, children);
          } else if (node.getValue() instanceof List) {
            List<Object> elements = (List<Object>) node.getValue();
            filterListElements(path, elements);
          }
        } finally {
          path.removeLast();
        }
      }
    }
  }
}
