package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import com.justdavis.karl.misc.exceptions.unchecked.UncheckedIoException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;

/**
 * This {@link RifFile} implementation operates on local files.
 */
final class LocalRifFile implements RifFile {
	private final Path file;
	private final Charset charset;

	/**
	 * Constructs a new {@link LocalRifFile}.
	 * 
	 * @param file
	 *            the local file {@link Path} to be represented
	 * @param charset
	 *            the value to use for {@link #getCharset()}
	 */
	public LocalRifFile(Path file, Charset charset) {
		this.file = file;
		this.charset = charset;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getKey()
	 */
	@Override
	public String getKey() {
		return file.getFileName().toString();
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return file.toString();
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getLastModifiedTimestamp()
	 */
	@Override
	public Instant getLastModifiedTimestamp() {
		try {
			return Files.getLastModifiedTime(file).toInstant();
		} catch (IOException e) {
			throw new UncheckedIoException(e);
		}
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getCharset()
	 */
	@Override
	public Charset getCharset() {
		return charset;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#open()
	 */
	@Override
	public InputStream open() {
		try {
			return Files.newInputStream(file);
		} catch (IOException e) {
			throw new UncheckedIoException(e);
		}
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#delete()
	 */
	@Override
	public void delete() {
		try {
			Files.delete(file);
		} catch (IOException e) {
			throw new UncheckedIoException(e);
		}
	}
}
