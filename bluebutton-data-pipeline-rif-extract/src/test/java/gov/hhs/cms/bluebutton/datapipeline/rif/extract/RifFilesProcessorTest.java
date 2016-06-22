package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifGenerator;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifResource;

/**
 * Unit tests for {@link RifFilesProcessor}.
 */
public final class RifFilesProcessorTest {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource}.
	 */
	@Test
	public void processBeneRecord() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.BENE_1);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.BENE_1.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.BENE_1.getRifFileType(), rifEventsList.get(0).getFile().getFileType());
	}
}
