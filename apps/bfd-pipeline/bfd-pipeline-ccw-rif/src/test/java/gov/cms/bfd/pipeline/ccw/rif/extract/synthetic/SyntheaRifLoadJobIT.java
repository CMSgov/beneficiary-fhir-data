package gov.cms.bfd.pipeline.ccw.rif.extract.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.codahale.metrics.Slf4jReporter;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.AbstractLocalStackS3Test;
import gov.cms.bfd.pipeline.PipelineTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusReporter;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFileRecords;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.PreValidationProperties;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.MockDataSetMonitorListener;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.NewDataSetQueue;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FileCache;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3FilesDao;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

/** Integration tests for Synthea pre-validation bucket handling. */
@ExtendWith(MockitoExtension.class)
final class SyntheaRifLoadJobIT extends AbstractLocalStackS3Test {
  private static final Logger LOGGER = LoggerFactory.getLogger(SyntheaRifLoadJobIT.class);

  /** Used to capture status updates from the job. */
  @Mock private CcwRifLoadJobStatusReporter statusReporter;

  /**
   * Ensures that each test case here starts with a clean/empty database, with the right schema.
   *
   * @param testInfo the test info
   */
  @BeforeEach
  public void prepareTestDatabase(TestInfo testInfo) {
    LOGGER.info("{}: starting.", testInfo.getDisplayName());
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Log a message after every test finishes.
   *
   * @param testInfo the test info
   */
  @AfterEach
  public void finished(TestInfo testInfo) {
    LOGGER.info("{}: finished.", testInfo.getDisplayName());
  }

  /**
   * Tests {@link CcwRifLoadJob} when run using data in the Synthetic/Incoming folder(s). Data
   * should be read and moved into the respective Synthetic/Failed folder(s).
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void syntheticDataSetPreValidationFailedTest() throws Exception {
    // load data into db
    loadSample(
        List.of(
            StaticRifResource.SAMPLE_SYNTHEA_BENES2011,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2012,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2013,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2014,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2015,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2016,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2017,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2018,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2019,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2020,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2021),
        CcwRifLoadTestUtils.getLoadOptions());

    String bucket = null;
    try {
      // Create (empty) bucket to run against, and populate it with a data set.
      bucket = s3Dao.createTestBucket();
      ExtractionOptions options =
          new ExtractionOptions(bucket, Optional.empty(), Optional.of(1), s3ClientConfig);
      LOGGER.info("Bucket created: '{}:{}'", s3Dao.readListBucketsOwner(), bucket);

      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now().minus(1, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS,
              new DataSetManifestEntry("beneficiary.rif", RifFileType.BENEFICIARY),
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));

      /*
       * create {@link PreValidationProperties} that includes a bene_id that is
       * already in db
       */
      PreValidationProperties preValProps = new PreValidationProperties();
      preValProps.setBeneIdStart(-1000006); // this one exists in db...should trigger failure
      preValProps.setBeneIdEnd(-1000010);
      preValProps.setCarrClmCntlNumStart(0);
      preValProps.setClmGrpIdStart(0);
      preValProps.setClmIdEnd(0);
      preValProps.setClmIdStart(0);
      preValProps.setFiDocCntlNumStart("JUNK");
      preValProps.setHicnStart("JUNK");
      preValProps.setMbiStart("JUNK");
      preValProps.setPdeIdEnd(0);
      preValProps.setPdeIdStart(0);
      manifest.setPreValidationProperties(preValProps);

      // upload data to Synthea S3 bucket
      final var manifestKey =
          putSampleFilesInTestBucket(
              bucket,
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
              manifest,
              List.of(
                  StaticRifResource.SAMPLE_SYNTHEA_BENES2021.getResourceUrl(),
                  StaticRifResource.SAMPLE_SYNTHEA_CARRIER.getResourceUrl()));

      // Run the job.
      final var listener = new MockDataSetMonitorListener();
      final var pipelineAppState = PipelineTestUtils.get().getPipelineApplicationState();
      final var transactionManager =
          new TransactionManager(pipelineAppState.getEntityManagerFactory());
      final var s3FilesDao = new S3FilesDao(transactionManager);
      final var s3FileCache = new S3FileCache(s3Dao, bucket);
      final var dataSetQueue =
          new NewDataSetQueue(
              s3Dao,
              bucket,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
              s3FilesDao,
              s3FileCache);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              pipelineAppState,
              options,
              dataSetQueue,
              listener,
              false,
              Optional.empty(),
              statusReporter);
      // Process dataset
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener; there should
      // be no events since the pre-validation failure short-circuits everything.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(0, listener.getDataEvents().size());

      verifyManifestFileStatus(s3FilesDao, manifestKey, S3ManifestFile.ManifestStatus.REJECTED);
    } finally {
      if (StringUtils.isNotBlank(bucket)) {
        s3Dao.deleteTestBucket(bucket);
      }
    }
  }

  /**
   * Tests {@link CcwRifLoadJob} when run in idempotent mode using data in the Synthetic/Incoming
   * folder(s). Data should be read and moved into the respective Synthetic/Done folder(s).
   *
   * @throws Exception (exceptions indicate test failure)
   */
  @Test
  public void syntheticDataSetPreValidationIdempotentSucceedsTest() throws Exception {
    // load data into db
    loadSample(
        List.of(
            StaticRifResource.SAMPLE_SYNTHEA_BENES2011,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2012,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2013,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2014,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2015,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2016,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2017,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2018,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2019,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2020,
            StaticRifResource.SAMPLE_SYNTHEA_BENES2021),
        CcwRifLoadTestUtils.getLoadOptions());

    String bucket = null;
    try {
      // Create (empty) bucket to run against, and populate it with a data set.
      bucket = s3Dao.createTestBucket();
      ExtractionOptions options =
          new ExtractionOptions(bucket, Optional.empty(), Optional.of(1), s3ClientConfig);
      LOGGER.info("Bucket created: '{}:{}'", s3Dao.readListBucketsOwner(), bucket);

      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now().minus(1, ChronoUnit.DAYS),
              0,
              true,
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_SYNTHETIC_DATA_SETS,
              new DataSetManifestEntry("beneficiary.rif", RifFileType.BENEFICIARY),
              new DataSetManifestEntry("carrier.rif", RifFileType.CARRIER));

      /*
       * create {@link PreValidationProperties} that includes a bene_id that is
       * already in db
       */
      PreValidationProperties preValProps = new PreValidationProperties();
      preValProps.setBeneIdStart(-1000006); // this one exists in db...but OK since idempotent mode
      preValProps.setBeneIdEnd(-1000010);
      preValProps.setCarrClmCntlNumStart(0);
      preValProps.setClmGrpIdStart(0);
      preValProps.setClmIdEnd(0);
      preValProps.setClmIdStart(0);
      preValProps.setFiDocCntlNumStart("JUNK");
      preValProps.setHicnStart("JUNK");
      preValProps.setMbiStart("JUNK");
      preValProps.setPdeIdEnd(0);
      preValProps.setPdeIdStart(0);
      manifest.setPreValidationProperties(preValProps);

      // upload data to Synthea S3 bucket
      final var manifestKey =
          putSampleFilesInTestBucket(
              bucket,
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
              manifest,
              List.of(
                  StaticRifResource.SAMPLE_SYNTHEA_BENES2021.getResourceUrl(),
                  StaticRifResource.SAMPLE_SYNTHEA_CARRIER.getResourceUrl()));

      // Run the job.
      final var listener = new MockDataSetMonitorListener();
      final var pipelineAppState = PipelineTestUtils.get().getPipelineApplicationState();
      final var transactionManager =
          new TransactionManager(pipelineAppState.getEntityManagerFactory());
      final var s3FilesDao = new S3FilesDao(transactionManager);
      final var s3FileCache = new S3FileCache(s3Dao, bucket);
      final var dataSetQueue =
          new NewDataSetQueue(
              s3Dao,
              bucket,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS,
              s3FilesDao,
              s3FileCache);
      CcwRifLoadJob ccwJob =
          new CcwRifLoadJob(
              pipelineAppState,
              options,
              dataSetQueue,
              listener,
              true, // run in idempotent mode
              Optional.empty(),
              statusReporter);
      // Process dataset
      ccwJob.call();

      // Verify what was handed off to the DataSetMonitorListener; there should be
      // no failure events since the pre-validation ran in idempotent mode which
      // means it is acceptable to have a bene_id overlaps.
      assertEquals(0, listener.getNoDataAvailableEvents());
      assertEquals(1, listener.getDataEvents().size());

      verifyManifestFileStatus(s3FilesDao, manifestKey, S3ManifestFile.ManifestStatus.COMPLETED);
    } finally {
      if (StringUtils.isNotBlank(bucket)) {
        s3Dao.deleteTestBucket(bucket);
      }
    }
  }

  /**
   * Runs {@link RifLoader} against the specified {@link StaticRifResourceGroup}.
   *
   * @param sampleResources the {@link StaticRifResourceGroup} to load
   * @param loadAppOptions the load app options
   */
  private void loadSample(List<StaticRifResource> sampleResources, LoadAppOptions loadAppOptions) {
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            false,
            sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));

    long loadCount =
        loadSample(
            sampleResources.get(0).getResourceUrl().toString(), loadAppOptions, rifFilesEvent);

    // Verify that the expected number of records were run successfully.
    assertEquals(
        sampleResources.stream().mapToInt(r -> r.getRecordCount()).sum(),
        loadCount,
        "Unexpected number of loaded records.");
  }

  /**
   * Runs {@link RifLoader} against the specified {@link StaticRifResourceGroup}.
   *
   * @param sampleName a human-friendly name that will be logged to identify the data load being
   *     kicked off here
   * @param options the {@link LoadAppOptions} to use
   * @param rifFilesEvent the {@link RifFilesEvent} to load
   * @return the number of RIF records that were loaded (as reported by the {@link RifLoader})
   */
  private long loadSample(String sampleName, LoadAppOptions options, RifFilesEvent rifFilesEvent) {
    LOGGER.info("Loading RIF files: '{}'...", sampleName);

    // Create the processors that will handle each stage of the pipeline.
    RifFilesProcessor processor = new RifFilesProcessor();
    RifLoader loader =
        new RifLoader(options, PipelineTestUtils.get().getPipelineApplicationState());

    // Link up the pipeline and run it.
    LOGGER.info("Loading RIF records...");
    int failureCount = 0;
    long loadCount = 0;
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
      try {
        loadCount += loader.processBlocking(rifFileRecords);
      } catch (Exception error) {
        failureCount += 1;
        LOGGER.warn("Record(s) failed to load.", error);
      }
      Slf4jReporter.forRegistry(rifFileEvent.getEventMetrics()).outputTo(LOGGER).build().report();
    }
    LOGGER.info("Loaded RIF files: '{}', record count: '{}'.", sampleName, loadCount);
    Slf4jReporter.forRegistry(PipelineTestUtils.get().getPipelineApplicationState().getMetrics())
        .outputTo(LOGGER)
        .build()
        .report();

    // Verify that the expected number of records were run successfully.
    assertEquals(0, failureCount, "Load errors encountered.");

    return loadCount;
  }

  /**
   * Put sample files in test specified bucket and key in s3.
   *
   * @param bucket the bucket to use for the test
   * @param location the key under which to put the file
   * @param manifest the manifest to use for the load files
   * @param resourcesToAdd the resource URLs to add to the bucket, see {@link StaticRifResource} for
   *     resource lists, should be in the order of the manifest
   */
  private String putSampleFilesInTestBucket(
      String bucket, String location, DataSetManifest manifest, List<URL> resourcesToAdd) {
    var manifestKey = DataSetTestUtilities.putObject(s3Dao, bucket, manifest, location);
    int index = 0;
    for (URL resource : resourcesToAdd) {
      DataSetTestUtilities.putObject(
          s3Dao, bucket, manifest, manifest.getEntries().get(index), resource, location);
      index++;
    }
    return manifestKey;
  }

  private void verifyManifestFileStatus(
      S3FilesDao s3FilesDao, String s3ManifestKey, S3ManifestFile.ManifestStatus expectedStatus) {
    S3ManifestFile manifestRecord = s3FilesDao.readS3ManifestAndDataFiles(s3ManifestKey);
    assertNotNull(manifestRecord, "no record in database for manifest: key=" + s3ManifestKey);
    assertEquals(expectedStatus, manifestRecord.getStatus());
  }
}
