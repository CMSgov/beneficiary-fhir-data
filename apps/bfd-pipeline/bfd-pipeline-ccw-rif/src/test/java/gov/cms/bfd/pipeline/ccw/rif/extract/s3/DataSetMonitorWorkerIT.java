package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link DataSetMonitorWorker}. */
public final class DataSetMonitorWorkerIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMonitorIT.class);

  /** Tests {@link DataSetMonitorWorker} when run against an empty bucket. */
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

      // Run the worker.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager = new S3TaskManager(new MetricRegistry(), options);
      DataSetMonitorWorker monitorWorker =
          new DataSetMonitorWorker(new MetricRegistry(), options, s3TaskManager, listener);
      monitorWorker.run();

      // Verify that no data sets were generated.
      Assert.assertEquals(1, listener.getNoDataAvailableEvents());
      Assert.assertEquals(0, listener.getDataEvents().size());
      Assert.assertEquals(0, listener.getErrorEvents().size());
    } finally {
      if (bucket != null) s3Client.deleteBucket(bucket.getName());
    }
  }

  /** Tests {@link DataSetMonitorWorker} when run against a bucket with a single data set. */
  @Test
  public void singleDataSetTest() {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with a
       * data set.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options = new ExtractionOptions(bucket.getName());
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());
      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now(),
              0,
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifest,
              manifest.getEntries().get(0),
              StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifest,
              manifest.getEntries().get(1),
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()));

      // Run the worker.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager = new S3TaskManager(new MetricRegistry(), options);
      DataSetMonitorWorker monitorWorker =
          new DataSetMonitorWorker(new MetricRegistry(), options, s3TaskManager, listener);
      monitorWorker.run();

      // Verify what was handed off to the DataSetMonitorListener.
      Assert.assertEquals(0, listener.getNoDataAvailableEvents());
      Assert.assertEquals(1, listener.getDataEvents().size());
      Assert.assertEquals(manifest.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
      Assert.assertEquals(
          manifest.getEntries().size(), listener.getDataEvents().get(0).getFileEvents().size());
      Assert.assertEquals(0, listener.getErrorEvents().size());

      // Verify that the data set was renamed.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          DataSetMonitorWorker.S3_PREFIX_COMPLETED_DATA_SETS,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /** Tests {@link DataSetMonitorWorker} when run against an empty bucket. */
  @Test
  public void multipleDataSetsTest() {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with
       * two data sets.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options = new ExtractionOptions(bucket.getName(), null, 1);
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
              StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));
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

      // Run the worker.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager = new S3TaskManager(new MetricRegistry(), options);
      DataSetMonitorWorker monitorWorker =
          new DataSetMonitorWorker(new MetricRegistry(), options, s3TaskManager, listener);
      monitorWorker.run();

      // Verify what was handed off to the DataSetMonitorListener.
      Assert.assertEquals(0, listener.getNoDataAvailableEvents());
      Assert.assertEquals(1, listener.getDataEvents().size());
      Assert.assertEquals(manifestA.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
      Assert.assertEquals(
          manifestA.getEntries().size(), listener.getDataEvents().get(0).getFileEvents().size());
      Assert.assertEquals(0, listener.getErrorEvents().size());

      /*
       * Verify that the first data set was renamed and the second is
       * still there.
       */
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS,
          1 + manifestB.getEntries().size() + 1 + manifestC.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          DataSetMonitorWorker.S3_PREFIX_COMPLETED_DATA_SETS,
          1 + manifestA.getEntries().size(),
          java.time.Duration.ofSeconds(10));
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Tests {@link DataSetMonitorWorker} when run against a bucket with a single data set that should
   * be skipped (per {@link ExtractionOptions#getDataSetFilter()}).
   */
  @Test
  public void skipDataSetTest() {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with a
       * data set.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options = new ExtractionOptions(bucket.getName(), RifFileType.PDE);
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());
      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now(),
              0,
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifest,
              manifest.getEntries().get(0),
              StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifest,
              manifest.getEntries().get(1),
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()));

      // Run the worker.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager = new S3TaskManager(new MetricRegistry(), options);
      DataSetMonitorWorker monitorWorker =
          new DataSetMonitorWorker(new MetricRegistry(), options, s3TaskManager, listener);
      monitorWorker.run();

      // Verify what was handed off to the DataSetMonitorListener.
      Assert.assertEquals(1, listener.getNoDataAvailableEvents());
      Assert.assertEquals(0, listener.getDataEvents().size());
      Assert.assertEquals(0, listener.getErrorEvents().size());

      // Verify that the data set was not renamed.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          DataSetMonitorWorker.S3_PREFIX_COMPLETED_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }
}
