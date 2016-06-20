package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifGenerator;

/**
 * Unit tests for {@link RifFilesProcessor}.
 */
public final class RifFilesProcessorTest {
	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle the files
	 * produced by {@link StaticRifGenerator}.
	 */
	@Test
	public void processStaticSamples() {
		StaticRifGenerator generator = new StaticRifGenerator();
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		processor.process(filesEvent);
	}
}
