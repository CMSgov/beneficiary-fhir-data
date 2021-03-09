package gov.cms.bfd.pipeline.benchmarks;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3Utilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is a small utility app designed to clean up after abnormally terminated runs of {@link
 * PipelineApplicationBenchmark}. It reads the metadata in <code>target/benchmark-iterations</code>
 * and runs the teardown scripts for each iteration.
 */
public final class BenchmarkCleanup {
  /**
   * The app driver for this utility.
   *
   * @param args (not used)
   * @throws Exception Any {@link Exception}s encountered will cause this app to terminate early.
   */
  public static void main(String[] args) throws Exception {
    Path ec2KeyFile = BenchmarkUtilities.findEc2KeyFile();
    Path benchmarksWorkDir = BenchmarkUtilities.findBenchmarksWorkDir();
    AmazonS3 s3Client = S3Utilities.createS3Client(S3Utilities.REGION_DEFAULT);

    Set<Integer> iterationIndices =
        Files.list(benchmarksWorkDir)
            .filter(p -> Files.isDirectory(p))
            .map(p -> p.getFileName().toString())
            .map(i -> Integer.parseInt(i))
            .collect(Collectors.toSet());

    for (int iterationIndex : iterationIndices) {
      int teardownExitCode =
          BenchmarkUtilities.runBenchmarkTeardown(
              iterationIndex, benchmarksWorkDir.resolve("" + iterationIndex), ec2KeyFile);
      System.out.printf(
          "Iteration '%d': teardown returned '%d'.\n", iterationIndex, teardownExitCode);

      try {
        Bucket iterationDataBucket =
            new Bucket(BenchmarkUtilities.computeBenchmarkDataBucketName(iterationIndex));
        DataSetTestUtilities.deleteObjectsAndBucket(s3Client, iterationDataBucket);
        System.out.printf("Iteration '%d': S3 bucket deleted.\n", iterationIndex);
      } catch (AmazonS3Exception e) {
        System.out.printf(
            "Iteration '%d': unable to delete S3 bucket ('%s').\n", iterationIndex, e.getMessage());
      }
    }
  }
}
