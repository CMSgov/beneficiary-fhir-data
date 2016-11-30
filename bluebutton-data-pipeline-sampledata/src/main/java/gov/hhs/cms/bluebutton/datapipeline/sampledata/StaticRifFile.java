package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.justdavis.karl.misc.exceptions.unchecked.UncheckedIoException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * This {@link RifFile} implementation operates on local files.
 */
final class StaticRifFile implements RifFile {
	private final StaticRifResource staticRifResource;

	/**
	 * Constructs a new {@link StaticRifFile}.
	 * 
	 * @param staticRifResource
	 *            the {@link StaticRifResource} that this {@link StaticRifFile}
	 *            will be based on
	 */
	public StaticRifFile(StaticRifResource staticRifResource) {
		this.staticRifResource = staticRifResource;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getFileType()
	 */
	@Override
	public RifFileType getFileType() {
		return staticRifResource.getRifFileType();
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return staticRifResource.name();
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
			return staticRifResource.getResourceUrl().openStream();
		} catch (IOException e) {
			throw new UncheckedIoException(e);
		}
	}
}
