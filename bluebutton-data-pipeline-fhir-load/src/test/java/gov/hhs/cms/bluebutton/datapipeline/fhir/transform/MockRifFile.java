package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;

/**
 * A mock {@link RifFile} implementation.
 */
public final class MockRifFile implements RifFile {
	private final Instant timestamp = Instant.now();

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getKey()
	 */
	@Override
	public String getKey() {
		return "foo";
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return getKey();
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getLastModifiedTimestamp()
	 */
	@Override
	public Instant getLastModifiedTimestamp() {
		return timestamp;
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
		throw new UnsupportedOperationException();
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#delete()
	 */
	@Override
	public void delete() {
		throw new UnsupportedOperationException();
	}
}
