package gov.hhs.cms.bluebutton.datapipeline.benchmarks;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.Writer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.SlidingWindowReservoir;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.app.S3ToFhirLoadApp;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetTestUtilities;
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
	 * The name of the {@link System#getProperty(String)} value that must be
	 * specified, which will provide the path to the AWS EC2 key PEM file to use
	 * when connecting to benchmark systems.
	 */
	private static final String SYS_PROP_EC2_KEY_FILE = "ec2KeyFile";

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
	private static final int NUMBER_OF_ITERATIONS = 30;

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
	 * Benchmarks against {@link StaticRifResourceGroup#SAMPLE_B}.
	 * 
	 * @throws IOException
	 *             (shouldn't happen)
	 * @throws InterruptedException
	 *             (shouldn't happen)
	 */
	@Test
	public void sampleB() throws IOException, InterruptedException {
		skipOnUnsupportedOs();
		Instant startInstant = Instant.now();
		StaticRifResourceGroup sampleData = StaticRifResourceGroup.SAMPLE_B;

		String ec2KeyName = System.getProperty(SYS_PROP_EC2_KEY_NAME, null);
		if (ec2KeyName == null)
			throw new IllegalArgumentException(
					String.format("The '%s' Java system property must be specified.", SYS_PROP_EC2_KEY_NAME));
		String ec2KeyFile = System.getProperty(SYS_PROP_EC2_KEY_FILE, null);
		if (ec2KeyFile == null)
			throw new IllegalArgumentException(
					String.format("The '%s' Java system property must be specified.", SYS_PROP_EC2_KEY_FILE));
		Path ec2KeyFilePath = Paths.get(ec2KeyFile);
		if (!Files.isReadable(ec2KeyFilePath))
			throw new IllegalArgumentException(String
					.format("The '%s' Java system property must specify a valid key file.", SYS_PROP_EC2_KEY_FILE));

		// Prepare the Python/Ansible virtualenv needed for the iterations.
		Process ansibleInitProcess = null;
		try {
			Path ansibleInitLog = BenchmarkUtilities.findProjectTargetDir().resolve("benchmark-iterations")
					.resolve("ansible_init.log");
			Files.createDirectories(ansibleInitLog.getParent());
			Path ansibleInitScript = BenchmarkUtilities.findProjectTargetDir().resolve("..").resolve("src")
					.resolve("test").resolve("ansible").resolve("ansible_init.sh");
			ProcessBuilder ansibleProcessBuilder = new ProcessBuilder(ansibleInitScript.toString());
			ansibleProcessBuilder.redirectErrorStream(true);
			ansibleProcessBuilder.redirectOutput(ansibleInitLog.toFile());

			ansibleInitProcess = ansibleProcessBuilder.start();
			int ansibleInitExitCode = ansibleInitProcess.waitFor();
			if (ansibleInitExitCode != 0)
				throw new BenchmarkError(
						String.format("Ansible initialization failed with exit code '%d'.", ansibleInitExitCode));
		} finally {
			if (ansibleInitProcess != null)
				ansibleInitProcess.destroyForcibly();
		}

		// Submit all of the iterations for execution.
		ExecutorService benchmarkExecutorService = Executors.newFixedThreadPool(MAX_ACTIVE_ENVIRONMENTS);
		Map<BenchmarkTask, Future<BenchmarkResult>> benchmarkTasks = new HashMap<>();
		for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
			BenchmarkTask benchmarkTask = new BenchmarkTask(i + 1, sampleData, ec2KeyName, ec2KeyFilePath);
			Future<BenchmarkResult> benchmarkResultFuture = benchmarkExecutorService.submit(benchmarkTask);
			benchmarkTasks.put(benchmarkTask, benchmarkResultFuture);
		}
		LOGGER.info("Initialized benchmarks: '{}' iterations, across '{}' threads.", NUMBER_OF_ITERATIONS,
				MAX_ACTIVE_ENVIRONMENTS);

		// Wait for all iterations to finish, collecting results.
		int numberOfFailedIterations = 0;
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
				numberOfFailedIterations++;
			}
		}
		benchmarkExecutorService.shutdown();
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
		String gitBranchName = runCommandAndGetOutput("git symbolic-ref --short HEAD");
		String gitCommitSha = runCommandAndGetOutput("git rev-parse HEAD");
		int recordCount = Arrays.stream(sampleData.getResources()).mapToInt(r -> r.getRecordCount()).sum();
		int beneficiaryCount = Arrays.stream(sampleData.getResources())
				.filter(r -> r.getRifFileType() == RifFileType.BENEFICIARY).mapToInt(r -> r.getRecordCount()).sum();
		for (BenchmarkResult benchmarkResult : benchmarkResults) {
			String testCaseExecutionData = String.format("{ \"iterationTotalRunTime\": %d }",
					benchmarkResult.getIterationTotalRunTime().toMillis());
			Object[] recordFields = new String[] {
					String.format("%s:%s", this.getClass().getSimpleName(), sampleData.name()), gitBranchName,
					gitCommitSha, projectVersion,
					"AWS: DB=db.m4.2xlarge, FHIR=c4.8xlarge, ETL=c4.8xlarge, ETL threads=35",
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
	 * @param command
	 *            the command to run
	 * @return the output of running the specified command as a separate process
	 */
	private static String runCommandAndGetOutput(String command) {
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
				.compile("^(\\S* \\S*) .* Data set finished uploading and ready to process.$");
		private static final Pattern PATTERN_DATA_SET_COMPLETE = Pattern
				.compile("^(\\S* \\S*) .* Data set deleted, now that processing is complete.$");
		private static final DateTimeFormatter ETL_LOG_DATE_TIME_FORMATTER = DateTimeFormatter
				.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

		private final int iterationIndex;
		private final StaticRifResourceGroup sampleData;
		private final String ec2KeyName;
		private final Path ec2KeyFile;

		/**
		 * Constructs a new {@link BenchmarkTask} instance.
		 * 
		 * @param iterationIndex
		 *            the index that distinguishes this {@link BenchmarkTask}
		 *            iteration from its peers
		 * @param sampleData
		 *            the {@link StaticRifResourceGroup} to push to S3 as a
		 *            single data set and then benchmark the processing of
		 * @param ec2KeyName
		 *            the name of the AWS EC2 key that the benchmark systems
		 *            should use
		 * @param ec2KeyFile
		 *            the {@link Path} to the AWS EC2 key PEM file that the
		 *            benchmark systems should use
		 */
		public BenchmarkTask(int iterationIndex, StaticRifResourceGroup sampleData, String ec2KeyName,
				Path ec2KeyFile) {
			this.iterationIndex = iterationIndex;
			this.sampleData = sampleData;
			this.ec2KeyName = ec2KeyName;
			this.ec2KeyFile = ec2KeyFile;
		}

		/**
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public BenchmarkResult call() throws Exception {
			Instant startInstant = Instant.now();
			LOGGER.debug("Iteration '{}' started.", iterationIndex);

			AmazonS3 s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
			Path benchmarksDir = BenchmarkUtilities.findProjectTargetDir().resolve("benchmark-iterations");

			/*
			 * Run Ansible to: 1) create the benchmark systems in AWS, and 2)
			 * configure the systems, starting the FHIR server and ETL service.
			 */
			Bucket bucket = null;
			Path benchmarkLog = benchmarksDir.resolve(String.format("ansible_benchmark_etl-%d.log", iterationIndex));
			Files.createDirectories(benchmarkLog.getParent());
			Process benchmarkProcess = null;
			int benchmarkExitCode = -1;
			try {
				bucket = s3Client.createBucket(String.format("bb-benchmark-%d", iterationIndex));
				pushDataSetToS3(s3Client, bucket, sampleData);

				Path benchmarkScript = BenchmarkUtilities.findProjectTargetDir().resolve("..").resolve("src")
						.resolve("test").resolve("ansible").resolve("benchmark_etl.sh");
				ProcessBuilder benchmarkProcessBuilder = new ProcessBuilder(benchmarkScript.toString(), "--iteration",
						("" + iterationIndex), "--ec2keyname", ec2KeyName, "--ec2keyfile", ec2KeyFile.toString());
				benchmarkProcessBuilder.redirectErrorStream(true);
				benchmarkProcessBuilder.redirectOutput(benchmarkLog.toFile());

				benchmarkProcess = benchmarkProcessBuilder.start();
				benchmarkExitCode = benchmarkProcess.waitFor();

				/*
				 * We don't want to check the value of the Ansible exit code yet
				 * (and blow up if it != 0), because that would not give the
				 * teardown a chance to run, which could leave things running in
				 * AWS, wasting money. We'll do that after the teardown.
				 */
			} finally {
				if (benchmarkProcess != null)
					benchmarkProcess.destroyForcibly();
				if (bucket != null)
					DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
			}

			/*
			 * Run Ansible again to: 1) Collect the ETL and FHIR logs, and 2)
			 * destroy the benchmark systems in AWS.
			 */
			Path teardownLog = benchmarksDir
					.resolve(String.format("ansible_benchmark_etl_teardown-%d.log", iterationIndex));
			Process teardownProcess = null;
			try {
				Path teardownScript = BenchmarkUtilities.findProjectTargetDir().resolve("..").resolve("src")
						.resolve("test").resolve("ansible").resolve("benchmark_etl_teardown.sh");
				ProcessBuilder teardownProcessBuilder = new ProcessBuilder(teardownScript.toString(), "--iteration",
						("" + iterationIndex), "--ec2keyfile", ec2KeyFile.toString());
				teardownProcessBuilder.redirectErrorStream(true);
				teardownProcessBuilder.redirectOutput(teardownLog.toFile());

				teardownProcess = teardownProcessBuilder.start();
				int teardownExitCode = teardownProcess.waitFor();
				if (teardownExitCode != 0)
					throw new BenchmarkError(
							String.format("Ansible failed with exit code '%d' for benchmark teardown iteration '%d'.",
									teardownExitCode, iterationIndex));
			} finally {
				if (teardownProcess != null)
					teardownProcess.destroyForcibly();
			}

			/*
			 * Now that we've given the teardown a chance to run, blow up if the
			 * initial Ansible run failed.
			 */
			if (benchmarkExitCode != 0)
				throw new BenchmarkError(
						String.format("Ansible failed with exit code '%d' for benchmark iteration '%d'.",
								benchmarkExitCode, iterationIndex));

			/*
			 * Parse the ETL log that Ansible pulled to find out how long things
			 * took.
			 */
			Path etlLogPath = benchmarksDir.resolve(String.format("etl-%d.log", iterationIndex));
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

			BenchmarkResult benchmarkResult = new BenchmarkResult(iterationIndex, dataSetProcessingTime,
					Duration.between(startInstant, Instant.now()));
			LOGGER.info("Iteration completed: {}.", benchmarkResult);
			return benchmarkResult;
		}

		/**
		 * Pushes a {@link DataSetManifest} and data objects to S3 for the
		 * specified data set.
		 * 
		 * @param s3Client
		 *            the {@link AmazonS3} client to use
		 * @param bucket
		 *            the S3 {@link Bucket} to push the data to
		 * @param dataResources
		 *            the {@link StaticRifResourceGroup} with the data files to
		 *            push to S3
		 */
		private static void pushDataSetToS3(AmazonS3 s3Client, Bucket bucket, StaticRifResourceGroup dataResources) {
			List<DataSetManifestEntry> manifestEntries = Arrays.stream(dataResources.getResources())
					.map(r -> new DataSetManifestEntry(r.name(), r.getRifFileType())).collect(Collectors.toList());
			DataSetManifest manifest = new DataSetManifest(Instant.now(), manifestEntries);
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
			for (int i = 0; i < dataResources.getResources().length; i++) {
				StaticRifResource dataResource = dataResources.getResources()[i];
				s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, manifest.getEntries().get(i),
						dataResource.getResourceUrl()));
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
