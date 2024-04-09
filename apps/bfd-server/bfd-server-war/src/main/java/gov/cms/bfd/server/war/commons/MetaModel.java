package gov.cms.bfd.server.war.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
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

  static {
    Configuration.setDefaults(
        new Configuration.Defaults() {
          private final JsonProvider jsonProvider = new JacksonJsonProvider();
          private final MappingProvider mappingProvider = new JacksonMappingProvider();

          @Override
          public JsonProvider jsonProvider() {
            return jsonProvider;
          }

          @Override
          public MappingProvider mappingProvider() {
            return mappingProvider;
          }

          @Override
          public Set<Option> options() {
            return EnumSet.noneOf(Option.class);
          }
        });
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
  }

  /**
   * main entry.
   *
   * @param args CLI args
   */
  public static void main(String[] args) {
    // dump all the dd components
    jacksonMapper.enable(SerializationFeature.INDENT_OUTPUT);
    JsonNode theMap = getFhirMapping("line_alowd_chrg_amt", 2);

    //    "resource" : "ExplanationOfBenefit",
    //    "element" : "item[N].adjudication[N].amount.value",
    //    "fhirPath" : "item[%n].adjudication.where(category.coding.where(system =
    // 'https://bluebutton.cms.gov/resources/codesystem/adjudication' and code =
    // 'https://bluebutton.cms.gov/resources/variables/line_alowd_chrg_amt')).amount.value",
    //    "discriminator" : [ "item[N].adjudication[N].category.coding[N].system =
    // 'https://bluebutton.cms.gov/resources/codesystem/adjudication'",
    // "item[N].adjudication[N].category.coding[N].code =
    // 'https://bluebutton.cms.gov/resources/variables/line_alowd_chrg_amt'" ],
    //    "additional" : [ "(eob.item[N].adjudication[N].category.coding[N].system =
    // 'http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication'",
    // "eob.item[N].adjudication[N].category.coding[N].code = 'eligible'",
    // "eob.item[N].adjudication[N].category.coding[N].display = 'Eligible Amount')",
    // "eob.item[N].adjudication[N].category.coding[N].display = 'Line Allowed Charge Amount'",
    // "eob.item[N].adjudication[N].amount.currency = 'USD'" ],

    // dump fhir schema
    dump(fhirJsonSchema, "FHIR_SCHEMA_JSON.json");
    // dump v2 fhir to bfd mapping
    dump(fhir2CCWMappingV1, "fhir2ccwV1.json");
    // dump v1 fhir to bfd mapping
    dump(fhir2CCWMappingV2, "fhir2ccwV2.json");
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

  // in V2, the fhir path is unique and always present
  // note, there are ccw vars with empty fhir path, 20 out of 629 (total), need to create them
  // ./0390_rev_cntr_pmt_mthd_ind_cd.json:            "fhirPath": "",
  // ./0388_rev_cntr_packg_ind_cd.json:            "fhirPath": "",
  // ./0443_bene_crnt_hic_num.json:            "fhirPath": "",
  // ./0444_bene_crnt_hic_num.json:            "fhirPath": "",
  // ./0034_clm_drg_outlier_stay_cd.json:            "fhirPath": "",
  // ./0581_bene_ptb_trmntn_cd.json:            "fhirPath": "",
  // ./0422_bene_id.json:            "fhirPath": "",
  // ./0446_bene_crnt_hic_num.json:            "fhirPath": "",
  // ./0381_rev_cntr_dscnt_ind_cd.json:            "fhirPath": "",
  // ./0573_bene_id.json:            "fhirPath": "",
  // ./0026_carr_line_rx_num.json:            "fhirPath": "",
  // ./0375_rev_cntr_apc_hipps_cd.json:            "fhirPath": "",
  // ./0355_prscrbr_id_qlfyr_cd.json:            "fhirPath": "",
  // ./0429_mbi_effective_end_date.json:            "fhirPath": "",
  // ./0130_hpsa_scrcty_ind_cd.json:            "fhirPath": "",
  // ./0028_carr_prfrng_pin_num.json:            "fhirPath": "",
  // ./0609_contract_reference.json:            "fhirPath": "",
  // ./0608_contract_identifier.json:            "fhirPath": "",
  // ./0387_rev_cntr_otaf_pmt_cd.json:            "fhirPath": "",
  // ./0445_mbi_num.json:            "fhirPath": "",

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
    assert resourceDictionaryData != null;
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
        fhir2ccwMapping.add(jacksonMapper.readTree(f));
      }
    } else {
      fhir2ccwMapping = (ArrayNode) jacksonMapper.readTree(dictionaryDataDir);
    }
    return fhir2ccwMapping;
  }

  /**
   * lookup fhir mapping by bfd fld name.
   *
   * @param bfdFldName bfd fld name
   * @param version version of fhir 1 - STU3, 2 - R4
   * @return fhirMappiong node
   */
  public static JsonNode getFhirMapping(String bfdFldName, int version) {
    JsonNode mapping = null;
    ArrayNode mappings = (version == 1) ? fhir2CCWMappingV1 : fhir2CCWMappingV2;
    for (JsonNode n : mappings) {
      String fldName = n.get("bfdColumnName").textValue();
      if (fldName.equals(bfdFldName)) {
        mapping = n;
        break;
      }
    }
    return mapping;
  }

  /**
   * Given resource name and claim type, return the c4bb structure definition json object, to e.g.
   * extract url etc.
   *
   * @param resourceName the resource name
   * @param claimType the claim type
   * @return json node for the structure definition resource
   */
  public static JsonNode getC4BBProfile(String resourceName, String claimType) {
    return c4bbProfiles.get(
        String.format("StructureDefinition-C4BB-%s-%s", resourceName, claimType));
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

  // Wrap JsonNode with jsTree node for tree rendering
  // in js
  //
  //  {
  //    id          : "string" // will be autogenerated if omitted
  //    text        : "string" // node text
  //    icon        : "string" // string for custom
  //    state       : {
  //      opened    : boolean  // is the node open
  //      disabled  : boolean  // is the node disabled
  //      selected  : boolean  // is the node selected
  //    },
  //    children    : []  // array of strings or objects
  //    li_attr     : {}  // attributes for the generated LI node
  //    a_attr      : {}  // attributes for the generated A node
  //  }

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
