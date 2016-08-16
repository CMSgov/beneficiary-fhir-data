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
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup.CarrierClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CompoundCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DrugCoverageStatus;
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
	 * {@link StaticRifResource#SAMPLE_A_BENES}.
	 */
	@Test
	public void process1BeneRecord() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.SAMPLE_A_BENES);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof BeneficiaryRow);

		BeneficiaryRow beneRow = (BeneficiaryRow) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, beneRow.version);
		Assert.assertEquals(RecordAction.INSERT, beneRow.recordAction);
		Assert.assertEquals("1", beneRow.beneficiaryId);
		Assert.assertEquals("CT", beneRow.stateCode);
		Assert.assertEquals("LITCHFIELD", beneRow.countyCode);
		Assert.assertEquals("060981009", beneRow.postalCode);
		Assert.assertEquals(LocalDate.of(1959, Month.MARCH, 17), beneRow.birthDate);
		Assert.assertEquals(('M'), beneRow.sex);
		Assert.assertEquals(('1'), beneRow.race);
		Assert.assertEquals(new Character('1'), beneRow.entitlementCodeOriginal.get());
		Assert.assertEquals(new Character('1'), beneRow.entitlementCodeCurrent.get());
		Assert.assertEquals(new Character('N'), beneRow.endStageRenalDiseaseCode.get());
		Assert.assertEquals(new String("20"), beneRow.medicareEnrollmentStatusCode.get());
		Assert.assertEquals(new Character('0'), beneRow.partBTerminationCode.get());
		Assert.assertEquals(new Character('0'), beneRow.partBTerminationCode.get());
		Assert.assertEquals("314747066U", beneRow.hicn);
		Assert.assertEquals("Hyxswp", beneRow.nameSurname);
		Assert.assertEquals("Axom", beneRow.nameGiven);
		Assert.assertEquals(new Character('A'), beneRow.nameMiddleInitial.get());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_BENES}.
	 */
	@Test
	public void process1000BeneRecords() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.SAMPLE_B_BENES);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_BENES.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_BENES.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_PDE}.
	 */
	@Test
	public void process1PDERecord() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.SAMPLE_A_PDE);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_PDE.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_PDE.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof PartDEventRow);

		PartDEventRow pdeRow = (PartDEventRow) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, pdeRow.version);
		Assert.assertEquals(RecordAction.INSERT, pdeRow.recordAction);
		Assert.assertEquals("89", pdeRow.partDEventId);
		Assert.assertEquals("1", pdeRow.beneficiaryId);
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
		Assert.assertEquals(CompoundCode.NOT_COMPOUNDED, pdeRow.compoundCode);
		Assert.assertEquals("0", pdeRow.dispenseAsWrittenProductSelectionCode);
		Assert.assertEquals(new BigDecimal("60"), pdeRow.quantityDispensed);
		Assert.assertEquals(new Integer(30), pdeRow.daysSupply);
		Assert.assertEquals(new Integer(3), pdeRow.fillNumber);
		Assert.assertEquals(new Character('P'), pdeRow.dispensingStatuscode.get());
		Assert.assertEquals(DrugCoverageStatus.COVERED, pdeRow.drugCoverageStatusCode);
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
	 * {@link StaticRifResource#SAMPLE_B_PDE}.
	 */
	@Test
	public void process1195PDERecords() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.SAMPLE_B_PDE);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_PDE.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_PDE.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_CARRIER}.
	 */
	@Test
	public void process1CarrierClaimRecord() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.SAMPLE_A_CARRIER);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRifFileType(),
				rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof InpatientClaimGroup);

		// Verify the claim header.
		InpatientClaimGroup claimGroup = (InpatientClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("1", claimGroup.beneficiaryId);
		Assert.assertEquals("1831831620", claimGroup.claimId);
		Assert.assertEquals(new Character('O'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("71", claimGroup.claimTypeCode);
		Assert.assertEquals(LocalDate.of(2015, 10, 27), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2015, 10, 27), claimGroup.dateThrough);
		Assert.assertEquals(LocalDate.of(2015, 11, 6), claimGroup.weeklyProcessDate);
		Assert.assertEquals(new Character('1'), claimGroup.claimEntryCode);
		Assert.assertEquals("01", claimGroup.claimDispositionCode);
		Assert.assertEquals("06102", claimGroup.carrierNumber);
		Assert.assertEquals("1", claimGroup.paymentDenialCode);
		Assert.assertEquals(new BigDecimal("130.32"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals("U91100", claimGroup.referringPhysicianUpin);
		Assert.assertEquals("1902880057", claimGroup.referringPhysicianNpi);
		Assert.assertEquals(new Character('A'), claimGroup.providerAssignmentIndicator);
		Assert.assertEquals(new BigDecimal("130.32"), claimGroup.providerPaymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.beneficiaryPaymentAmount);
		Assert.assertEquals(new BigDecimal("245.04"), claimGroup.submittedChargeAmount);
		Assert.assertEquals(new BigDecimal("166.23"), claimGroup.allowedChargeAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.beneficiaryPartBDeductAmount);
		Assert.assertEquals(new Character('5'), claimGroup.hcpcsYearCode);
		Assert.assertEquals("K25852", claimGroup.referringProviderIdNumber);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H40013"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(4, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H26493"), claimGroup.diagnosesAdditional.get(2));
		Assert.assertEquals(7, claimGroup.lines.size());

		// Verify one of the claim lines.
		CarrierClaimLine claimLine = claimGroup.lines.get(5);
		Assert.assertEquals("00000000", claimLine.clinicalTrialNumber);
		Assert.assertEquals(new Integer(1), claimLine.number);
		Assert.assertEquals("K25852", claimLine.performingProviderIdNumber);
		Assert.assertFalse(claimLine.performingPhysicianUpin.isPresent());
		Assert.assertEquals("1902880057", claimLine.performingPhysicianNpi);
		Assert.assertFalse(claimLine.organizationNpi.isPresent());
		Assert.assertEquals(new Character('0'), claimLine.providerTypeCode);
		Assert.assertEquals("204240126", claimLine.providerTaxNumber);
		Assert.assertEquals("IL", claimLine.providerStateCode);
		Assert.assertEquals("604428202", claimLine.providerZipCode);
		Assert.assertEquals("41", claimLine.providerSpecialityCode);
		Assert.assertEquals(new Character('1'), claimLine.providerParticipatingIndCode);
		Assert.assertEquals(new Character('0'), claimLine.reducedPaymentPhysicianAsstCode);
		Assert.assertEquals(new BigDecimal("1"), claimLine.serviceCount);
		Assert.assertEquals("1", claimLine.cmsServiceTypeCode);
		Assert.assertEquals("11", claimLine.placeOfServiceCode);
		Assert.assertEquals("15", claimLine.linePricingLocalityCode);
		Assert.assertEquals(LocalDate.of(2015, 10, 27), claimLine.firstExpenseDate);
		Assert.assertEquals(LocalDate.of(2015, 10, 27), claimLine.lastExpenseDate);
		Assert.assertEquals("92012", claimLine.hcpcsCode);
		Assert.assertFalse(claimLine.hcpcsInitialModifierCode.isPresent());
		Assert.assertFalse(claimLine.hcpcsSecondModifierCode.isPresent());
		Assert.assertEquals("M5C", claimLine.betosCode);
		Assert.assertEquals(new BigDecimal("70.79"), claimLine.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimLine.beneficiaryPaymentAmount);
		Assert.assertEquals(new BigDecimal("70.79"), claimLine.providerPaymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimLine.beneficiaryPartBDeductAmount);
		Assert.assertFalse(claimLine.primaryPayerCode.isPresent());
		Assert.assertEquals(new BigDecimal("0"), claimLine.primaryPayerPaidAmount);
		Assert.assertEquals(new BigDecimal("18.06"), claimLine.coinsuranceAmount);
		Assert.assertEquals(new BigDecimal("110"), claimLine.submittedChargeAmount);
		Assert.assertEquals(new BigDecimal("90.29"), claimLine.allowedChargeAmount);
		Assert.assertEquals("A", claimLine.processingIndicatorCode);
		Assert.assertEquals(new Character('0'), claimLine.paymentCode);
		Assert.assertEquals(new Character('0'), claimLine.serviceDeductibleCode);
		Assert.assertEquals(new BigDecimal("1"), claimLine.mtusCount);
		Assert.assertEquals(new Character('3'), claimLine.mtusCode);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H40013"), claimLine.diagnosis);
		Assert.assertFalse(claimLine.hpsaScarcityCode.isPresent());
		Assert.assertFalse(claimLine.rxNumber.isPresent());
		Assert.assertEquals(new BigDecimal("0"), claimLine.hctHgbTestResult);
		Assert.assertFalse(claimLine.hctHgbTestTypeCode.isPresent());
		Assert.assertFalse(claimLine.nationalDrugCode.isPresent());
		Assert.assertFalse(claimLine.cliaLabNumber.isPresent());
		Assert.assertEquals(new Integer(0), claimLine.anesthesiaUnitCount);
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_CARRIER}.
	 */
	@Test
	public void process1091CarrierClaimRecords() {
		StaticRifGenerator generator = new StaticRifGenerator(StaticRifResource.SAMPLE_B_CARRIER);
		Stream<RifFile> rifFiles = generator.generate();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFiles.collect(Collectors.toSet()));

		RifFilesProcessor processor = new RifFilesProcessor();
		Stream<RifRecordEvent<?>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_CARRIER.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_CARRIER.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}
}
