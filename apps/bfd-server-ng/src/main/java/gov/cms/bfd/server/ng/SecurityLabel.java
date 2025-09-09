package gov.cms.bfd.server.ng;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A security label created by deserializing the security labels YAML file. */
@NoArgsConstructor
@Getter
public class SecurityLabel {
  private String code;
  private String system;
  private String startDate;
  private String endDate;
  // This is set in the input file, but it's for display purposes only
  // so we can ignore it
  private String comment;

  /**
   * Set the code.
   *
   * @param code code
   */
  public void setCode(String code) {
    this.code = normalize(code);
  }

  /**
   * Set the system.
   *
   * @param system system
   */
  public void setSystem(String system) {
    this.system = system.trim();
  }

  /**
   * Set the start date.
   *
   * @param startDate startDate
   */
  public void setStartDate(String startDate) {
    this.startDate = startDate.trim();
  }

  /**
   * Set the end date.
   *
   * @param endDate endDate
   */
  public void setEndDate(String endDate) {
    this.endDate = endDate.trim();
  }

  /**
   * Get the start date as a LocalDate.
   *
   * @return date
   */
  public LocalDate getStartDateAsDate() {
    return LocalDate.parse(this.startDate);
  }

  /**
   * Get the end date as a LocalDate.
   *
   * @return date
   */
  public LocalDate getEndDateAsDate() {
    return LocalDate.parse(this.endDate);
  }

  private void validate() {
    Objects.requireNonNull(this.code);
    Objects.requireNonNull(this.system);
    Objects.requireNonNull(this.startDate);
    Objects.requireNonNull(this.endDate);
  }

  private static Map<String, List<SecurityLabel>> readYamlToMap(InputStream inputStream) {
    try {
      var resultMap = new HashMap<String, List<SecurityLabel>>();
      var yamlData =
          new YAMLMapper().readValue(inputStream, new TypeReference<List<SecurityLabel>>() {});
      for (var entry : yamlData) {
        entry.validate();
        resultMap.computeIfAbsent(entry.getSystem(), k -> new ArrayList<>()).add(entry);
      }
      return resultMap;
    } catch (Exception e) {
      throw new RuntimeException("Failed to read YAML file", e);
    }
  }

  /**
   * Loads and returns a dictionary of security labels grouped by their "system" key. This method
   * uses security_labels YAML file.
   *
   * @return a map where each key is a "system" value, and the value is a list of entries (maps)
   *     associated with that system
   */
  public static Map<String, List<SecurityLabel>> securityLabelsMap() {
    var inputStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("security_labels.yml");

    if (inputStream == null) {
      throw new IllegalArgumentException("File not found in classpath: security_labels.yml");
    }

    return readYamlToMap(inputStream);
  }

  /**
   * Checks if the normalized codes match.
   *
   * @param code code
   * @return boolean
   */
  public boolean matches(String code) {
    return this.code.equals(normalize(code));
  }

  /**
   * Returns the normalized code (whitespace trimmed and periods removed).
   *
   * @param code code
   * @return boolean
   */
  public static String normalize(String code) {
    return code.trim().replace(".", "").toLowerCase();
  }
}
