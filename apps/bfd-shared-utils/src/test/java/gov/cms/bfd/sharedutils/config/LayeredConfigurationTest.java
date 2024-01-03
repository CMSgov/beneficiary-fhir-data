package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LayeredConfiguration}. */
public class LayeredConfigurationTest {
  /**
   * Verify that {@link LayeredConfiguration#splitPathCsv} properly splits the string and skips
   * empty strings in the resulting list.
   */
  @Test
  void splitPathsStringEdgeCases() {
    assertEquals(List.of(), LayeredConfiguration.splitPathCsv(""));
    assertEquals(List.of(), LayeredConfiguration.splitPathCsv(", "));
    assertEquals(List.of("a"), LayeredConfiguration.splitPathCsv("a,"));
    assertEquals(List.of("b"), LayeredConfiguration.splitPathCsv(",b"));
    assertEquals(List.of("a", "b"), LayeredConfiguration.splitPathCsv("a, , b"));
  }

  /** Verify that default settings are used when no environment variables are present. */
  @Test
  void loadsSettingsWithNoEnvVars() {
    final var expectedSettings = LayeredConfigurationSettings.builder().build();
    final var configMap = new HashMap<String, String>();
    final var configLoader = ConfigLoader.builder().addMap(configMap).build();

    final var actualSettings = LayeredConfiguration.loadLayeredConfigurationSettings(configLoader);
    assertEquals(expectedSettings, actualSettings);
  }

  /**
   * Verify that json env var is used when present.
   *
   * @throws JsonProcessingException pass through from jackson
   */
  @Test
  void loadsSettingsFromFullJson() throws JsonProcessingException {
    final var objectMapper = new ObjectMapper();
    final var expectedSettings =
        LayeredConfigurationSettings.builder()
            .propertiesFile("myProperties.properties")
            .ssmHierarchies(List.of("common", "specific"))
            .build();
    final var settingsJson = objectMapper.writeValueAsString(expectedSettings);

    final var configMap = new HashMap<String, String>();
    configMap.put(LayeredConfiguration.SSM_PATH_CONFIG_SETTINGS_JSON, settingsJson);
    final var configLoader = ConfigLoader.builder().addMap(configMap).build();

    final var actualSettings = LayeredConfiguration.loadLayeredConfigurationSettings(configLoader);
    assertEquals(expectedSettings, actualSettings);
  }

  /** Verify that json with no defined properties parses using defaults. */
  @Test
  void loadsSettingsFromMinimalJson() {
    final var expectedSettings = LayeredConfigurationSettings.builder().build();
    final var settingsJson = "{}";

    final var configMap = new HashMap<String, String>();
    configMap.put(LayeredConfiguration.SSM_PATH_CONFIG_SETTINGS_JSON, settingsJson);
    final var configLoader = ConfigLoader.builder().addMap(configMap).build();

    final var actualSettings = LayeredConfiguration.loadLayeredConfigurationSettings(configLoader);
    assertEquals(expectedSettings, actualSettings);
  }
}
