package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.codahale.metrics.MetricRegistry;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.AwsFailureException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorWorker;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetTestUtilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.S3Utilities;


/**
 * Tests downloaded S3 file attributes such as MD5ChkSum
 */
public final class ManifestEntryDownloadTaskIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

	private S3TaskManager s3TaskManager;

	public ManifestEntryDownloadTaskIT() {
		this.s3TaskManager = null;

	}

	/**
	 * Test to ensure the MD5ChkSum of the downloaded S3 file matches the generated
	 * MD5ChkSum value
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testMD5ChkSum()
			throws Exception {
		ExtractionOptions options = new ExtractionOptions(String.format("bb-test-%d", new Random().nextInt(1000)));
		AmazonS3 s3Client = S3Utilities.createS3Client(options);
		Bucket bucket = null;
		try {
			bucket = s3Client.createBucket(options.getS3BucketName());
			LOGGER.info("Bucket created: '{}:{}'", s3Client.getS3AccountOwner().getDisplayName(), bucket.getName());
			DataSetManifest manifest = new DataSetManifest(Instant.now(), 0,
					new DataSetManifestEntry("beneficiaries.rif", RifFileType.BENEFICIARY));

			// upload beneficiary sample file to S3 bucket created above
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest));
			s3Client.putObject(DataSetTestUtilities.createPutRequest(bucket, manifest, manifest.getEntries().get(0),
					StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));

			// download file from S3 that was just uploaded above
			GetObjectRequest objectRequest = new GetObjectRequest(bucket.getName(),
					String.format("%s/%s/%s", DataSetMonitorWorker.S3_PREFIX_PENDING_DATA_SETS,
							manifest.getEntries().get(0).getParentManifest().getTimestampText(),
							manifest.getEntries().get(0).getName()));
			Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
			s3TaskManager = new S3TaskManager(new MetricRegistry(), new ExtractionOptions(options.getS3BucketName()));
			LOGGER.info("Downloading '{}' to '{}'...", objectRequest.getKey(),
					localTempFile.toAbsolutePath().toString());
			Download downloadHandle = s3TaskManager.getS3TransferManager().download(objectRequest,
							localTempFile.toFile());
			downloadHandle.waitForCompletion();

			InputStream downloadedInputStream = new FileInputStream(localTempFile.toString());
			String generatedMD5ChkSum = ManifestEntryDownloadTask.computeMD5ChkSum(downloadedInputStream);
			LOGGER.info("The generated MD5 value from Java (Base64 encoded) is:" + generatedMD5ChkSum);

			String downloadedFileMD5ChkSum = downloadHandle.getObjectMetadata().getUserMetaDataOf("md5chksum");
			LOGGER.info("The MD5 value from AWS S3 file's metadata is: " + downloadedFileMD5ChkSum);
			Assert.assertEquals("Checksum doesn't match on downloaded file " + objectRequest.getKey(),
					downloadedFileMD5ChkSum, generatedMD5ChkSum);
			LOGGER.info("Downloaded '{}' to '{}'.", objectRequest.getKey(), localTempFile.toAbsolutePath().toString());

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (AmazonClientException e) {
			throw new AwsFailureException(e);
		} catch (InterruptedException e) {
			// Shouldn't happen, as our apps don't use thread interrupts.
			throw new BadCodeMonkeyException(e);
		}
		finally {
			if (bucket != null)
				DataSetTestUtilities.deleteObjectsAndBucket(s3Client, bucket);
		}
	}



}
