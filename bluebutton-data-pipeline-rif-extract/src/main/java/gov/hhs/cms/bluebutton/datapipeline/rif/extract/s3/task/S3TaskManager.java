package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.task;

import com.amazonaws.services.s3.AmazonS3;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.S3Utilities;

/**
 * Handles the execution and management of S3-related tasks.
 */
public final class S3TaskManager {
	private final AmazonS3 s3Client;

	/**
	 * Constructs a new {@link S3TaskManager}.
	 * 
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 */
	public S3TaskManager(ExtractionOptions options) {
		this.s3Client = S3Utilities.createS3Client(options);
	}

	/**
	 * @return the {@link AmazonS3} client being used by this
	 *         {@link S3TaskManager}
	 */
	public AmazonS3 getS3Client() {
		return s3Client;
	}

	/**
	 * TODO
	 */
	public void shutdownSafely() {
		// TODO Auto-generated method stub

	}

	/**
	 * TODO
	 * 
	 * @param downloadManifestFilesTask
	 */
	public void addTask(DownloadManifestFilesTask downloadManifestFilesTask) {
		// TODO Auto-generated method stub
	}
}
