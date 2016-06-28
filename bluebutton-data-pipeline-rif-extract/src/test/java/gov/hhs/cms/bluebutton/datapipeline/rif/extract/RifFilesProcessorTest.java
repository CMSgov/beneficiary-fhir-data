package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
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
	 * {@link StaticRifResource#BENES_1}.
	 */
	@Test
	public void process1BeneRecord() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.BENES_1);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.BENES_1.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.BENES_1.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof BeneficiaryRow);

		BeneficiaryRow beneRow = (BeneficiaryRow) rifRecordEvent.getRecord();
		Assert.assertEquals(1, beneRow.version);
		Assert.assertEquals(RecordAction.INSERT, beneRow.recordAction);
		Assert.assertEquals("1", beneRow.beneficiaryId);
		Assert.assertEquals("CT", beneRow.stateCode);
		Assert.assertEquals("LITCHFIELD", beneRow.countyCode);
		Assert.assertEquals("060981009", beneRow.postalCode);
		// TODO test the rest of the columns once they're all ready
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#BENES_1000}.
	 */
	@Test
	public void process1000BeneRecords() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.BENES_1000);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.BENES_1000.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.BENES_1000.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#PDE_1}.
	 */
	@Test
	public void process1PDERecord() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.PDE_1);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.PDE_1.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.PDE_1.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof PartDEventRow);

		PartDEventRow pdeRow = (PartDEventRow) rifRecordEvent.getRecord();
		Assert.assertEquals(1, pdeRow.version);
		Assert.assertEquals(RecordAction.INSERT, pdeRow.recordAction);
		Assert.assertEquals("96", pdeRow.partDEventId);
		Assert.assertEquals("63", pdeRow.beneficiaryId);
		Assert.assertEquals(LocalDate.of(2015, Month.MAY, 6), pdeRow.prescriptionFillDate);
		Assert.assertFalse(pdeRow.paymentDate.isPresent());

	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#PDE_1195}.
	 */
	@Test
	public void process1195PDERecords() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.PDE_1195);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.PDE_1195.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.PDE_1195.getRifFileType(), rifEventsList.get(0).getFile().getFileType());
	}
}
