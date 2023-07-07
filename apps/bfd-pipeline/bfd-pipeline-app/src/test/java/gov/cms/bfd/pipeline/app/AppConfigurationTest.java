package gov.cms.bfd.pipeline.app;

import static gov.cms.bfd.pipeline.app.AppConfiguration.loadBeneificiaryPerformanceSettings;
import static gov.cms.bfd.pipeline.app.AppConfiguration.loadClaimPerformanceSettings;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.core.instrument.config.validate.ValidationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.HashMap;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AppConfiguration}. */
public class AppConfigurationTest {
  /**
   * Verifies that {@link AppConfiguration#loadConfig} works as expected when passed valid
   * configuration environment variables.
   *
   * @throws Exception (indicates a test error)
   */
  @Test
  public void normalUsage() throws Exception {
    final var envVars = new HashMap<String, String>();
    envVars.put(AppConfiguration.ENV_VAR_KEY_BUCKET, "foo");
    envVars.put(AppConfiguration.ENV_VAR_KEY_ALLOWED_RIF_TYPE, RifFileType.BENEFICIARY.name());
    envVars.put(
        AppConfiguration.ENV_VAR_KEY_HICN_HASH_ITERATIONS,
        String.valueOf(CcwRifLoadTestUtils.HICN_HASH_ITERATIONS));
    envVars.put(
        AppConfiguration.ENV_VAR_KEY_HICN_HASH_PEPPER,
        Hex.encodeHexString(CcwRifLoadTestUtils.HICN_HASH_PEPPER));
    envVars.put(AppConfiguration.ENV_VAR_KEY_DATABASE_URL, "some_url");
    envVars.put(AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME, "some_user");
    envVars.put(AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD, "some_password");
    envVars.put(AppConfiguration.ENV_VAR_KEY_LOADER_THREADS, "42");
    envVars.put(AppConfiguration.ENV_VAR_KEY_IDEMPOTENCY_REQUIRED, "true");
    final var configLoader = AppConfiguration.createConfigLoader(envVars::get);
    final var encodedBytes = loadAndWriteConfig(configLoader);

    // NOTE: Retained the serialize/deserialize step even though it's not necessary
    // when values are loaded from a map.  The reason is that it provides a test that
    // the configuration objects are actually serializable.
    ObjectInputStream testAppOutput = new ObjectInputStream(new ByteArrayInputStream(encodedBytes));
    AppConfiguration testAppConfig = (AppConfiguration) testAppOutput.readObject();
    assertNotNull(testAppConfig);
    assertEquals(
        envVars.get(AppConfiguration.ENV_VAR_KEY_BUCKET),
        testAppConfig.getCcwRifLoadOptions().get().getExtractionOptions().getS3BucketName());
    assertEquals(
        envVars.get(AppConfiguration.ENV_VAR_KEY_ALLOWED_RIF_TYPE),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getExtractionOptions()
            .getAllowedRifFileType()
            .get()
            .name());
    assertEquals(
        Integer.parseInt(envVars.get(AppConfiguration.ENV_VAR_KEY_HICN_HASH_ITERATIONS)),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getLoadOptions()
            .getIdHasherConfig()
            .getHashIterations());
    assertArrayEquals(
        Hex.decodeHex(envVars.get(AppConfiguration.ENV_VAR_KEY_HICN_HASH_PEPPER).toCharArray()),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getLoadOptions()
            .getIdHasherConfig()
            .getHashPepper());
    assertEquals(
        envVars.get(AppConfiguration.ENV_VAR_KEY_DATABASE_URL),
        testAppConfig.getDatabaseOptions().getDatabaseUrl());
    assertEquals(
        envVars.get(AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME),
        testAppConfig.getDatabaseOptions().getDatabaseUsername());
    assertEquals(
        envVars.get(AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD),
        testAppConfig.getDatabaseOptions().getDatabasePassword());
    assertEquals(
        Integer.parseInt(envVars.get(AppConfiguration.ENV_VAR_KEY_LOADER_THREADS)),
        testAppConfig
            .getCcwRifLoadOptions()
            .get()
            .getLoadOptions()
            .getBeneficiaryPerformanceSettings()
            .getLoaderThreads());
    assertEquals(
        envVars.get(AppConfiguration.ENV_VAR_KEY_IDEMPOTENCY_REQUIRED),
        "" + testAppConfig.getCcwRifLoadOptions().get().getLoadOptions().isIdempotencyRequired());
  }

  /**
   * Verifies that {@link AppConfiguration#loadBeneificiaryPerformanceSettings} enforces field
   * requirements and parses settings correctly.
   */
  @Test
  void testBeneficiaryPerformanceSettings() {
    final var envVars = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoader(envVars::get);

    assertThrows(ConfigException.class, () -> loadBeneificiaryPerformanceSettings(configLoader));
    envVars.put(AppConfiguration.ENV_VAR_KEY_LOADER_THREADS, "10");
    envVars.put(AppConfiguration.ENV_VAR_KEY_RIF_JOB_BATCH_SIZE, "11");
    envVars.put(AppConfiguration.ENV_VAR_KEY_RIF_JOB_QUEUE_SIZE_MULTIPLE, "12");
    assertEquals(
        new LoadAppOptions.PerformanceSettings(10, 11, 12),
        loadBeneificiaryPerformanceSettings(configLoader));
  }

  /**
   * Verifies that {@link AppConfiguration#loadClaimPerformanceSettings} uses defaults as necessary
   * and parses settings correctly.
   */
  @Test
  void testClaimPerformanceSettings() {
    final var envVars = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoader(envVars::get);

    final var benePerformanceSettings = new LoadAppOptions.PerformanceSettings(1, 2, 3);
    assertEquals(
        benePerformanceSettings,
        loadClaimPerformanceSettings(configLoader, benePerformanceSettings));
    envVars.put(AppConfiguration.ENV_VAR_KEY_CLAIM_LOADER_THREADS, "20");
    envVars.put(AppConfiguration.ENV_VAR_KEY_CLAIM_RIF_JOB_BATCH_SIZE, "21");
    envVars.put(AppConfiguration.ENV_VAR_KEY_CLAIM_RIF_JOB_QUEUE_SIZE_MULTIPLE, "22");
    assertEquals(
        new LoadAppOptions.PerformanceSettings(20, 21, 22),
        loadClaimPerformanceSettings(configLoader, benePerformanceSettings));
  }

  /**
   * Verifies that {@link AppConfiguration#loadConfig} fails as expected when it's called in an
   * application that hasn't had any of the configuration environment variables set.
   */
  @Test
  public void noEnvVarsSpecified() {
    final var configLoader = ConfigLoader.builder().build();
    assertThrows(ConfigException.class, () -> loadAndWriteConfig(configLoader));
  }

  /** Verifies that micrometer settings are correctly extracted using environment variable names. */
  @Test
  public void testCloudWatchMicrometerConfigSettings() {
    final var envVars = new HashMap<String, String>();
    final var configLoader = AppConfiguration.createConfigLoader(envVars::get);
    final var helper = AppConfiguration.createMicrometerConfigHelper(configLoader);
    final CloudWatchConfig config = helper::get;
    assertEquals("cloudwatch", config.prefix());

    // confirm the defaults work as expected
    assertFalse(config.enabled());
    assertEquals(Duration.ofMinutes(1), config.step());
    assertThrows(ValidationException.class, () -> config.namespace());

    // confirm explicit values are parsed correctly
    envVars.put(AppConfiguration.ENV_VAR_MICROMETER_CW_ENABLED, "true");
    envVars.put(AppConfiguration.ENV_VAR_MICROMETER_CW_INTERVAL, "PT28S");
    envVars.put(AppConfiguration.ENV_VAR_MICROMETER_CW_NAMESPACE, "my-namespace");
    assertTrue(config.enabled());
    assertEquals(Duration.ofSeconds(28), config.step());
    assertEquals("my-namespace", config.namespace());
  }

  /**
   * Calls {@link AppConfiguration#loadConfig} and serializes the resulting {@link AppConfiguration}
   * instance out an array of bytes.
   *
   * @param configLoader used to load the app config
   * @return the decoded bytes
   * @throws IOException if serializing the config failed
   */
  private byte[] loadAndWriteConfig(ConfigLoader configLoader) throws IOException {
    AppConfiguration appConfig = AppConfiguration.loadConfig(configLoader);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    // Serialize data object to a file
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(appConfig);
    }
    return bytes.toByteArray();
  }
}
