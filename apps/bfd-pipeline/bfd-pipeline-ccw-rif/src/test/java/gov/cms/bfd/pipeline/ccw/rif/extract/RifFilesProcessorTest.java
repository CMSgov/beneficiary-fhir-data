package gov.cms.bfd.pipeline.ccw.rif.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimLine;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HHAClaimLine;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.HospiceClaimLine;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.InpatientClaimLine;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaimLine;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.RecordAction;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.SNFClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RifFilesProcessor}. */
public final class RifFilesProcessorTest {

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_BENES}.
   */
  @Test
  public void process1BeneRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_BENES.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_BENES.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_BENES.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof Beneficiary);

    Beneficiary beneRow = (Beneficiary) rifRecordEvent.getRecord();
    assertEquals(beneRow.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(567834L, beneRow.getBeneficiaryId());
    assertEquals("MO", beneRow.getStateCode());
    assertEquals("123", beneRow.getCountyCode());
    assertEquals("12345", beneRow.getPostalCode());
    assertEquals(LocalDate.of(1981, Month.MARCH, 17), beneRow.getBirthDate());
    assertEquals(('1'), beneRow.getSex());
    assertEquals(new Character('1'), beneRow.getRace().get());
    assertEquals(new Character('1'), beneRow.getEntitlementCodeOriginal().get());
    assertEquals(new Character('1'), beneRow.getEntitlementCodeCurrent().get());
    assertEquals(new Character('N'), beneRow.getEndStageRenalDiseaseCode().get());
    assertEquals(new String("20"), beneRow.getMedicareEnrollmentStatusCode().get());
    assertEquals(new Character('0'), beneRow.getPartBTerminationCode().get());
    assertEquals(new Character('0'), beneRow.getPartBTerminationCode().get());
    assertEquals("543217066U", beneRow.getHicnUnhashed().orElse(null));
    assertEquals("Doe", beneRow.getNameSurname());
    assertEquals("John", beneRow.getNameGiven());
    assertEquals(new Character('A'), beneRow.getNameMiddleInitial().get());

    assertEquals("3456789", beneRow.getMedicareBeneficiaryId().get());
    assertEquals(LocalDate.of(1981, Month.MARCH, 17), beneRow.getBeneficiaryDateOfDeath().get());
    assertEquals(
        LocalDate.of(1963, Month.OCTOBER, 3), beneRow.getMedicareCoverageStartDate().get());
    assertEquals(new Character('1'), beneRow.getHmoIndicatorAprInd().get());
    assertEquals(new BigDecimal(5), beneRow.getPartDMonthsCount().get());
    assertEquals("00", beneRow.getPartDLowIncomeCostShareGroupFebCode().get());
    assertEquals(new Character('N'), beneRow.getPartDRetireeDrugSubsidyDecInd().get());
    assertEquals("204 SOUTH ST", beneRow.getDerivedMailingAddress1().get());
    assertEquals("7560 123TH ST", beneRow.getDerivedMailingAddress2().get());
    assertEquals("SURREY", beneRow.getDerivedMailingAddress3().get());
    assertEquals("DAEJEON SI 34867", beneRow.getDerivedMailingAddress4().get());
    assertEquals("COLOMBIA", beneRow.getDerivedMailingAddress5().get());
    assertEquals("SURREY", beneRow.getDerivedMailingAddress6().get());
    assertEquals("PODUNK", beneRow.getDerivedCityName().get());
    assertEquals("IA", beneRow.getDerivedStateCode().get());
    assertEquals("123456789", beneRow.getDerivedZipCode().get());
    assertEquals(LocalDate.of(2020, Month.JULY, 30), beneRow.getMbiEffectiveDate().get());
    assertEquals(new BigDecimal("1"), beneRow.getBeneLinkKey().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_BENES}.
   */
  @Test
  public void process1BeneRecordWithBackslash() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(
            Instant.now(), StaticRifResource.SAMPLE_A_BENES_WITH_BACKSLASH.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(
        StaticRifResource.SAMPLE_A_BENES_WITH_BACKSLASH.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_BENES_WITH_BACKSLASH.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof Beneficiary);

    Beneficiary beneRow = (Beneficiary) rifRecordEvent.getRecord();
    assertEquals(beneRow.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals("DAEJEON SI 34867", beneRow.getDerivedMailingAddress4().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_BENEFICIARY_HISTORY}.
   */
  @Test
  public void processBeneficiaryHistoryRecord_SAMPLE_A() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(
            Instant.now(), StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(
        StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent0 = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.getRifFileType(),
        rifRecordEvent0.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent0.getRecord());
    assertTrue(rifRecordEvent0.getRecord() instanceof BeneficiaryHistory);
    BeneficiaryHistory beneficiaryHistory0 = (BeneficiaryHistory) rifRecordEvent0.getRecord();
    assertEquals(beneficiaryHistory0.getBeneficiaryId(), rifRecordEvent0.getBeneficiaryId());
    assertEquals(RecordAction.INSERT, rifRecordEvent0.getRecordAction());
    assertEquals(567834L, beneficiaryHistory0.getBeneficiaryId());
    assertEquals(LocalDate.of(1979, Month.MARCH, 17), beneficiaryHistory0.getBirthDate());
    assertEquals(('2'), beneficiaryHistory0.getSex());
    assertEquals("543217066Z", beneficiaryHistory0.getHicn());
    assertEquals(Optional.of("3456689"), beneficiaryHistory0.getMedicareBeneficiaryId());
    assertEquals(
        LocalDate.of(1990, Month.MARCH, 17), beneficiaryHistory0.getMbiEffectiveDate().get());
    assertEquals(
        LocalDate.of(1995, Month.MARCH, 17), beneficiaryHistory0.getMbiObsoleteDate().get());
    /*
     * We should expect and be able to cope with BENEFICIARY_HISTORY records that
     * are exact duplicates.
     */
    for (RifRecordEvent<?> rifRecordEvent :
        new RifRecordEvent<?>[] {rifEventsList.get(1), rifEventsList.get(2)}) {
      assertEquals(
          StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY.getRifFileType(),
          rifRecordEvent.getFileEvent().getFile().getFileType());
      assertNotNull(rifRecordEvent.getRecord());
      assertTrue(rifRecordEvent.getRecord() instanceof BeneficiaryHistory);
      BeneficiaryHistory beneficiaryHistory = (BeneficiaryHistory) rifRecordEvent.getRecord();
      assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
      assertEquals(567834L, beneficiaryHistory.getBeneficiaryId());
      assertEquals(LocalDate.of(1980, Month.MARCH, 17), beneficiaryHistory.getBirthDate());
      assertEquals(('1'), beneficiaryHistory.getSex());
      assertEquals("543217066T", beneficiaryHistory.getHicn());
      assertEquals(Optional.of("3456789"), beneficiaryHistory.getMedicareBeneficiaryId());
      assertEquals(
          LocalDate.of(1990, Month.MARCH, 17), beneficiaryHistory0.getMbiEffectiveDate().get());
      assertEquals(
          LocalDate.of(1995, Month.MARCH, 17), beneficiaryHistory0.getMbiObsoleteDate().get());
    }
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY}.
   */
  @Test
  public void process1MedicareBeneficiaryIdHistoryRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(
            Instant.now(), StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(
        StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY.getRecordCount(),
        rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent0 = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY.getRifFileType(),
        rifRecordEvent0.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent0.getRecord());
    assertTrue(rifRecordEvent0.getRecord() instanceof MedicareBeneficiaryIdHistory);
    MedicareBeneficiaryIdHistory medicareBeneficiaryIdHistory =
        (MedicareBeneficiaryIdHistory) rifRecordEvent0.getRecord();

    assertEquals(567834L, medicareBeneficiaryIdHistory.getBeneficiaryId().get());
    assertEquals(
        LocalDate.of(2011, Month.APRIL, 16),
        medicareBeneficiaryIdHistory.getMbiEffectiveDate().get());
    assertEquals("9AB2WW3GR44", medicareBeneficiaryIdHistory.getMedicareBeneficiaryId().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_PDE}.
   */
  @Test
  public void process1PDERecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_PDE.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_PDE.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_PDE.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof PartDEvent);

    PartDEvent pdeRow = (PartDEvent) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(pdeRow.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(89L, pdeRow.getEventId());
    assertEquals(new BigDecimal(900), pdeRow.getClaimGroupId());
    assertEquals(567834L, pdeRow.getBeneficiaryId());
    assertEquals(LocalDate.of(2015, Month.MAY, 12), pdeRow.getPrescriptionFillDate());
    assertEquals(LocalDate.of(2015, Month.MAY, 27), pdeRow.getPaymentDate().get());
    assertEquals("01", pdeRow.getServiceProviderIdQualiferCode());
    assertEquals("1023011079", pdeRow.getServiceProviderId());
    assertEquals("01", pdeRow.getPrescriberIdQualifierCode());
    assertEquals("1750384806", pdeRow.getPrescriberId());
    assertEquals(new BigDecimal(799999), pdeRow.getPrescriptionReferenceNumber());
    assertEquals("500904610", pdeRow.getNationalDrugCode());
    assertEquals("H9999", pdeRow.getPlanContractId());
    assertEquals("020", pdeRow.getPlanBenefitPackageId());
    assertEquals(1, pdeRow.getCompoundCode());
    assertEquals('0', pdeRow.getDispenseAsWrittenProductSelectionCode());
    assertEquals(new BigDecimal("60"), pdeRow.getQuantityDispensed());
    assertEquals(new BigDecimal(30), pdeRow.getDaysSupply());
    assertEquals(new BigDecimal(3), pdeRow.getFillNumber());
    assertEquals(new Character('P'), pdeRow.getDispensingStatusCode().get());
    assertEquals('C', pdeRow.getDrugCoverageStatusCode());
    assertEquals(new Character('A'), pdeRow.getAdjustmentDeletionCode().get());
    assertEquals(new Character('X'), pdeRow.getNonstandardFormatCode().get());
    assertEquals(new Character('M'), pdeRow.getPricingExceptionCode().get());
    assertEquals(new Character('C'), pdeRow.getCatastrophicCoverageCode().get());
    assertEquals(new BigDecimal("995.34"), pdeRow.getGrossCostBelowOutOfPocketThreshold());
    assertEquals(new BigDecimal("15.25"), pdeRow.getGrossCostAboveOutOfPocketThreshold());
    assertEquals(new BigDecimal("235.85"), pdeRow.getPatientPaidAmount());
    assertEquals(new BigDecimal("17.30"), pdeRow.getOtherTrueOutOfPocketPaidAmount());
    assertEquals(new BigDecimal("122.23"), pdeRow.getLowIncomeSubsidyPaidAmount());
    assertEquals(new BigDecimal("42.42"), pdeRow.getPatientLiabilityReductionOtherPaidAmount());
    assertEquals(new BigDecimal("126.99"), pdeRow.getPartDPlanCoveredPaidAmount());
    assertEquals(new BigDecimal("17.98"), pdeRow.getPartDPlanNonCoveredPaidAmount());
    assertEquals(new BigDecimal("550.00"), pdeRow.getTotalPrescriptionCost());
    assertEquals(new Character('3'), pdeRow.getPrescriptionOriginationCode().get());
    assertEquals(new BigDecimal("317.22"), pdeRow.getGapDiscountAmount());
    assertEquals(new Character('G'), pdeRow.getBrandGenericCode().get());
    assertEquals("01", pdeRow.getPharmacyTypeCode());
    assertEquals("02", pdeRow.getPatientResidenceCode());
    assertEquals("08", pdeRow.getSubmissionClarificationCode().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_CARRIER}.
   */
  @Test
  public void process1CarrierClaimRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_CARRIER.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_CARRIER.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof CarrierClaim);

    // Verify the claim header.
    CarrierClaim claimGroup = (CarrierClaim) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(claimGroup.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(567834L, claimGroup.getBeneficiaryId());
    assertEquals(9991831999L, claimGroup.getClaimId());
    assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
    assertEquals('O', claimGroup.getNearLineRecordIdCode());
    assertEquals("71", claimGroup.getClaimTypeCode());
    assertEquals(LocalDate.of(1999, 10, 27), claimGroup.getDateFrom());
    assertEquals(LocalDate.of(1999, 10, 27), claimGroup.getDateThrough());
    assertEquals(LocalDate.of(1999, 11, 6), claimGroup.getWeeklyProcessDate());
    assertEquals('1', claimGroup.getClaimEntryCode());
    assertEquals("1", claimGroup.getClaimDispositionCode());
    assertEquals("61026", claimGroup.getCarrierNumber());
    assertEquals("1", claimGroup.getPaymentDenialCode());
    assertEquals(new BigDecimal("199.99"), claimGroup.getPaymentAmount());
    assertEquals(new BigDecimal("0"), claimGroup.getPrimaryPayerPaidAmount());
    assertEquals("1234534", claimGroup.getReferringPhysicianUpin().get());
    assertEquals("8765676", claimGroup.getReferringPhysicianNpi().get());
    assertEquals(new Character('A'), claimGroup.getProviderAssignmentIndicator().get());
    assertEquals(new BigDecimal("123.45"), claimGroup.getProviderPaymentAmount());
    assertEquals(new BigDecimal("888.00"), claimGroup.getBeneficiaryPaymentAmount());
    assertEquals(new BigDecimal("245.04"), claimGroup.getSubmittedChargeAmount());
    assertEquals(new BigDecimal("166.23"), claimGroup.getAllowedChargeAmount());
    assertEquals(new BigDecimal("777.00"), claimGroup.getBeneficiaryPartBDeductAmount());
    assertEquals(new Character('5'), claimGroup.getHcpcsYearCode().get());
    assertEquals("K25852", claimGroup.getReferringProviderIdNumber());
    assertEquals("H5555", claimGroup.getDiagnosisPrincipalCode().get());
    assertEquals(new Character('0'), claimGroup.getDiagnosisPrincipalCodeVersion().get());
    assertEquals("H5555", claimGroup.getDiagnosis1Code().get());
    assertEquals(new Character('0'), claimGroup.getDiagnosis1CodeVersion().get());
    assertEquals("H8888", claimGroup.getDiagnosis2Code().get());
    assertEquals(new Character('0'), claimGroup.getDiagnosis2CodeVersion().get());
    assertEquals("H66666", claimGroup.getDiagnosis3Code().get());
    assertEquals(new Character('0'), claimGroup.getDiagnosis3CodeVersion().get());
    assertEquals("H77777", claimGroup.getDiagnosis4Code().get());
    assertEquals(new Character('0'), claimGroup.getDiagnosis4CodeVersion().get());
    assertFalse(claimGroup.getDiagnosis5Code().isPresent());
    assertFalse(claimGroup.getDiagnosis5CodeVersion().isPresent());
    assertEquals(1, claimGroup.getLines().size());
    assertEquals("0", claimGroup.getClinicalTrialNumber().get());
    assertEquals("74655592568216", claimGroup.getClaimCarrierControlNumber().get());

    // Verify one of the claim lines.
    CarrierClaimLine claimLine = claimGroup.getLines().get(0);
    assertEquals(new BigDecimal(6), claimLine.getLineNumber());
    assertEquals("K25555", claimLine.getPerformingProviderIdNumber());
    assertFalse(claimLine.getPerformingPhysicianUpin().isPresent());
    assertEquals("1923124", claimLine.getPerformingPhysicianNpi().get());
    assertTrue(claimLine.getOrganizationNpi().isPresent());
    assertEquals('0', claimLine.getProviderTypeCode());
    assertEquals("204299999", claimLine.getProviderTaxNumber());
    assertEquals("IL", claimLine.getProviderStateCode().get());
    assertEquals("555558202", claimLine.getProviderZipCode().get());
    assertEquals("41", claimLine.getProviderSpecialityCode().get());
    assertEquals(new Character('1'), claimLine.getProviderParticipatingIndCode().get());
    assertEquals('0', claimLine.getReducedPaymentPhysicianAsstCode());
    assertEquals(new BigDecimal(1), claimLine.getServiceCount());
    assertEquals('1', claimLine.getCmsServiceTypeCode());
    assertEquals("11", claimLine.getPlaceOfServiceCode());
    assertEquals("15", claimLine.getLinePricingLocalityCode());
    assertEquals(LocalDate.of(1999, 10, 27), claimLine.getFirstExpenseDate().get());
    assertEquals(LocalDate.of(1999, 10, 27), claimLine.getLastExpenseDate().get());
    assertEquals("92999", claimLine.getHcpcsCode().get());
    assertTrue(claimLine.getHcpcsInitialModifierCode().isPresent());
    assertFalse(claimLine.getHcpcsSecondModifierCode().isPresent());
    assertEquals("T2D", claimLine.getBetosCode().get());
    assertEquals(new BigDecimal("37.5"), claimLine.getPaymentAmount());
    assertEquals(new BigDecimal("0"), claimLine.getBeneficiaryPaymentAmount());
    assertEquals(new BigDecimal("37.5"), claimLine.getProviderPaymentAmount());
    assertEquals(new BigDecimal("0"), claimLine.getBeneficiaryPartBDeductAmount());
    assertTrue(claimLine.getPrimaryPayerCode().isPresent());
    assertEquals(new BigDecimal("0"), claimLine.getPrimaryPayerPaidAmount());
    assertEquals(new BigDecimal("9.57"), claimLine.getCoinsuranceAmount());
    assertEquals(new BigDecimal("75"), claimLine.getSubmittedChargeAmount());
    assertEquals(new BigDecimal("47.84"), claimLine.getAllowedChargeAmount());
    assertEquals("A", claimLine.getProcessingIndicatorCode().get());
    assertEquals(new Character('0'), claimLine.getPaymentCode().get());
    assertEquals(new Character('0'), claimLine.getServiceDeductibleCode().get());
    assertEquals(new BigDecimal("1"), claimLine.getMtusCount());
    assertEquals(new Character('3'), claimLine.getMtusCode().get());
    assertEquals("H12345", claimLine.getDiagnosisCode().get());
    assertEquals(new Character('0'), claimLine.getDiagnosisCodeVersion().get());
    assertFalse(claimLine.getHpsaScarcityCode().isPresent());
    assertFalse(claimLine.getRxNumber().isPresent());
    assertEquals(new BigDecimal("42.0"), claimLine.getHctHgbTestResult());
    assertEquals("R1", claimLine.getHctHgbTestTypeCode().get());
    assertEquals("49035044700", claimLine.getNationalDrugCode().get());
    assertEquals("BB889999AA", claimLine.getCliaLabNumber().get());
    assertEquals(new BigDecimal(0), claimLine.getAnesthesiaUnitCount());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_INPATIENT}.
   */
  @Test
  public void process1InpatientClaimRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_INPATIENT.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_INPATIENT.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_INPATIENT.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof InpatientClaim);

    // Verify the claim header.
    InpatientClaim claimGroup = (InpatientClaim) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(claimGroup.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(567834L, claimGroup.getBeneficiaryId());
    assertEquals(333333222222L, claimGroup.getClaimId());
    assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
    assertEquals('V', claimGroup.getNearLineRecordIdCode());
    assertEquals("60", claimGroup.getClaimTypeCode());
    assertEquals(LocalDate.of(2016, 01, 15), claimGroup.getDateFrom());
    assertEquals(LocalDate.of(2016, 01, 27), claimGroup.getDateThrough());
    assertEquals('3', claimGroup.getClaimQueryCode());
    assertEquals("777776", claimGroup.getProviderNumber());
    assertEquals('1', claimGroup.getClaimServiceClassificationTypeCode());
    assertTrue(claimGroup.getClaimNonPaymentReasonCode().isPresent());
    assertEquals(new BigDecimal("7699.48"), claimGroup.getPaymentAmount());
    assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
    assertEquals("IA", claimGroup.getProviderStateCode());
    assertEquals("5555553305", claimGroup.getOrganizationNpi().get());
    assertEquals("161999999", claimGroup.getAttendingPhysicianNpi().get());
    assertEquals("3333444555", claimGroup.getOperatingPhysicianNpi().get());
    assertEquals("161943433", claimGroup.getOtherPhysicianNpi().get());
    assertEquals("51", claimGroup.getPatientDischargeStatusCode());
    assertEquals(new BigDecimal("84999.37"), claimGroup.getTotalChargeAmount());
    assertEquals(LocalDate.of(2016, 1, 15), claimGroup.getClaimAdmissionDate().get());
    assertEquals('1', claimGroup.getAdmissionTypeCd());
    assertEquals(new Character('4'), claimGroup.getSourceAdmissionCd().get());
    assertEquals(new BigDecimal("10.00"), claimGroup.getPassThruPerDiemAmount());
    assertEquals(new BigDecimal("112.00"), claimGroup.getDeductibleAmount());
    assertEquals(new BigDecimal("5.00"), claimGroup.getPartACoinsuranceLiabilityAmount());
    assertEquals(new BigDecimal("6.00"), claimGroup.getBloodDeductibleLiabilityAmount());
    assertEquals(new BigDecimal("4.00"), claimGroup.getProfessionalComponentCharge());
    assertEquals(new BigDecimal("33.00"), claimGroup.getNoncoveredCharge());
    assertEquals(new BigDecimal("14.00"), claimGroup.getTotalDeductionAmount());
    assertEquals(new BigDecimal("646.23"), claimGroup.getClaimTotalPPSCapitalAmount().get());
    assertEquals(new BigDecimal("552.56"), claimGroup.getClaimPPSCapitalFSPAmount().get());

    assertEquals(new BigDecimal("0"), claimGroup.getClaimPPSCapitalOutlierAmount().get());
    assertEquals(new BigDecimal("68.58"), claimGroup.getClaimPPSCapitalIMEAmount().get());
    assertEquals(new BigDecimal("0"), claimGroup.getClaimPPSCapitalExceptionAmount().get());
    assertEquals(new BigDecimal("0"), claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount().get());
    assertEquals(new BigDecimal(12), claimGroup.getUtilizationDayCount());
    assertEquals(new BigDecimal(0), claimGroup.getCoinsuranceDayCount());
    assertEquals(new BigDecimal(0), claimGroup.getNonUtilizationDayCount());
    assertEquals(new BigDecimal(19), claimGroup.getBloodPintsFurnishedQty());
    assertEquals(LocalDate.of(2016, 1, 27), claimGroup.getBeneficiaryDischargeDate().get());
    assertEquals(new BigDecimal("23.99"), claimGroup.getDrgOutlierApprovedPaymentAmount().get());

    assertEquals("R4444", claimGroup.getDiagnosisAdmittingCode().get());
    assertEquals('0', claimGroup.getDiagnosisAdmittingCodeVersion().get().charValue());

    assertEquals("R5555", claimGroup.getDiagnosisPrincipalCode().get());
    assertEquals('0', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());

    assertEquals("R5555", claimGroup.getDiagnosis1Code().get());
    assertEquals('0', claimGroup.getDiagnosis1CodeVersion().get().charValue());
    assertEquals('Y', claimGroup.getDiagnosis1PresentOnAdmissionCode().get().charValue());

    assertEquals("0TCDDEE", claimGroup.getProcedure1Code().get());
    assertEquals('0', claimGroup.getProcedure1CodeVersion().get().charValue());
    assertEquals(LocalDate.of(2016, 1, 16), claimGroup.getProcedure1Date().get());
    assertEquals(new BigDecimal("120.56"), claimGroup.getClaimUncompensatedCareAmount().get());
    assertEquals("28486613848", claimGroup.getFiDocumentClaimControlNumber().get());
    assertEquals("261660474641024", claimGroup.getFiOriginalClaimControlNumber().get());

    assertEquals(1, claimGroup.getLines().size());
    // Verify one of the claim lines.
    InpatientClaimLine claimLine = claimGroup.getLines().get(0);
    assertEquals(new BigDecimal("84888.88"), claimLine.getTotalChargeAmount());
    assertEquals(new BigDecimal("3699.00"), claimLine.getNonCoveredChargeAmount());
    assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_OUTPATIENT}.
   */
  @Test
  public void process1OutpatientClaimRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_OUTPATIENT.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_OUTPATIENT.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_OUTPATIENT.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof OutpatientClaim);

    // Verify the claim header.
    OutpatientClaim claimGroup = (OutpatientClaim) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(claimGroup.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(567834L, claimGroup.getBeneficiaryId());
    assertEquals(1234567890L, claimGroup.getClaimId());
    assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
    assertEquals('W', claimGroup.getNearLineRecordIdCode());
    assertEquals("40", claimGroup.getClaimTypeCode());
    assertEquals(LocalDate.of(2011, 01, 24), claimGroup.getDateFrom());
    assertEquals(LocalDate.of(2011, 01, 24), claimGroup.getDateThrough());
    assertEquals('3', claimGroup.getClaimQueryCode());
    assertEquals("999999", claimGroup.getProviderNumber());
    assertEquals("A", claimGroup.getClaimNonPaymentReasonCode().get());
    assertEquals(new BigDecimal("693.11"), claimGroup.getPaymentAmount());
    assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
    assertEquals("KY", claimGroup.getProviderStateCode());
    assertEquals("1497758544", claimGroup.getOrganizationNpi().get());
    assertEquals("2222222222", claimGroup.getAttendingPhysicianNpi().get());
    assertEquals("3333333333", claimGroup.getOperatingPhysicianNpi().get());
    assertTrue(claimGroup.getOtherPhysicianNpi().isPresent());
    assertEquals("1", claimGroup.getPatientDischargeStatusCode().get());
    assertEquals(new BigDecimal("8888.85"), claimGroup.getTotalChargeAmount());
    assertEquals(new BigDecimal("6.00"), claimGroup.getBloodDeductibleLiabilityAmount());
    assertEquals(new BigDecimal("66.89"), claimGroup.getProfessionalComponentCharge());
    assertEquals("R5555", claimGroup.getDiagnosisPrincipalCode().get());
    assertEquals('0', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());
    assertEquals("R5555", claimGroup.getDiagnosis1Code().get());
    assertEquals('0', claimGroup.getDiagnosis1CodeVersion().get().charValue());
    assertEquals(new BigDecimal("112.00"), claimGroup.getDeductibleAmount());
    assertEquals(new BigDecimal("175.73"), claimGroup.getCoinsuranceAmount());
    assertEquals(new BigDecimal("693.92"), claimGroup.getProviderPaymentAmount());
    assertEquals(new BigDecimal("44.00"), claimGroup.getBeneficiaryPaymentAmount());
    assertEquals("32490593716374487", claimGroup.getFiDocumentClaimControlNumber().get());
    assertEquals("373273882012", claimGroup.getFiOriginalClaimControlNumber().get());

    assertEquals(1, claimGroup.getLines().size());
    // Verify one of the claim lines.
    OutpatientClaimLine claimLine = claimGroup.getLines().get(0);
    assertEquals(new BigDecimal(25), claimLine.getLineNumber());
    assertEquals("M99", claimGroup.getLines().get(0).getHcpcsCode().get());
    assertEquals("XX", claimGroup.getLines().get(0).getHcpcsInitialModifierCode().get());
    assertFalse(claimLine.getHcpcsSecondModifierCode().isPresent());
    assertEquals(new BigDecimal("10.45"), claimGroup.getLines().get(0).getBloodDeductibleAmount());
    assertEquals(new BigDecimal("12.89"), claimGroup.getLines().get(0).getCashDeductibleAmount());
    assertEquals(new BigDecimal(5000.00), claimGroup.getLines().get(0).getPaymentAmount());
    assertEquals(new BigDecimal(134.00), claimGroup.getLines().get(0).getNonCoveredChargeAmount());
    assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_SNF}.
   */
  @Test
  public void process1SNFClaimRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_SNF.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_SNF.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_SNF.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof SNFClaim);

    // Verify the claim header.
    SNFClaim claimGroup = (SNFClaim) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(claimGroup.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(567834L, claimGroup.getBeneficiaryId());
    assertEquals(777777777L, claimGroup.getClaimId());
    assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
    assertEquals('V', claimGroup.getNearLineRecordIdCode());
    assertEquals("20", claimGroup.getClaimTypeCode());
    assertEquals(LocalDate.of(2013, 12, 01), claimGroup.getDateFrom());
    assertEquals(LocalDate.of(2013, 12, 18), claimGroup.getDateThrough());
    assertEquals('3', claimGroup.getClaimQueryCode());
    assertEquals("299999", claimGroup.getProviderNumber());
    assertEquals("B", claimGroup.getClaimNonPaymentReasonCode().get());
    assertEquals('1', claimGroup.getClaimServiceClassificationTypeCode());
    assertEquals(new BigDecimal("3333.33"), claimGroup.getPaymentAmount());
    assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
    assertEquals("FL", claimGroup.getProviderStateCode());
    assertEquals("1111111111", claimGroup.getOrganizationNpi().get());
    assertEquals("2222222222", claimGroup.getAttendingPhysicianNpi().get());
    assertEquals("3333333333", claimGroup.getOperatingPhysicianNpi().get());
    assertEquals("4444444444", claimGroup.getOtherPhysicianNpi().get());
    assertEquals("1", claimGroup.getPatientDischargeStatusCode());
    assertEquals(new BigDecimal("5555.03"), claimGroup.getTotalChargeAmount());
    assertEquals(LocalDate.of(2013, 11, 5), claimGroup.getClaimAdmissionDate().get());
    assertEquals('3', claimGroup.getAdmissionTypeCd());
    assertEquals('4', claimGroup.getSourceAdmissionCd().get().charValue());
    assertEquals(new BigDecimal("112.00"), claimGroup.getDeductibleAmount());
    assertEquals(new BigDecimal("5.00"), claimGroup.getPartACoinsuranceLiabilityAmount());
    assertEquals(new BigDecimal("6.00"), claimGroup.getBloodDeductibleLiabilityAmount());
    assertEquals(new BigDecimal("33.00"), claimGroup.getNoncoveredCharge());
    assertEquals(new BigDecimal("14.00"), claimGroup.getTotalDeductionAmount());
    assertEquals(new BigDecimal("9.00"), claimGroup.getClaimPPSCapitalFSPAmount().get());
    assertEquals(new BigDecimal("8.00"), claimGroup.getClaimPPSCapitalOutlierAmount().get());
    assertEquals(
        new BigDecimal("7.00"), claimGroup.getClaimPPSCapitalDisproportionateShareAmt().get());
    assertEquals(new BigDecimal("6.00"), claimGroup.getClaimPPSCapitalIMEAmount().get());
    assertEquals(new BigDecimal("5.00"), claimGroup.getClaimPPSCapitalExceptionAmount().get());
    assertEquals(
        new BigDecimal("4.00"), claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount().get());
    assertEquals(new BigDecimal(17), claimGroup.getUtilizationDayCount());
    assertEquals(new BigDecimal(17), claimGroup.getCoinsuranceDayCount());
    assertEquals(new BigDecimal(0), claimGroup.getNonUtilizationDayCount());
    assertEquals(new BigDecimal(19), claimGroup.getBloodPintsFurnishedQty());
    assertEquals(LocalDate.of(2013, 9, 23), claimGroup.getQualifiedStayFromDate().get());
    assertEquals(LocalDate.of(2013, 11, 5), claimGroup.getQualifiedStayThroughDate().get());
    assertEquals(LocalDate.of(2002, 1, 11), claimGroup.getNoncoveredStayFromDate().get());
    assertEquals(LocalDate.of(2002, 1, 21), claimGroup.getNoncoveredStayThroughDate().get());
    assertFalse(claimGroup.getCoveredCareThroughDate().isPresent());
    assertEquals(LocalDate.of(2002, 1, 31), claimGroup.getMedicareBenefitsExhaustedDate().get());
    assertEquals(LocalDate.of(2013, 12, 18), claimGroup.getBeneficiaryDischargeDate().get());

    assertEquals("R4444", claimGroup.getDiagnosisAdmittingCode().get());
    assertEquals('9', claimGroup.getDiagnosisAdmittingCodeVersion().get().charValue());
    assertEquals("R2222", claimGroup.getDiagnosisExternal1Code().get());
    assertEquals('9', claimGroup.getDiagnosisExternal1CodeVersion().get().charValue());
    assertEquals("23443453453", claimGroup.getFiDocumentClaimControlNumber().get());
    assertEquals("34534535535", claimGroup.getFiOriginalClaimControlNumber().get());

    assertEquals(1, claimGroup.getLines().size());
    // Verify one of the claim lines.
    SNFClaimLine claimLine = claimGroup.getLines().get(0);
    assertEquals("MMM", claimLine.getHcpcsCode().get());
    assertEquals(new BigDecimal("95.00"), claimLine.getTotalChargeAmount());
    assertEquals(new BigDecimal("88.00"), claimLine.getNonCoveredChargeAmount());
    assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_HOSPICE}.
   */
  @Test
  public void process1HospiceClaimRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_HOSPICE.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_HOSPICE.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_HOSPICE.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof HospiceClaim);

    // Verify the claim header.
    HospiceClaim claimGroup = (HospiceClaim) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(claimGroup.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(567834L, claimGroup.getBeneficiaryId());
    assertEquals(9992223422L, claimGroup.getClaimId());
    assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
    assertEquals('V', claimGroup.getNearLineRecordIdCode());
    assertEquals("50", claimGroup.getClaimTypeCode());
    assertEquals(LocalDate.of(2014, 1, 01), claimGroup.getDateFrom());
    assertEquals(LocalDate.of(2014, 1, 30), claimGroup.getDateThrough());
    assertEquals("12345", claimGroup.getProviderNumber());
    assertEquals("P", claimGroup.getClaimNonPaymentReasonCode().get());
    assertEquals('1', claimGroup.getClaimServiceClassificationTypeCode());
    assertEquals(new BigDecimal("130.32"), claimGroup.getPaymentAmount());
    assertEquals(new BigDecimal("0"), claimGroup.getPrimaryPayerPaidAmount());
    assertEquals("AZ", claimGroup.getProviderStateCode());
    assertEquals("999999999", claimGroup.getOrganizationNpi().get());
    assertEquals("8888888888", claimGroup.getAttendingPhysicianNpi().get());
    assertEquals("30", claimGroup.getPatientDischargeStatusCode());
    assertEquals(new BigDecimal("199.99"), claimGroup.getTotalChargeAmount());
    assertEquals(new Character('C'), claimGroup.getPatientStatusCd().get());
    assertEquals(new BigDecimal(30), claimGroup.getUtilizationDayCount());
    assertEquals(LocalDate.of(2015, 6, 29), claimGroup.getBeneficiaryDischargeDate().get());
    assertEquals("R5555", claimGroup.getDiagnosisPrincipalCode().get());
    assertEquals('9', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());
    assertEquals(LocalDate.of(2014, 7, 06), claimGroup.getClaimHospiceStartDate().get());
    assertEquals("38875439343923937", claimGroup.getFiOriginalClaimControlNumber().get());
    assertEquals("2718813985998", claimGroup.getFiDocumentClaimControlNumber().get());

    assertEquals(1, claimGroup.getLines().size());
    // Verify one of the claim lines.
    HospiceClaimLine claimLine = claimGroup.getLines().get(0);
    assertEquals(new BigDecimal(1), claimLine.getLineNumber());
    assertEquals("651", claimLine.getRevenueCenterCode());
    assertEquals(new BigDecimal("26.00"), claimGroup.getLines().get(0).getPaymentAmount());
    assertEquals(
        new BigDecimal("300.00"), claimGroup.getLines().get(0).getNonCoveredChargeAmount().get());
    assertEquals("Q9999", claimGroup.getLines().get(0).getHcpcsInitialModifierCode().get());
    assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_HHA}.
   */
  @Test
  public void process1HHAClaimRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_HHA.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_HHA.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_HHA.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof HHAClaim);

    // Verify the claim header.
    HHAClaim claimGroup = (HHAClaim) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(claimGroup.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(567834L, claimGroup.getBeneficiaryId());
    assertEquals(2925555555L, claimGroup.getClaimId());
    assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
    assertEquals('W', claimGroup.getNearLineRecordIdCode());
    assertEquals("10", claimGroup.getClaimTypeCode());
    assertEquals('2', claimGroup.getClaimServiceClassificationTypeCode());
    assertEquals(LocalDate.of(2015, 6, 23), claimGroup.getDateFrom());
    assertEquals(LocalDate.of(2015, 6, 23), claimGroup.getDateThrough());
    assertEquals("45645", claimGroup.getProviderNumber());
    assertEquals("P", claimGroup.getClaimNonPaymentReasonCode().get());
    assertEquals(new BigDecimal("188.00"), claimGroup.getPaymentAmount());
    assertEquals(new BigDecimal("11.00"), claimGroup.getPrimaryPayerPaidAmount());
    assertEquals("UT", claimGroup.getProviderStateCode());
    assertEquals("1811111111", claimGroup.getOrganizationNpi().get());
    assertEquals("2222222222", claimGroup.getAttendingPhysicianNpi().get());
    assertEquals("30", claimGroup.getPatientDischargeStatusCode());
    assertEquals(new BigDecimal("199.99"), claimGroup.getTotalChargeAmount());
    assertEquals("H5555", claimGroup.getDiagnosisPrincipalCode().get());
    assertEquals('9', claimGroup.getDiagnosisPrincipalCodeVersion().get().charValue());
    assertEquals('L', claimGroup.getClaimLUPACode().get().charValue());
    assertEquals('1', claimGroup.getClaimReferralCode().get().charValue());
    assertEquals(new BigDecimal(3), claimGroup.getTotalVisitCount());
    assertEquals(LocalDate.of(2015, 6, 23), claimGroup.getCareStartDate().get());
    // assertEquals("308683096577486", claimGroup.getFiDocumentClaimControlNumber().get());
    // assertEquals("10493204767560565", claimGroup.getFiOriginalClaimControlNumber().get());

    assertEquals(1, claimGroup.getLines().size());
    // Verify one of the claim lines.
    HHAClaimLine claimLine = claimGroup.getLines().get(0);
    assertEquals(new BigDecimal(1), claimLine.getLineNumber());
    assertEquals("0023", claimLine.getRevenueCenterCode());
    assertEquals(new BigDecimal("26.00"), claimGroup.getLines().get(0).getPaymentAmount());
    assertEquals(new BigDecimal("24.00"), claimGroup.getLines().get(0).getNonCoveredChargeAmount());
    assertEquals(new BigDecimal("25.00"), claimGroup.getLines().get(0).getTotalChargeAmount());
    assertEquals("345345345", claimLine.getRevenueCenterRenderingPhysicianNPI().get());
  }

  /**
   * Ensures that {@link RifFilesProcessor} can correctly handle {@link
   * StaticRifResource#SAMPLE_A_DME}.
   */
  @Test
  public void process1DMEClaimRecord() {
    RifFilesEvent filesEvent =
        new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_DME.toRifFile());
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());

    assertEquals(StaticRifResource.SAMPLE_A_DME.getRecordCount(), rifEventsList.size());

    RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
    assertEquals(
        StaticRifResource.SAMPLE_A_DME.getRifFileType(),
        rifRecordEvent.getFileEvent().getFile().getFileType());
    assertNotNull(rifRecordEvent.getRecord());
    assertTrue(rifRecordEvent.getRecord() instanceof DMEClaim);

    // Verify the claim header.
    DMEClaim claimGroup = (DMEClaim) rifRecordEvent.getRecord();
    assertEquals(RecordAction.INSERT, rifRecordEvent.getRecordAction());
    assertEquals(claimGroup.getBeneficiaryId(), rifRecordEvent.getBeneficiaryId());
    assertEquals(567834L, claimGroup.getBeneficiaryId());
    assertEquals(2188888888L, claimGroup.getClaimId());
    assertEquals(new BigDecimal(900), claimGroup.getClaimGroupId());
    assertEquals('M', claimGroup.getNearLineRecordIdCode());
    assertEquals("82", claimGroup.getClaimTypeCode());
    assertEquals(LocalDate.of(2014, 02, 03), claimGroup.getDateFrom());
    assertEquals(LocalDate.of(2014, 02, 03), claimGroup.getDateThrough());
    assertEquals(LocalDate.of(2014, 02, 14), claimGroup.getWeeklyProcessDate());
    assertEquals('1', claimGroup.getClaimEntryCode());
    assertEquals("01", claimGroup.getClaimDispositionCode());
    assertEquals("99999", claimGroup.getCarrierNumber());
    assertEquals("1", claimGroup.getPaymentDenialCode());
    assertEquals(new BigDecimal("777.75"), claimGroup.getPaymentAmount());
    assertEquals(new BigDecimal("0"), claimGroup.getPrimaryPayerPaidAmount());
    assertEquals('A', claimGroup.getProviderAssignmentIndicator());
    assertEquals(new BigDecimal("666.75"), claimGroup.getProviderPaymentAmount());
    assertEquals(new BigDecimal("666.66"), claimGroup.getBeneficiaryPaymentAmount());
    assertEquals(new BigDecimal("1752.75"), claimGroup.getSubmittedChargeAmount());
    assertEquals(new BigDecimal("754.79"), claimGroup.getAllowedChargeAmount());
    assertEquals(new BigDecimal("777.00"), claimGroup.getBeneficiaryPartBDeductAmount());
    assertEquals('3', claimGroup.getHcpcsYearCode().get().charValue());
    assertEquals("R5555", claimGroup.getDiagnosis1Code().get());
    assertEquals('0', claimGroup.getDiagnosis1CodeVersion().get().charValue());
    assertEquals("1306849450", claimGroup.getReferringPhysicianNpi().get());
    assertEquals("0", claimGroup.getClinicalTrialNumber().get());
    assertEquals(1, claimGroup.getLines().size());
    assertEquals("74655592568216", claimGroup.getClaimCarrierControlNumber().get());

    // Verify one of the claim lines.
    DMEClaimLine claimLine = claimGroup.getLines().get(0);

    assertEquals(new BigDecimal(1), claimLine.getLineNumber());
    assertEquals("9994931888", claimLine.getProviderTaxNumber());
    assertEquals("A5", claimLine.getProviderSpecialityCode().get());
    assertEquals('1', claimLine.getProviderParticipatingIndCode().get().charValue());
    assertEquals(new BigDecimal("60"), claimLine.getServiceCount());
    assertEquals('P', claimLine.getCmsServiceTypeCode());
    assertEquals("12", claimLine.getPlaceOfServiceCode());
    assertEquals(LocalDate.of(2014, 02, 03), claimLine.getFirstExpenseDate().get());
    assertEquals(LocalDate.of(2014, 02, 03), claimLine.getLastExpenseDate().get());
    assertEquals("345", claimLine.getHcpcsCode().get());
    assertFalse(claimLine.getHcpcsSecondModifierCode().isPresent());
    assertFalse(claimLine.getHcpcsThirdModifierCode().isPresent());
    assertFalse(claimLine.getHcpcsFourthModifierCode().isPresent());
    assertEquals("D9Z", claimLine.getBetosCode().get());
    assertEquals(new BigDecimal("123.45"), claimLine.getPaymentAmount());
    assertEquals(new BigDecimal("11.00"), claimLine.getBeneficiaryPaymentAmount());
    assertEquals(new BigDecimal("120.00"), claimLine.getProviderPaymentAmount());
    assertEquals(new BigDecimal("18.00"), claimLine.getBeneficiaryPartBDeductAmount());
    assertTrue(claimLine.getPrimaryPayerCode().isPresent());
    assertEquals(new BigDecimal("11.00"), claimLine.getPrimaryPayerPaidAmount());
    assertEquals(new BigDecimal("20.20"), claimLine.getCoinsuranceAmount());
    assertEquals(new BigDecimal("20.29"), claimLine.getPrimaryPayerAllowedChargeAmount());
    assertEquals(new BigDecimal("130.45"), claimLine.getSubmittedChargeAmount());
    assertEquals(new BigDecimal("129.45"), claimLine.getAllowedChargeAmount());
    assertEquals("A", claimLine.getProcessingIndicatorCode().get());
    assertEquals('0', claimLine.getPaymentCode().get().charValue());
    assertEquals('0', claimLine.getServiceDeductibleCode().get().charValue());
    assertEquals(new BigDecimal("82.29"), claimLine.getPurchasePriceAmount());
    assertEquals("1244444444", claimLine.getProviderNPI().get());
    assertEquals("AL", claimLine.getPricingStateCode().get());
    assertEquals("MO", claimLine.getProviderStateCode());
    assertEquals(new Character('3'), claimLine.getSupplierTypeCode().get());
    assertEquals(new BigDecimal("0.00"), claimLine.getScreenSavingsAmount().get());
    assertEquals(new BigDecimal("60"), claimLine.getMtusCount());
    assertEquals('3', claimLine.getMtusCode().get().charValue());
    assertEquals(new BigDecimal("44.4"), claimLine.getHctHgbTestResult());
    assertEquals("R2", claimLine.getHctHgbTestTypeCode().get());
    assertEquals("500904610", claimLine.getNationalDrugCode().get());
  }
}
