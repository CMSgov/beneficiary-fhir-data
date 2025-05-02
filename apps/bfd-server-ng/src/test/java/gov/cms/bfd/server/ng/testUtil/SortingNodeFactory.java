package gov.cms.bfd.server.ng.testUtil;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.TreeMap;

/**
 * Creates a JsonNode factory that uses a {@link TreeMap} instead of the default {@link
 * java.util.LinkedHashMap}. This forces the JSON document to serialize its nodes in alphabetical
 * order rather than insertion order.
 */
public class SortingNodeFactory extends JsonNodeFactory {
  @Override
  public ObjectNode objectNode() {
    return new ObjectNode(this, new TreeMap<>());
  }
}
