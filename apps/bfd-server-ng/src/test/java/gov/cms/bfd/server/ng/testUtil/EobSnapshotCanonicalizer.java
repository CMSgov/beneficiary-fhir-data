package gov.cms.bfd.server.ng.testUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;

/** Helper class for reporter and comparator to use the deterministic orderer, wraps that logic. */
public final class EobSnapshotCanonicalizer {

  private EobSnapshotCanonicalizer() {}

  /**
   * Searches and orders EoBs in a JSON string.
   *
   * @param rawJson the JSON string from a result or snapshot
   * @return a JsonNode with all the EoB's reordered
   */
  @SneakyThrows
  public static JsonNode canonicalize(String rawJson) {
    var keyOrderingObjectMapper =
        JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();
    var node = keyOrderingObjectMapper.reader().readTree(rawJson);
    searchAndCanonicalizeEobRecursive(node);
    return node;
  }

  /**
   * Sometimes EoB's are in bundles or entries, need to just search the node recursively for all
   * EoB's. This is good because it means it's easier to make it resource agnostic in the future.
   * Same recursive logic as the orderer.
   *
   * @param node the root node of whatever test output and snapshot input we have
   */
  private static void searchAndCanonicalizeEobRecursive(JsonNode node) {
    if (node.isArray()) {
      node.forEach(EobSnapshotCanonicalizer::searchAndCanonicalizeEobRecursive);
    } else if (node.isObject()) {
      // We've found an EoB, order the elements it
      if (node.path("resourceType").asText("").equals("ExplanationOfBenefit")) {
        SnapshotDeterministicOrderer.order(node);
      } else {
        node.forEach(EobSnapshotCanonicalizer::searchAndCanonicalizeEobRecursive);
      }
    }
  }
}
