package gov.cms.bfd.server.ng.testUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Creates a JsonNode factory that uses a {@link TreeMap} instead of the default {@link
 * java.util.LinkedHashMap}. This forces the JSON document to serialize its nodes in alphabetical
 * order rather than insertion order.
 */
public class SortingNodeFactory extends JsonNodeFactory {

  // List of component arrays (only for ExplanationOfBenefits at the moment) to be sorted
  private static final Set<String> COMPONENT_ARRAYS =
      Set.of("supportingInfo", "careTeam", "procedure", "diagnosis", "item", "extension");

  @Override
  public ObjectNode objectNode() {
    return new SortingObjectNode(this);
  }

  private static class SortingObjectNode extends ObjectNode {
    SortingObjectNode(JsonNodeFactory nc) {
      super(nc, new TreeMap<>());
    }

    @Override
    public <T extends JsonNode> T set(String fieldName, JsonNode value) {
      // only sort the lists of components we care about across all FHIR resources
      if (COMPONENT_ARRAYS.contains(fieldName) && value instanceof ArrayNode) {
        sortArray((ArrayNode) value);
      }
      return super.set(fieldName, value);
    }

    private void sortArray(ArrayNode array) {
      List<JsonNode> elements = new ArrayList<>();
      for (JsonNode n : array) {
        elements.add(n);
      }
      elements.sort(Comparator.comparing(JsonNode::toString));
      array.removeAll();
      for (JsonNode n : elements) {
        array.add(n);
      }
    }
  }
}
