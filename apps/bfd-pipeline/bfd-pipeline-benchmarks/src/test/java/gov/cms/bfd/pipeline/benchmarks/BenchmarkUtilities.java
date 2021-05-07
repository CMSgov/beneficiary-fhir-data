package gov.cms.bfd.pipeline.benchmarks;

import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** A set of shared utilities for the benchmark code. */
final class BenchmarkUtilities {
  /**
   * The name of the {@link System#getProperty(String)} value that must be specified, which will
   * provide the path to the AWS EC2 key PEM file to use when connecting to benchmark systems.
   */
  static final String SYS_PROP_EC2_KEY_FILE = "ec2KeyFile";

  /**
   * @return the {@link Path} specified by the value of the {@link
   *     BenchmarkUtilities#SYS_PROP_EC2_KEY_FILE} Java system property
   */
  static Path findEc2KeyFile() {
    String ec2KeyFile = System.getProperty(BenchmarkUtilities.SYS_PROP_EC2_KEY_FILE, null);
    if (ec2KeyFile == null)
      throw new IllegalArgumentException(
          String.format(
              "The '%s' Java system property must be specified.",
              BenchmarkUtilities.SYS_PROP_EC2_KEY_FILE));
    Path ec2KeyFilePath = Paths.get(ec2KeyFile);
    if (!Files.isReadable(ec2KeyFilePath))
      throw new IllegalArgumentException(
          String.format(
              "The '%s' Java system property must specify a valid key file.",
              BenchmarkUtilities.SYS_PROP_EC2_KEY_FILE));
    return ec2KeyFilePath;
  }

  /** @return the {@link Path} to this project's <code>target</code> directory */
  static Path findProjectTargetDir() {
    Path targetDir = Paths.get(".", "bfd-pipeline-benchmarks", "target");
    if (!Files.isDirectory(targetDir)) targetDir = Paths.get(".", "target");
    if (!Files.isDirectory(targetDir)) throw new IllegalStateException();

    return targetDir;
  }

  /**
   * @return the {@link Path} to this project's <code>target/benchmark-iterations</code> directory
   */
  static Path findBenchmarksWorkDir() {
    return findProjectTargetDir().resolve("benchmark-iterations");
  }

  /**
   * @param iterationIndex the index/ID of the benchmark iteration to compute the bucket name for
   * @return the name of the S3 bucket that the specified benchmark iteration should store its data
   *     in
   */
  static String computeBenchmarkDataBucketName(int iterationIndex) {
    return String.format(
        "gov-hhs-cms-bluebutton-datapipeline-benchmark-iteration%d", iterationIndex);
  }

  /**
   * @param processBuilder the process to run
   * @param logFile the {@link Path} to write the process' output to
   * @return the process' exit code
   */
  static int runProcessAndLogOutput(ProcessBuilder processBuilder, Path logFile) {
    Process process = null;
    try {
      processBuilder.redirectErrorStream(true);
      processBuilder.redirectOutput(logFile.toFile());

      process = processBuilder.start();
      int exitCode = process.waitFor();
      return exitCode;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      throw new BadCodeMonkeyException(e);
    } finally {
      if (process != null) process.destroyForcibly();
    }
  }

  /**
   * Runs the <code>teardown.sh</code> script for the specified benchmark iteration. This script
   * will collect any relevant logs, then terminate the EC2 instances, etc. for the specified
   * benchmark iteration.
   *
   * @param iterationIndex the index/ID of the benchmark iteration to clean up
   * @param benchmarkIterationDir the {@link Path} of the directory that the benchmark iteration
   *     stored its working files in
   * @param ec2KeyFile the {@link Path} to the AWS EC2 key PEM file that the benchmark systems
   *     should use
   * @return the exit/return code of the <code>teardown.sh</code> script
   */
  static int runBenchmarkTeardown(int iterationIndex, Path benchmarkIterationDir, Path ec2KeyFile) {
    Path teardownLog = benchmarkIterationDir.resolve("ansible_teardown.log");
    Path teardownScript =
        BenchmarkUtilities.findProjectTargetDir()
            .resolve("..")
            .resolve("src")
            .resolve("test")
            .resolve("ansible")
            .resolve("teardown.sh");
    ProcessBuilder teardownProcessBuilder =
        new ProcessBuilder(
            teardownScript.toString(),
            "--iteration",
            ("" + iterationIndex),
            "--ec2keyfile",
            ec2KeyFile.toString());
    int teardownExitCode =
        BenchmarkUtilities.runProcessAndLogOutput(teardownProcessBuilder, teardownLog);
    return teardownExitCode;
  }
}
