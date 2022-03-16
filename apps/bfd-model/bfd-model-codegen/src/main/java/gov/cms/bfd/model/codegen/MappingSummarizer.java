package gov.cms.bfd.model.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;

public class MappingSummarizer {
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
  private final ArrayNode root = objectMapper.createArrayNode();

  public ObjectNode createObject() {
    return objectMapper.createObjectNode();
  }

  public ArrayNode createArray() {
    return objectMapper.createArrayNode();
  }

  public void addObject(JsonNode node) {
    root.add(node);
  }

  public void writeToFile(File file) throws IOException {
    objectMapper.writeValue(file, root);
  }
}
