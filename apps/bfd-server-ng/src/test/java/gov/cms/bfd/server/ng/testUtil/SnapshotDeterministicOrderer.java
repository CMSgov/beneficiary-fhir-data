package gov.cms.bfd.server.ng.testUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This used to be fully recursive and could work for any resource, and the bones of that code are
 * still in here. Right now, this only works for an ExplanationOfBenefit represented by a Jackson
 * JsonNode.
 */
public class SnapshotDeterministicOrderer {

  // Component arrays that can be referenced by ExplanationOfBenefit.Item
  private static final Set<String> SEQUENCED_COMPONENTS =
      Set.of("diagnosis", "procedure", "careTeam", "supportingInfo");

  // Mapping of the ExplanationOfBenefit.Item.xSequence to the array in the EoB
  private static final Map<String, String> LINE_ITEM_MAP =
      Map.of(
          "diagnosisSequence", "diagnosis",
          "procedureSequence", "procedure",
          "careTeamSequence", "careTeam",
          "informationSequence", "supportingInfo");

  private record SequencedComponent(ObjectNode component, Integer oldSequence) {}

  /**
   * State exists over the course of ordering and then is thrown away, wrapped in static function
   * call for convenience.
   *
   * @param eob the root JsonNode of a serialized ExplanationOfBenefit
   */
  public static void order(JsonNode eob) {
    if (eob.isObject()) {
      new SnapshotDeterministicOrderer().orderInternal((ObjectNode) eob);
    }
  }

  private void orderInternal(ObjectNode eob) {

    // Extensions can appear anywhere on the object, so we need to recursively scan the object for
    // extension arrays.
    orderExtensionsRecursively(eob);

    // EoB.Item contains important sequence references to 4 other component arrays, those are
    // handled together.
    orderItemArray(eob);

    // Insurance is a top level array, but it doesn't have a sequence mapping back to .Line. This
    // can be generalized
    // in the future if there are additional unsequenced component arrays that we need to order.
    var insuranceArrayNode = eob.path("insurance");
    if (insuranceArrayNode.isArray()) {
      orderUnsequencedArray((ArrayNode) insuranceArrayNode);
    }
  }

  /**
   * Recursive extension array finder and reorderer
   *
   * @param node the component, wherever in the chain
   */
  private static void orderExtensionsRecursively(JsonNode node) {
    if (node.isArray()) {
      node.forEach(SnapshotDeterministicOrderer::orderExtensionsRecursively);
    } else if (node.isObject()) {
      node.forEach(SnapshotDeterministicOrderer::orderExtensionsRecursively);
      var extensionNode = node.path("extension");
      if (extensionNode.isArray()) {
        orderUnsequencedArray((ArrayNode) extensionNode);
      }
    }
  }

  /**
   * Simple sorting of an ArrayNode based on the internal JsonNode's toString result
   *
   * @param node the ArrayNode to be sorted
   */
  private static void orderUnsequencedArray(ArrayNode node) {
    var components = new ArrayList<JsonNode>();
    node.forEach(components::add);
    components.sort(Comparator.comparing(JsonNode::toString));
    node.removeAll();
    node.addAll(components);
  }

  /**
   * Orders each SEQUENCED_COMPONENTS array first, remember their old sequence, map to new sequence,
   * then populate item array with the new sequence numbers so that the numbers are all correct
   * still.
   */
  private void orderItemArray(ObjectNode eob) {
    var sequenceMap = new HashMap<String, Map<Integer, Integer>>();

    for (var field : SEQUENCED_COMPONENTS) {
      var componentArrayNode = eob.path(field);
      if (componentArrayNode.isArray()) {
        sequenceMap.put(field, sortAndRenumber((ArrayNode) componentArrayNode));
      }
    }

    var itemNode = eob.path("item");
    if (itemNode.isArray()) {
      var itemArray = (ArrayNode) itemNode;

      for (var item : itemArray) {
        var itemComponent = (ObjectNode) item;

        for (var entry : LINE_ITEM_MAP.entrySet()) {
          var itemSequenceName = entry.getKey();
          var componentArrayName = entry.getValue();

          var remap = sequenceMap.get(componentArrayName);

          // This is always true for CareTeam and Procedure, and sometimes true for the other two.
          if (remap == null || !itemComponent.has(itemSequenceName)) {
            continue;
          }

          var newSequence = remap.get(itemComponent.get(itemSequenceName).asInt());
          if (newSequence != null) {
            itemComponent.set(itemSequenceName, IntNode.valueOf(newSequence));
          }
        }
      }

      sortAndRenumber(itemArray);
    }
  }

  /**
   * Sorts elements by toString after removing sequence, then renumbers them and re-adds the
   * sequence element.
   */
  private static Map<Integer, Integer> sortAndRenumber(ArrayNode node) {
    var elements = removeAndRecordSequence(node);
    elements.sort(Comparator.comparing(e -> e.component().toString()));

    var remap = new HashMap<Integer, Integer>();
    node.removeAll();
    for (int i = 0; i < elements.size(); i++) {
      var element = elements.get(i);
      remap.put(element.oldSequence(), i + 1);
      // Re-add sequence
      element.component().set("sequence", IntNode.valueOf(i + 1));
      node.add(element.component());
    }
    return remap;
  }

  /**
   * Strips the sequence element, creates a tuple of the element and the old sequence
   *
   * @param node the array we're stripping and recording
   * @return a list of Tuple(component, sequence)
   */
  private static List<SequencedComponent> removeAndRecordSequence(ArrayNode node) {
    var elements = new ArrayList<SequencedComponent>();
    node.forEach(
        e -> {
          var component = (ObjectNode) e;
          var oldSequence = component.get("sequence").asInt();
          component.remove("sequence");
          elements.add(new SequencedComponent(component, oldSequence));
        });
    return elements;
  }
}
