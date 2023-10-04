package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

public class LayeredConfigurationTest {
  @Test
  void loadsSettingsWithNoEnvVars() {
    final var expectedSettings = LayeredConfigurationSettings.builder().build();
    final var configMap = new HashMap<String, String>();
    final var configLoader = ConfigLoader.builder().addMap(configMap).build();

    final var actualSettings = LayeredConfiguration.loadLayeredConfigurationSettings(configLoader);
    assertEquals(expectedSettings, actualSettings);
  }

  @Test
  void loadsSettingsFromSeparateEnvVars() {
    final var expectedSettings =
        LayeredConfigurationSettings.builder()
            .propertiesFile("myProperties.properties")
            .ssmPaths(List.of("top", "bottom"))
            .build();
    final var configMap = new HashMap<String, String>();
    configMap.put(LayeredConfiguration.ENV_VAR_KEY_SSM_PARAMETER_PATH, "top,bottom");
    configMap.put(LayeredConfiguration.ENV_VAR_KEY_PROPERTIES_FILE, "myProperties.properties");
    final var configLoader = ConfigLoader.builder().addMap(configMap).build();

    final var actualSettings = LayeredConfiguration.loadLayeredConfigurationSettings(configLoader);
    assertEquals(expectedSettings, actualSettings);
  }

  @Test
  void loadsSettingsFromFullJson() throws JsonProcessingException {
    final var objectMapper = new ObjectMapper();
    final var expectedSettings =
        LayeredConfigurationSettings.builder()
            .propertiesFile("myProperties.properties")
            .ssmHierarchies(List.of("common", "specific"))
            .ssmPaths(List.of("top", "bottom"))
            .build();
    final var settingsJson = objectMapper.writeValueAsString(expectedSettings);

    final var configMap = new HashMap<String, String>();
    configMap.put(LayeredConfiguration.ENV_VAR_SETTINGS_JSON, settingsJson);
    final var configLoader = ConfigLoader.builder().addMap(configMap).build();

    final var actualSettings = LayeredConfiguration.loadLayeredConfigurationSettings(configLoader);
    assertEquals(expectedSettings, actualSettings);
  }

  @Test
  void loadsSettingsFromMinimalJson() throws JsonProcessingException {
    final var expectedSettings = LayeredConfigurationSettings.builder().build();
    final var settingsJson = "{}";

    final var configMap = new HashMap<String, String>();
    configMap.put(LayeredConfiguration.ENV_VAR_SETTINGS_JSON, settingsJson);
    final var configLoader = ConfigLoader.builder().addMap(configMap).build();

    final var actualSettings = LayeredConfiguration.loadLayeredConfigurationSettings(configLoader);
    assertEquals(expectedSettings, actualSettings);
  }
}
