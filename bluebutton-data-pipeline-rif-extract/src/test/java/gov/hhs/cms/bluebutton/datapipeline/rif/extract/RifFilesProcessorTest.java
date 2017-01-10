package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup.CarrierClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CompoundCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup.DMEClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DrugCoverageStatus;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup.HHAClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup.HospiceClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode.IcdVersion;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup.InpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup.SNFClaimLine;
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
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_BENES.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof BeneficiaryRow);

		BeneficiaryRow beneRow = (BeneficiaryRow) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, beneRow.version);
		Assert.assertEquals(RecordAction.INSERT, beneRow.recordAction);
		Assert.assertEquals("567834", beneRow.beneficiaryId);
		Assert.assertEquals("MO", beneRow.stateCode);
		Assert.assertEquals("Transylvania", beneRow.countyCode);
		Assert.assertEquals("12345", beneRow.postalCode);
		Assert.assertEquals(LocalDate.of(1981, Month.MARCH, 17), beneRow.birthDate);
		Assert.assertEquals(('M'), beneRow.sex);
		Assert.assertEquals(new Character('1'), beneRow.race.get());
		Assert.assertEquals(new Character('1'), beneRow.entitlementCodeOriginal.get());
		Assert.assertEquals(new Character('1'), beneRow.entitlementCodeCurrent.get());
		Assert.assertEquals(new Character('N'), beneRow.endStageRenalDiseaseCode.get());
		Assert.assertEquals(new String("20"), beneRow.medicareEnrollmentStatusCode.get());
		Assert.assertEquals(new Character('0'), beneRow.partBTerminationCode.get());
		Assert.assertEquals(new Character('0'), beneRow.partBTerminationCode.get());
		Assert.assertEquals("543217066U", beneRow.hicn);
		Assert.assertEquals("Doe", beneRow.nameSurname);
		Assert.assertEquals("John", beneRow.nameGiven);
		Assert.assertEquals(new Character('A'), beneRow.nameMiddleInitial.get());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_BENES}.
	 */
	@Test
	@Ignore
	public void processBeneRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_BENES.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_BENES.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_BENES.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_PDE}.
	 */
	@Test
	@Ignore
	public void process1PDERecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_PDE.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
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
		Assert.assertEquals(new Character('P'), pdeRow.dispensingStatusCode.get());
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
	@Ignore
	public void processPDERecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_PDE.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
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
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_CARRIER.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRifFileType(),
				rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof CarrierClaimGroup);

		// Verify the claim header.
		CarrierClaimGroup claimGroup = (CarrierClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("567834", claimGroup.beneficiaryId);
		Assert.assertEquals("9991831999", claimGroup.claimId);
		Assert.assertEquals(new Character('O'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("71", claimGroup.claimTypeCode);
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimGroup.dateThrough);
		Assert.assertEquals(LocalDate.of(1999, 11, 6), claimGroup.weeklyProcessDate);
		Assert.assertEquals(new Character('1'), claimGroup.claimEntryCode);
		Assert.assertEquals("1", claimGroup.claimDispositionCode);
		Assert.assertEquals("61026666", claimGroup.carrierNumber);
		Assert.assertEquals("1", claimGroup.paymentDenialCode);
		Assert.assertEquals(new BigDecimal("199.99"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals("1234534", claimGroup.referringPhysicianUpin);
		Assert.assertEquals("8765676", claimGroup.referringPhysicianNpi.get());
		Assert.assertEquals(new Character('A'), claimGroup.providerAssignmentIndicator);
		Assert.assertEquals(new BigDecimal("123.45"), claimGroup.providerPaymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.beneficiaryPaymentAmount);
		Assert.assertEquals(new BigDecimal("245.04"), claimGroup.submittedChargeAmount);
		Assert.assertEquals(new BigDecimal("166.23"), claimGroup.allowedChargeAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.beneficiaryPartBDeductAmount);
		Assert.assertEquals(new Character('5'), claimGroup.hcpcsYearCode);
		Assert.assertEquals("K25852", claimGroup.referringProviderIdNumber);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H33333"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(4, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H44444"), claimGroup.diagnosesAdditional.get(0));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H55555"), claimGroup.diagnosesAdditional.get(1));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H66666"), claimGroup.diagnosesAdditional.get(2));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H77777"), claimGroup.diagnosesAdditional.get(3));
		Assert.assertEquals(1, claimGroup.lines.size());
		Assert.assertEquals("0", claimGroup.clinicalTrialNumber.get());

		// Verify one of the claim lines.
		CarrierClaimLine claimLine = claimGroup.lines.get(0);
		Assert.assertEquals("0", claimLine.clinicalTrialNumber);
		Assert.assertEquals(new Integer(6), claimLine.number);
		Assert.assertEquals("K25555", claimLine.performingProviderIdNumber);
		Assert.assertFalse(claimLine.performingPhysicianUpin.isPresent());
		Assert.assertEquals("1923124", claimLine.performingPhysicianNpi.get());
		Assert.assertTrue(claimLine.organizationNpi.isPresent());
		Assert.assertEquals(new Character('0'), claimLine.providerTypeCode);
		Assert.assertEquals("204299999", claimLine.providerTaxNumber);
		Assert.assertEquals("IL", claimLine.providerStateCode.get());
		Assert.assertEquals("555558202", claimLine.providerZipCode.get());
		Assert.assertEquals("41", claimLine.providerSpecialityCode);
		Assert.assertEquals(new Character('1'), claimLine.providerParticipatingIndCode);
		Assert.assertEquals(new Character('0'), claimLine.reducedPaymentPhysicianAsstCode);
		Assert.assertEquals(new BigDecimal("1"), claimLine.serviceCount);
		Assert.assertEquals("1", claimLine.cmsServiceTypeCode);
		Assert.assertEquals("11", claimLine.placeOfServiceCode);
		Assert.assertEquals("15", claimLine.linePricingLocalityCode);
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimLine.firstExpenseDate);
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimLine.lastExpenseDate);
		Assert.assertEquals("92999", claimLine.hcpcsCode.get());
		Assert.assertTrue(claimLine.hcpcsInitialModifierCode.isPresent());
		Assert.assertFalse(claimLine.hcpcsSecondModifierCode.isPresent());
		Assert.assertEquals("T2D", claimLine.betosCode.get());
		Assert.assertEquals(new BigDecimal("37.5"), claimLine.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimLine.beneficiaryPaymentAmount);
		Assert.assertEquals(new BigDecimal("37.5"), claimLine.providerPaymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimLine.beneficiaryPartBDeductAmount);
		Assert.assertTrue(claimLine.primaryPayerCode.isPresent());
		Assert.assertEquals(new BigDecimal("0"), claimLine.primaryPayerPaidAmount);
		Assert.assertEquals(new BigDecimal("9.57"), claimLine.coinsuranceAmount);
		Assert.assertEquals(new BigDecimal("75"), claimLine.submittedChargeAmount);
		Assert.assertEquals(new BigDecimal("47.84"), claimLine.allowedChargeAmount);
		Assert.assertEquals("A", claimLine.processingIndicatorCode);
		Assert.assertEquals(new Character('0'), claimLine.paymentCode);
		Assert.assertEquals(new Character('0'), claimLine.serviceDeductibleCode);
		Assert.assertEquals(new BigDecimal("1"), claimLine.mtusCount);
		Assert.assertEquals(new Character('3'), claimLine.mtusCode);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "H12345"), claimLine.diagnosis);
		Assert.assertFalse(claimLine.hpsaScarcityCode.isPresent());
		Assert.assertFalse(claimLine.rxNumber.isPresent());
		Assert.assertEquals(new BigDecimal("0"), claimLine.hctHgbTestResult);
		Assert.assertFalse(claimLine.hctHgbTestTypeCode.isPresent());
		Assert.assertEquals("777666666", claimLine.nationalDrugCode.get());
		Assert.assertFalse(claimLine.cliaLabNumber.isPresent());
		Assert.assertEquals(new Integer(0), claimLine.anesthesiaUnitCount);
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_CARRIER}.
	 */
	@Test
	@Ignore
	public void processCarrierClaimRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_CARRIER.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_CARRIER.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_CARRIER.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_INPATIENT}.
	 */
	@Test
	public void process1InpatientClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_INPATIENT.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_INPATIENT.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_INPATIENT.getRifFileType(),
				rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof InpatientClaimGroup);

		// Verify the claim header.
		InpatientClaimGroup claimGroup = (InpatientClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("567834", claimGroup.beneficiaryId);
		Assert.assertEquals("333333222222", claimGroup.claimId);
		Assert.assertEquals(new Character('V'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("60", claimGroup.claimTypeCode);
		Assert.assertEquals(LocalDate.of(2016, 01, 15), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2016, 01, 27), claimGroup.dateThrough);
		Assert.assertEquals("7777766666", claimGroup.providerNumber);
		Assert.assertEquals(new Character('1'), claimGroup.claimServiceClassificationTypeCode);
		Assert.assertFalse(claimGroup.claimNonPaymentReasonCode.isPresent());
		Assert.assertEquals(new BigDecimal("7699.48"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("11.00"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals("IA", claimGroup.providerStateCode);
		Assert.assertEquals("5555553305", claimGroup.organizationNpi.get());
		Assert.assertEquals("161999999", claimGroup.attendingPhysicianNpi.get());
		Assert.assertEquals("3333444555", claimGroup.operatingPhysicianNpi.get());
		Assert.assertEquals("161943433", claimGroup.otherPhysicianNpi.get());
		Assert.assertEquals("51", claimGroup.patientDischargeStatusCode);
		Assert.assertEquals(new BigDecimal("84999.37"), claimGroup.totalChargeAmount);
		Assert.assertEquals(new BigDecimal("10.00"), claimGroup.passThruPerDiemAmount);
		Assert.assertEquals(new BigDecimal("112.00"), claimGroup.deductibleAmount);
		Assert.assertEquals(new BigDecimal("5.00"), claimGroup.partACoinsuranceLiabilityAmount);
		Assert.assertEquals(new BigDecimal("6.00"), claimGroup.bloodDeductibleLiabilityAmount);
		Assert.assertEquals(new BigDecimal("4.00"), claimGroup.professionalComponentCharge);
		Assert.assertEquals(new BigDecimal("33.00"), claimGroup.noncoveredCharge);
		Assert.assertEquals(new BigDecimal("14.00"), claimGroup.totalDeductionAmount);
		Assert.assertEquals(new BigDecimal("646.23"), claimGroup.claimTotalPPSCapitalAmount.get());
		Assert.assertEquals(new BigDecimal("552.56"), claimGroup.claimPPSCapitalFSPAmount.get());
		
		Assert.assertEquals(new BigDecimal("0"), claimGroup.claimPPSCapitalOutlierAmount.get());
		Assert.assertEquals(new BigDecimal("68.58"), claimGroup.claimPPSCapitalIMEAmount.get());
		Assert.assertEquals(new BigDecimal("0"), claimGroup.claimPPSCapitalExceptionAmount.get());
		Assert.assertEquals(new BigDecimal("0"), claimGroup.claimPPSOldCapitalHoldHarmlessAmount.get());
		Assert.assertEquals(new BigDecimal("23.99"), claimGroup.nchDrugOutlierApprovedPaymentAmount.get());
		
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "R4444"), claimGroup.diagnosisAdmitting);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "R5555"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(5, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "R6666", "Y"), claimGroup.diagnosesAdditional.get(0));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "A7777", "N"), claimGroup.diagnosesAdditional.get(1));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "0TCDDEE", LocalDate.of(2016, 01, 16)),
				claimGroup.procedureCodes.get(0));
		Assert.assertTrue(claimGroup.diagnosisFirstClaimExternal.isPresent());
		Assert.assertEquals(1, claimGroup.lines.size());
		// Verify one of the claim lines.
		InpatientClaimLine claimLine = claimGroup.lines.get(0);
		Assert.assertEquals(new BigDecimal("84888.88"), claimLine.totalChargeAmount);
		Assert.assertEquals(new BigDecimal("3699.00"), claimLine.nonCoveredChargeAmount);
		Assert.assertEquals("345345345", claimLine.revenueCenterRenderingPhysicianNPI.get());

	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_INPATIENT}.
	 */
	@Test
	@Ignore
	public void processInpatientClaimRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_INPATIENT.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_INPATIENT.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_INPATIENT.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_OUTPATIENT}.
	 */
	@Test
	public void process1OutpatientClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_OUTPATIENT.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_OUTPATIENT.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_OUTPATIENT.getRifFileType(),
				rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof OutpatientClaimGroup);

		// Verify the claim header.
		OutpatientClaimGroup claimGroup = (OutpatientClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("567834", claimGroup.beneficiaryId);
		Assert.assertEquals("1234567890", claimGroup.claimId);
		Assert.assertEquals(new Character('W'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("40", claimGroup.claimTypeCode);
		Assert.assertEquals(LocalDate.of(2011, 01, 24), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2011, 01, 24), claimGroup.dateThrough);
		Assert.assertEquals("9999999", claimGroup.providerNumber);
		Assert.assertEquals("A", claimGroup.claimNonPaymentReasonCode.get());
		Assert.assertEquals(new BigDecimal("693.11"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("11.00"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals("KY", claimGroup.providerStateCode);
		Assert.assertEquals("1111111111", claimGroup.organizationNpi.get());
		Assert.assertEquals("2222222222", claimGroup.attendingPhysicianNpi.get());
		Assert.assertEquals("3333333333", claimGroup.operatingPhysicianNpi.get());
		Assert.assertFalse(claimGroup.otherPhysicianNpi.isPresent());
		Assert.assertEquals("1", claimGroup.patientDischargeStatusCode.get());
		Assert.assertEquals(new BigDecimal("8888.85"), claimGroup.totalChargeAmount);
		Assert.assertEquals(new BigDecimal("6.00"), claimGroup.bloodDeductibleLiabilityAmount);
		Assert.assertEquals(new BigDecimal("66.89"), claimGroup.professionalComponentCharge);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "R5555"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(2, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "R8888"), claimGroup.diagnosesAdditional.get(0));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "I9999"), claimGroup.diagnosesAdditional.get(1));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "R1122"), claimGroup.diagnosesReasonForVisit.get(0));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_10, "R2222"), claimGroup.diagnosisFirstClaimExternal.get());
		Assert.assertEquals(new BigDecimal("112.00"), claimGroup.deductibleAmount);
		Assert.assertEquals(new BigDecimal("175.73"), claimGroup.coninsuranceAmount);
		Assert.assertEquals(new BigDecimal("693.92"), claimGroup.providerPaymentAmount);
		Assert.assertEquals(new BigDecimal("44.00"), claimGroup.beneficiaryPaymentAmount);
		Assert.assertEquals(1, claimGroup.lines.size());
		// Verify one of the claim lines.
		OutpatientClaimLine claimLine = claimGroup.lines.get(0);
		Assert.assertEquals(new Integer(25), claimLine.lineNumber);
		Assert.assertEquals("M99", claimGroup.lines.get(0).hcpcsCode.get());
		Assert.assertEquals("XX", claimGroup.lines.get(0).hcpcsInitialModifierCode.get());
		Assert.assertFalse(claimLine.hcpcsSecondModifierCode.isPresent());
		Assert.assertEquals(new BigDecimal("10.45"), claimGroup.lines.get(0).bloodDeductibleAmount);
		Assert.assertEquals(new BigDecimal("12.89"), claimGroup.lines.get(0).cashDeductibleAmount);
		Assert.assertEquals(new BigDecimal(5000.00), claimGroup.lines.get(0).paymentAmount);
		Assert.assertEquals(new BigDecimal(134.00), claimGroup.lines.get(0).nonCoveredChargeAmount);
		Assert.assertEquals("345345345", claimLine.revenueCenterRenderingPhysicianNPI.get());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_OUTPATIENT}.
	 */
	@Test
	@Ignore
	public void processOutpatientClaimRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_OUTPATIENT.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_OUTPATIENT.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_OUTPATIENT.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_SNF}.
	 */
	@Test
	@Ignore
	public void process1SNFClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_SNF.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_SNF.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_SNF.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof SNFClaimGroup);

		// Verify the claim header.
		SNFClaimGroup claimGroup = (SNFClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("74", claimGroup.beneficiaryId);
		Assert.assertEquals("4706073107", claimGroup.claimId);
		Assert.assertEquals(new Character('V'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("20", claimGroup.claimTypeCode);
		Assert.assertEquals(LocalDate.of(2013, 12, 01), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2013, 12, 18), claimGroup.dateThrough);
		Assert.assertEquals("295052", claimGroup.providerNumber);
		Assert.assertFalse(claimGroup.claimNonPaymentReasonCode.isPresent());
		Assert.assertEquals(new Character('1'), claimGroup.claimServiceClassificationTypeCode);
		Assert.assertEquals(new BigDecimal("3063.35"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals("NV", claimGroup.providerStateCode);
		Assert.assertEquals("1407894314", claimGroup.organizationNpi.get());
		Assert.assertEquals("1629099379", claimGroup.attendingPhysicianNpi.get());
		Assert.assertFalse(claimGroup.operatingPhysicianNpi.isPresent());
		Assert.assertFalse(claimGroup.otherPhysicianNpi.isPresent());
		Assert.assertEquals("01", claimGroup.patientDischargeStatusCode);
		Assert.assertEquals(new BigDecimal("5156.03"), claimGroup.totalChargeAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.deductibleAmount);
		Assert.assertEquals(new BigDecimal("2516"), claimGroup.partACoinsuranceLiabilityAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.bloodDeductibleLiabilityAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.noncoveredCharge);
		Assert.assertEquals(new BigDecimal("2516"), claimGroup.totalDeductionAmount);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "V5789"), claimGroup.diagnosisAdmitting);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "V5789"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(13, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "V5789"), claimGroup.diagnosesAdditional.get(0));
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "49121"), claimGroup.diagnosesAdditional.get(1));
		Assert.assertFalse(claimGroup.diagnosisFirstClaimExternal.isPresent());

		Assert.assertEquals(7, claimGroup.lines.size());
		// Verify one of the claim lines.
		SNFClaimLine claimLine = claimGroup.lines.get(0);
		Assert.assertFalse(claimLine.hcpcsCode.isPresent());
		Assert.assertEquals(new BigDecimal("66.66"), claimLine.totalChargeAmount);
		Assert.assertEquals(new BigDecimal("45.23"), claimLine.nonCoveredChargeAmount);
		Assert.assertEquals("345345345", claimLine.revenueCenterRenderingPhysicianNPI.get());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_SNF}.
	 */
	@Test
	@Ignore
	public void processSNFClaimRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_SNF.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_SNF.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_SNF.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_HOSPICE}.
	 */
	@Test
	@Ignore
	public void process1HospiceClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_HOSPICE.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_HOSPICE.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_HOSPICE.getRifFileType(),
				rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof HospiceClaimGroup);

		// Verify the claim header.
		HospiceClaimGroup claimGroup = (HospiceClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("246", claimGroup.beneficiaryId);
		Assert.assertEquals("9302293110", claimGroup.claimId);
		Assert.assertEquals(new Character('V'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("50", claimGroup.claimTypeCode);
		Assert.assertEquals(LocalDate.of(2014, 9, 01), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2014, 9, 30), claimGroup.dateThrough);
		Assert.assertEquals("051543", claimGroup.providerNumber);
		Assert.assertFalse(claimGroup.claimNonPaymentReasonCode.isPresent());
		Assert.assertEquals(new Character('1'), claimGroup.claimServiceClassificationTypeCode);
		Assert.assertEquals(new BigDecimal("5558.52"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals("CA", claimGroup.providerStateCode);
		Assert.assertEquals("1043326531", claimGroup.organizationNpi.get());
		Assert.assertEquals("1154428621", claimGroup.attendingPhysicianNpi.get());
		Assert.assertEquals("30", claimGroup.patientDischargeStatusCode);
		Assert.assertEquals(new BigDecimal("6158.1"), claimGroup.totalChargeAmount);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "3310"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(1, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "3310"), claimGroup.diagnosesAdditional.get(0));
		Assert.assertFalse(claimGroup.diagnosisFirstClaimExternal.isPresent());
		Assert.assertEquals(LocalDate.of(2014, 7, 06), claimGroup.claimHospiceStartDate);
				
		Assert.assertEquals(9, claimGroup.lines.size());
		// Verify one of the claim lines.
		HospiceClaimLine claimLine = claimGroup.lines.get(5);
		Assert.assertEquals(new Integer(6), claimLine.lineNumber);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.lines.get(0).paymentAmount);
		Assert.assertEquals(new BigDecimal("5672.1"), claimGroup.lines.get(0).nonCoveredChargeAmount);
		Assert.assertEquals("00000", claimGroup.lines.get(0).hcpcsInitialModifierCode.get());
		Assert.assertEquals("Q5001", claimGroup.lines.get(0).hcpcsSecondModifierCode.get());
		Assert.assertEquals("345345345", claimLine.revenueCenterRenderingPhysicianNPI.get());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_HOSPICE}.
	 */
	@Test
	@Ignore
	public void processHospiceClaimRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_HOSPICE.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_HOSPICE.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_HOSPICE.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_HHA}.
	 */
	@Test
	@Ignore
	public void process1HHAClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_HHA.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_HHA.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_HHA.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof HHAClaimGroup);

		// Verify the claim header.
		HHAClaimGroup claimGroup = (HHAClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("140", claimGroup.beneficiaryId);
		Assert.assertEquals("2929923122", claimGroup.claimId);
		Assert.assertEquals(new Character('W'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("10", claimGroup.claimTypeCode);
		Assert.assertEquals(new Character('2'), claimGroup.claimServiceClassificationTypeCode);
		Assert.assertEquals(LocalDate.of(2015, 6, 23), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2015, 6, 23), claimGroup.dateThrough);
		Assert.assertEquals("467248", claimGroup.providerNumber);
		Assert.assertFalse(claimGroup.claimNonPaymentReasonCode.isPresent());
		Assert.assertEquals(new BigDecimal("2126.18"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals("UT", claimGroup.providerStateCode);
		Assert.assertEquals("1811226772", claimGroup.organizationNpi.get());
		Assert.assertEquals("1760659130", claimGroup.attendingPhysicianNpi.get());
		Assert.assertEquals("30", claimGroup.patientDischargeStatusCode);
		Assert.assertEquals(new BigDecimal("2126.18"), claimGroup.totalChargeAmount);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "72887"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(5, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "72887"), claimGroup.diagnosesAdditional.get(0));
		Assert.assertFalse(claimGroup.diagnosisFirstClaimExternal.isPresent());
		Assert.assertEquals(2, claimGroup.lines.size());
		// Verify one of the claim lines.
		HHAClaimLine claimLine = claimGroup.lines.get(1);
		Assert.assertEquals(new Integer(2), claimLine.lineNumber);
		Assert.assertEquals(new BigDecimal("2126.18"), claimGroup.lines.get(0).paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.lines.get(0).nonCoveredChargeAmount);
		Assert.assertEquals(new BigDecimal("2126.18"), claimGroup.lines.get(0).totalChargeAmount);
		Assert.assertEquals("345345345", claimLine.revenueCenterRenderingPhysicianNPI.get());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_HHA}.
	 */
	@Test
	@Ignore
	public void processHHAClaimRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_HHA.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_HHA.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_HHA.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_DME}.
	 */
	@Test
	@Ignore
	public void process1DMEClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_DME.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_DME.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_DME.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof DMEClaimGroup);

		// Verify the claim header.
		DMEClaimGroup claimGroup = (DMEClaimGroup) rifRecordEvent.getRecord();
		Assert.assertEquals(RifFilesProcessor.RECORD_FORMAT_VERSION, claimGroup.version);
		Assert.assertEquals(RecordAction.INSERT, claimGroup.recordAction);
		Assert.assertEquals("7", claimGroup.beneficiaryId);
		Assert.assertEquals("02199961612", claimGroup.claimId);
		Assert.assertEquals(new Character('M'), claimGroup.nearLineRecordIdCode);
		Assert.assertEquals("82", claimGroup.claimTypeCode);
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimGroup.dateFrom);
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimGroup.dateThrough);
		Assert.assertEquals(LocalDate.of(2014, 02, 14), claimGroup.weeklyProcessDate);
		Assert.assertEquals(new Character('1'), claimGroup.claimEntryCode);
		Assert.assertEquals("01", claimGroup.claimDispositionCode);
		Assert.assertEquals("18003", claimGroup.carrierNumber);
		Assert.assertEquals("1", claimGroup.paymentDenialCode);
		Assert.assertEquals(new BigDecimal("591.75"), claimGroup.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.primaryPayerPaidAmount);
		Assert.assertEquals(new Character('A'), claimGroup.providerAssignmentIndicator);
		Assert.assertEquals(new BigDecimal("591.75"), claimGroup.providerPaymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.beneficiaryPaymentAmount);
		Assert.assertEquals(new BigDecimal("1752.75"), claimGroup.submittedChargeAmount);
		Assert.assertEquals(new BigDecimal("754.79"), claimGroup.allowedChargeAmount);
		Assert.assertEquals(new BigDecimal("0"), claimGroup.beneficiaryPartBDeductAmount);
		Assert.assertEquals(new Character('3'), claimGroup.hcpcsYearCode);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "496"), claimGroup.diagnosisPrincipal);
		Assert.assertEquals(1, claimGroup.diagnosesAdditional.size());
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "496"), claimGroup.diagnosesAdditional.get(0));
		Assert.assertEquals("1891704375", claimGroup.referringPhysicianNpi.get());
		Assert.assertEquals("00000000", claimGroup.clinicalTrialNumber.get());
		Assert.assertEquals(4, claimGroup.lines.size());

		// Verify one of the claim lines.
		DMEClaimLine claimLine = claimGroup.lines.get(3);

		Assert.assertEquals(new Integer(4), claimLine.number);
		Assert.assertEquals("5934931963", claimLine.providerTaxNumber);
		Assert.assertEquals("A5", claimLine.providerSpecialityCode);
		Assert.assertEquals(new Character('1'), claimLine.providerParticipatingIndCode);
		Assert.assertEquals(new BigDecimal("1"), claimLine.serviceCount);
		Assert.assertEquals("9", claimLine.cmsServiceTypeCode);
		Assert.assertEquals("12", claimLine.placeOfServiceCode);
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimLine.firstExpenseDate);
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimLine.lastExpenseDate);
		Assert.assertEquals("Q0513", claimLine.hcpcsCode.get());
		Assert.assertFalse(claimLine.hcpcsInitialModifierCode.isPresent());
		Assert.assertFalse(claimLine.hcpcsSecondModifierCode.isPresent());
		Assert.assertFalse(claimLine.hcpcsThirdModifierCode.isPresent());
		Assert.assertFalse(claimLine.hcpcsFourthModifierCode.isPresent());
		Assert.assertEquals("O1E", claimLine.betosCode.get());
		Assert.assertEquals(new BigDecimal("25.87"), claimLine.paymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimLine.beneficiaryPaymentAmount);
		Assert.assertEquals(new BigDecimal("25.87"), claimLine.providerPaymentAmount);
		Assert.assertEquals(new BigDecimal("0"), claimLine.beneficiaryPartBDeductAmount);
		Assert.assertFalse(claimLine.primaryPayerCode.isPresent());
		Assert.assertEquals(new BigDecimal("0"), claimLine.primaryPayerPaidAmount);
		Assert.assertEquals(new BigDecimal("6.6"), claimLine.coinsuranceAmount);
		Assert.assertEquals(new BigDecimal("0"), claimLine.primaryPayerAllowedChargeAmount);
		Assert.assertEquals(new BigDecimal("132"), claimLine.submittedChargeAmount);
		Assert.assertEquals(new BigDecimal("33"), claimLine.allowedChargeAmount);
		Assert.assertEquals("A", claimLine.processingIndicatorCode);
		Assert.assertEquals(new Character('0'), claimLine.paymentCode);
		Assert.assertEquals(new Character('0'), claimLine.serviceDeductibleCode);
		Assert.assertEquals(new IcdCode(IcdVersion.ICD_9, "496"), claimLine.diagnosis);
		Assert.assertEquals(new BigDecimal("33"), claimLine.purchasePriceAmount);
		Assert.assertEquals("1275697435", claimLine.providerNPI);
		Assert.assertEquals("MO", claimLine.providerStateCode);
		Assert.assertEquals(new BigDecimal("1"), claimLine.mtusCount);
		Assert.assertEquals(new Character('4'), claimLine.mtusCode);
		Assert.assertFalse(claimLine.nationalDrugCode.isPresent());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_B_DME}.
	 */
	@Test
	@Ignore
	public void processDMEClaimRecords() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_B_DME.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent);

		Assert.assertNotNull(rifEvents);
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_DME.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_DME.getRifFileType(),
				rifEventsList.get(0).getFile().getFileType());
	}
}
