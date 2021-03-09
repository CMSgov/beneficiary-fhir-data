package gov.cms.bfd.pipeline.ccw.rif.extract.s3.task;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.AwsFailureException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.S3Utilities;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests downloaded S3 file attributes such as MD5ChkSum */
public final class ManifestEntryDownloadTaskIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

  private S3TaskManager s3TaskManager;

  public ManifestEntryDownloadTaskIT() {
    this.s3TaskManager = null;
  }

  /**
   * Test to ensure the MD5ChkSum of the downloaded S3 file matches the generated MD5ChkSum value
   */
  @SuppressWarnings("deprecation")
  @Test
  public void testMD5ChkSum() throws Exception {
    AmazonS3 s3Client = S3Utilities.createS3Client(new ExtractionOptions("foo"));
    Bucket bucket = null;
    try {
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
              new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY));

      // upload beneficiary sample file to S3 bucket created above
      s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
      s3Client.putObject(
          DataSetTestUtilities.createPutRequest(
              bucket,
              manifest,
              manifest.getEntries().get(0),
              StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));

      // download file from S3 that was just uploaded above
      GetObjectRequest objectRequest =
          new GetObjectRequest(
              bucket.getName(),
              String.format(
                  "%s/%s/%s",
                  CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
                  manifest.getEntries().get(0).getParentManifest().getTimestampText(),
                  manifest.getEntries().get(0).getName()));
      Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
      s3TaskManager =
          new S3TaskManager(new MetricRegistry(), new ExtractionOptions(options.getS3BucketName()));
      LOGGER.info(
          "Downloading '{}' to '{}'...",
          objectRequest.getKey(),
          localTempFile.toAbsolutePath().toString());
      Download downloadHandle =
          s3TaskManager.getS3TransferManager().download(objectRequest, localTempFile.toFile());
      downloadHandle.waitForCompletion();

      InputStream downloadedInputStream = new FileInputStream(localTempFile.toString());
      String generatedMD5ChkSum = ManifestEntryDownloadTask.computeMD5ChkSum(downloadedInputStream);
      LOGGER.info("The generated MD5 value from Java (Base64 encoded) is:" + generatedMD5ChkSum);

      String downloadedFileMD5ChkSum =
          downloadHandle.getObjectMetadata().getUserMetaDataOf("md5chksum");
      LOGGER.info("The MD5 value from AWS S3 file's metadata is: " + downloadedFileMD5ChkSum);
      Assert.assertEquals(
          "Checksum doesn't match on downloaded file " + objectRequest.getKey(),
          downloadedFileMD5ChkSum,
          generatedMD5ChkSum);
      LOGGER.info(
          "Downloaded '{}' to '{}'.",
          objectRequest.getKey(),
          localTempFile.toAbsolutePath().toString());

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (AmazonClientException e) {
      throw new AwsFailureException(e);
    } catch (InterruptedException e) {
      // Shouldn't happen, as our apps don't use thread interrupts.
      throw new BadCodeMonkeyException(e);
    } finally {
      if (bucket != null) DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
    }
  }
}
