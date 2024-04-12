package gov.cms.bfd.server.war.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gov.cms.bfd.server.war.commons.fhir.ccw.mapper.FHIR2CCWMapper;
import gov.cms.bfd.server.war.commons.fhir.ccw.mapper.FHIR2CCWMappingBuilder;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;

/** MetaModel a container for FHIR resources to CCW VARs (Fields). */
public class MetaModel {
  /** map of fhir 2 ccw mapper element path component to HAPI FHIR model FQCN. */
  public static Map<String, String> elementToClazz =
      Map.ofEntries(
          Map.entry("identifier", "org.hl7.fhir.r4.model.Identifier"),
          Map.entry("codeableConcept", "org.hl7.fhir.r4.model.CodeableConcept"));

  /** logger used to log messages etc. */
  private static Logger logger = Logger.getLogger("MetaModelLogger");

  /** json file suffix. */
  private static final String JSON_SUFFIX = ".json";

  /** C4BB profiles (json) folder. */
  private static final String C4BB_DIR = "/c4bb";

  /** Data dictionary json files. */
  private static final String DD_BASE_DIR = "/data-dictionary/data";

  /** FHIR json schema file. */
  private static final String FHIR_JSON_SCHEMA = "/fhir.schema.json";

  /** FHIR resource name: EOB. */
  private static final String RESOURCE_NAME_EOB = "ExplanationOfBenefit";

  /** FHIR resource name: Coverage. */
  private static final String RESOURCE_NAME_COVERAGE = "Coverage";

  /** FHIR resource name: Patient. */
  private static final String RESOURCE_NAME_PATIENT = "Patient";

  /** Json property name for CCW var (bfd table field) name. */
  private static final String CCW_VAR_NAME =
      "bfdColumnName"; // dd fld json attribute "bfdColumnName" is actually the ccw var name

  /** Json property name for CCW var (bfd table field) 'fhirMapping' attribute. */
  private static final String CCW_VAR_FHIR_MAPPING_KEY = "fhirMapping";

  /** Jackson object mapper. */
  private static final ObjectMapper jacksonMapper = new ObjectMapper();

  /**
   * fhir schema as json model, could be used for reference to standard FHIR resource definition.
   */
  @Getter private static final JsonNode fhirJsonSchema;

  /** C4BB profiles as json models. */
  @Getter private static final Map<String, JsonNode> c4bbProfiles;

  /** FHIR to CCW VARs (bfd fields) mapping in json model - V1 STU3. */
  @Getter private static ArrayNode fhir2CCWMappingV1 = null;

  /** FHIR to CCW VARs (bfd fields) mapping in json model - V2 R4. */
  @Getter private static ArrayNode fhir2CCWMappingV2 = null;

  /** FHIR to CCW VARs (bfd fields) mapping: ccw var name keyed mapping POJO - V2 R4. */
  @Getter private static Map<String, FHIR2CCWMappingBuilder> fhir2CCWMappingBuildersV2 = null;

  static {
    // load fhir json schema
    try {
      fhirJsonSchema = loadFhirJsonSchema();
    } catch (Exception e) {
      logger.log(Level.INFO, "Exception when load fhir schema json: " + e.toString());
      throw new RuntimeException(e);
    }
    // load C4BB profiles
    try {
      c4bbProfiles = loadC4BBProfiles();
    } catch (Exception e) {
      logger.log(Level.INFO, "Exception when load C4BB profiles (json): " + e.toString());
      throw new RuntimeException(e);
    }
    // load Dictionary
    try {
      // try consolidated if exists
      fhir2CCWMappingV1 = loadFHIR2CCWMapping("/fhir2ccwV1.json");
      if (fhir2CCWMappingV1 == null) {
        fhir2CCWMappingV1 = loadFHIR2CCWMapping(DD_BASE_DIR + "/V1");
      }
    } catch (Exception e) {
      logger.log(
          Level.INFO, "Exception when load fhir to ccw mapping v1 files (json): " + e.toString());
      throw new RuntimeException(e);
    }
    try {
      fhir2CCWMappingV2 = loadFHIR2CCWMapping("/fhir2ccwV2.json");
      if (fhir2CCWMappingV2 == null) {
        fhir2CCWMappingV2 = loadFHIR2CCWMapping(DD_BASE_DIR + "/V2");
      }
    } catch (Exception e) {
      logger.log(
          Level.INFO, "Exception when load fhir to ccw mapping v2 files (json): " + e.toString());
      throw new RuntimeException(e);
    }

    if (fhir2CCWMappingV2 != null) {
      try {
        fhir2CCWMappingBuildersV2 = loadMappingBuilders(fhir2CCWMappingV2);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * main entry.
   *
   * @param args CLI args
   */
  public static void main(String[] args) {
    // dump all the dd components
    jacksonMapper.enable(SerializationFeature.INDENT_OUTPUT);
    // dump fhir schema
    dump(fhirJsonSchema, "FHIR_SCHEMA_JSON.json");
    // dump v2 fhir to bfd mapping
    dump(fhir2CCWMappingV2, "fhir2ccwV2.json");
    // dump v1 fhir to bfd mapping
    dump(fhir2CCWMappingV1, "fhir2ccwV1.json");
  }

  /**
   * Helper: dump json model to file.
   *
   * @param json Json tree model
   * @param filePath file to write to
   */
  private static void dump(JsonNode json, String filePath) {
    try {
      String fhirSchemaStr = jacksonMapper.writeValueAsString(json);
      Path file = Paths.get(filePath);
      Files.writeString(file, fhirSchemaStr);
    } catch (IOException e) {
      // throw new RuntimeException(e);
      logger.log(
          Level.INFO,
          String.format(
              "IOException caught when write to file: %s, error: %s.", filePath, e.getMessage()));
    }
  }

  /**
   * Load fhir schema json from the resource folder.
   *
   * @return json tree model
   * @throws IOException IO error when reading the json file
   * @throws Exception Error when locate the json file as resource
   */
  private static JsonNode loadFhirJsonSchema() throws IOException, Exception {
    URL resourcesURL = MetaModel.class.getResource(FHIR_JSON_SCHEMA);
    assert resourcesURL != null;
    return jacksonMapper.readTree(Paths.get(resourcesURL.toURI()).toFile());
  }

  /**
   * Load C4BB profiles as json models and put them into a map, keyed by profile file name.
   *
   * @return map of all c4bb profiles
   * @throws IOException Error reading profile
   * @throws Exception Error locating the file
   */
  private static Map<String, JsonNode> loadC4BBProfiles() throws IOException, Exception {
    Map<String, JsonNode> c4bbProfileMap = new HashMap<String, JsonNode>();
    URL resourcesURL = MetaModel.class.getResource(C4BB_DIR);
    assert resourcesURL != null;
    File c4bbDir = Paths.get(resourcesURL.toURI()).toFile();
    File[] c4bbProfiles =
        c4bbDir.listFiles(
            new FilenameFilter() {
              public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(JSON_SUFFIX);
              }
            });
    assert c4bbProfiles != null;
    for (File f : c4bbProfiles) {
      JsonNode r = jacksonMapper.readTree(f);
      c4bbProfileMap.put(r.get("id").textValue(), r);
    }
    return c4bbProfileMap;
  }

  // ExplanationOfBenefit, Patient, Coverage as key to a map of CCW var to its diction descriptor
  // keyed by: resource name, fhir path
  // for now just keep the whole descriptor (v), extract only used bits and save in the map later
  // in V1, for now, use col name + element as place holder, total 628
  /**
   * Load FHIR to CCW VARs (bfd fields) mapping descriptor json files from a folder.
   *
   * @param path where the mapping json files live
   * @return a json array of fhir to ccw mapping models
   * @throws IOException Error when reading json files
   * @throws Exception Error when resolving json files location
   */
  private static ArrayNode loadFHIR2CCWMapping(String path) throws IOException, Exception {
    URL resourceDictionaryData = MetaModel.class.getResource(path);
    if (resourceDictionaryData == null) return null;
    File dictionaryDataDir = Paths.get(resourceDictionaryData.toURI()).toFile();
    ArrayNode fhir2ccwMapping = null;
    if (dictionaryDataDir.isDirectory()) {
      File[] dictionFldJson =
          dictionaryDataDir.listFiles(
              new FilenameFilter() {
                public boolean accept(File dir, String name) {
                  return name.toLowerCase().endsWith(JSON_SUFFIX);
                }
              });
      assert dictionFldJson != null;
      fhir2ccwMapping = jacksonMapper.createArrayNode();
      for (File f : dictionFldJson) {
        FHIR2CCWMapper pojo = null;
        if (path.endsWith("V2")) {
          pojo = jacksonMapper.readValue(f, FHIR2CCWMappingBuilder.class);
        }
        fhir2ccwMapping.add(jacksonMapper.readTree(f));
      }
    } else {
      fhir2ccwMapping = (ArrayNode) jacksonMapper.readTree(dictionaryDataDir);
    }
    return fhir2ccwMapping;
  }

  /**
   * Build POJO based mapper from JsonNode based.
   *
   * @param mappingJsonArray json based mapper
   * @return map of pojo based mapper keyed by ccw var name (bfd column name)
   * @throws JsonProcessingException malformed json
   */
  private static Map<String, FHIR2CCWMappingBuilder> loadMappingBuilders(ArrayNode mappingJsonArray)
      throws JsonProcessingException {
    Map<String, FHIR2CCWMappingBuilder> mapper = new HashMap<String, FHIR2CCWMappingBuilder>();
    for (JsonNode n : mappingJsonArray) {
      FHIR2CCWMappingBuilder pojo = jacksonMapper.treeToValue(n, FHIR2CCWMappingBuilder.class);
      // there are ccw mappings that do not have BFD column:
      // e.g. id = 610, 611, 614, 615 etc. coverage group, sub group
      // in such case, use id + name as key
      String key =
          pojo.getBfdColumnName().isEmpty()
              ? pojo.getId() + ":" + pojo.getName()
              : pojo.getBfdColumnName();
      mapper.put(pojo.getBfdColumnName(), pojo);
    }
    return mapper;
  }

  /**
   * lookup fhir mapping by bfd fld name.
   *
   * @param bfdFldName bfd fld name
   * @return fhirMappiong node
   */
  public static FHIR2CCWMappingBuilder getFhirMapping(String bfdFldName) {
    return fhir2CCWMappingBuildersV2.get(bfdFldName);
  }

  /**
   * Given resource name and claim type and property of the struct def, return the prop value, to
   * e.g. the url etc.
   *
   * @param resourceName the resource name
   * @param claimType the claim type
   * @param propertyName the prop of the struct def
   * @return the prop value
   */
  public static String getC4BBProfile(String resourceName, String claimType, String propertyName) {
    return c4bbProfiles
        .get(String.format("C4BB-%s-%s", resourceName, claimType))
        .get(propertyName)
        .textValue();
  }

  /**
   * Helper generate jsTree node tree from data json tree.
   *
   * @param root - root of the json tree
   * @param jsTree - corresponding jsTree node
   */
  public static void traverse(JsonNode root, JsonNode jsTree) {
    if (root.isObject()) {
      Iterator<String> fieldNames = root.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        JsonNode fieldValue = root.get(fieldName);
        traverse(fieldValue, jsTree);
      }
    } else if (root.isArray()) {
      ArrayNode arrayNode = (ArrayNode) root;
      for (int i = 0; i < arrayNode.size(); i++) {
        JsonNode arrayElement = arrayNode.get(i);
        traverse(arrayElement, jsTree);
      }
    } else {
      // JsonNode root represents a single value field - do something with it.
    }
  }
}
