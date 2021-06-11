package gov.cms.bfd.pipeline.app;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3Utilities;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link PipelineManager}. */
public final class PipelineManagerIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineManagerIT.class);

  /**
   * Verifies that {@link PipelineManager} handles errors as expected when asked to run against an
   * S3 bucket that doesn't exist. This test case isn't so much needed to test that one specific
   * failure case, but to instead verify the overall error handling.
   *
   * @throws InterruptedException (shouldn't happen)
   */
  @Test
  public void missingBucket() throws InterruptedException {
    // Start the pipeline against a bucket that doesn't exist.
    MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
    PipelineManager pipeline =
        new PipelineManager(new MetricRegistry(), new ExtractionOptions("foo"), 100, listener);
    pipeline.start();

    // Wait for the pipeline to error out.
    Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> !listener.errorEvents.isEmpty());
    pipeline.stop();
    Assert.assertEquals(0, listener.getNoDataAvailableEvents());
    Assert.assertNotEquals(0, listener.getErrorEvents().size());
    Assert.assertEquals(0, listener.getDataEvents().size());
  }

  /** Tests {@link PipelineManager} when run against an empty bucket. */
  @Test
  public void emptyBucketTest() {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      // Create the (empty) bucket to run against.
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options = new ExtractionOptions(bucket.getName());
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());

      // Start the pipeline and then stop it.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      PipelineManager pipeline = new PipelineManager(new MetricRegistry(), options, 100, listener);
      pipeline.start();
      Awaitility.await()
          .atMost(Duration.TEN_SECONDS)
          .until(() -> listener.getNoDataAvailableEvents() > 0);
      pipeline.stop();

      // Verify that no data sets were generated.
      Assert.assertNotEquals(0, listener.getNoDataAvailableEvents());
      Assert.assertEquals(0, listener.getDataEvents().size());
      Assert.assertEquals(0, listener.errorEvents.size());
    } finally {
      if (bucket != null) s3Client.deleteBucket(bucket.getName());
    }
  }

  /**
   * Tests {@link PipelineManager} when run against a bucket with two data sets in it.
   *
   * @throws InterruptedException (shouldn't happen)
   */
  @Test
  public void multipleDataSetsTest() throws InterruptedException {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with
       * two data sets.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options = new ExtractionOptions(bucket.getName());
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());
      DataSetManifest manifestA =
          new DataSetManifest(
              Instant.now().minus(1L, ChronoUnit.HOURS),
              0,
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY));
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestA));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifestA,
              manifestA.getEntries().get(0),
              StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));
      DataSetManifest manifestB =
          new DataSetManifest(
              manifestA.getTimestampText(),
              1,
              new DataSetManifestEntry("pde.rif", RifFileType.PDE));
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestB));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifestB,
              manifestB.getEntries().get(0),
              StaticRifResource.SAMPLE_A_PDE.getResourceUrl()));
      DataSetManifest manifestC =
          new DataSetManifest(
              Instant.now(), 0, new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestC));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifestC,
              manifestC.getEntries().get(0),
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()));

      // Start the pipeline up.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      PipelineManager pipeline = new PipelineManager(new MetricRegistry(), options, 100, listener);
      pipeline.start();

      // Wait for the job to generate events for the three data sets.
      Awaitility.await()
          .atMost(Duration.ONE_MINUTE)
          .until(() -> listener.getDataEvents().size() >= 3);
      pipeline.stop();

      // Verify what was handed off to the DataSetMonitorListener.
      Assert.assertEquals(3, listener.getDataEvents().size());
      Assert.assertEquals(
          "Errors encountered: " + listener.getErrorEvents(), 0, listener.getErrorEvents().size());
      Assert.assertEquals(manifestA.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
      Assert.assertEquals(manifestB.getTimestamp(), listener.getDataEvents().get(1).getTimestamp());
      Assert.assertEquals(manifestC.getTimestamp(), listener.getDataEvents().get(2).getTimestamp());

      // Verify that the data sets were both renamed.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
          1
              + manifestA.getEntries().size()
              + 1
              + manifestB.getEntries().size()
              + 1
              + manifestC.getEntries().size(),
          java.time.Duration.ofSeconds(10));
    } catch (Exception e) {
      LOGGER.warn("Test case failed.", e);
      throw new RuntimeException(e);
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }
}
