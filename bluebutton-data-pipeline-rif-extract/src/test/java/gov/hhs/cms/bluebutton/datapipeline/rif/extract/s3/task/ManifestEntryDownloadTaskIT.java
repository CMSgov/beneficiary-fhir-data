package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.AwsFailureException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.ChecksumException;

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
	@Test
	public void testMD5ChkSum()
			throws Exception {
		try {
			GetObjectRequest objectRequest = new GetObjectRequest("pdcws301.etl",
					String.format("%s/%s/%s", "Sample", "2017-10-21T11:48:29Z", "manifest_manifest.xml"));
			Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
			s3TaskManager = new S3TaskManager(new ExtractionOptions("pdcws301.etl"));
			LOGGER.info("Downloading '{}' to '{}'...", objectRequest.getKey(),
					localTempFile.toAbsolutePath().toString());
			Download downloadHandle = s3TaskManager.getS3TransferManager().download(objectRequest,
							localTempFile.toFile());
			downloadHandle.getProgress();
			downloadHandle.waitForCompletion();

			LOGGER.info("localTempFile is  " + localTempFile.toString());
			ManifestEntryDownloadTask manifestEntryDownloadTask = new ManifestEntryDownloadTask(s3TaskManager, null,
					null, null);
			String generatedMD5ChkSum = manifestEntryDownloadTask.
					getMD5ChkSum(localTempFile);
			LOGGER.info("The generated MD5 value from Java (Base64 encoded) is:" + generatedMD5ChkSum);

			String downloadedFileMD5ChkSum = downloadHandle.getObjectMetadata().getUserMetaDataOf("md5chksum");
			LOGGER.info("The MD5 value from AWS S3 file's metadata is: " + downloadedFileMD5ChkSum);

			if (!generatedMD5ChkSum.equals(downloadedFileMD5ChkSum))
				throw new ChecksumException("Checksum doesn't match on downloaded file " + objectRequest.getKey());

			LOGGER.info("Downloaded '{}' to '{}'.", objectRequest.getKey(), localTempFile.toAbsolutePath().toString());

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (AmazonClientException e) {
			throw new AwsFailureException(e);
		} catch (InterruptedException e) {
			// Shouldn't happen, as our apps don't use thread interrupts.
			throw new BadCodeMonkeyException(e);
		}
	}

}
