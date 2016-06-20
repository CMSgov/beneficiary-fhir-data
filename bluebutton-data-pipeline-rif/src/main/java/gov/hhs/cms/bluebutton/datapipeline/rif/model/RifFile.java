package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;

/**
 * Represents a RIF file that can be read and deleted.
 */
public interface RifFile {
	/**
	 * @return the file name or object key that uniquely identifies this
	 *         {@link RifFile} within its directory or S3 bucket
	 */
	String getKey();

	/**
	 * @return the {@link RifFileType} for this {@link RifFile}, which
	 *         identifies its format/contents
	 */
	default RifFileType getFileType() {
		return RifFileType.selectTypeForFilename(getKey());
	}

	/**
	 * @return a name that can be used in logs and such to identify and help
	 *         debug this {@link RifFile}
	 */
	String getDisplayName();

	/**
	 * @return an {@link Instant} representing when this {@link RifFile}'s
	 *         contents were last modified (or created)
	 */
	Instant getLastModifiedTimestamp();

	/**
	 * @return the {@link Charset} that the data in {@link #open()} is encoded
	 *         in
	 */
	Charset getCharset();

	/**
	 * @return a new {@link InputStream} to the RIF file's contents
	 */
	InputStream open();

	/**
	 * Deletes the object/file/whatever represented by this {@link RifFile},
	 * freeing its storage.
	 */
	void delete();
}
