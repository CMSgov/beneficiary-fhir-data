package gov.hhs.cms.bluebutton.datapipeline.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Test;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirTestUtilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetTestUtilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * <p>
 * Integration tests for {@link S3ToFhirLoadApp}.
 * </p>
 * <p>
 * These tests require the application capsule JAR to be built and available.
 * Accordingly, they may not run correctly in Eclipse: if the capsule isn't
 * built yet, they'll just fail, but if an older capsule exists (because you
 * haven't rebuilt it), it'll run using the old code, which probably isn't what
 * you want.
 * </p>
 * <p>
 * Also: when run on the command line via Maven, these tests rely on a local
 * FHIR server provided by the Cargo plugin. To run these tests in Eclipse, you
 * can launch the server manually, as follows:
 * </p>
 * <ol>
 * <li>Right-click the <code>bluebutton-data-pipeline-fhir-load</code> project,
 * and select <strong>Run As > Maven build...</strong>.</li>
 * <li>Set <strong>goal</strong> to <code>cargo:run</code>.</li>
 * <li>Click <strong>Run</strong>.</li>
 * </ol>
 * <p>
 * When done with the server, you can stop it by terminating the launch in
 * Eclipse's <em>Console</em> view (with the red "Stop" button). Once it's been
 * run the first time, the server can be re-launched from Eclipse's
 * <strong>Run</strong> toolbar dropdown button, just like any other Java
 * application, unit test, etc. Logs from the server can be found in the
 * project's <code>target/failsafe-reports</code> and
 * <code>target/cargo/configurations/jetty9x/logs</code> directories.
 * </p>
 */
public final class S3ToFhirLoadAppIT {
	/**
	 * The POSIX signal number for the <code>SIGTERM</code> signal.
	 */
	private static final int SIGTERM = 15;

	/**
	 * Verifies that {@link S3ToFhirLoadApp} exits as expected when launched
	 * with no configuration environment variables.
	 * 
	 * @throws IOException
	 *             (indicates a test error)
	 * @throws InterruptedException
	 *             (indicates a test error)
	 */
	@Test
	public void missingConfig() throws IOException, InterruptedException {
		// Start the app with no config env vars.
		ProcessBuilder appRunBuilder = createAppProcessBuilder(new Bucket("foo"));
		appRunBuilder.environment().clear();
		appRunBuilder.redirectErrorStream(true);
		Process appProcess = appRunBuilder.start();
		new ProcessOutputConsumer(appProcess);

		// Wait for it to exit with an error.
		appProcess.waitFor(1, TimeUnit.MINUTES);

		// Verify that the application exited as expected.
		Assert.assertEquals(S3ToFhirLoadApp.EXIT_CODE_BAD_CONFIG, appProcess.exitValue());
	}

	/**
	 * Verifies that {@link S3ToFhirLoadApp} exits as expected when asked to run
	 * against an S3 bucket that doesn't exist. This test case isn't so much
	 * needed to test that one specific failure case, but to instead verify that
	 * the application dies as expected when something goes sideways.
	 * 
	 * @throws IOException
	 *             (indicates a test error)
	 * @throws InterruptedException
	 *             (indicates a test error)
	 */
	@Test
	public void missingBucket() throws IOException, InterruptedException {
		Process appProcess = null;
		try {
			// Start the app.
			ProcessBuilder appRunBuilder = createAppProcessBuilder(new Bucket("foo"));
			appRunBuilder.redirectErrorStream(true);
			appProcess = appRunBuilder.start();
			ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);

			// Wait for it to exit with an error.
			appProcess.waitFor(1, TimeUnit.MINUTES);

			// Verify that the application exited as expected.
			Assert.assertEquals(String.format("Wrong exit code. Output [\n%s]\n", appRunConsumer.getStdoutContents()),
					S3ToFhirLoadApp.EXIT_CODE_MONITOR_ERROR, appProcess.exitValue());
		} finally {
			if (appProcess != null)
				appProcess.destroyForcibly();
		}
	}

	/**
	 * Verifies that {@link S3ToFhirLoadApp} works as expected when no data is
	 * made available for it to process. Basically, it should just sit there and
	 * wait for data, doing nothing.
	 * 
	 * @throws IOException
	 *             (indicates a test error)
	 * @throws InterruptedException
	 *             (indicates a test error)
	 */
	@Test
	public void noRifData() throws IOException, InterruptedException {
		skipOnUnsupportedOs();

		AmazonS3 s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		Bucket bucket = null;
		Process appProcess = null;
		try {
			// Create the (empty) bucket to run against.
			bucket = s3Client.createBucket(String.format("bb-test-%d", new Random().nextInt(1000)));

			// Start the app.
			ProcessBuilder appRunBuilder = createAppProcessBuilder(bucket);
			appRunBuilder.redirectErrorStream(true);
			appProcess = appRunBuilder.start();
			ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);

			// Wait for it to complete a scan.
			Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> hasAScanFoundNothing(appRunConsumer));

			// Stop the application.
			sendSigterm(appProcess);
			appProcess.waitFor(1, TimeUnit.MINUTES);

			// Verify that the application exited as expected.
			verifyExitValueMatchesSignal(SIGTERM, appProcess);
		} finally {
			if (appProcess != null)
				appProcess.destroyForcibly();
			if (bucket != null)
				s3Client.deleteBucket(bucket.getName());
		}
	}

	/**
	 * Verifies that {@link S3ToFhirLoadApp} works as expected against a small
	 * amount of data. We trust that other tests elsewhere are covering the ETL
	 * results' correctness; here we're just verifying the overall flow. Does it
	 * find the data set, process it, and then not find a data set anymore?
	 * 
	 * @throws IOException
	 *             (indicates a test error)
	 * @throws InterruptedException
	 *             (indicates a test error)
	 */
	@Test
	public void smallAmountOfRifData() throws IOException, InterruptedException {
		skipOnUnsupportedOs();

		AmazonS3 s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		Bucket bucket = null;
		Process appProcess = null;
		try {
			/*
			 * Create the (empty) bucket to run against, and populate it with a
			 * data set.
			 */
			bucket = s3Client.createBucket(String.format("bb-test-%d", new Random().nextInt(1000)));
			DataSetManifest manifest = new DataSetManifest(Instant.now(),
					new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
					new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, manifest.getEntries().get(0),
					"rif-static-samples/sample-a-beneficiaries.txt"));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, manifest.getEntries().get(1),
					"rif-static-samples/sample-a-bcarrier.txt"));

			// Start the app.
			ProcessBuilder appRunBuilder = createAppProcessBuilder(bucket);
			appRunBuilder.redirectErrorStream(true);
			appProcess = appRunBuilder.start();
			ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);

			// Wait for it to process a data set.
			Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> hasADataSetBeenProcessed(appRunConsumer));

			// Wait for it to have a scan that finds nothing.
			Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> hasAScanFoundNothing(appRunConsumer));

			// Stop the application.
			sendSigterm(appProcess);
			appProcess.waitFor(1, TimeUnit.MINUTES);

			// Verify that the application exited as expected.
			verifyExitValueMatchesSignal(SIGTERM, appProcess);
		} finally {
			if (appProcess != null)
				appProcess.destroyForcibly();
			if (bucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
		}
	}

	/**
	 * Throws an {@link AssumptionViolatedException} if the OS doesn't support
	 * <strong>graceful</strong> shutdowns via {@link Process#destroy()}.
	 */
	private static void skipOnUnsupportedOs() {
		/*
		 * The only OS I know for sure that handles this correctly is Linux,
		 * because I've verified that there. However, the following project
		 * seems to indicate that Linux really might be it:
		 * https://github.com/zeroturnaround/zt-process-killer. Some further
		 * research indicates that this could be supported on Windows for GUI
		 * apps, but not console apps. If this lack of OS support ever proves to
		 * be a problem, the best thing to do would be to enhance our
		 * application such that it listens on a particular port for shutdown
		 * requests, and handles them gracefully.
		 */

		Assume.assumeTrue("Unsupported OS for this test case.", "Linux".equals(System.getProperty("os.name")));
	}

	/**
	 * @param appRunConsumer
	 *            the {@link ProcessOutputConsumer} whose output should be
	 *            checked
	 * @return <code>true</code> if the application output indicates that a data
	 *         set scan has completed and found nothing, <code>false</code> if
	 *         not
	 */
	private static boolean hasAScanFoundNothing(ProcessOutputConsumer appRunConsumer) {
		return appRunConsumer.getStdoutContents().toString().contains("No data sets to process found");
	}

	/**
	 * @param appRunConsumer
	 *            the {@link ProcessOutputConsumer} whose output should be
	 *            checked
	 * @return <code>true</code> if the application output indicates that a data
	 *         set has been processed, <code>false</code> if not
	 */
	private static boolean hasADataSetBeenProcessed(ProcessOutputConsumer appRunConsumer) {
		return appRunConsumer.getStdoutContents().toString()
				.contains("Data set deleted, now that processing is complete");
	}

	/**
	 * Sends a <code>SIGTERM</code> to the specified {@link Process}, causing it
	 * to exit, but giving it a chance to do so gracefully.
	 * 
	 * @param process
	 *            the {@link Process} to signal
	 */
	private static void sendSigterm(Process process) {
		/*
		 * Verified on Ubuntu Trusty with Oracle Java 8: calling this sends a
		 * SIGTERM to the process.
		 */
		process.destroy();
	}

	/**
	 * Verifies that the specified {@link Process} has exited, due to the
	 * specified signal.
	 * 
	 * @param signalNumber
	 *            the POSIX signal number to check for
	 * @param process
	 *            the {@link Process} to check the {@link Process#exitValue()}
	 *            of
	 */
	private static void verifyExitValueMatchesSignal(int signalNumber, Process process) {
		/*
		 * Per POSIX (by way of http://unix.stackexchange.com/a/99143),
		 * applications that exit due to a signal should return an exit code
		 * that is 128 + the signal number.
		 */
		Assert.assertEquals(128 + signalNumber, process.exitValue());
	}

	/**
	 * Ensures that {@link FhirTestUtilities#cleanFhirServer()} is called after
	 * each test case.
	 */
	@After
	public void cleanFhirServerAfterEachTestCase() {
		FhirTestUtilities.cleanFhirServer();
	}

	/**
	 * @param bucket
	 *            the S3 {@link Bucket} that the application will be configured
	 *            to pull RIF data from
	 * @return a {@link ProcessBuilder} that can be used to launch the
	 *         application
	 */
	private static ProcessBuilder createAppProcessBuilder(Bucket bucket) {
		String[] command = createCommandForCapsule();
		ProcessBuilder appRunBuilder = new ProcessBuilder(command);

		appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_BUCKET, bucket.getName());
		appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_FHIR, FhirTestUtilities.FHIR_API);
		appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_KEY_STORE_PATH,
				FhirTestUtilities.getClientKeyStorePath().toString());
		appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_KEY_STORE_PASSWORD,
				String.valueOf(FhirTestUtilities.CLIENT_KEY_STORE_PASSWORD));
		appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_TRUST_STORE_PATH,
				FhirTestUtilities.getClientTrustStorePath().toString());
		appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_TRUST_STORE_PASSWORD,
				String.valueOf(FhirTestUtilities.CLIENT_TRUST_STORE_PASSWORD));
		/*
		 * Note: Not explicitly providing AWS credentials here, as the child
		 * process will inherit any that are present in this build/test process.
		 */
		return appRunBuilder;
	}

	/**
	 * @return the command array for
	 *         {@link ProcessBuilder#ProcessBuilder(String...)} that will launch
	 *         the application via its <code>.x</code> capsule executable
	 */
	private static String[] createCommandForCapsule() {
		try {
			Path buildTargetDir = Paths.get(".", "target");
			Path appExe = Files.list(buildTargetDir)
					.filter(f -> f.getFileName().toString().startsWith("bluebutton-data-pipeline-app-"))
					.filter(f -> f.getFileName().toString().endsWith("-capsule-fat.x")).findFirst().get();

			return new String[] { appExe.toAbsolutePath().toString() };
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Managing external processes is tricky: at the OS level, all processes'
	 * output is sent to a buffer. If that buffer fills up (because you're not
	 * reading the output), the process will block -- forever. To avoid that,
	 * it's best to always have a separate thread running that consumes a
	 * process' output. This {@link ProcessOutputConsumer} is designed to allow
	 * for just that.
	 */
	private static final class ProcessOutputConsumer implements Runnable {
		private final BufferedReader stdoutReader;
		private final StringBuffer stdoutContents;

		/**
		 * Constructs a new {@link ProcessOutputConsumer} instance.
		 * 
		 * @param the
		 *            {@link ProcessOutputConsumer} whose output should be
		 *            consumed
		 */
		public ProcessOutputConsumer(Process process) {
			/*
			 * Note: we're only grabbing STDOUT, because we're assuming that
			 * STDERR has been piped to/merged with it. If that's not the case,
			 * you'd need a separate thread consuming that stream, too.
			 */

			InputStream stdout = process.getInputStream();
			this.stdoutReader = new BufferedReader(new InputStreamReader(stdout));
			this.stdoutContents = new StringBuffer();

			// Start this running.
			new Thread(this).start();
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			/*
			 * Note: This will naturally stop once the process exits (due to the
			 * null check below).
			 */

			try {
				String line;
				while ((line = stdoutReader.readLine()) != null) {
					stdoutContents.append(line);
					stdoutContents.append('\n');
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * @return a {@link StringBuffer} that contains the <code>STDOUT</code>
		 *         contents so far
		 */
		public StringBuffer getStdoutContents() {
			return stdoutContents;
		}
	}
}
