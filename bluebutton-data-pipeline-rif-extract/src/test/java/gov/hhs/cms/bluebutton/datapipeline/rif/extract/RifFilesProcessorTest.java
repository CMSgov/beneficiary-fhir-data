package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.MedicareBeneficiaryIdHistory;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RecordAction;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileRecords;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;

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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRifFileType(), rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof Beneficiary);

		Beneficiary beneRow = (Beneficiary) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", beneRow.getBeneficiaryId());
		Assert.assertEquals("MO", beneRow.getStateCode());
		Assert.assertEquals("123", beneRow.getCountyCode());
		Assert.assertEquals("12345", beneRow.getPostalCode());
		Assert.assertEquals(LocalDate.of(1981, Month.MARCH, 17), beneRow.getBirthDate());
		Assert.assertEquals(('1'), beneRow.getSex());
		Assert.assertEquals(new Character('1'), beneRow.getRace().get());
		Assert.assertEquals(new Character('1'), beneRow.getEntitlementCodeOriginal().get());
		Assert.assertEquals(new Character('1'), beneRow.getEntitlementCodeCurrent().get());
		Assert.assertEquals(new Character('N'), beneRow.getEndStageRenalDiseaseCode().get());
		Assert.assertEquals(new String("20"), beneRow.getMedicareEnrollmentStatusCode().get());
		Assert.assertEquals(new Character('0'), beneRow.getPartBTerminationCode().get());
		Assert.assertEquals(new Character('0'), beneRow.getPartBTerminationCode().get());
		Assert.assertEquals("543217066U", beneRow.getHicn());
		Assert.assertEquals("Doe", beneRow.getNameSurname());
		Assert.assertEquals("John", beneRow.getNameGiven());
		Assert.assertEquals(new Character('A'), beneRow.getNameMiddleInitial().get());

		Assert.assertEquals("3456789", beneRow.getMedicareBeneficiaryId().get());
		Assert.assertEquals(LocalDate.of(1981, Month.MARCH, 17), beneRow.getBeneficiaryDateOfDeath().get());
		Assert.assertEquals(LocalDate.of(1963, Month.OCTOBER, 3), beneRow.getMedicareCoverageStartDate().get());
		Assert.assertEquals(new Character('1'), beneRow.getHmoIndicatorAprInd().get());
		Assert.assertEquals(new BigDecimal(5), beneRow.getPartDMonthsCount().get());
		Assert.assertEquals("00", beneRow.getPartDLowIncomeCostShareGroupFebCode().get());
		Assert.assertEquals(new Character('N'), beneRow.getPartDRetireeDrugSubsidyDecInd().get());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_BENEFICIARY_HISTORY}.
	 */
	@Test
	public void processBeneficiaryHistoryRecord_SAMPLE_A() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(),
				StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent0 = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.getRifFileType(),
				rifRecordEvent0.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent0.getRecord());
		Assert.assertTrue(rifRecordEvent0.getRecord() instanceof BeneficiaryHistory);
		BeneficiaryHistory beneficiaryHistory0 = (BeneficiaryHistory) rifRecordEvent0.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent0.getRecordAction());
		Assert.assertEquals("567834", beneficiaryHistory0.getBeneficiaryId());
		Assert.assertEquals(LocalDate.of(1979, Month.MARCH, 17), beneficiaryHistory0.getBirthDate());
		Assert.assertEquals(('2'), beneficiaryHistory0.getSex());
		Assert.assertEquals("543217066Z", beneficiaryHistory0.getHicn());

		/*
		 * We should expect and be able to cope with BENEFICIARY_HISTORY records that
		 * are exact duplicates.
		 */
		for (RifRecordEvent<?> rifRecordEvent : new RifRecordEvent<?>[] { rifEventsList.get(1),
				rifEventsList.get(2) }) {
			Assert.assertEquals(StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.getRifFileType(),
					rifRecordEvent.getFileEvent().getFile().getFileType());
			Assert.assertNotNull(rifRecordEvent.getRecord());
			Assert.assertTrue(rifRecordEvent.getRecord() instanceof BeneficiaryHistory);
			BeneficiaryHistory beneficiaryHistory = (BeneficiaryHistory) rifRecordEvent.getRecord();
			Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
			Assert.assertEquals("567834", beneficiaryHistory.getBeneficiaryId());
			Assert.assertEquals(LocalDate.of(1980, Month.MARCH, 17), beneficiaryHistory.getBirthDate());
			Assert.assertEquals(('1'), beneficiaryHistory.getSex());
			Assert.assertEquals("543217066T", beneficiaryHistory.getHicn());
		}
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY}.
	 */
	@Test
	public void process1MedicareBeneficiaryIdHistoryRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(),
				StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY.getRecordCount(),
				rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent0 = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY.getRifFileType(),
				rifRecordEvent0.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent0.getRecord());
		Assert.assertTrue(rifRecordEvent0.getRecord() instanceof MedicareBeneficiaryIdHistory);
		MedicareBeneficiaryIdHistory medicareBeneficiaryIdHistory = (MedicareBeneficiaryIdHistory) rifRecordEvent0
				.getRecord();

		Assert.assertEquals("400", medicareBeneficiaryIdHistory.getBeneficiaryId().get());
		Assert.assertEquals(LocalDate.of(2011, Month.APRIL, 16),
				medicareBeneficiaryIdHistory.getMbiEffectiveDate().get());
		Assert.assertEquals("9AB2WW3GR44", medicareBeneficiaryIdHistory.getMedicareBeneficiaryId().get());

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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_BENES.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_BENES.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_PDE}.
	 */
	@Test
	public void process1PDERecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_PDE.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_PDE.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_PDE.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof PartDEvent);

		PartDEvent pdeRow = (PartDEvent) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("89", pdeRow.getEventId());
		Assert.assertEquals(new BigDecimal(900), pdeRow.getClaimGroupId());
		Assert.assertEquals("567834", pdeRow.getBeneficiaryId());
		Assert.assertEquals(LocalDate.of(2015, Month.MAY, 12), pdeRow.getPrescriptionFillDate());
		Assert.assertEquals(LocalDate.of(2015, Month.MAY, 27), pdeRow.getPaymentDate().get());
		Assert.assertEquals("01", pdeRow.getServiceProviderIdQualiferCode());
		Assert.assertEquals("1023011079", pdeRow.getServiceProviderId());
		Assert.assertEquals("01", pdeRow.getPrescriberIdQualifierCode());
		Assert.assertEquals("1750384806", pdeRow.getPrescriberId());
		Assert.assertEquals(new BigDecimal(799999), pdeRow.getPrescriptionReferenceNumber());
		Assert.assertEquals("51270012299", pdeRow.getNationalDrugCode());
		Assert.assertEquals("H9999", pdeRow.getPlanContractId());
		Assert.assertEquals("020", pdeRow.getPlanBenefitPackageId());
		Assert.assertEquals(1, pdeRow.getCompoundCode());
		Assert.assertEquals('0', pdeRow.getDispenseAsWrittenProductSelectionCode());
		Assert.assertEquals(new BigDecimal("60"), pdeRow.getQuantityDispensed());
		Assert.assertEquals(new BigDecimal(30), pdeRow.getDaysSupply());
		Assert.assertEquals(new BigDecimal(3), pdeRow.getFillNumber());
		Assert.assertEquals(new Character('P'), pdeRow.getDispensingStatusCode().get());
		Assert.assertEquals('C', pdeRow.getDrugCoverageStatusCode());
		Assert.assertEquals(new Character('A'), pdeRow.getAdjustmentDeletionCode().get());
		Assert.assertEquals(new Character('X'), pdeRow.getNonstandardFormatCode().get());
		Assert.assertEquals(new Character('M'), pdeRow.getPricingExceptionCode().get());
		Assert.assertEquals(new Character('C'), pdeRow.getCatastrophicCoverageCode().get());
		Assert.assertEquals(new BigDecimal("995.34"), pdeRow.getGrossCostBelowOutOfPocketThreshold());
		Assert.assertEquals(new BigDecimal("15.25"), pdeRow.getGrossCostAboveOutOfPocketThreshold());
		Assert.assertEquals(new BigDecimal("235.85"), pdeRow.getPatientPaidAmount());
		Assert.assertEquals(new BigDecimal("17.30"), pdeRow.getOtherTrueOutOfPocketPaidAmount());
		Assert.assertEquals(new BigDecimal("122.23"), pdeRow.getLowIncomeSubsidyPaidAmount());
		Assert.assertEquals(new BigDecimal("42.42"), pdeRow.getPatientLiabilityReductionOtherPaidAmount());
		Assert.assertEquals(new BigDecimal("126.99"), pdeRow.getPartDPlanCoveredPaidAmount());
		Assert.assertEquals(new BigDecimal("17.98"), pdeRow.getPartDPlanNonCoveredPaidAmount());
		Assert.assertEquals(new BigDecimal("550.00"), pdeRow.getTotalPrescriptionCost());
		Assert.assertEquals(new Character('3'), pdeRow.getPrescriptionOriginationCode().get());
		Assert.assertEquals(new BigDecimal("317.22"), pdeRow.getGapDiscountAmount());
		Assert.assertEquals(new Character('G'), pdeRow.getBrandGenericCode().get());
		Assert.assertEquals("01", pdeRow.getPharmacyTypeCode());
		Assert.assertEquals("02", pdeRow.getPatientResidenceCode());
		Assert.assertEquals("08", pdeRow.getSubmissionClarificationCode().get());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_PDE.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_PDE.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_CARRIER}.
	 */
	@Test
	public void process1CarrierClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_CARRIER.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof CarrierClaim);

		// Verify the claim header.
		CarrierClaim claimGroup = (CarrierClaim) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", claimGroup.getBeneficiaryId());
		Assert.assertEquals("9991831999", claimGroup.getClaimId());
		Assert.assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
		Assert.assertEquals('O', claimGroup.getNearLineRecordIdCode());
		Assert.assertEquals("71", claimGroup.getClaimTypeCode());
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimGroup.getDateFrom());
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimGroup.getDateThrough());
		Assert.assertEquals(LocalDate.of(1999, 11, 6), claimGroup.getWeeklyProcessDate());
		Assert.assertEquals('1', claimGroup.getClaimEntryCode());
		Assert.assertEquals("1", claimGroup.getClaimDispositionCode());
		Assert.assertEquals("61026", claimGroup.getCarrierNumber());
		Assert.assertEquals("1", claimGroup.getPaymentDenialCode());
		Assert.assertEquals(new BigDecimal("199.99"), claimGroup.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("0"), claimGroup.getPrimaryPayerPaidAmount());
		Assert.assertEquals("1234534", claimGroup.getReferringPhysicianUpin().get());
		Assert.assertEquals("8765676", claimGroup.getReferringPhysicianNpi().get());
		Assert.assertEquals(new Character('A'), claimGroup.getProviderAssignmentIndicator().get());
		Assert.assertEquals(new BigDecimal("123.45"), claimGroup.getProviderPaymentAmount());
		Assert.assertEquals(new BigDecimal("888.00"), claimGroup.getBeneficiaryPaymentAmount());
		Assert.assertEquals(new BigDecimal("245.04"), claimGroup.getSubmittedChargeAmount());
		Assert.assertEquals(new BigDecimal("166.23"), claimGroup.getAllowedChargeAmount());
		Assert.assertEquals(new BigDecimal("777.00"), claimGroup.getBeneficiaryPartBDeductAmount());
		Assert.assertEquals(new Character('5'), claimGroup.getHcpcsYearCode().get());
		Assert.assertEquals("K25852", claimGroup.getReferringProviderIdNumber());
		Assert.assertEquals("H5555", claimGroup.getDiagnosisPrincipalCode().get());
		Assert.assertEquals(new Character('0'), claimGroup.getDiagnosisPrincipalCodeVersion().get());
		Assert.assertEquals("H5555", claimGroup.getDiagnosis1Code().get());
		Assert.assertEquals(new Character('0'), claimGroup.getDiagnosis1CodeVersion().get());
		Assert.assertEquals("H8888", claimGroup.getDiagnosis2Code().get());
		Assert.assertEquals(new Character('0'), claimGroup.getDiagnosis2CodeVersion().get());
		Assert.assertEquals("H66666", claimGroup.getDiagnosis3Code().get());
		Assert.assertEquals(new Character('0'), claimGroup.getDiagnosis3CodeVersion().get());
		Assert.assertEquals("H77777", claimGroup.getDiagnosis4Code().get());
		Assert.assertEquals(new Character('0'), claimGroup.getDiagnosis4CodeVersion().get());
		Assert.assertFalse(claimGroup.getDiagnosis5Code().isPresent());
		Assert.assertFalse(claimGroup.getDiagnosis5CodeVersion().isPresent());
		Assert.assertEquals(1, claimGroup.getLines().size());
		Assert.assertEquals("0", claimGroup.getClinicalTrialNumber().get());

		// Verify one of the claim lines.
		CarrierClaimLine claimLine = claimGroup.getLines().get(0);
		Assert.assertEquals(new BigDecimal(6), claimLine.getLineNumber());
		Assert.assertEquals("K25555", claimLine.getPerformingProviderIdNumber());
		Assert.assertFalse(claimLine.getPerformingPhysicianUpin().isPresent());
		Assert.assertEquals("1923124", claimLine.getPerformingPhysicianNpi().get());
		Assert.assertTrue(claimLine.getOrganizationNpi().isPresent());
		Assert.assertEquals('0', claimLine.getProviderTypeCode());
		Assert.assertEquals("204299999", claimLine.getProviderTaxNumber());
		Assert.assertEquals("IL", claimLine.getProviderStateCode().get());
		Assert.assertEquals("555558202", claimLine.getProviderZipCode().get());
		Assert.assertEquals("41", claimLine.getProviderSpecialityCode().get());
		Assert.assertEquals(new Character('1'), claimLine.getProviderParticipatingIndCode().get());
		Assert.assertEquals('0', claimLine.getReducedPaymentPhysicianAsstCode());
		Assert.assertEquals(new BigDecimal(1), claimLine.getServiceCount());
		Assert.assertEquals('1', claimLine.getCmsServiceTypeCode());
		Assert.assertEquals("11", claimLine.getPlaceOfServiceCode());
		Assert.assertEquals("15", claimLine.getLinePricingLocalityCode());
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimLine.getFirstExpenseDate().get());
		Assert.assertEquals(LocalDate.of(1999, 10, 27), claimLine.getLastExpenseDate().get());
		Assert.assertEquals("92999", claimLine.getHcpcsCode().get());
		Assert.assertTrue(claimLine.getHcpcsInitialModifierCode().isPresent());
		Assert.assertFalse(claimLine.getHcpcsSecondModifierCode().isPresent());
		Assert.assertEquals("T2D", claimLine.getBetosCode().get());
		Assert.assertEquals(new BigDecimal("37.5"), claimLine.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("0"), claimLine.getBeneficiaryPaymentAmount());
		Assert.assertEquals(new BigDecimal("37.5"), claimLine.getProviderPaymentAmount());
		Assert.assertEquals(new BigDecimal("0"), claimLine.getBeneficiaryPartBDeductAmount());
		Assert.assertTrue(claimLine.getPrimaryPayerCode().isPresent());
		Assert.assertEquals(new BigDecimal("0"), claimLine.getPrimaryPayerPaidAmount());
		Assert.assertEquals(new BigDecimal("9.57"), claimLine.getCoinsuranceAmount());
		Assert.assertEquals(new BigDecimal("75"), claimLine.getSubmittedChargeAmount());
		Assert.assertEquals(new BigDecimal("47.84"), claimLine.getAllowedChargeAmount());
		Assert.assertEquals("A", claimLine.getProcessingIndicatorCode().get());
		Assert.assertEquals(new Character('0'), claimLine.getPaymentCode().get());
		Assert.assertEquals(new Character('0'), claimLine.getServiceDeductibleCode().get());
		Assert.assertEquals(new BigDecimal("1"), claimLine.getMtusCount());
		Assert.assertEquals(new Character('3'), claimLine.getMtusCode().get());
		Assert.assertEquals("H12345", claimLine.getDiagnosisCode().get());
		Assert.assertEquals(new Character('0'), claimLine.getDiagnosisCodeVersion().get());
		Assert.assertFalse(claimLine.getHpsaScarcityCode().isPresent());
		Assert.assertFalse(claimLine.getRxNumber().isPresent());
		Assert.assertEquals(new BigDecimal("42.0"), claimLine.getHctHgbTestResult());
		Assert.assertEquals("R1", claimLine.getHctHgbTestTypeCode().get());
		Assert.assertEquals("51270012299", claimLine.getNationalDrugCode().get());
		Assert.assertEquals("BB889999AA", claimLine.getCliaLabNumber().get());
		Assert.assertEquals(new BigDecimal(0), claimLine.getAnesthesiaUnitCount());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_CARRIER.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_CARRIER.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_INPATIENT}.
	 */
	@Test
	public void process1InpatientClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_INPATIENT.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_INPATIENT.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_INPATIENT.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof InpatientClaim);

		// Verify the claim header.
		InpatientClaim claimGroup = (InpatientClaim) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", claimGroup.getBeneficiaryId());
		Assert.assertEquals("333333222222", claimGroup.getClaimId());
		Assert.assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
		Assert.assertEquals('V', claimGroup.getNearLineRecordIdCode());
		Assert.assertEquals("60", claimGroup.getClaimTypeCode());
		Assert.assertEquals(LocalDate.of(2016, 01, 15), claimGroup.getDateFrom());
		Assert.assertEquals(LocalDate.of(2016, 01, 27), claimGroup.getDateThrough());
		Assert.assertEquals('3', claimGroup.getClaimQueryCode());
		Assert.assertEquals("777776", claimGroup.getProviderNumber());
		Assert.assertEquals('1', claimGroup.getClaimServiceClassificationTypeCode());
		Assert.assertTrue(claimGroup.getClaimNonPaymentReasonCode().isPresent());
		Assert.assertEquals(new BigDecimal("7699.48"), claimGroup.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
		Assert.assertEquals("IA", claimGroup.getProviderStateCode());
		Assert.assertEquals("5555553305", claimGroup.getOrganizationNpi().get());
		Assert.assertEquals("161999999", claimGroup.getAttendingPhysicianNpi().get());
		Assert.assertEquals("3333444555", claimGroup.getOperatingPhysicianNpi().get());
		Assert.assertEquals("161943433", claimGroup.getOtherPhysicianNpi().get());
		Assert.assertEquals("51", claimGroup.getPatientDischargeStatusCode());
		Assert.assertEquals(new BigDecimal("84999.37"), claimGroup.getTotalChargeAmount());
		Assert.assertEquals(LocalDate.of(2016, 1, 15), claimGroup.getClaimAdmissionDate().get());
		Assert.assertEquals('1', claimGroup.getAdmissionTypeCd());
		Assert.assertEquals(new Character('4'), claimGroup.getSourceAdmissionCd().get());
		Assert.assertEquals(new BigDecimal("10.00"), claimGroup.getPassThruPerDiemAmount());
		Assert.assertEquals(new BigDecimal("112.00"), claimGroup.getDeductibleAmount());
		Assert.assertEquals(new BigDecimal("5.00"), claimGroup.getPartACoinsuranceLiabilityAmount());
		Assert.assertEquals(new BigDecimal("6.00"), claimGroup.getBloodDeductibleLiabilityAmount());
		Assert.assertEquals(new BigDecimal("4.00"), claimGroup.getProfessionalComponentCharge());
		Assert.assertEquals(new BigDecimal("33.00"), claimGroup.getNoncoveredCharge());
		Assert.assertEquals(new BigDecimal("14.00"), claimGroup.getTotalDeductionAmount());
		Assert.assertEquals(new BigDecimal("646.23"), claimGroup.getClaimTotalPPSCapitalAmount().get());
		Assert.assertEquals(new BigDecimal("552.56"), claimGroup.getClaimPPSCapitalFSPAmount().get());
		
		Assert.assertEquals(new BigDecimal("0"), claimGroup.getClaimPPSCapitalOutlierAmount().get());
		Assert.assertEquals(new BigDecimal("68.58"), claimGroup.getClaimPPSCapitalIMEAmount().get());
		Assert.assertEquals(new BigDecimal("0"), claimGroup.getClaimPPSCapitalExceptionAmount().get());
		Assert.assertEquals(new BigDecimal("0"), claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount().get());
		Assert.assertEquals(new BigDecimal(12), claimGroup.getUtilizationDayCount());
		Assert.assertEquals(new BigDecimal(0), claimGroup.getCoinsuranceDayCount());
		Assert.assertEquals(new BigDecimal(0), claimGroup.getNonUtilizationDayCount());
		Assert.assertEquals(new BigDecimal(19), claimGroup.getBloodPintsFurnishedQty());
		Assert.assertEquals(LocalDate.of(2016, 1, 27), claimGroup.getBeneficiaryDischargeDate().get());
		Assert.assertEquals(new BigDecimal("23.99"), claimGroup.getDrgOutlierApprovedPaymentAmount().get());
		
		Assert.assertEquals("R4444", claimGroup.getDiagnosisAdmittingCode().get());
		Assert.assertEquals('0', claimGroup.getDiagnosisAdmittingCodeVersion().get().charValue());

		Assert.assertEquals("R5555", claimGroup.getDiagnosisPrincipalCode().get());
		Assert.assertEquals('0', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());

		Assert.assertEquals("R5555", claimGroup.getDiagnosis1Code().get());
		Assert.assertEquals('0', claimGroup.getDiagnosis1CodeVersion().get().charValue());
		Assert.assertEquals('Y', claimGroup.getDiagnosis1PresentOnAdmissionCode().get().charValue());

		Assert.assertEquals("0TCDDEE", claimGroup.getProcedure1Code().get());
		Assert.assertEquals('0', claimGroup.getProcedure1CodeVersion().get().charValue());
		Assert.assertEquals(LocalDate.of(2016, 1, 16), claimGroup.getProcedure1Date().get());

		Assert.assertEquals(1, claimGroup.getLines().size());
		// Verify one of the claim lines.
		InpatientClaimLine claimLine = claimGroup.getLines().get(0);
		Assert.assertEquals(new BigDecimal("84888.88"), claimLine.getTotalChargeAmount());
		Assert.assertEquals(new BigDecimal("3699.00"), claimLine.getNonCoveredChargeAmount());
		Assert.assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_INPATIENT.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_INPATIENT.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_OUTPATIENT}.
	 */
	@Test
	public void process1OutpatientClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_OUTPATIENT.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_OUTPATIENT.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_OUTPATIENT.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof OutpatientClaim);

		// Verify the claim header.
		OutpatientClaim claimGroup = (OutpatientClaim) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", claimGroup.getBeneficiaryId());
		Assert.assertEquals("1234567890", claimGroup.getClaimId());
		Assert.assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
		Assert.assertEquals('W', claimGroup.getNearLineRecordIdCode());
		Assert.assertEquals("40", claimGroup.getClaimTypeCode());
		Assert.assertEquals(LocalDate.of(2011, 01, 24), claimGroup.getDateFrom());
		Assert.assertEquals(LocalDate.of(2011, 01, 24), claimGroup.getDateThrough());
		Assert.assertEquals('3', claimGroup.getClaimQueryCode());
		Assert.assertEquals("999999", claimGroup.getProviderNumber());
		Assert.assertEquals("A", claimGroup.getClaimNonPaymentReasonCode().get());
		Assert.assertEquals(new BigDecimal("693.11"), claimGroup.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
		Assert.assertEquals("KY", claimGroup.getProviderStateCode());
		Assert.assertEquals("1497758544", claimGroup.getOrganizationNpi().get());
		Assert.assertEquals("2222222222", claimGroup.getAttendingPhysicianNpi().get());
		Assert.assertEquals("3333333333", claimGroup.getOperatingPhysicianNpi().get());
		Assert.assertTrue(claimGroup.getOtherPhysicianNpi().isPresent());
		Assert.assertEquals("1", claimGroup.getPatientDischargeStatusCode().get());
		Assert.assertEquals(new BigDecimal("8888.85"), claimGroup.getTotalChargeAmount());
		Assert.assertEquals(new BigDecimal("6.00"), claimGroup.getBloodDeductibleLiabilityAmount());
		Assert.assertEquals(new BigDecimal("66.89"), claimGroup.getProfessionalComponentCharge());
		Assert.assertEquals("R5555", claimGroup.getDiagnosisPrincipalCode().get());
		Assert.assertEquals('0', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());
		Assert.assertEquals("R5555", claimGroup.getDiagnosis1Code().get());
		Assert.assertEquals('0', claimGroup.getDiagnosis1CodeVersion().get().charValue());
		Assert.assertEquals(new BigDecimal("112.00"), claimGroup.getDeductibleAmount());
		Assert.assertEquals(new BigDecimal("175.73"), claimGroup.getCoinsuranceAmount());
		Assert.assertEquals(new BigDecimal("693.92"), claimGroup.getProviderPaymentAmount());
		Assert.assertEquals(new BigDecimal("44.00"), claimGroup.getBeneficiaryPaymentAmount());

		Assert.assertEquals(1, claimGroup.getLines().size());
		// Verify one of the claim lines.
		OutpatientClaimLine claimLine = claimGroup.getLines().get(0);
		Assert.assertEquals(new BigDecimal(25), claimLine.getLineNumber());
		Assert.assertEquals("M99", claimGroup.getLines().get(0).getHcpcsCode().get());
		Assert.assertEquals("XX", claimGroup.getLines().get(0).getHcpcsInitialModifierCode().get());
		Assert.assertFalse(claimLine.getHcpcsSecondModifierCode().isPresent());
		Assert.assertEquals(new BigDecimal("10.45"), claimGroup.getLines().get(0).getBloodDeductibleAmount());
		Assert.assertEquals(new BigDecimal("12.89"), claimGroup.getLines().get(0).getCashDeductibleAmount());
		Assert.assertEquals(new BigDecimal(5000.00), claimGroup.getLines().get(0).getPaymentAmount());
		Assert.assertEquals(new BigDecimal(134.00), claimGroup.getLines().get(0).getNonCoveredChargeAmount());
		Assert.assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_OUTPATIENT.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_OUTPATIENT.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_SNF}.
	 */
	@Test
	public void process1SNFClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_SNF.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_SNF.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_SNF.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof SNFClaim);

		// Verify the claim header.
		SNFClaim claimGroup = (SNFClaim) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", claimGroup.getBeneficiaryId());
		Assert.assertEquals("777777777", claimGroup.getClaimId());
		Assert.assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
		Assert.assertEquals('V', claimGroup.getNearLineRecordIdCode());
		Assert.assertEquals("20", claimGroup.getClaimTypeCode());
		Assert.assertEquals(LocalDate.of(2013, 12, 01), claimGroup.getDateFrom());
		Assert.assertEquals(LocalDate.of(2013, 12, 18), claimGroup.getDateThrough());
		Assert.assertEquals('3', claimGroup.getClaimQueryCode());
		Assert.assertEquals("299999", claimGroup.getProviderNumber());
		Assert.assertEquals("B", claimGroup.getClaimNonPaymentReasonCode().get());
		Assert.assertEquals('1', claimGroup.getClaimServiceClassificationTypeCode());
		Assert.assertEquals(new BigDecimal("3333.33"), claimGroup.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
		Assert.assertEquals("FL", claimGroup.getProviderStateCode());
		Assert.assertEquals("1111111111", claimGroup.getOrganizationNpi().get());
		Assert.assertEquals("2222222222", claimGroup.getAttendingPhysicianNpi().get());
		Assert.assertEquals("3333333333", claimGroup.getOperatingPhysicianNpi().get());
		Assert.assertEquals("4444444444", claimGroup.getOtherPhysicianNpi().get());
		Assert.assertEquals("1", claimGroup.getPatientDischargeStatusCode());
		Assert.assertEquals(new BigDecimal("5555.03"), claimGroup.getTotalChargeAmount());
		Assert.assertEquals(LocalDate.of(2013, 11, 5), claimGroup.getClaimAdmissionDate().get());
		Assert.assertEquals('3', claimGroup.getAdmissionTypeCd());
		Assert.assertEquals('4', claimGroup.getSourceAdmissionCd().get().charValue());
		Assert.assertEquals(new BigDecimal("112.00"), claimGroup.getDeductibleAmount());
		Assert.assertEquals(new BigDecimal("5.00"), claimGroup.getPartACoinsuranceLiabilityAmount());
		Assert.assertEquals(new BigDecimal("6.00"), claimGroup.getBloodDeductibleLiabilityAmount());
		Assert.assertEquals(new BigDecimal("33.00"), claimGroup.getNoncoveredCharge());
		Assert.assertEquals(new BigDecimal("14.00"), claimGroup.getTotalDeductionAmount());
		Assert.assertEquals(new BigDecimal("9.00"), claimGroup.getClaimPPSCapitalFSPAmount().get());
		Assert.assertEquals(new BigDecimal("8.00"), claimGroup.getClaimPPSCapitalOutlierAmount().get());
		Assert.assertEquals(new BigDecimal("7.00"), claimGroup.getClaimPPSCapitalDisproportionateShareAmt().get());
		Assert.assertEquals(new BigDecimal("6.00"), claimGroup.getClaimPPSCapitalIMEAmount().get());
		Assert.assertEquals(new BigDecimal("5.00"), claimGroup.getClaimPPSCapitalExceptionAmount().get());
		Assert.assertEquals(new BigDecimal("4.00"), claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount().get());
		Assert.assertEquals(new BigDecimal(17), claimGroup.getUtilizationDayCount());
		Assert.assertEquals(new BigDecimal(17), claimGroup.getCoinsuranceDayCount());
		Assert.assertEquals(new BigDecimal(0), claimGroup.getNonUtilizationDayCount());
		Assert.assertEquals(new BigDecimal(19), claimGroup.getBloodPintsFurnishedQty());
		Assert.assertEquals(LocalDate.of(2013, 9, 23), claimGroup.getQualifiedStayFromDate().get());
		Assert.assertEquals(LocalDate.of(2013, 11, 5), claimGroup.getQualifiedStayThroughDate().get());
		Assert.assertEquals(LocalDate.of(2002, 1, 11), claimGroup.getNoncoveredStayFromDate().get());
		Assert.assertEquals(LocalDate.of(2002, 1, 21), claimGroup.getNoncoveredStayThroughDate().get());
		Assert.assertFalse(claimGroup.getCoveredCareThroughDate().isPresent());
		Assert.assertEquals(LocalDate.of(2002, 1, 31), claimGroup.getMedicareBenefitsExhaustedDate().get());
		Assert.assertEquals(LocalDate.of(2013, 12, 18), claimGroup.getBeneficiaryDischargeDate().get());

		Assert.assertEquals("R4444", claimGroup.getDiagnosisAdmittingCode().get());
		Assert.assertEquals('9', claimGroup.getDiagnosisAdmittingCodeVersion().get().charValue());
		Assert.assertEquals("R2222", claimGroup.getDiagnosisExternal1Code().get());
		Assert.assertEquals('9', claimGroup.getDiagnosisExternal1CodeVersion().get().charValue());

		Assert.assertEquals(1, claimGroup.getLines().size());
		// Verify one of the claim lines.
		SNFClaimLine claimLine = claimGroup.getLines().get(0);
		Assert.assertEquals("MMM", claimLine.getHcpcsCode().get());
		Assert.assertEquals(new BigDecimal("95.00"), claimLine.getTotalChargeAmount());
		Assert.assertEquals(new BigDecimal("88.00"), claimLine.getNonCoveredChargeAmount());
		Assert.assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_SNF.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_SNF.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_HOSPICE}.
	 */
	@Test
	public void process1HospiceClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_HOSPICE.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_HOSPICE.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_HOSPICE.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof HospiceClaim);

		// Verify the claim header.
		HospiceClaim claimGroup = (HospiceClaim) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", claimGroup.getBeneficiaryId());
		Assert.assertEquals("9992223422", claimGroup.getClaimId());
		Assert.assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
		Assert.assertEquals('V', claimGroup.getNearLineRecordIdCode());
		Assert.assertEquals("50", claimGroup.getClaimTypeCode());
		Assert.assertEquals(LocalDate.of(2014, 1, 01), claimGroup.getDateFrom());
		Assert.assertEquals(LocalDate.of(2014, 1, 30), claimGroup.getDateThrough());
		Assert.assertEquals("12345", claimGroup.getProviderNumber());
		Assert.assertEquals("P", claimGroup.getClaimNonPaymentReasonCode().get());
		Assert.assertEquals('1', claimGroup.getClaimServiceClassificationTypeCode());
		Assert.assertEquals(new BigDecimal("130.32"), claimGroup.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("0"), claimGroup.getPrimaryPayerPaidAmount());
		Assert.assertEquals("AZ", claimGroup.getProviderStateCode());
		Assert.assertEquals("999999999", claimGroup.getOrganizationNpi().get());
		Assert.assertEquals("8888888888", claimGroup.getAttendingPhysicianNpi().get());
		Assert.assertEquals("30", claimGroup.getPatientDischargeStatusCode());
		Assert.assertEquals(new BigDecimal("199.99"), claimGroup.getTotalChargeAmount());
		Assert.assertEquals(new Character('C'), claimGroup.getPatientStatusCd().get());
		Assert.assertEquals(new BigDecimal(30), claimGroup.getUtilizationDayCount());
		Assert.assertEquals(LocalDate.of(2015, 6, 29), claimGroup.getBeneficiaryDischargeDate().get());
		Assert.assertEquals("R5555", claimGroup.getDiagnosisPrincipalCode().get());
		Assert.assertEquals('9', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());
		Assert.assertEquals(LocalDate.of(2014, 7, 06), claimGroup.getClaimHospiceStartDate().get());
				
		Assert.assertEquals(1, claimGroup.getLines().size());
		// Verify one of the claim lines.
		HospiceClaimLine claimLine = claimGroup.getLines().get(0);
		Assert.assertEquals(new BigDecimal(1), claimLine.getLineNumber());
		Assert.assertEquals("651", claimLine.getRevenueCenterCode());
		Assert.assertEquals(new BigDecimal("26.00"), claimGroup.getLines().get(0).getPaymentAmount());
		Assert.assertEquals(new BigDecimal("300.00"), claimGroup.getLines().get(0).getNonCoveredChargeAmount().get());
		Assert.assertEquals("Q9999", claimGroup.getLines().get(0).getHcpcsInitialModifierCode().get());
		Assert.assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_HOSPICE.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_HOSPICE.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_HHA}.
	 */
	@Test
	public void process1HHAClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_HHA.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_HHA.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_HHA.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof HHAClaim);

		// Verify the claim header.
		HHAClaim claimGroup = (HHAClaim) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", claimGroup.getBeneficiaryId());
		Assert.assertEquals("2925555555", claimGroup.getClaimId());
		Assert.assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
		Assert.assertEquals('W', claimGroup.getNearLineRecordIdCode());
		Assert.assertEquals("10", claimGroup.getClaimTypeCode());
		Assert.assertEquals('2', claimGroup.getClaimServiceClassificationTypeCode());
		Assert.assertEquals(LocalDate.of(2015, 6, 23), claimGroup.getDateFrom());
		Assert.assertEquals(LocalDate.of(2015, 6, 23), claimGroup.getDateThrough());
		Assert.assertEquals("45645", claimGroup.getProviderNumber());
		Assert.assertEquals("P", claimGroup.getClaimNonPaymentReasonCode().get());
		Assert.assertEquals(new BigDecimal("188.00"), claimGroup.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
		Assert.assertEquals("UT", claimGroup.getProviderStateCode());
		Assert.assertEquals("1811111111", claimGroup.getOrganizationNpi().get());
		Assert.assertEquals("2222222222", claimGroup.getAttendingPhysicianNpi().get());
		Assert.assertEquals("30", claimGroup.getPatientDischargeStatusCode());
		Assert.assertEquals(new BigDecimal("199.99"), claimGroup.getTotalChargeAmount());
		Assert.assertEquals("H5555", claimGroup.getDiagnosisPrincipalCode().get());
		Assert.assertEquals('9', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());
		Assert.assertEquals('L', claimGroup.getClaimLUPACode().get().charValue());
		Assert.assertEquals('1', claimGroup.getClaimReferralCode().get().charValue());
		Assert.assertEquals(new BigDecimal(3), claimGroup.getTotalVisitCount());
		Assert.assertEquals(LocalDate.of(2015, 6, 23), claimGroup.getCareStartDate().get());

		Assert.assertEquals(1, claimGroup.getLines().size());
		// Verify one of the claim lines.
		HHAClaimLine claimLine = claimGroup.getLines().get(0);
		Assert.assertEquals(new BigDecimal(1), claimLine.getLineNumber());
		Assert.assertEquals("0023", claimLine.getRevenueCenterCode());
		Assert.assertEquals(new BigDecimal("26.00"), claimGroup.getLines().get(0).getPaymentAmount());
		Assert.assertEquals(new BigDecimal("24.00"), claimGroup.getLines().get(0).getNonCoveredChargeAmount());
		Assert.assertEquals(new BigDecimal("25.00"), claimGroup.getLines().get(0).getTotalChargeAmount());
		Assert.assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_HHA.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_HHA.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}

	/**
	 * Ensures that {@link RifFilesProcessor} can correctly handle
	 * {@link StaticRifResource#SAMPLE_A_DME}.
	 */
	@Test
	public void process1DMEClaimRecord() {
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_DME.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_A_DME.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_DME.getRifFileType(),
				rifRecordEvent.getFileEvent().getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof DMEClaim);

		// Verify the claim header.
		DMEClaim claimGroup = (DMEClaim) rifRecordEvent.getRecord();
		Assert.assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
		Assert.assertEquals("567834", claimGroup.getBeneficiaryId());
		Assert.assertEquals("2188888888", claimGroup.getClaimId());
		Assert.assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
		Assert.assertEquals('M', claimGroup.getNearLineRecordIdCode());
		Assert.assertEquals("82", claimGroup.getClaimTypeCode());
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimGroup.getDateFrom());
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimGroup.getDateThrough());
		Assert.assertEquals(LocalDate.of(2014, 02, 14), claimGroup.getWeeklyProcessDate());
		Assert.assertEquals('1', claimGroup.getClaimEntryCode());
		Assert.assertEquals("01", claimGroup.getClaimDispositionCode());
		Assert.assertEquals("99999", claimGroup.getCarrierNumber());
		Assert.assertEquals("1", claimGroup.getPaymentDenialCode());
		Assert.assertEquals(new BigDecimal("777.75"), claimGroup.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("0"), claimGroup.getPrimaryPayerPaidAmount());
		Assert.assertEquals('A', claimGroup.getProviderAssignmentIndicator());
		Assert.assertEquals(new BigDecimal("666.75"), claimGroup.getProviderPaymentAmount());
		Assert.assertEquals(new BigDecimal("666.66"), claimGroup.getBeneficiaryPaymentAmount());
		Assert.assertEquals(new BigDecimal("1752.75"), claimGroup.getSubmittedChargeAmount());
		Assert.assertEquals(new BigDecimal("754.79"), claimGroup.getAllowedChargeAmount());
		Assert.assertEquals(new BigDecimal("777.00"), claimGroup.getBeneficiaryPartBDeductAmount());
		Assert.assertEquals('3', claimGroup.getHcpcsYearCode().get().charValue());
		Assert.assertEquals("R5555", claimGroup.getDiagnosis1Code().get());
		Assert.assertEquals('0', claimGroup.getDiagnosis1CodeVersion().get().charValue());
		Assert.assertEquals("1306849450", claimGroup.getReferringPhysicianNpi().get());
		Assert.assertEquals("0", claimGroup.getClinicalTrialNumber().get());
		Assert.assertEquals(1, claimGroup.getLines().size());

		// Verify one of the claim lines.
		DMEClaimLine claimLine = claimGroup.getLines().get(0);

		Assert.assertEquals(new BigDecimal(1), claimLine.getLineNumber());
		Assert.assertEquals("9994931888", claimLine.getProviderTaxNumber());
		Assert.assertEquals("A5", claimLine.getProviderSpecialityCode().get());
		Assert.assertEquals('1', claimLine.getProviderParticipatingIndCode().get().charValue());
		Assert.assertEquals(new BigDecimal("60"), claimLine.getServiceCount());
		Assert.assertEquals('P', claimLine.getCmsServiceTypeCode());
		Assert.assertEquals("12", claimLine.getPlaceOfServiceCode());
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimLine.getFirstExpenseDate().get());
		Assert.assertEquals(LocalDate.of(2014, 02, 03), claimLine.getLastExpenseDate().get());
		Assert.assertEquals("345", claimLine.getHcpcsCode().get());
		Assert.assertFalse(claimLine.getHcpcsSecondModifierCode().isPresent());
		Assert.assertFalse(claimLine.getHcpcsThirdModifierCode().isPresent());
		Assert.assertFalse(claimLine.getHcpcsFourthModifierCode().isPresent());
		Assert.assertEquals("D9Z", claimLine.getBetosCode().get());
		Assert.assertEquals(new BigDecimal("123.45"), claimLine.getPaymentAmount());
		Assert.assertEquals(new BigDecimal("11.00"), claimLine.getBeneficiaryPaymentAmount());
		Assert.assertEquals(new BigDecimal("120.00"), claimLine.getProviderPaymentAmount());
		Assert.assertEquals(new BigDecimal("18.00"), claimLine.getBeneficiaryPartBDeductAmount());
		Assert.assertTrue(claimLine.getPrimaryPayerCode().isPresent());
		Assert.assertEquals(new BigDecimal("11.00"), claimLine.getPrimaryPayerPaidAmount());
		Assert.assertEquals(new BigDecimal("20.20"), claimLine.getCoinsuranceAmount());
		Assert.assertEquals(new BigDecimal("20.29"), claimLine.getPrimaryPayerAllowedChargeAmount());
		Assert.assertEquals(new BigDecimal("130.45"), claimLine.getSubmittedChargeAmount());
		Assert.assertEquals(new BigDecimal("129.45"), claimLine.getAllowedChargeAmount());
		Assert.assertEquals("A", claimLine.getProcessingIndicatorCode().get());
		Assert.assertEquals('0', claimLine.getPaymentCode().get().charValue());
		Assert.assertEquals('0', claimLine.getServiceDeductibleCode().get().charValue());
		Assert.assertEquals(new BigDecimal("82.29"), claimLine.getPurchasePriceAmount());
		Assert.assertEquals("1244444444", claimLine.getProviderNPI().get());
		Assert.assertEquals("AL", claimLine.getPricingStateCode().get());
		Assert.assertEquals("MO", claimLine.getProviderStateCode());
		Assert.assertEquals(new Character('3'), claimLine.getSupplierTypeCode().get());
		Assert.assertEquals(new BigDecimal("0.00"), claimLine.getScreenSavingsAmount().get());
		Assert.assertEquals(new BigDecimal("60"), claimLine.getMtusCount());
		Assert.assertEquals('3', claimLine.getMtusCode().get().charValue());
		Assert.assertEquals(new BigDecimal("44.4"), claimLine.getHctHgbTestResult());
		Assert.assertEquals("R2", claimLine.getHctHgbTestTypeCode().get());
		Assert.assertEquals("51270012299", claimLine.getNationalDrugCode().get());
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
		RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
		List<RifRecordEvent<?>> rifEventsList = rifFileRecords.getRecords().collect(Collectors.toList());

		Assert.assertEquals(StaticRifResource.SAMPLE_B_DME.getRecordCount(), rifEventsList.size());
		Assert.assertEquals(StaticRifResource.SAMPLE_B_DME.getRifFileType(),
				rifEventsList.get(0).getFileEvent().getFile().getFileType());
	}
}
