package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.AwsFailureException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions.ChecksumException;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;

/**
 * Represents an asynchronous operation to download the contents of a specific
 * {@link DataSetManifestEntry} from S3.
 */
public final class ManifestEntryDownloadTaskIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManifestEntryDownloadTask.class);

	private final S3TaskManager s3TaskManager;
	private final MetricRegistry appMetrics;
	private final ExtractionOptions options;
	private final DataSetManifestEntry manifestEntry;

	/**
	 * Constructs a new {@link ManifestEntryDownloadTask}.
	 * 
	 * @param s3TaskManager
	 *            the {@link S3TaskManager} to use
	 * @param appMetrics
	 *            the {@link MetricRegistry} for the overall application
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param manifestEntry
	 *            the {@link DataSetManifestEntry} to download the file for
	 */
	/*
	 * public ManifestEntryDownloadTaskIT(S3TaskManager s3TaskManager,
	 * MetricRegistry appMetrics, ExtractionOptions options, DataSetManifestEntry
	 * manifestEntry) { this.s3TaskManager = s3TaskManager; this.appMetrics =
	 * appMetrics; this.options = options; this.manifestEntry = manifestEntry; }
	 */

	public ManifestEntryDownloadTaskIT() {
		this.s3TaskManager = null;
		this.appMetrics = null;
		this.options = null;
		this.manifestEntry = null;
	}

	@Test
	public void testMD5()
			throws Exception {
		try {
			GetObjectRequest objectRequest = new GetObjectRequest("pdcws301.etl",
					String.format("%s/%s/%s", "Sample", "2017-10-21T11:40:29Z", "manifest_manifest.xml"));
			Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
			 S3TaskManager s3TaskManagerDEH = new S3TaskManager(new
			 ExtractionOptions("pdcws301.etl"));
			 LOGGER.info("Downloading '{}' to '{}'...", manifestEntry,
			 localTempFile.toAbsolutePath().toString());
			 Download downloadHandle =
			 s3TaskManagerDEH.getS3TransferManager().download(objectRequest,
							localTempFile.toFile());

			/*
			 * AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
			 * S3Object object = s3Client.getObject(new GetObjectRequest("pdcws301.etl",
			 * String.format("%s/%s/%s", "Sample", "2017-10-21T11:40:29Z",
			 * "manifest_manifest.xml"))); InputStream objectData =
			 * object.getObjectContent(); // Process the objectData stream. String result =
			 * IOUtils.toString(objectData); objectData.close();
			 */
			// deh-start
			LOGGER.info("localTempFile is  " + localTempFile.toString());

			String generatedMD5Hash = getMD5Hash(localTempFile);
			LOGGER.info("deh-The generated from java MD5 (hexadecimal encoded) hash is:" + generatedMD5Hash);

			String downloadedFileMD5Value = downloadHandle.getObjectMetadata().getUserMetaDataOf("md5chksum");
			LOGGER.info("deh-ObjectMetadata MD5 value: " + downloadedFileMD5Value);
			LOGGER.info("md5 from S3-Base64 decoded is " + Base64.getDecoder().decode(downloadedFileMD5Value));

			byte[] downloadedFileMD5Value2 = downloadHandle.getObjectMetadata().getUserMetaDataOf("md5chksum")
					.getBytes();
			LOGGER.info("md5 from S3-Base64 decoded is " + Base64.getDecoder().decode(downloadedFileMD5Value2));
			LOGGER.info("md5 from S3-Base64 encoded is " + Base64.getEncoder().encodeToString(downloadedFileMD5Value2));
			LOGGER.info("md5 from S3-: hash.toString " + downloadedFileMD5Value2.toString());

			String downloadedFileETagValue = downloadHandle.getObjectMetadata().getETag();
			LOGGER.info("deh--ObjectMetadata ETag value: " + downloadedFileETagValue);
			LOGGER.info("md5 from S3-etag-Base64 decoded is " + Base64.getDecoder().decode(downloadedFileETagValue));

			if (!generatedMD5Hash.equalsIgnoreCase(downloadedFileETagValue))
				throw new ChecksumException("Checksum doesn't match on downloaded file " + localTempFile);
			// deh-end
			// downloadHandle.waitForCompletion();
			LOGGER.info("Downloaded '{}' to '{}'.", manifestEntry, localTempFile.toAbsolutePath().toString());

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (AmazonClientException e) {
			throw new AwsFailureException(e);
			// } catch (InterruptedException e) {
			// Shouldn't happen, as our apps don't use thread interrupts.
			// throw new BadCodeMonkeyException(e);
		}
	}



	/*
	 * public static void main(String[] args) { Scanner sn = new Scanner(System.in);
	 * System.out.print("Please enter data for which MD5 is required:"); String data
	 * = sn.nextLine();
	 * 
	 * MD5HashGenerator sj = new MD5HashGenerator(); String hash =
	 * sj.getMD5Hash(data);
	 * System.out.println("The MD5 (hexadecimal encoded) hash is:"+hash); }
	 */
	/**
	 * Returns a hexadecimal encoded MD5 hash for the input String.
	 * 
	 * @param data
	 * @return
	 */
	private String getMD5Hash(Path localTempFile) {
		String result = null;
		try {
			// byte[] bytes = localTempFile.toFile().toString().getBytes("UTF-8");
			byte[] bytes = Files.readAllBytes(localTempFile);
			byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
			LOGGER.info("deh-: hash value after digest command " + hash.toString());

			return bytesToHex(hash); // make it printable
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Use javax.xml.bind.DatatypeConverter class in JDK to convert byte array to a
	 * hexadecimal string. Note that this generates hexadecimal in upper case.
	 * 
	 * @param hash
	 * @return
	 */
	private String bytesToHex(byte[] hash) {
		LOGGER.info("Base64 encoded is " + Base64.getEncoder().encodeToString(hash));
		LOGGER.info("Base64 encoded is " + Base64.getEncoder().encode(hash));
		// LOGGER.info("Base64 decoded is " + Base64.decode(hash.toString()));

		LOGGER.info("deh-: printBase64Binary " + DatatypeConverter.printBase64Binary(hash));
		LOGGER.info("deh-: parseBase64Binary " + DatatypeConverter.parseBase64Binary(hash.toString()));
		// LOGGER.info("deh-: printString " +
		// DatatypeConverter.printString(hash.toString()));

		LOGGER.info("deh-: printHexBinary " + DatatypeConverter.printHexBinary(hash).toLowerCase());
		return DatatypeConverter.printHexBinary(hash).toLowerCase();
	}
}
