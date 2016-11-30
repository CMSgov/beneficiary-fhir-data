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
import java.nio.file.StandardCopyOption;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

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

		try (InputStream s3ObjectStream = s3Client.getObject(objectRequest).getObjectContent();) {
			Path localTempFile = Files.createTempFile("data-pipeline-s3-temp", ".rif");
			Files.copy(s3ObjectStream, localTempFile, StandardCopyOption.REPLACE_EXISTING);

			this.localTempFile = localTempFile;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getFileType()
	 */
	@Override
	public RifFileType getFileType() {
		return fileType;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getCharset()
	 */
	@Override
	public Charset getCharset() {
		return StandardCharsets.UTF_8;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#open()
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
	void cleanupTempFile() {
		try {
			Files.delete(localTempFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
