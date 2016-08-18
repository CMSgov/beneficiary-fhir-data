package gov.hhs.cms.bluebutton.datapipeline.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

/**
 * <p>
 * Unit(ish) tests for {@link AppConfiguration}.
 * </p>
 * <p>
 * Since Java apps can't modify their environment variables at runtime, this
 * class has a {@link #main(String[])} method that the test cases will launch as
 * an application in a separate process.
 * </p>
 */
public final class AppConfigurationTest {
	/**
	 * Verifies that
	 * {@link AppConfiguration#readConfigFromEnvironmentVariables()} works as
	 * expected when passed valid configuration environment variables.
	 * 
	 * @throws IOException
	 *             (indicates a test error)
	 * @throws InterruptedException
	 *             (indicates a test error)
	 * @throws ClassNotFoundException
	 *             (indicates a test error)
	 */
	@Test
	public void normalUsage() throws IOException, InterruptedException, ClassNotFoundException {
		ProcessBuilder testAppBuilder = createProcessBuilderForTestDriver();
		testAppBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_BUCKET, "foo");
		testAppBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_FHIR, "http://example.com/bar");
		Process testApp = testAppBuilder.start();

		Assert.assertEquals(0, testApp.waitFor());
		ObjectInputStream testAppOutput = new ObjectInputStream(testApp.getErrorStream());
		AppConfiguration testAppConfig = (AppConfiguration) testAppOutput.readObject();
		Assert.assertNotNull(testAppConfig);
		Assert.assertEquals(testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_BUCKET),
				testAppConfig.getS3BucketName());
		Assert.assertEquals(new URL(testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_FHIR)),
				testAppConfig.getFhirServer());
	}

	/**
	 * Verifies that
	 * {@link AppConfiguration#readConfigFromEnvironmentVariables()} fails as
	 * expected when it's called in an application that hasn't had any of the
	 * configuration environment variables set.
	 * 
	 * @throws IOException
	 *             (indicates a test error)
	 * @throws InterruptedException
	 *             (indicates a test error)
	 */
	@Test
	public void noEnvVarsSpecified() throws IOException, InterruptedException {
		ProcessBuilder testAppBuilder = createProcessBuilderForTestDriver();
		Process testApp = testAppBuilder.start();

		Assert.assertNotEquals(0, testApp.waitFor());
		String testAppError = new BufferedReader(new InputStreamReader(testApp.getErrorStream())).lines()
				.collect(Collectors.joining("\n"));
		Assert.assertTrue(testAppError.contains(AppConfigurationException.class.getName()));
	}

	/**
	 * @return a {@link ProcessBuilder} that will launch {@link #main(String[])}
	 *         as a separate JVM process
	 */
	private static ProcessBuilder createProcessBuilderForTestDriver() {
		Path java = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java");
		String classpath = System.getProperty("java.class.path");
		ProcessBuilder testAppBuilder = new ProcessBuilder(java.toAbsolutePath().toString(), "-classpath", classpath,
				AppConfigurationTest.class.getName());
		testAppBuilder.environment().clear();
		return testAppBuilder;
	}

	/**
	 * Calls {@link AppConfiguration#readConfigFromEnvironmentVariables()} and
	 * serializes the resulting {@link AppConfiguration} instance out to
	 * {@link System#err}. (Can't use {@link System#out} as it might have
	 * logging noise on it.
	 * 
	 * @param args
	 *            (not used)
	 */
	public static void main(String[] args) {
		AppConfiguration appConfig = AppConfiguration.readConfigFromEnvironmentVariables();

		try {
			// Serialize data object to a file
			ObjectOutputStream out = new ObjectOutputStream(System.err);
			out.writeObject(appConfig);
			out.close();
		} catch (IOException e) {
			System.exit(2);
		}
	}
}
