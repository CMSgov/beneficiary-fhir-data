package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.math.BigDecimal;
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
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup.CarrierClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode.IcdVersion;
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
		Assert.assertEquals("89", pdeRow.partDEventId);
		Assert.assertEquals("103", pdeRow.beneficiaryId);
		Assert.assertEquals(LocalDate.of(2015, Month.MAY, 12), pdeRow.prescriptionFillDate);
		Assert.assertEquals(LocalDate.of(2015, Month.MAY, 27), pdeRow.paymentDate.get());
		Assert.assertEquals("01", pdeRow.serviceProviderIdQualiferCode);
		Assert.assertEquals("1124137542", pdeRow.serviceProviderId);
		Assert.assertEquals("01", pdeRow.prescriberIdQualifierCode);
		Assert.assertEquals("1225061591", pdeRow.prescriberId);
		Assert.assertEquals(new Long(791569), pdeRow.prescriptionReferenceNumber);
		Assert.assertEquals("49884009902", pdeRow.nationalDrugCode);
		Assert.assertEquals("H8552", pdeRow.planContractId);
		Assert.assertEquals("020", pdeRow.planBenefitPackageId);
		Assert.assertEquals(new Integer(1), pdeRow.compoundCode);
		Assert.assertEquals("0", pdeRow.dispenseAsWrittenProductSelectionCode);
		Assert.assertEquals(new BigDecimal("60"), pdeRow.quantityDispensed);
		Assert.assertEquals(new Integer(30), pdeRow.daysSupply);
		Assert.assertEquals(new Integer(3), pdeRow.fillNumber);
		Assert.assertEquals(new Character('P'), pdeRow.dispensingStatuscode.get());
		Assert.assertEquals(new Character('C'), pdeRow.drugCoverageStatusCode);
		Assert.assertEquals(new Character('A'), pdeRow.adjustmentDeletionCode.get());
		Assert.assertEquals(new Character('X'), pdeRow.nonstandardFormatCode.get());
		Assert.assertEquals(new Character('M'), pdeRow.pricingExceptionCode.get());
		Assert.assertEquals(new Character('C'), pdeRow.catastrophicCoverageCode.get());
		Assert.assertEquals(new BigDecimal("362.84"), pdeRow.grossCostBelowOutOfPocketThreshold);
		Assert.assertEquals(new BigDecimal("15.25"), pdeRow.grossCostAboveOutOfPocketThreshold);
		Assert.assertEquals(new BigDecimal("235.85"), pdeRow.patientPaidAmount);
		Assert.assertEquals(new BigDecimal("17.30"), pdeRow.otherTrueOutOfPocketPaidAmount);
		Assert.assertEquals(new BigDecimal("122.23"), pdeRow.lowIncomeSubsidyPaidAmount);
		Assert.assertEquals(new BigDecimal("42.42"), pdeRow.patientLiabilityReductionOtherPaidAmount);
		Assert.assertEquals(new BigDecimal("126.99"), pdeRow.partDPlanCoveredPaidAmount);
		Assert.assertEquals(new BigDecimal("17.98"), pdeRow.partDPlanNonCoveredPaidAmount);
		Assert.assertEquals(new BigDecimal("362.84"), pdeRow.totalPrescriptionCost);
		Assert.assertEquals(new Character('3'), pdeRow.prescriptionOriginationCode.get());
		Assert.assertEquals(new BigDecimal("317.22"), pdeRow.gapDiscountAmount);
		/*
		 * TODO Re-enable this test case once it is determined if this field is
		 * optional or not.
		 */
		// Assert.assertEquals(new Character('G'), pdeRow.brandGenericCode);
		Assert.assertEquals("01", pdeRow.pharmacyTypeCode);
		Assert.assertEquals("02", pdeRow.patientResidenceCode);
		Assert.assertEquals("08", pdeRow.submissionClarificationCode.get());
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

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#CARRIER_1}.
	 */
	@Test
	public void process1CarrierClaimRecord() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.CARRIER_1);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.CARRIER_1.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.CARRIER_1.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof CarrierClaimGroup);

		// Verify the claim header.
		CarrierClaimGroup claimGroup = (CarrierClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(1, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("91", claimGroup.beneficiaryId);
		Assert.assertEquals("1831831620", claimGroup.claimId);
		Assert.assertEquals("1831831620", claimGroup.claimId);
		Assert.assertEquals(LocalDate.of(2015, 10, 27), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2015, 10, 27), claimGroup.dateThrough);
		Assert.assertEquals("1902880057", claimGroup.referringPhysicianNpi);
		Assert.assertEquals(new BigDecimal("130.32"), claimGroup.providerPaymentAmount);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H40013"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(4, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H26493"), claimGroup.diagnosesAdditional.get(2));
		Assert.assertEquals(7, claimGroup.lines.size());
		// TODO test the rest of the columns once they're all ready

		// Verify one of the claim lines.
		CarrierClaimLine claimLine = claimGroup.lines.get(5);
		Assert.assertEquals(1, claimLine.number);
		Assert.assertFalse(claimLine.organizationNpi.isPresent());
		Assert.assertEquals("1", claimLine.cmsServiceTypeCode);
		Assert.assertEquals("92012", claimLine.hcpcsCode);
		Assert.assertEquals(new BigDecimal("70.79"), claimLine.providerPaymentAmount);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H40013"), claimLine.diagnosis);
		// TODO test the rest of the columns once they're all ready
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#CARRIER_1091}.
	 */
	@Test
	public void process1091CarrierClaimRecords() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.CARRIER_1091);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.CARRIER_1091.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.CARRIER_1091.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}
}
