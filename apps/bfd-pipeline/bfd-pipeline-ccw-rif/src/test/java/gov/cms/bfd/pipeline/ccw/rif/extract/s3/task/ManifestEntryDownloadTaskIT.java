package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.AbstractLocalStackS3Test;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.AwsFailureException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.StringUtils;

/** Tests downloaded S3 file attributes such as MD5ChkSum. */
final class ManifestEntryDownloadTaskIT extends AbstractLocalStackS3Test {
  private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

  /**
   * Test to ensure the MD5ChkSum of the downloaded S3 file matches the generated MD5ChkSum value.
   */
  @Test
  void testMD5ChkSum() throws Exception {
    String bucket = null;
    try {
      bucket = DataSetTestUtilities.createTestBucket(s3Dao);
      ExtractionOptions options =
          new ExtractionOptions(bucket, Optional.empty(), Optional.empty(), s3ClientConfig);
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
      DataSetTestUtilities.putObject(s3Dao, bucket, manifest);
      DataSetTestUtilities.putObject(
          s3Dao,
          bucket,
          manifest,
          manifest.getEntries().get(0),
          StaticRifResource.SAMPLE_A_BENES.getResourceUrl());

      // download file from S3 that was just uploaded above

      String s3Key =
          String.format(
              "%s/%s/%s",
              CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
              manifest.getEntries().get(0).getParentManifest().getTimestampText(),
              manifest.getEntries().get(0).getName());
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(bucket).key(s3Key).build();
      Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
      LOGGER.info(
          "Downloading '{}' to '{}'...", getObjectRequest.key(), localTempFile.toAbsolutePath());

      GetObjectResponse downloadFileResponse = s3Dao.downloadObject(bucket, s3Key, localTempFile);

      InputStream downloadedInputStream = new FileInputStream(localTempFile.toString());
      String generatedMD5ChkSum = ManifestEntryDownloadTask.computeMD5ChkSum(downloadedInputStream);
      LOGGER.info("The generated MD5 value from Java (Base64 encoded) is:" + generatedMD5ChkSum);

      String downloadedFileMD5ChkSum = downloadFileResponse.metadata().get("md5chksum");
      LOGGER.info("The MD5 value from AWS S3 file's metadata is: " + downloadedFileMD5ChkSum);
      assertEquals(
          downloadedFileMD5ChkSum,
          generatedMD5ChkSum,
          "Checksum doesn't match on downloaded file " + getObjectRequest.key());
      LOGGER.info(
          "Downloaded '{}' to '{}'.", getObjectRequest.key(), localTempFile.toAbsolutePath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (SdkClientException e) {
      throw new AwsFailureException(e);
    } catch (CancellationException e) {
      // Shouldn't happen, as our apps don't use thread interrupts.
      throw new BadCodeMonkeyException(e);
    } finally {
      if (StringUtils.isNotBlank(bucket))
        DataSetTestUtilities.deleteObjectsAndBucket(s3Dao, bucket);
    }
  }
}
