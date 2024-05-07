package gov.cms.bfd.server.war.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import gov.cms.bfd.server.war.commons.fhir.ccw.mapper.FHIR2CCWMapper;
import gov.cms.bfd.server.war.commons.fhir.ccw.mapper.FHIR2CCWMappingBuilder;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** MetaModel a container for FHIR resources to CCW VARs (Fields). */
@SuppressWarnings("all")
public class MetaModel {
  public enum C4BBProfile {
    CODESYSTEM("CodeSystem"),
    VALUESET("ValueSet"),
    SEARCHPARAMETER("SearchParameter"),
    STRUCTUREDEFINITION("StructureDefinition");

    public final String label;

    private C4BBProfile(String label) {
      this.label = label;
    }
  }

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
      fhir2CCWMappingV1 = loadFHIR2CCWMapping("/fhir2ccw.v1.json");
      if (fhir2CCWMappingV1 == null) {
        fhir2CCWMappingV1 = loadFHIR2CCWMapping(DD_BASE_DIR + "/V1");
      }
    } catch (Exception e) {
      logger.log(
          Level.INFO, "Exception when load fhir to ccw mapping v1 files (json): " + e.toString());
      throw new RuntimeException(e);
    }
    try {
      fhir2CCWMappingV2 = loadFHIR2CCWMapping("/fhir2ccw.v2.json");
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
    jacksonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    jacksonMapper.enable(SerializationFeature.INDENT_OUTPUT);
    // dump fhir schema
    dump(fhirJsonSchema, "FHIR_SCHEMA_JSON.json");
    // dump v2 fhir to bfd mapping
    dump(fhir2CCWMappingV2, "fhir2ccw.v2.json");
    // dump v1 fhir to bfd mapping
    dump(fhir2CCWMappingV1, "fhir2ccw.v1.json");
    // normalize fhir to ccw mapping section of fhir2ccw.v1.json and fhir2ccw.v2.json
    // and dump the results to fhir2ccw.v1.normalized.json and fhir2ccw.v2.normalized.json
    // respectively
    //    try {
    //      dump(normalizeFHIR2CCWMapping("/fhir2ccw.v1.json"), "fhir2ccw.v1.normalized.json");
    //    } catch (Exception e) {
    //      throw new RuntimeException(e);
    //    }
    try {
      dump(normalizeFHIR2CCWMapping("/fhir2ccw.v2.json"), "fhir2ccw.v2.normalized.json");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
      c4bbProfileMap.put(r.get("resourceType").textValue() + "-" + r.get("id").textValue(), r);
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
   * Load FHIR to CCW VARs (bfd fields) mapping descriptor json files from a folder. Parse the path
   * expressions in fhirMapping and re-construct them into FHIRPaths, and wrap them in
   * fhirMappingNormalized structure.
   *
   * @param path to the fhir to ccw mapping json (consolidated)
   * @return a json array of fhir to ccw mapping models with the normalized "fhirMappingNormalized"
   *     structure attached
   * @throws IOException Error when reading json files
   * @throws Exception Error when resolving json files location
   */
  private static ArrayNode normalizeFHIR2CCWMapping(String path) throws IOException, Exception {
    URL fhir2ccwMappingsURL = MetaModel.class.getResource(path);
    if (fhir2ccwMappingsURL == null) return null;
    File jsonFile = Paths.get(fhir2ccwMappingsURL.toURI()).toFile();
    ArrayNode fhir2ccwMappings = (ArrayNode) jacksonMapper.readTree(jsonFile);
    for (JsonNode n : fhir2ccwMappings) {
      String ccwVarStr = n.get("bfdColumnName").textValue();
      JsonNode fmList = n.get("fhirMapping");
      JsonNode fmObj = fmList.get(0);
      String resource = fmObj.get("resource").textValue();
      // element e.g. item[N].adjudication[N].amount.value
      // convert to fhir path: ExplanationOfBenefit.item.adjudication.amount.value
      JsonNode element = fmObj.get("element");
      JsonNode fhirPath = fmObj.get("fhirPath");
      JsonNode additional = fmObj.get("additional");
      JsonNode derived = fmObj.get("derived");
      // after normalization, "example" ignored by jackson
      JsonNode example = fmObj.get("example");
      if (example != null) {
        String exampleStr = example.textValue().replaceAll("\\\\n", "");
        JsonNode exampleJson = jacksonMapper.readTree(exampleStr);
        ((ObjectNode) fmObj).put("example", exampleJson == null ? new TextNode("") : exampleJson);
      } else {
        System.out.println("example is null" + ccwVarStr);
      }
      JsonNode fmCopy = fmList.deepCopy();
      ArrayList listDiscriminator =
          convertExpressionList("discriminator", fmObj.get("discriminator"));
      ArrayList listAdditional = convertExpressionList("additional", fmObj.get("additional"));
      ((ObjectNode) fmCopy.get(0))
          .set("discriminator", jacksonMapper.valueToTree(listDiscriminator));
      ((ObjectNode) fmCopy.get(0)).set("additional", jacksonMapper.valueToTree(listAdditional));
      ((ObjectNode) n).put("fhirMapping", fmCopy);
    }
    return fhir2ccwMappings;
  }

  private static ArrayList convertExpressionList(String listName, JsonNode list) {
    Map<String, Map<String, String>> kv = new HashMap();
    Pattern p = Pattern.compile("[\s]*([a-zA-Z0-9.]+)[\s]*=[\s]*(.*)[\s]*");
    if (list.isArray()) {
      ArrayList result = new ArrayList();
      for (JsonNode d : list) {
        String elemStr = d.textValue();
        Matcher m = p.matcher(elemStr);
        if (m.matches()) {
          String l = m.group(1);
          String r = m.group(2);
          System.out.println(listName + ":L: " + l);
          System.out.println(listName + ":R: " + r);
          boolean hit = false;
          for (String arg : new String[] {"system", "code", "display"}) {
            String tail = String.format(".%s", arg);
            if (l.endsWith(tail)) {
              hit = true;
              String prefix = getPrefix(l, tail);
              if (kv.get(prefix) == null) {
                kv.put(prefix, new HashMap<>());
              }
              kv.get(prefix).put(arg, r);
            }
          }
          if (!hit) {
            System.err.println(
                listName + ":Keep as is: left side has no system / code / display..." + l);
          }
        } else {
          // skip a line
          System.err.println(listName + ":Keep as is: no match the pattern L=R" + elemStr);
          result.add(elemStr);
        }
      }
      // generate discriminator list and set it
      for (String k : kv.keySet()) {
        Map obj = kv.get(k);
        String args = null;
        for (String arg : new String[] {"system", "code", "display"}) {
          if (obj.get(arg) != null) {
            if (args == null) {
              args = String.format("%s=%s", arg, obj.get(arg));
            } else {
              args = args + String.format(", %s=%s", arg, obj.get(arg));
            }
          }
        }
        String dStr = String.format("%s(%s)", k, args);
        result.add(dStr);
      }
      return result;
    }
    throw new RuntimeException(String.format("listName = %s, Malformed : not an array.", listName));
  }

  private static String getPrefix(String str, String tail) {
    String r = null;
    int index = str.lastIndexOf(tail);
    if (index > 0) {
      r = str.substring(0, index);
    }
    return r;
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
  public static String getC4BBProfile(
      C4BBProfile c4bbProfile, String resourceName, String claimType, String propertyName) {
    String k = String.format("%s-C4BB-%s-%s", c4bbProfile.label, resourceName, claimType);
    JsonNode r = c4bbProfiles.get(k);
    return r.get(propertyName).textValue();
  }

  /**
   * Get CC from C4BB profiles.
   *
   * @param systemUrl system url of the code
   * @param conceptCode the code value
   * @return the CC
   */
  public static CodeableConcept getC4BBCodeSystem(
      C4BBProfile c4bbProfile, String systemUrl, String conceptCode) {
    JsonNode cc4bCodeSystem = c4bbProfiles.get(c4bbProfile.label + "-" + getURLLast(systemUrl));
    JsonNode concepts = cc4bCodeSystem.get("concept");
    CodeableConcept cc = new CodeableConcept();
    if (concepts.isArray()) {
      for (JsonNode c : concepts) {
        if (c.get("code").textValue().equals(conceptCode)) {
          cc.setCoding(
              List.of(
                  new Coding(systemUrl, c.get("code").textValue(), c.get("display").textValue())));
          break;
        }
      }
    }
    return cc;
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

  /**
   * helper get the last part of cc4b url which is the id of the resource, and also the key of the
   * profile map.
   *
   * @param url the system url.
   * @return the last part of url.
   */
  private static String getURLLast(String url) {
    URL urlObj = null;
    try {
      urlObj = new URL(url.replace("'", ""));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    String[] comps = urlObj.getPath().split("/");
    return comps[comps.length - 1];
  }
}
