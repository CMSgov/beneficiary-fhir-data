package gov.cms.bfd.server.ng;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Utility class for reading and organizing security label data from YAML files.
 *
 * <p>This class provides methods to parse YAML files containing structured metadata entries (such
 * as system, code, startDate, and endDate), and group them by the "system" key. It is primarily
 * used to support security labeling and classification tasks in applications that rely on
 * standardized metadata formats.
 *
 * <p>Example YAML structure:
 *
 * <pre>
 * - system: http://hl7.org/fhir/sid/icd-10-cm
 *   code: F19.159
 *   startDate: 1970-01-01
 *   endDate: ACTIVE
 * </pre>
 *
 * @version 1.0
 */
public class SecurityLabels {

  // Private constructor to prevent instantiation
  private SecurityLabels() {}

  /**
   * Reads a YAML file containing entries with keys such as "system", "code", "startDate", and
   * "endDate", and groups the entries by the "system" key.
   *
   * @param inputStream the path to the YAML file to be read
   * @return a map where each key is a "system" value, and the value is a list of entries (maps)
   *     associated with that system
   */
  private static Map<String, List<Map<String, Object>>> readYamlToMap(InputStream inputStream) {
    Yaml yaml = new Yaml();
    Map<String, List<Map<String, Object>>> resultMap = new HashMap<>();

    try {
      List<Map<String, Object>> yamlData = yaml.load(inputStream);

      for (Map<String, Object> entry : yamlData) {
        String system = (String) entry.get("system");
        resultMap.computeIfAbsent(system, k -> new ArrayList<>()).add(entry);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to read YAML file", e);
    }

    return resultMap;
  }

  /**
   * Loads and returns a dictionary of security labels grouped by their "system" key. This method
   * uses security_labels YAML file.
   *
   * @return a map where each key is a "system" value, and the value is a list of entries (maps)
   *     associated with that system
   */
  public static Map<String, List<Map<String, Object>>> securityLabelsMap() {
    var inputStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("security_labels.yml");

    if (inputStream == null) {
      throw new IllegalArgumentException("File not found in classpath: security_labels.yml");
    }

    return readYamlToMap(inputStream);
  }

  /**
   * Retrieves the list of keys from the security labels YAML file.
   *
   * <p>This method loads the YAML file named {@code security_labels.yml} from the classpath, parses
   * it into a map structure, and returns the top-level keys from the map. Each key typically
   * represents a system or category defined in the YAML.
   *
   * @param sLabels Security labels dictionary
   * @return a list of keys present in the security labels map
   * @throws IllegalArgumentException if the YAML file is not found in the classpath
   */
  public static List<String> getSecurityLabelKeys(Map<String, List<Map<String, Object>>> sLabels) {
    return new ArrayList<>(sLabels.keySet());
  }
}
