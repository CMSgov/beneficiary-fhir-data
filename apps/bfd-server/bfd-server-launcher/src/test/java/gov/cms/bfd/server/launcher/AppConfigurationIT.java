package gov.cms.bfd.server.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AppConfiguration}. Has to be an integration test because it depends on the
 * existence of a war file that isn't present when unit tests are run.
 */
public final class AppConfigurationIT {
  /**
   * Verifies that {@link AppConfiguration#loadConfig} works as expected when passed valid
   * configuration environment variables.
   */
  @Test
  public void normalUsage() throws IOException {
    Map<String, String> envValues = new HashMap<>();
    envValues.put(AppConfiguration.ENV_VAR_KEY_PORT, "1");
    envValues.put(
        AppConfiguration.ENV_VAR_KEY_KEYSTORE,
        getProjectDirectory()
            .resolve(Paths.get("..", "dev", "ssl-stores", "server-keystore.pfx"))
            .toString());
    envValues.put(
        AppConfiguration.ENV_VAR_KEY_TRUSTSTORE,
        getProjectDirectory()
            .resolve(Paths.get("..", "dev", "ssl-stores", "server-truststore.pfx"))
            .toString());
    envValues.put(AppConfiguration.ENV_VAR_KEY_WAR, getSampleWar().toString());

    ConfigLoader config = ConfigLoader.builder().addSingle(envValues::get).build();

    AppConfiguration testAppConfig = AppConfiguration.loadConfig(config);
    assertNotNull(testAppConfig);
    assertEquals(Optional.<String>empty(), testAppConfig.getHost());
    assertEquals(
        Integer.parseInt(envValues.get(AppConfiguration.ENV_VAR_KEY_PORT)),
        testAppConfig.getPort());
    assertEquals(
        envValues.get(AppConfiguration.ENV_VAR_KEY_KEYSTORE),
        testAppConfig.getKeystore().toString());
    assertEquals(
        envValues.get(AppConfiguration.ENV_VAR_KEY_TRUSTSTORE),
        testAppConfig.getTruststore().toString());
    assertEquals(
        envValues.get(AppConfiguration.ENV_VAR_KEY_WAR), testAppConfig.getWar().toString());
  }

  /**
   * Gets the project directory.
   *
   * @return the local {@link Path} to this project/module
   */
  static Path getProjectDirectory() {
    /*
     * The working directory for tests will either be the module directory or their parent
     * directory. With that knowledge, we're searching for the bluebutton-data-server-launcher
     * directory.
     */
    String projectName = "bfd-server-launcher";
    Path projectDir = Paths.get(".");
    if (!resolveRealFileName(projectDir).equals(projectName))
      projectDir = Paths.get(".", projectName);
    if (!Files.isDirectory(projectDir)) throw new IllegalStateException();
    return projectDir;
  }

  /**
   * Resolves the real file name for a {@link Path}.
   *
   * @param path the {@link Path} to resolve the real {@link Path#getFileName()} of
   * @return the real {@link Path#getFileName()} of the specified {@link Path}
   */
  static String resolveRealFileName(Path path) {
    try {
      return path.toRealPath().getFileName().toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Verifies that {@link AppConfiguration#loadConfig} fails as expected when it's called with no
   * configuration environment variables set.
   */
  @Test
  public void noEnvVarsSpecified() {
    Map<String, String> envValues = Collections.emptyMap();
    ConfigLoader config = ConfigLoader.builder().addSingle(envValues::get).build();
    ConfigException exception =
        assertThrows(ConfigException.class, () -> AppConfiguration.loadConfig(config));
    assertEquals(AppConfiguration.ENV_VAR_KEY_PORT, exception.getName());
    assertEquals(
        "Configuration value error: name='BFD_PORT' detail='required option not provided'",
        exception.getMessage());
  }

  /**
   * Gets the {@link Path} to the <code>bfd-server-launcher-sample</code> WAR.
   *
   * @return the {@link Path}
   */
  static Path getSampleWar() {
    try {
      final var sampleDirectory =
          AppConfigurationIT.getProjectDirectory().resolve(Paths.get("target", "sample"));
      return Files.find(
              sampleDirectory,
              5,
              (path, attr) ->
                  path.getFileName().toString().matches("bfd-server-launcher-sample-.*\\.war"))
          .findFirst()
          .orElseThrow();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
