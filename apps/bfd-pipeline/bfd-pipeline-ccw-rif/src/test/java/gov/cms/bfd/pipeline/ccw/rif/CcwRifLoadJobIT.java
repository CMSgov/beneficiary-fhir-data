package gov.cms.bfd.pipeline.ccw.rif;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.PipelineTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.PreValidationProperties;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.MockDataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3Utilities;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link CcwRifLoadJob}. */
public final class CcwRifLoadJobIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcwRifLoadJobIT.class);

  /**
   * Tests {@link CcwRifLoadJob} when run against an empty bucket.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void emptyBucketTest() throws Exception {
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

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      ccwJob.call();

      // Verify that no data sets were generated.
      assertEquals(1, listener.getNoDataAvailableEvents());
      assertEquals(0, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());
    } finally {
      if (bucket != null) s3Client.deleteBucket(bucket.getName());
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run against a bucket with a single data set.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void singleDataSetTest() throws Exception {
    validateLoadAtLocations(
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
        CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
        List.of(
            StaticRifResource.SAMPLE_A_BENES.getResourceUrl(),
            StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()),
        null);
  }

  /**
   * Tests {@link CcwRifLoadJob} when run against a bucket with a single data set within
   * Synthetic/Incoming.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void singleSyntheticDataSetTest() throws Exception {
    validateLoadAtLocations(
        CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
        CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS,
        List.of(
            StaticRifResource.SAMPLE_SYNTHEA_BENES2011.getResourceUrl(),
            StaticRifResource.SAMPLE_SYNTHEA_CARRIER.getResourceUrl()),
        null);
  }

  /**
   * Tests {@link CcwRifLoadJob} when run with data in the Synthetic/Incoming and Incoming folders.
   * Data should be read and moved into the respective Done and Synthetic/Done folders.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void multipleDataSetsWithSyntheticTest() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with
       * two data sets.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options =
          new ExtractionOptions(bucket.getName(), Optional.empty(), Optional.of(1));
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());

      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now(),
              0,
              false,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY));
      DataSetManifest manifestSynthetic =
          new DataSetManifest(
              Instant.now().minus(1, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER),
              new DataSetManifestEntry("inpatient.rif", RifFileType.INPATIENT));

      // Add files to each location the test wants them in
      putSampleFilesInTestBucket(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
          manifest,
          List.of(StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));
      putSampleFilesInTestBucket(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          manifestSynthetic,
          List.of(
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl(),
              StaticRifResource.SAMPLE_A_INPATIENT.getResourceUrl()));

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      // Process both sets
      ccwJob.call();
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(2, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      /*
       * Verify that the datasets were moved to their respective locations.
       */
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS,
          1 + manifestSynthetic.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifestSynthetic.getTimestampText()
                  + "/0_manifest.xml"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifestSynthetic.getTimestampText()
                  + "/carrier.rif"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifestSynthetic.getTimestampText()
                  + "/inpatient.rif"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/0_manifest.xml"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/beneficiaries.rif"));

    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run with data in the Synthetic/Incoming folder(s). Data should
   * be read and moved into the respective Synthetic/Failed folder(s).
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void syntheticDataSetPreValidationFailedTest() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      // Create (empty) bucket to run against, and populate it with a data set.
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options =
          new ExtractionOptions(bucket.getName(), Optional.empty(), Optional.of(1));
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());

      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now().minus(1, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER),
              new DataSetManifestEntry("inpatient.rif", RifFileType.INPATIENT));

      /** set things up to force a failure; this mock class's isValid() returns false (not valid) */
      PreValidationProperties preValProps =
          generatePreValidationProperties(
              Optional.of(
                  "gov.cms.bfd.pipeline.ccw.rif.extract.s3.MockRifLoadPreValidationSynthea"));
      manifest.setPreValidationProperties(preValProps);

      putSampleFilesInTestBucket(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          manifest,
          List.of(
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl(),
              StaticRifResource.SAMPLE_A_INPATIENT.getResourceUrl()));

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      // Process dataset
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(0, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      // Verify that the datasets were moved to their respective 'failed' locations.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_FAILED_SYNTHETIC_DATA_SETS,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_FAILED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/0_manifest.xml"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_FAILED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/carrier.rif"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_FAILED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/inpatient.rif"));

    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run with data in the Synthetic/Incoming folder(s). The
   * pre-validation class does not exist so an exception is thrown. Data should remain in the
   * Synthetic/Incoming folder.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void syntheticDataSetWithNonExistentPreValidationClass() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      // Create (empty) bucket to run against, and populate it
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options =
          new ExtractionOptions(bucket.getName(), Optional.empty(), Optional.of(1));
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());

      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now().minus(1, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));

      /** set things up to force an exception by specifying a non-existent pre-validation class */
      PreValidationProperties preValProps =
          generatePreValidationProperties(Optional.of("foo.bar.junk.PreValidationTrash"));
      manifest.setPreValidationProperties(preValProps);

      putSampleFilesInTestBucket(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          manifest,
          List.of(StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()));

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      // Process dataset; should throw exception
      Throwable exception =
          assertThrows(
              BadCodeMonkeyException.class,
              () -> {
                ccwJob.call();
              });
      assertEquals(
          "Failed to create CcwRifLoadPreValidateInterface; class 'foo.bar.junk.PreValidationTrash' not found!",
          exception.getMessage());

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(0, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      // Verify that the datasets still exist in the /Incoming bucket folder.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          2,
          java.time.Duration.ofSeconds(10));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/0_manifest.xml"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/carrier.rif"));

    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run with data in the Synthetic/Incoming folder(s). The
   * pre-validation class is a valid class but it does not implement the {@link
   * CcwRifLoadPreValidateInterface} so an exception is thrown. Data should remain in the
   * Synthetic/Incoming folder.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void syntheticDataSetWithNoPreValidationInterface() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      // Create (empty) bucket to run against, and populate it
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options =
          new ExtractionOptions(bucket.getName(), Optional.empty(), Optional.of(1));
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());

      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now().minus(1, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));

      /**
       * set things up to force an exception by specifying a pre-validation class that resolves to a
       * valid Java class, but the class does not implement the {@link
       * CcwRifLoadPreValidateInterface} so it throws an exception.
       */
      PreValidationProperties preValProps =
          generatePreValidationProperties(
              Optional.of("gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3Utilities"));
      manifest.setPreValidationProperties(preValProps);

      putSampleFilesInTestBucket(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          manifest,
          List.of(StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()));

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      // Process dataset; should throw exception
      Throwable exception =
          assertThrows(
              BadCodeMonkeyException.class,
              () -> {
                ccwJob.call();
              });
      assertEquals(
          "specified class 'gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3Utilities' does not support CcwRifLoadPreValidateInterface!",
          exception.getMessage());

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(0, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      // Verify that the datasets still exist in the /Incoming bucket folder.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          2,
          java.time.Duration.ofSeconds(10));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/0_manifest.xml"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/carrier.rif"));

    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run with data in the Synthetic/Incoming folder(s). Data should
   * be read and moved into the respective Synthetic/Done folder(s).
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void syntheticDataSetUsingDefaultPreValidationTest() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with
       * two data sets.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options =
          new ExtractionOptions(bucket.getName(), Optional.empty(), Optional.of(1));
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());

      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now().minus(1, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER),
              new DataSetManifestEntry("inpatient.rif", RifFileType.INPATIENT));

      /**
       * set things up to to use a default implementation of the Synthea {@link
       * CcwRifLoadPreValidateInterface} by not providing a class name.
       */
      PreValidationProperties preValProps = generatePreValidationProperties(Optional.empty());
      manifest.setPreValidationProperties(preValProps);

      putSampleFilesInTestBucket(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
          manifest,
          List.of(
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl(),
              StaticRifResource.SAMPLE_A_INPATIENT.getResourceUrl()));

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      // Process dataset
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(1, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      // Verify that the datasets were moved to their respective 'failed' locations.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/0_manifest.xml"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/carrier.rif"));
      assertTrue(
          s3Client.doesObjectExist(
              bucket.getName(),
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS
                  + "/"
                  + manifest.getTimestampText()
                  + "/inpatient.rif"));

    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Utility method to create {@link PreValidationProperties} object that can be used in various
   * tests for Synthea pre-validation tasks.
   *
   * @param className optional {@link String} denoting Java class that will be instantiated
   * @returns {@link PreValidationProperties}
   */
  private PreValidationProperties generatePreValidationProperties(Optional<String> className) {
    PreValidationProperties preValProps = new PreValidationProperties();
    preValProps.setBeneIdStart(0);
    preValProps.setBeneIdEnd(0);
    preValProps.setCarrClmCntlNumStart(0);
    preValProps.setClmGrpIdStart(0);
    preValProps.setClmIdEnd(0);
    preValProps.setClmIdStart(0);
    preValProps.setFiDocCntlNumStart("JUNK");
    preValProps.setHicnStart("JUNK");
    preValProps.setMbiStart("JUNK");
    preValProps.setPdeIdEnd(0);
    preValProps.setPdeIdStart(0);
    if (className.isPresent()) {
      preValProps.setPreValClassName(className.get());
    }
    return preValProps;
  }

  /**
   * Tests {@link CcwRifLoadJob} when run against an empty bucket.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void multipleDataSetsTest() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with
       * two data sets.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options =
          new ExtractionOptions(bucket.getName(), Optional.empty(), Optional.of(1));
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());
      DataSetManifest manifestA =
          new DataSetManifest(
              Instant.now().minus(1L, ChronoUnit.HOURS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
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
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
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
              Instant.now(),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifestC));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifestC,
              manifestC.getEntries().get(0),
              StaticRifResource.SAMPLE_A_CARRIER.getResourceUrl()));

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(1, listener.getDataEvents().size());
      assertEquals(manifestA.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
      assertEquals(
          manifestA.getEntries().size(), listener.getDataEvents().get(0).getFileEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      /*
       * Verify that the first data set was renamed and the second is
       * still there.
       */
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
          1 + manifestB.getEntries().size() + 1 + manifestC.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
          1 + manifestA.getEntries().size(),
          java.time.Duration.ofSeconds(10));
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run against a bucket with a single data set that should be
   * skipped (per {@link ExtractionOptions#getDataSetFilter()}).
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void skipDataSetTest() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
      /*
       * Create the (empty) bucket to run against, and populate it with a
       * data set.
       */
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options =
          new ExtractionOptions(bucket.getName(), Optional.of(RifFileType.PDE));
      LOGGER.info(
          "Bucket created: '{}:{}'",
          s3Client.getS3AccountOwner().getDisplayName(),
          bucket.getName());
      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now(),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
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

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(1, listener.getNoDataAvailableEvents());
      assertEquals(0, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      // Verify that the data set was not renamed.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run against a bucket with a single data set that should be
   * skipped due to a future date.
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void skipDataSetTestForFutureManifestDate() throws Exception {
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
              Instant.now().plus(3, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
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

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(1, listener.getNoDataAvailableEvents());
      assertEquals(0, listener.getDataEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      // Verify that the data set was not renamed.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
          0,
          java.time.Duration.ofSeconds(10));
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Validate load given the input location to load files and output location to look for the files
   * once they're loaded.
   *
   * @param inputLocation the input location (bucket key) where files should be placed initially
   * @param expectedOutputLocation the expected output location (bucket key) where files are
   *     expected to be moved after processing
   * @throws Exception the exception
   */
  private void validateLoadAtLocations(
      String inputLocation,
      String expectedOutputLocation,
      List<URL> fileList,
      DataSetManifest inManifest)
      throws Exception {
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

      DataSetManifest manifest = inManifest;
      if (manifest == null) {
        manifest =
            new DataSetManifest(
                Instant.now(),
                0,
                false,
                inputLocation,
                expectedOutputLocation,
                new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY),
                new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));
      }

      // Add files to each location the test wants them in
      putSampleFilesInTestBucket(s3Client, bucket, inputLocation, manifest, fileList);

      // Run the job.
      MockDataSetMonitorListener listener = new MockDataSetMonitorListener();
      S3TaskManager s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(), options);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              PipelineTestUtils.get().getPipelineApplicationState(),
              options,
              s3TaskManager,
              listener,
              false);

      ccwJob.call();
      // Verify what was handed off to the DataSetMonitorListener.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(1, listener.getDataEvents().size());
      assertEquals(manifest.getTimestamp(), listener.getDataEvents().get(0).getTimestamp());
      assertEquals(
          manifest.getEntries().size(), listener.getDataEvents().get(0).getFileEvents().size());
      assertEquals(0, listener.getErrorEvents().size());

      // Verify that the data set was renamed.
      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client, bucket, inputLocation, 0, java.time.Duration.ofSeconds(10));

      DataSetTestUtilities.waitForBucketObjectCount(
          s3Client,
          bucket,
          expectedOutputLocation,
          1 + manifest.getEntries().size(),
          java.time.Duration.ofSeconds(10));
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }

  /**
   * Put sample files in test specified bucket and key in s3.
   *
   * @param s3Client the s3 client
   * @param bucket the bucket to use for the test
   * @param location the key under which to put the file
   * @param manifest the manifest to use for the load files
   * @param resourcesToAdd the resource URLs to add to the bucket, see {@link StaticRifResource} for
   *     resource lists, should be in the order of the manifest
   */
  private void putSampleFilesInTestBucket(
      AmazonS3 s3Client,
      Bucket bucket,
      String location,
      DataSetManifest manifest,
      List<URL> resourcesToAdd) {
    s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, location));
    int index = 0;
    for (URL resource : resourcesToAdd) {
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket, manifest, manifest.getEntries().get(index), resource, location));
      index++;
    }
  }
}
