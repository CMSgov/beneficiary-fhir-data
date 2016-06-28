package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.util.stream.Stream;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;

/**
 * Generates RIF files with sample data.
 */
public interface RifGenerator {
	/**
	 * @return the {@link RifFile}s produced by this {@link RifGenerator}
	 */
	Stream<RifFile> generate();
}
