package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.RifFile;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;

/**
 * This {@link RifFile} implementation can be used for files that are backed by
 * {@link S3Object}s. Note that this lazy-loads the files, to ensure that
 * connections are not opened until needed.
 */
public final class S3RifFile implements RifFile {
	private final RifFileType fileType;
	private final String displayName;
	private final Path localTempFile;

	/**
	 * Constructs a new {@link S3RifFile} instance.
	 * 
	 * @param s3Client
	 *            the {@link AmazonS3} client to use to get the contents of the
	 *            object that backs this {@link S3RifFile}
	 * @param fileType
	 *            the {@link RifFileType} of the object that backs this
	 *            {@link S3RifFile}
	 * @param objectRequest
	 *            an S3 {@link GetObjectRequest} that will retrieve the object
	 *            represented by this {@link S3RifFile}
	 */
	public S3RifFile(AmazonS3 s3Client, RifFileType fileType, GetObjectRequest objectRequest) {
		this.fileType = fileType;
		this.displayName = String.format("%s:%s", objectRequest.getBucketName(), objectRequest.getKey());

		TransferManager s3TransferManager = null;
		try {
			s3TransferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
			Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
			Download downloadHandle = s3TransferManager.download(objectRequest, localTempFile.toFile());
			downloadHandle.waitForCompletion();
			s3TransferManager.shutdownNow(false);

			this.localTempFile = localTempFile;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (AmazonClientException e) {
			// FIXME better exception type
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			// Shouldn't happen, as our apps don't use thread interrupts.
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @see gov.hhs.cms.bluebutton.data.model.rif.RifFile#getFileType()
	 */
	@Override
	public RifFileType getFileType() {
		return fileType;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.data.model.rif.RifFile#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.data.model.rif.RifFile#getCharset()
	 */
	@Override
	public Charset getCharset() {
		return StandardCharsets.UTF_8;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.data.model.rif.RifFile#open()
	 */
	@Override
	public InputStream open() {
		try {
			return new BufferedInputStream(new FileInputStream(localTempFile.toFile()));
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Removes the local temporary file that was used to cache this
	 * {@link S3RifFile}'s corresponding S3 object data locally.
	 */
	public void cleanupTempFile() {
		try {
			Files.delete(localTempFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("S3RifFile [fileType=");
		builder.append(fileType);
		builder.append(", displayName=");
		builder.append(displayName);
		builder.append(", localTempFile=");
		builder.append(localTempFile);
		builder.append("]");
		return builder.toString();
	}
}
