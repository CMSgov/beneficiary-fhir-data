package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.PipelineTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.AwsFailureException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.utils.StringUtils;

/** Tests downloaded S3 file attributes such as MD5ChkSum. */
public final class ManifestEntryDownloadTaskIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

  /** only need a single instance of the S3 client. */
  private static S3Client s3Client;
  /** The S3 task manager. */
  private S3TaskManager s3TaskManager;
  /** Dummy S3 bucket name. */
  private static final String DUMMY_S3_BUCKET_NAME = "foo";

  /** Sets the minio test container. */
  @BeforeAll
  public static void setupMinioS3Client() {
    s3Client =
        SharedS3Utilities.createS3Client(new ExtractionOptions(DUMMY_S3_BUCKET_NAME).getS3Region());
  }

  /**
   * Test to ensure the MD5ChkSum of the downloaded S3 file matches the generated MD5ChkSum value.
   */
  @SuppressWarnings("deprecation")
  @Test
  public void testMD5ChkSum() throws Exception {
    String bucket = null;
    try {
      bucket = DataSetTestUtilities.createTestBucket(s3Client);
      ExtractionOptions options = new ExtractionOptions(bucket);
      LOGGER.info("Bucket created: '{}:{}'", s3Client.listBuckets().owner().displayName(), bucket);
      DataSetManifest manifest =
          new DataSetManifest(
              Instant.now(),
              0,
              false,
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY));

      // upload beneficiary sample file to S3 bucket created above
      DataSetTestUtilities.putObject(s3Client, bucket, manifest);
      DataSetTestUtilities.putObject(
          s3Client,
          bucket,
          manifest,
          manifest.getEntries().get(0),
          StaticRifResource.SAMPLE_A_BENES.getResourceUrl());

      // download file from S3 that was just uploaded above

      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder()
              .bucket(bucket)
              .key(
                  String.format(
                      "%s/%s/%s",
                      CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
                      manifest.getEntries().get(0).getParentManifest().getTimestampText(),
                      manifest.getEntries().get(0).getName()))
              .build();
      Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
      s3TaskManager =
          new S3TaskManager(
              PipelineTestUtils.get().getPipelineApplicationState().getMetrics(),
              new ExtractionOptions(options.getS3BucketName()));
      LOGGER.info(
          "Downloading '{}' to '{}'...",
          getObjectRequest.key(),
          localTempFile.toAbsolutePath().toString());

      DownloadFileRequest downloadFileRequest =
          DownloadFileRequest.builder()
              .getObjectRequest(getObjectRequest)
              .addTransferListener(LoggingTransferListener.create())
              .destination(localTempFile.toFile())
              .build();

      FileDownload downloadFile =
          s3TaskManager.getS3TransferManager().downloadFile(downloadFileRequest);
      CompletedFileDownload downloadResult = downloadFile.completionFuture().join();

      InputStream downloadedInputStream = new FileInputStream(localTempFile.toString());
      String generatedMD5ChkSum = ManifestEntryDownloadTask.computeMD5ChkSum(downloadedInputStream);
      LOGGER.info("The generated MD5 value from Java (Base64 encoded) is:" + generatedMD5ChkSum);

      String downloadedFileMD5ChkSum = downloadResult.response().metadata().get("md5chksum");
      LOGGER.info("The MD5 value from AWS S3 file's metadata is: " + downloadedFileMD5ChkSum);
      assertEquals(
          downloadedFileMD5ChkSum,
          generatedMD5ChkSum,
          "Checksum doesn't match on downloaded file " + getObjectRequest.key());
      LOGGER.info(
          "Downloaded '{}' to '{}'.",
          getObjectRequest.key(),
          localTempFile.toAbsolutePath().toString());

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (SdkClientException e) {
      throw new AwsFailureException(e);
    } catch (CancellationException e) {
      // Shouldn't happen, as our apps don't use thread interrupts.
      throw new BadCodeMonkeyException(e);
    } finally {
      if (StringUtils.isNotBlank(bucket))
        DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }
}
