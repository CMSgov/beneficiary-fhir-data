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
 * This used to be fully recursive and could work for any resource, and then I realized that that
 * was really complicated and painful to edge case test. Right now only works for an
 * ExplanationOfBenefit represented by a Jackson JsonNode (which is conveniently what we get in
 * JsonSnapshotSerializer).
 */
public class SnapshotOrderDeterminizer3000 {

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

  // Records are tuples, TIL
  private record SequencedComponent(ObjectNode component, Integer oldSequence) {}

  /**
   * Wanted static calling with state, apparently this is a thing in Java.
   *
   * @param eob The JsonNode root of a
   */
  public static void order(JsonNode eob) {
    if (eob.isObject()) {
      new SnapshotOrderDeterminizer3000().orderInternal((ObjectNode) eob);
    }
  }

  private void orderInternal(ObjectNode eob) {

    // Okay so SOME recursion is still here, because extension arrays can be in many many different
    // places, so I didn't have to throw away all of my old code.
    orderExtensionsRecursively(eob);

    orderItemArray(eob);

    // Insurance is a top level array, but it doesn't have a sequence mapping back to .Line, so it's
    // chill
    var insuranceArrayNode = eob.path("insurance");
    if (insuranceArrayNode.isArray()) {
      orderUnsequencedArray((ArrayNode) insuranceArrayNode);
    }
  }

  /**
   * Recursive extension order and checker
   *
   * @param node the component, wherever in the chain
   */
  private static void orderExtensionsRecursively(JsonNode node) {
    if (node.isArray()) {
      node.forEach(SnapshotOrderDeterminizer3000::orderExtensionsRecursively);
    } else if (node.isObject()) {
      node.forEach(SnapshotOrderDeterminizer3000::orderExtensionsRecursively);
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
   * still. This is the most important method to get right.
   */
  private void orderItemArray(ObjectNode eob) {
    var sequenceMap = new HashMap<String, Map<Integer, Integer>>();

    for (var field : SEQUENCED_COMPONENTS) {
      var componentArray = eob.path(field);
      if (componentArray.isArray()) {
        sequenceMap.put(field, sortAndRenumber((ArrayNode) componentArray));
      }
    }

    var itemNode = eob.path("item");
    if (itemNode.isArray()) {
      var itemArray = (ArrayNode) itemNode;

      for (var item : itemArray) {
        var itemObj = (ObjectNode) item;

        for (var entry : LINE_ITEM_MAP.entrySet()) {
          var itemSequenceName = entry.getKey();
          var componentArrayName = entry.getValue();

          var remap = sequenceMap.get(componentArrayName);
          if (remap == null || !itemObj.has(itemSequenceName)) {
            continue;
          }

          var newSequence = remap.get(itemObj.get(itemSequenceName).asInt());
          if (newSequence != null) {
            itemObj.set(itemSequenceName, IntNode.valueOf(newSequence));
          }
        }
      }

      sortAndRenumber(itemArray);
    }
  }

  /**
   * Sorts elements by toString after removing sequence, then renumbers them and maps the old
   * sequence to the new
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
   * Strips the sequence element, creates a tuple of the element and the old sequence so the main
   * method can map them.
   *
   * @param node the array we're stripping and recording
   * @return a list of Tuple(component, sequence)
   */
  private static List<SequencedComponent> removeAndRecordSequence(ArrayNode node) {
    var elements = new ArrayList<SequencedComponent>();
    node.forEach(
        e -> {
          var obj = (ObjectNode) e;
          Integer oldSequence = obj.get("sequence").asInt();
          obj.remove("sequence");
          elements.add(new SequencedComponent(obj, oldSequence));
        });
    return elements;
  }
}
