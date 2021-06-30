package gov.cms.bfd.pipeline.app;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils.DataSourceComponents;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit(ish) tests for {@link gov.cms.bfd.pipeline.app.AppConfiguration}.
 *
 * <p>Since Java apps can't modify their environment variables at runtime, this class has a {@link
 * #main(String[])} method that the test cases will launch as an application in a separate process.
 */
public final class AppConfigurationTestIT {
  /**
   * Verifies that {@link
   * gov.cms.bfd.pipeline.app.AppConfiguration#readConfigFromEnvironmentVariables()} works as
   * expected when passed valid configuration environment variables.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   * @throws ClassNotFoundException (indicates a test error)
   * @throws URISyntaxException (indicates a test error)
   * @throws DecoderException (indicates a test error)
   */
  @Test
  public void normalUsage()
      throws IOException, InterruptedException, ClassNotFoundException, URISyntaxException,
          DecoderException {
    DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    ProcessBuilder testAppBuilder = createProcessBuilderForTestDriver();
    testAppBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_BUCKET, "foo");
    testAppBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_ALLOWED_RIF_TYPE, RifFileType.BENEFICIARY.name());
    testAppBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_HICN_HASH_ITERATIONS,
            String.valueOf(CcwRifLoadTestUtils.HICN_HASH_ITERATIONS));
    testAppBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_HICN_HASH_PEPPER,
            Hex.encodeHexString(CcwRifLoadTestUtils.HICN_HASH_PEPPER));
    testAppBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_URL, dataSourceComponents.getUrl());
    testAppBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME, dataSourceComponents.getUsername());
    testAppBuilder
        .environment()
        .put(AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD, dataSourceComponents.getPassword());
    testAppBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_LOADER_THREADS, "42");
    testAppBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_IDEMPOTENCY_REQUIRED, "true");
    Process testApp = testAppBuilder.start();

    int testAppExitCode = testApp.waitFor();
    /*
     * Only pull the output if things failed, as doing so will break the
     * deserialization happening below.
     */
    String output = "";
    if (testAppExitCode != 0) output = collectOutput(testApp);
    Assert.assertEquals(
        String.format("Wrong exit code. Output[\n%s]\n", output), 0, testAppExitCode);

    ObjectInputStream testAppOutput = new ObjectInputStream(testApp.getErrorStream());
    AppConfiguration testAppConfig = (AppConfiguration) testAppOutput.readObject();
    Assert.assertNotNull(testAppConfig);
    Assert.assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_BUCKET),
        testAppConfig.getCcwRifLoadOptions().getExtractionOptions().getS3BucketName());
    Assert.assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_ALLOWED_RIF_TYPE),
        testAppConfig.getCcwRifLoadOptions().getExtractionOptions().getAllowedRifFileType().name());
    Assert.assertEquals(
        Integer.parseInt(
            testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_HICN_HASH_ITERATIONS)),
        testAppConfig
            .getCcwRifLoadOptions()
            .getLoadOptions()
            .getIdHasherConfig()
            .getHashIterations());
    Assert.assertArrayEquals(
        Hex.decodeHex(
            testAppBuilder
                .environment()
                .get(AppConfiguration.ENV_VAR_KEY_HICN_HASH_PEPPER)
                .toCharArray()),
        testAppConfig.getCcwRifLoadOptions().getLoadOptions().getIdHasherConfig().getHashPepper());
    Assert.assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_DATABASE_URL),
        testAppConfig.getDatabaseOptions().getDatabaseUrl());
    Assert.assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_DATABASE_USERNAME),
        testAppConfig.getDatabaseOptions().getDatabaseUsername());
    Assert.assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_DATABASE_PASSWORD),
        testAppConfig.getDatabaseOptions().getDatabasePassword());
    Assert.assertEquals(
        Integer.parseInt(
            testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_LOADER_THREADS)),
        testAppConfig.getCcwRifLoadOptions().getLoadOptions().getLoaderThreads());
    Assert.assertEquals(
        AppConfiguration.parseBoolean(
                testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_IDEMPOTENCY_REQUIRED))
            .get(),
        testAppConfig.getCcwRifLoadOptions().getLoadOptions().isIdempotencyRequired());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.pipeline.app.AppConfiguration#readConfigFromEnvironmentVariables()} fails as
   * expected when it's called in an application that hasn't had any of the configuration
   * environment variables set.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void noEnvVarsSpecified() throws IOException, InterruptedException {
    ProcessBuilder testAppBuilder = createProcessBuilderForTestDriver();
    Process testApp = testAppBuilder.start();

    Assert.assertNotEquals(0, testApp.waitFor());
    String testAppError =
        new BufferedReader(new InputStreamReader(testApp.getErrorStream()))
            .lines()
            .collect(Collectors.joining("\n"));
    Assert.assertTrue(testAppError.contains(AppConfigurationException.class.getName()));
  }

  /**
   * @return a {@link ProcessBuilder} that will launch {@link #main(String[])} as a separate JVM
   *     process
   */
  private static ProcessBuilder createProcessBuilderForTestDriver() {
    Path java = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java");
    String classpath = System.getProperty("java.class.path");
    ProcessBuilder testAppBuilder =
        new ProcessBuilder(
            java.toAbsolutePath().toString(),
            "-classpath",
            classpath,
            AppConfigurationTestIT.class.getName());
    return testAppBuilder;
  }

  /**
   * @param process the {@link Process} to collect the output of
   * @return the output of the specified {@link Process} in a format suitable for debugging
   */
  private static String collectOutput(Process process) {
    String stderr =
        new BufferedReader(new InputStreamReader(process.getErrorStream()))
            .lines()
            .map(l -> "\t" + l)
            .collect(Collectors.joining("\n"));
    String stdout =
        new BufferedReader(new InputStreamReader(process.getInputStream()))
            .lines()
            .map(l -> "\t" + l)
            .collect(Collectors.joining("\n"));
    return String.format("STDERR:\n[%s]\nSTDOUT:\n[%s]", stderr, stdout);
  }

  /**
   * Calls {@link gov.cms.bfd.pipeline.app.AppConfiguration#readConfigFromEnvironmentVariables()}
   * and serializes the resulting {@link gov.cms.bfd.pipeline.app.AppConfiguration} instance out to
   * {@link System#err}. (Can't use {@link System#out} as it might have logging noise on it.
   *
   * @param args (not used)
   */
  public static void main(String[] args) {
    AppConfiguration appConfig = AppConfiguration.readConfigFromEnvironmentVariables();

    try {
      // Serialize data object to a file
      ObjectOutputStream out = new ObjectOutputStream(System.err);
      out.writeObject(appConfig);
      out.close();
    } catch (IOException e) {
      System.out.printf("Error occurred: %s: '%s'", e.getClass().getName(), e.getMessage());
      System.exit(2);
    }
  }
}
