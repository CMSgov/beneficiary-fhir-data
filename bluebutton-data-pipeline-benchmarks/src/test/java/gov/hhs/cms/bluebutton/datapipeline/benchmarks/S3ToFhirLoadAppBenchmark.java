package gov.hhs.cms.bluebutton.datapipeline.benchmarks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.SlidingWindowReservoir;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.app.S3ToFhirLoadApp;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorWorker;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetTestUtilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.S3Utilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifResource;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifResourceGroup;

/**
 * Benchmarks for {@link S3ToFhirLoadApp} by running it against
 * {@link StaticRifResourceGroup} data sets.
 */
public final class S3ToFhirLoadAppBenchmark {
	private static final Logger LOGGER = LoggerFactory.getLogger(S3ToFhirLoadAppBenchmark.class);

	/**
	 * The name of the {@link System#getProperty(String)} value that must be
	 * specified, which will provide the name of the AWS EC2 key pair to use
	 * when creating benchmark systems.
	 */
	private static final String SYS_PROP_EC2_KEY_NAME = "ec2KeyName";

	/**
	 * The name of the optional {@link System#getProperty(String)} value that
	 * can be specified, which will provide the path that the benchmarks will
	 * write their results out to. If not specified, this will default to
	 * <code>bluebutton-data-pipeline.git/dev/benchmark-data.csv</code>.
	 */
	private static final String SYS_PROP_RESULTS_FILE = "benchmarkDataFile";

	/**
	 * The number of iterations that will be performed for each benchmark.
	 */
	private static final int NUMBER_OF_ITERATIONS = 2;

	/**
	 * <p>
	 * The maximum number of benchmark environments that will be active at one
	 * time in AWS.
	 * </p>
	 * <p>
	 * As of 2016-10-17, I've verified that the HHS IDEA Lab AWS sandbox can
	 * support up to at least 10 environments (20 EC2 instances, 10 RDS
	 * instances) at one time. This is after requesting Amazon raise our EC2
	 * instance cap to 50.
	 * </p>
	 */
	private static final int MAX_ACTIVE_ENVIRONMENTS = 10;

	/**
	 * The name of the S3 {@link Bucket} to store the original copy of the RIF
	 * data set to benchmark the processing of in.
	 */
	private static final String BUCKET_DATA_SET_MASTER = "gov.hhs.cms.bluebutton.datapipeline.benchmark.dataset";

	/**
	 * Benchmarks against {@link StaticRifResourceGroup#SAMPLE_B}.
	 * 
	 * @throws IOException
	 *             (shouldn't happen)
	 */
	@Test
	@Ignore("Not enough data to be useful most of the time.")
	public void sampleB() throws IOException {
		runBenchmark(StaticRifResourceGroup.SAMPLE_B);
	}

	/**
	 * Benchmarks against {@link StaticRifResourceGroup#SAMPLE_C}.
	 * 
	 * @throws IOException
	 *             (shouldn't happen)
	 */
	@Test
	public void sampleC() throws IOException {
		runBenchmark(StaticRifResourceGroup.SAMPLE_C);
	}

	/**
	 * Runs the ETL benchmark against the specified
	 * {@link StaticRifResourceGroup} set of of sample data.
	 * 
	 * @param sampleData
	 *            the {@link StaticRifResourceGroup} with the sample data to run
	 *            against
	 * @throws IOException
	 *             (shouldn't happen)
	 */
	private void runBenchmark(StaticRifResourceGroup sampleData) throws IOException {
		skipOnUnsupportedOs();
		Instant startInstant = Instant.now();

		String ec2KeyName = System.getProperty(SYS_PROP_EC2_KEY_NAME, null);
		if (ec2KeyName == null)
			throw new IllegalArgumentException(
					String.format("The '%s' Java system property must be specified.", SYS_PROP_EC2_KEY_NAME));
		Path ec2KeyFilePath = BenchmarkUtilities.findEc2KeyFile();

		Path benchmarksDir = BenchmarkUtilities.findBenchmarksWorkDir();
		Files.createDirectories(benchmarksDir);

		// Prepare the Python/Ansible virtualenv needed for the iterations.
		Path ansibleInitLog = benchmarksDir.resolve("ansible_init.log");
		Path ansibleInitScript = BenchmarkUtilities.findProjectTargetDir().resolve("..").resolve("src").resolve("test")
				.resolve("ansible").resolve("ansible_init.sh");
		ProcessBuilder ansibleProcessBuilder = new ProcessBuilder(ansibleInitScript.toString());
		int ansibleInitExitCode = BenchmarkUtilities.runProcessAndLogOutput(ansibleProcessBuilder, ansibleInitLog);
		if (ansibleInitExitCode != 0)
			throw new BenchmarkError(
					String.format("Ansible initialization failed with exit code '%d'.", ansibleInitExitCode));

		// Upload the benchmark file resources to S3.
		AmazonS3 s3Client = S3Utilities.createS3Client(S3Utilities.REGION_DEFAULT);
		Bucket resourcesBucket = null;
		Bucket dataSetBucket = null;
		List<BenchmarkResult> benchmarkResults;
		try {
			resourcesBucket = s3Client.createBucket("gov.hhs.cms.bluebutton.datapipeline.benchmark.resources");
			pushResourcesToS3(s3Client, resourcesBucket);
			dataSetBucket = s3Client.createBucket(BUCKET_DATA_SET_MASTER);
			pushDataSetToS3(s3Client, dataSetBucket, sampleData);

			benchmarkResults = runBenchmarkIterations(ec2KeyName, ec2KeyFilePath);
		} finally {
			// Clean up the benchmark resources from S3.
			if (resourcesBucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, resourcesBucket);
			if (dataSetBucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, dataSetBucket);
		}

		int numberOfFailedIterations = NUMBER_OF_ITERATIONS - benchmarkResults.size();
		int failedIterationsPercentage = 100 * numberOfFailedIterations / NUMBER_OF_ITERATIONS;
		if (failedIterationsPercentage >= 100)
			throw new BenchmarkError("Too many failed benchmark iterations: " + numberOfFailedIterations);
		Collections.sort(benchmarkResults,
				(o1, o2) -> Integer.valueOf(o1.getIterationIndex()).compareTo(Integer.valueOf(o2.getIterationIndex())));

		// Find the benchmark data file to write the results to.
		String benchmarkResultsFile = System.getProperty(SYS_PROP_RESULTS_FILE, null);
		Path benchmarkResultsFilePath;
		if (benchmarkResultsFile != null) {
			benchmarkResultsFilePath = Paths.get(benchmarkResultsFile);
			if (!Files.isReadable(benchmarkResultsFilePath))
				Files.createFile(benchmarkResultsFilePath);
		} else {
			benchmarkResultsFilePath = Paths.get(".", "dev", "benchmark-data.csv");
			if (!Files.isReadable(benchmarkResultsFilePath))
				benchmarkResultsFilePath = Paths.get("..", "dev", "benchmark-data.csv");
			if (!Files.isReadable(benchmarkResultsFilePath))
				throw new BadCodeMonkeyException();
		}

		// Write the results out to the data file.
		Writer dataWriter = new FileWriter(benchmarkResultsFilePath.toFile(), true);
		CSVPrinter dataPrinter = CSVFormat.RFC4180.print(dataWriter);
		String projectVersion = new BufferedReader(new InputStreamReader(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("project.properties"))).readLine();
		String gitBranchName = runProcessAndGetOutput("git symbolic-ref --short HEAD");
		String gitCommitSha = runProcessAndGetOutput("git rev-parse HEAD");
		int recordCount = Arrays.stream(sampleData.getResources()).mapToInt(r -> r.getRecordCount()).sum();
		int beneficiaryCount = Arrays.stream(sampleData.getResources())
				.filter(r -> r.getRifFileType() == RifFileType.BENEFICIARY).mapToInt(r -> r.getRecordCount()).sum();
		for (BenchmarkResult benchmarkResult : benchmarkResults) {
			String testCaseExecutionData = String.format("{ \"iterationTotalRunTime\": %d }",
					benchmarkResult.getIterationTotalRunTime().toMillis());
			Object[] recordFields = new String[] {
					String.format("%s:%s", this.getClass().getSimpleName(), sampleData.name()), gitBranchName,
					gitCommitSha, projectVersion,
					"AWS: DB=db.m4.10xlarge, FHIR=c4.8xlarge, ETL=c4.8xlarge, ETL threads=70",
					DateTimeFormatter.ISO_INSTANT.format(startInstant), "" + benchmarkResult.getIterationIndex(),
					"" + benchmarkResult.getDataSetProcessingTime().toMillis(), "" + recordCount, "" + beneficiaryCount,
					testCaseExecutionData };
			dataPrinter.printRecord(recordFields);
		}
		dataPrinter.close();

		// Log the results out to LOGGER, as well.
		MetricRegistry metrics = new MetricRegistry();
		Histogram iterationTotalRunHistogram = new Histogram(new SlidingWindowReservoir(NUMBER_OF_ITERATIONS));
		metrics.register("iterationTotalRunTimeMilliseconds", iterationTotalRunHistogram);
		Histogram dataSetProcessingHistogram = new Histogram(new SlidingWindowReservoir(NUMBER_OF_ITERATIONS));
		metrics.register("dataSetProcessingTimeMilliseconds", dataSetProcessingHistogram);
		metrics.counter("iterations").inc(NUMBER_OF_ITERATIONS);
		metrics.counter("dataSetRecordCount").inc(recordCount);
		for (BenchmarkResult benchmarkResult : benchmarkResults) {
			iterationTotalRunHistogram.update(benchmarkResult.getIterationTotalRunTime().toMillis());
			dataSetProcessingHistogram.update(benchmarkResult.getDataSetProcessingTime().toMillis());
		}
		Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build().report();

		/*
		 * Calculate a confidence interval for the expected processing time of
		 * the number of records that were processed, in milliseconds.
		 */
		DescriptiveStatistics stats = new DescriptiveStatistics();
		benchmarkResults.stream().forEach(r -> stats.addValue(r.getDataSetProcessingTime().toMillis()));
		if (stats.getN() > 1) {
			TDistribution tDist = new TDistribution(stats.getN() - 1);
			double confidenceLevel = 0.95;
			double criticalValue = tDist.inverseCumulativeProbability(1.0 - (1 - confidenceLevel) / 2);
			double meanConfidenceInterval = criticalValue * stats.getStandardDeviation() / Math.sqrt(stats.getN());
			double upperConfidenceBound = stats.getMean() + meanConfidenceInterval;

			// Project how long an initial load might take using that CI.
			double millisecondsPerRecord = upperConfidenceBound / recordCount;
			long initialLoadRecordCount = Arrays.stream(ExpectedRecordCount.values()).mapToLong(c -> c.getInitialLoad())
					.sum();
			Duration initialLoadDuration = Duration
					.ofMillis(Math.round(millisecondsPerRecord) * initialLoadRecordCount);
			LOGGER.info(
					"Projection: '{}' records in an initial load would take '{}',"
							+ " averaging a record every '{}' milliseconds (at a '{}' confidence interval).",
					initialLoadRecordCount, initialLoadDuration.toString(), millisecondsPerRecord, confidenceLevel);
		}
		LOGGER.info("This benchmark took '{}' to run, in wall clock time.",
				Duration.between(startInstant, Instant.now()).toString());
	}

	/**
	 * @param ec2KeyName
	 *            the name of the AWS EC2 key that the benchmark systems should
	 *            use
	 * @param ec2KeyFile
	 *            the {@link Path} to the AWS EC2 key PEM file that the
	 *            benchmark systems should use
	 * @return the {@link BenchmarkResult}s from the benchmark iterations that
	 *         completed successfully (failed iterations will be logged)
	 */
	private static List<BenchmarkResult> runBenchmarkIterations(String ec2KeyName, Path ec2KeyFilePath) {
		// Submit all of the iterations for execution.
		ExecutorService benchmarkExecutorService = Executors.newFixedThreadPool(MAX_ACTIVE_ENVIRONMENTS);
		Map<BenchmarkTask, Future<BenchmarkResult>> benchmarkTasks = new HashMap<>();
		for (int i = 1; i <= NUMBER_OF_ITERATIONS; i++) {
			BenchmarkTask benchmarkTask = new BenchmarkTask(i, ec2KeyName, ec2KeyFilePath);
			Future<BenchmarkResult> benchmarkResultFuture = benchmarkExecutorService.submit(benchmarkTask);
			benchmarkTasks.put(benchmarkTask, benchmarkResultFuture);
		}
		LOGGER.info("Initialized benchmarks: '{}' iterations, across '{}' threads.", NUMBER_OF_ITERATIONS,
				MAX_ACTIVE_ENVIRONMENTS);

		// Wait for all iterations to finish, collecting results.
		List<BenchmarkResult> benchmarkResults = new ArrayList<>(NUMBER_OF_ITERATIONS);
		for (BenchmarkTask benchmarkTask : benchmarkTasks.keySet()) {
			BenchmarkResult benchmarkResult;
			try {
				benchmarkResult = benchmarkTasks.get(benchmarkTask).get();
				benchmarkResults.add(benchmarkResult);
			} catch (InterruptedException e) {
				/*
				 * Shouldn't happen, as we're not using interrupts at this
				 * level.
				 */
				throw new BadCodeMonkeyException(e);
			} catch (ExecutionException e) {
				/*
				 * Given the large number of iterations we'd like to run here,
				 * it's become clear that we're going to be playing whack-a-mole
				 * with intermittent problems for a while. Accordingly, we'll
				 * allow some failures.
				 */
				LOGGER.warn("Benchmark iteration failed due to exception.", e);
			}
		}
		benchmarkExecutorService.shutdown();
		return benchmarkResults;
	}

	/**
	 * Uploads the resources that will be used by the benchmark iterations to
	 * S3. We do this here, once for all iterations, to cut down on the transfer
	 * times (and costs).
	 * 
	 * @param s3Client
	 *            the {@link AmazonS3} client to use
	 * @param bucket
	 *            the S3 {@link Bucket} to store the resources in
	 * @throws IOException
	 *             Any {@link IOException}s encountered will be bubbled up.
	 */
	private static void pushResourcesToS3(AmazonS3 s3Client, Bucket bucket) throws IOException {
		/*
		 * Upload all of the files in `target/bluebutton-server/` and
		 * `target/pipeline-app/`.
		 */
		Path targetDir = BenchmarkUtilities.findProjectTargetDir();
		Path[] directories = new Path[] { targetDir.resolve("bluebutton-server"), targetDir.resolve("pipeline-app") };
		for (Path directory : directories) {
			try (Stream<Path> files = Files.find(directory, 1, (f, a) -> Files.isRegularFile(f));) {
				files.forEach(f -> s3Client
						.putObject(new PutObjectRequest(bucket.getName(), f.getFileName().toString(), f.toFile())));
			}
		}

		LOGGER.info("Resources uploaded to S3.");
	}

	/**
	 * Pushes a {@link DataSetManifest} and data objects to S3 for the specified
	 * data set.
	 * 
	 * @param s3Client
	 *            the {@link AmazonS3} client to use
	 * @param bucket
	 *            the S3 {@link Bucket} to push the data to
	 * @param dataResources
	 *            the {@link StaticRifResourceGroup} with the data files to push
	 *            to S3
	 */
	private static void pushDataSetToS3(AmazonS3 s3Client, Bucket bucket, StaticRifResourceGroup dataResources) {
		List<DataSetManifestEntry> manifestEntries = Arrays.stream(dataResources.getResources())
				.map(r -> new DataSetManifestEntry(r.name(), r.getRifFileType())).collect(Collectors.toList());
		DataSetManifest manifest = new DataSetManifest(Instant.now(), 0, manifestEntries);
		s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));

		TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
		for (int i = 0; i < dataResources.getResources().length; i++) {
			StaticRifResource dataResource = dataResources.getResources()[i];
			URL dataResourceUrl = dataResource.getResourceUrl();

			/*
			 * This is a bit hacky, but we're coping with the S3 API's
			 * non-support for PUTing from a remote URL. Attempting a normal PUT
			 * request for the S3 HTTP URLs used in the SAMPLE_C resources will
			 * result in an error. In addition, some of the SAMPLE_C files are
			 * larger than 5GB and thus can't be copied using a non-multipart
			 * request. So instead, we pull those URLs apart and copy them using
			 * multipart requests via TransferManager, instead.
			 */
			Pattern s3HttpObjectRegex = Pattern.compile("http://(.+)\\.s3\\.amazonaws\\.com/(.+)");
			Matcher s3HttpObjectMatcher = s3HttpObjectRegex.matcher(dataResourceUrl.toString());
			if (s3HttpObjectMatcher.matches()) {
				String sourceBucketName = s3HttpObjectMatcher.group(1);
				String sourceKey = s3HttpObjectMatcher.group(2);
				String objectKey = String.format("%s/%s/%s", DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS,
						DateTimeFormatter.ISO_INSTANT.format(manifest.getTimestamp()),
						manifest.getEntries().get(i).getName());

				Copy s3CopyOperation = transferManager
						.copy(new CopyObjectRequest(sourceBucketName, sourceKey, bucket.getName(), objectKey));
				try {
					s3CopyOperation.waitForCopyResult();
				} catch (InterruptedException e) {
					throw new BadCodeMonkeyException(e);
				}
			} else {
				s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, manifest.getEntries().get(i),
						dataResourceUrl));
			}
		}

		transferManager.shutdownNow(false);
		LOGGER.info("Data set uploaded to S3.");
	}

	/**
	 * @param command
	 *            the command to run
	 * @return the output of running the specified command as a separate process
	 */
	private static String runProcessAndGetOutput(String command) {
		/*
		 * This doesn't support quoted args in the command, but we don't
		 * currently need to.
		 */
		if (command.contains("\"") || command.contains("'"))
			throw new IllegalArgumentException();

		ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
		processBuilder.redirectErrorStream(true);
		Process process = null;
		BufferedReader processOutputReader = null;
		try {
			process = processBuilder.start();
			processOutputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String processOutput = processOutputReader.lines().collect(Collectors.joining("\n"));
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				// We're not using interrupts for anything.
				throw new IllegalStateException(e);
			}

			return processOutput;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				if (processOutputReader != null)
					processOutputReader.close();
			} catch (IOException e) {
				LOGGER.warn("Unable to close process STDOUT reader.", e);
			}
			if (process != null && process.isAlive())
				process.destroyForcibly();
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
	 * This {@link Callable} runs a single benchmark iteratior in an AWS
	 * environment that it provisions beforehand, and tears down afterwards.
	 */
	private static final class BenchmarkTask implements Callable<BenchmarkResult> {
		private static final Pattern PATTERN_DATA_SET_START = Pattern
				.compile("^(\\S* \\S*) .* Data set ready. Processing it...$");
		private static final Pattern PATTERN_DATA_SET_COMPLETE = Pattern
				.compile("^(\\S* \\S*) .* Data set renamed in S3, now that processing is complete.$");
		private static final DateTimeFormatter ETL_LOG_DATE_TIME_FORMATTER = DateTimeFormatter
				.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

		private final int iterationIndex;
		private final String ec2KeyName;
		private final Path ec2KeyFile;
		private final Path benchmarkIterationDir;

		/**
		 * Constructs a new {@link BenchmarkTask} instance.
		 * 
		 * @param iterationIndex
		 *            the index that distinguishes this {@link BenchmarkTask}
		 *            iteration from its peers
		 * @param ec2KeyName
		 *            the name of the AWS EC2 key that the benchmark systems
		 *            should use
		 * @param ec2KeyFile
		 *            the {@link Path} to the AWS EC2 key PEM file that the
		 *            benchmark systems should use
		 */
		public BenchmarkTask(int iterationIndex, String ec2KeyName, Path ec2KeyFile) {
			this.iterationIndex = iterationIndex;
			this.ec2KeyName = ec2KeyName;
			this.ec2KeyFile = ec2KeyFile;

			this.benchmarkIterationDir = BenchmarkUtilities.findBenchmarksWorkDir().resolve("" + iterationIndex);
			try {
				/*
				 * Ensure that any previous iteration's results are cleaned up
				 * first, to avoid confusion.
				 */
				if (Files.exists(benchmarkIterationDir))
					Files.walk(benchmarkIterationDir).sorted(Comparator.reverseOrder()).map(Path::toFile)
							.forEach(File::delete);
				Files.createDirectories(benchmarkIterationDir);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public BenchmarkResult call() throws Exception {
			Instant startInstant = Instant.now();
			LOGGER.debug("Iteration '{}' started.", iterationIndex);

			Duration dataSetProcessingTime = provisionAndRunBenchmark();

			BenchmarkResult benchmarkResult = new BenchmarkResult(iterationIndex, dataSetProcessingTime,
					Duration.between(startInstant, Instant.now()));
			LOGGER.info("Iteration completed: {}.", benchmarkResult);
			return benchmarkResult;
		}

		/**
		 * Runs the whole benchmark (mostly by using various Ansible playbooks).
		 * Will throw a {@link BenchmarkError} if the benchmark fails.
		 * 
		 * @return the data set processing time {@link Duration}
		 * @throws IOException
		 *             Any {@link IOException}s that occur will be bubbled up
		 */
		private Duration provisionAndRunBenchmark() throws IOException {
			Path provisionLog = benchmarkIterationDir.resolve(String.format("ansible_provision.log", iterationIndex));
			Path provisionScript = BenchmarkUtilities.findProjectTargetDir().resolve("..").resolve("src")
					.resolve("test").resolve("ansible").resolve("provision.sh");
			ProcessBuilder provisionProcessBuilder = new ProcessBuilder(provisionScript.toString(), "--iteration",
					("" + iterationIndex), "--ec2keyname", ec2KeyName);

			try {
				// Run Ansible to create the benchmark systems in AWS.
				int provisionExitCode = BenchmarkUtilities.runProcessAndLogOutput(provisionProcessBuilder,
						provisionLog);
				if (provisionExitCode != 0)
					throw new BenchmarkError(String.format(
							"Ansible provisioning failed with exit code '%d' for benchmark iteration '%d'.",
							provisionExitCode, iterationIndex));

				runBenchmark();
			} finally {
				/*
				 * Always ensure that the Ansible `teardown.yml` playbook is run
				 * to: 1) Collect the ETL and FHIR logs, and 2) destroy the
				 * benchmark systems in AWS.
				 */
				int teardownExitCode = BenchmarkUtilities.runBenchmarkTeardown(iterationIndex, benchmarkIterationDir,
						ec2KeyFile);
				if (teardownExitCode != 0)
					throw new BenchmarkError(
							String.format("Ansible failed with exit code '%d' for benchmark teardown iteration '%d'.",
									teardownExitCode, iterationIndex));
			}

			/*
			 * Parse the ETL log that the Ansible `teardown.yml` playbook pulled
			 * to find out how long things took.
			 */
			Path etlLogPath = benchmarkIterationDir
					.resolve(String.format("bluebutton-data-pipeline-app.log", iterationIndex));
			if (!Files.isReadable(etlLogPath))
				throw new BenchmarkError(
						String.format("Failed to collect ETL log for benchmark iteration '%d'.", iterationIndex));
			String dataSetStartText = Files.lines(etlLogPath).map(l -> PATTERN_DATA_SET_START.matcher(l))
					.filter(m -> m.matches()).map(m -> m.group(1)).findFirst().get();
			LocalDateTime dataSetStart = LocalDateTime.parse(dataSetStartText, ETL_LOG_DATE_TIME_FORMATTER);
			String dataSetCompleteText = Files.lines(etlLogPath).map(l -> PATTERN_DATA_SET_COMPLETE.matcher(l))
					.filter(m -> m.matches()).map(m -> m.group(1)).findFirst().get();
			LocalDateTime dataSetComplete = LocalDateTime.parse(dataSetCompleteText, ETL_LOG_DATE_TIME_FORMATTER);
			Duration dataSetProcessingTime = Duration.between(dataSetStart, dataSetComplete);

			return dataSetProcessingTime;
		}

		/**
		 * Configures the benchmark system, kicks off the data set processing on
		 * them, and waits for that processing to complete. Will throw a
		 * {@link BenchmarkError} if any of that fails.
		 */
		private void runBenchmark() {
			AmazonS3 s3Client = S3Utilities.createS3Client(S3Utilities.REGION_DEFAULT);
			Bucket bucket = null;
			try {
				bucket = s3Client.createBucket(BenchmarkUtilities.computeBenchmarkDataBucketName(iterationIndex));
				copyDataSetInS3(s3Client, bucket);
				LOGGER.info("Benchmark iteration '{}': provisioned and data set copied.", iterationIndex);

				/*
				 * Run the `benchmark_etl.yml` playbook to do everything we need
				 * to here.
				 */
				Path benchmarkLog = benchmarkIterationDir
						.resolve(String.format("ansible_benchmark_etl.log", iterationIndex));
				Path benchmarkScript = BenchmarkUtilities.findProjectTargetDir().resolve("..").resolve("src")
						.resolve("test").resolve("ansible").resolve("benchmark_etl.sh");
				ProcessBuilder benchmarkProcessBuilder = new ProcessBuilder(benchmarkScript.toString(), "--iteration",
						("" + iterationIndex), "--ec2keyfile", ec2KeyFile.toString());
				int benchmarkExitCode = BenchmarkUtilities.runProcessAndLogOutput(benchmarkProcessBuilder,
						benchmarkLog);

				if (benchmarkExitCode != 0)
					throw new BenchmarkError(
							String.format("Ansible failed with exit code '%d' for benchmark iteration '%d'.",
									benchmarkExitCode, iterationIndex));
			} finally {
				if (bucket != null)
					DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
			}
		}

		/**
		 * @param s3Client
		 *            the {@link AmazonS3} client to use
		 * @param bucket
		 *            the S3 {@link Bucket} to copy the data set to
		 */
		private static void copyDataSetInS3(AmazonS3 s3Client, Bucket bucket) {
			TransferManager transferManager = null;
			try {
				transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();

				final ListObjectsRequest bucketListRequest = new ListObjectsRequest()
						.withBucketName(BUCKET_DATA_SET_MASTER);
				ObjectListing objectListing;
				do {
					objectListing = s3Client.listObjects(bucketListRequest);

					for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
						Copy copyOperation = transferManager.copy(BUCKET_DATA_SET_MASTER, objectSummary.getKey(),
								bucket.getName(), objectSummary.getKey());
						try {
							copyOperation.waitForCopyResult();
						} catch (InterruptedException e) {
							throw new BadCodeMonkeyException(e);
						}
					}

					objectListing = s3Client.listNextBatchOfObjects(objectListing);
				} while (objectListing.isTruncated());
			} finally {
				if (transferManager != null)
					transferManager.shutdownNow(false);
			}
		}
	}

	/**
	 * Represents the results from a single benchmark iteration.
	 */
	private static final class BenchmarkResult {
		private final int iterationIndex;
		private final Duration dataSetProcessingTime;
		private final Duration iterationTotalRunTime;

		/**
		 * Constructs a new {@link BenchmarkResult} instance.
		 * 
		 * @param iterationIndex
		 *            the value to use for {@link #getIterationIndex()}
		 * @param dataSetProcessingTime
		 *            the value to use for {@link #getDataSetProcessingTime()}
		 * @param iterationTotalRunTime
		 *            the value to use for {@link #getIterationTotalRunTime()}
		 */
		public BenchmarkResult(int iterationIndex, Duration dataSetProcessingTime, Duration iterationTotalRunTime) {
			this.iterationIndex = iterationIndex;
			this.dataSetProcessingTime = dataSetProcessingTime;
			this.iterationTotalRunTime = iterationTotalRunTime;
		}

		/**
		 * @return the index of the {@link BenchmarkTask} that this
		 *         {@link BenchmarkResult} was collected for
		 */
		public int getIterationIndex() {
			return iterationIndex;
		}

		/**
		 * @return the length of time that it took the ETL application to
		 *         process the data set it was tasked with
		 */
		public Duration getDataSetProcessingTime() {
			return dataSetProcessingTime;
		}

		/**
		 * @return the total {@link Duration} that the {@link BenchmarkTask} ran
		 *         for, including time spent on setup and teardown
		 */
		public Duration getIterationTotalRunTime() {
			return iterationTotalRunTime;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("BenchmarkResult [iterationIndex=");
			builder.append(iterationIndex);
			builder.append(", dataSetProcessingTime=");
			builder.append(dataSetProcessingTime);
			builder.append(", iterationTotalRunTime=");
			builder.append(iterationTotalRunTime);
			builder.append("]");
			return builder.toString();
		}
	}
}
