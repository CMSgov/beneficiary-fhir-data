package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Represents a RIF file that can be read and deleted.
 */
public interface RifFile {
	/**
	 * @return a name that can be used in logs and such to identify and help
	 *         debug this {@link RifFile}
	 */
	String getDisplayName();

	/**
	 * @return the {@link RifFileType} for this {@link RifFile}, which
	 *         identifies its format/contents
	 */
	RifFileType getFileType();

	/**
	 * @return the {@link Charset} that the data in {@link #open()} is encoded
	 *         in
	 */
	Charset getCharset();

	/**
	 * @return a new {@link InputStream} to the RIF file's contents
	 */
	InputStream open();
}
