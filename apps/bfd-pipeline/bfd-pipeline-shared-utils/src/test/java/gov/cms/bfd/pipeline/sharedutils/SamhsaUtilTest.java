package gov.cms.bfd.pipeline.sharedutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.model.rda.samhsa.FissTag;
import gov.cms.bfd.model.rda.samhsa.McsTag;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.model.rif.samhsa.DmeTag;
import gov.cms.bfd.model.rif.samhsa.HhaTag;
import gov.cms.bfd.model.rif.samhsa.HospiceTag;
import gov.cms.bfd.model.rif.samhsa.InpatientTag;
import gov.cms.bfd.model.rif.samhsa.OutpatientTag;
import gov.cms.bfd.model.rif.samhsa.SnfTag;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaEntry;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit tests for SamhsaUtil class. */
public class SamhsaUtilTest {
  /** An instance of SamhsaUtil. */
  SamhsaUtil samhsaUtil;

  /** Entity manager that will be mocked. */
  EntityManager entityManager;

  /** A SAMHSA hcpcs code to use in the tests. */
  private static final String TEST_SAMHSA_HCPCS_CODE = "H0005";

  /** HCPCS column to use in the tests. */
  private static final String TEST_SAMHSA_HCPCS_COLUMN = "hcpcs_code";

  /** A SAMHSA procedure code to use in the tests. */
  private static final String TEST_SAMHSA_PROC_CODE = "HZ2ZZZZ";

  /** Procedure column to use in the tests. */
  private static final String TEST_SAMHSA_PROC_COLUMN = "proc_code";

  /** A SAMHSA drug code to use in the tests. */
  private static final String TEST_SAMHSA_DRG_CODE = "522";

  /** Drug column to use in the tests. */
  private static final String TEST_SAMHSA_DRG_COLUMN = "drg_code";

  /** A SAMHSA procedure code to use in the tests. */
  private static final String TEST_SAMHSA_DIAG_CODE = "F10.10";

  /** Procedure column to use in the tests. */
  private static final String TEST_SAMHSA_DIAG_COLUMN = "diag_code";

  /** Test setup. */
  @BeforeEach
  void setup() {
    samhsaUtil = SamhsaUtil.getSamhsaUtil();
    entityManager = mock(EntityManager.class);
  }

  /** This test should return a SAMHSA HCPCS code entry for the given code. */
  @Test
  public void shouldReturnSamhsaHCPCS() {
    Optional<SamhsaEntry> entry =
        samhsaUtil.getSamhsaCode(Optional.of(TEST_SAMHSA_HCPCS_CODE), Optional.of(TEST_SAMHSA_HCPCS_COLUMN));

    assertTrue(entry.isPresent(), "Expected a SAMHSA entry to be present");

    assertEquals("https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets",
            entry.get().getSystem(),
            "Unexpected system for HCPCS column");
  }

  /** This test should return a SAMHSA HCPCS code entry for the given code. */
  @Test
  public void shouldReturnSamhsaProcedure() {
    Optional<SamhsaEntry> entry =
            samhsaUtil.getSamhsaCode(Optional.of(TEST_SAMHSA_PROC_CODE), Optional.of(TEST_SAMHSA_PROC_COLUMN));

    assertTrue(entry.isPresent(), "Expected a SAMHSA entry to be present");

    assertEquals("http://www.cms.gov/Medicare/Coding/ICD10",
            entry.get().getSystem(),
            "Unexpected system for Procedure column");
  }

  /** This test should return a SAMHSA DRG code entry for the given code. */
  @Test
  public void shouldReturnSamhsaDrug() {
    Optional<SamhsaEntry> entry =
            samhsaUtil.getSamhsaCode(Optional.of(TEST_SAMHSA_DRG_CODE), Optional.of(TEST_SAMHSA_DRG_COLUMN));

    assertTrue(entry.isPresent(), "Expected a SAMHSA entry to be present");

    assertEquals("https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software",
            entry.get().getSystem(),
            "Unexpected system for DRG column");
  }

  /** This test should return a SAMHSA Diagnosis code entry for the given code. */
  @Test
  public void shouldReturnSamhsaDiagnosis() {
    Optional<SamhsaEntry> entry =
            samhsaUtil.getSamhsaCode(Optional.of(TEST_SAMHSA_DIAG_CODE), Optional.of(TEST_SAMHSA_DIAG_COLUMN));

    assertTrue(entry.isPresent(), "Expected a SAMHSA entry to be present");

    assertEquals("http://hl7.org/fhir/sid/icd-10-cm",
            entry.get().getSystem(),
            "Unexpected system for DIAG column");
  }

  /** This test should create FISS Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveFissTags() {
    ArgumentCaptor<FissTag> captor = ArgumentCaptor.forClass(FissTag.class);
    RdaFissClaim fissClaim = getSAMHSAFissClaim();
    samhsaUtil.processRdaClaim(fissClaim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<FissTag> tags = captor.getAllValues();
    assertEquals(tags.stream().filter(t -> t.getClaim().equals(fissClaim.getClaimId())).count(), 2);
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveFissTag() {
    RdaFissClaim fissClaim = getNonSAMHSAFissClaim();
    ArgumentCaptor<FissTag> captor = ArgumentCaptor.forClass(FissTag.class);
    samhsaUtil.processRdaClaim(fissClaim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create MCS Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveMcsTag() {
    RdaMcsClaim mcsClaim = getSAMHSAMcsClaim();
    ArgumentCaptor<McsTag> captor = ArgumentCaptor.forClass(McsTag.class);
    samhsaUtil.processRdaClaim(mcsClaim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<McsTag> tags = captor.getAllValues();
    assertEquals(
        tags.stream().filter(t -> t.getClaim().equals(mcsClaim.getIdrClmHdIcn())).count(), 2);
  }

  /** This test should create Carrier Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveCarrierTag() {
    CarrierClaim carrierClaim = getSamhsaCarrierClaim();
    ArgumentCaptor<CarrierTag> captor = ArgumentCaptor.forClass(CarrierTag.class);
    samhsaUtil.processCcwClaim(carrierClaim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<CarrierTag> tags = captor.getAllValues();
    assertEquals(
        2, tags.stream().filter(t -> t.getClaim().equals(carrierClaim.getClaimId())).count());
    assertEquals(5, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveCarrierTag() {
    CarrierClaim carrierClaim = getNonSamhsaCarrierClaim();
    ArgumentCaptor<CarrierTag> captor = ArgumentCaptor.forClass(CarrierTag.class);
    samhsaUtil.processCcwClaim(carrierClaim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveMcsTag() {
    RdaMcsClaim mcsClaim = getNonSAMHSAMcsClaim();
    ArgumentCaptor<McsTag> captor = ArgumentCaptor.forClass(McsTag.class);
    samhsaUtil.processRdaClaim(mcsClaim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create DME Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveDMETag() {
    DMEClaim claim = getSamhsaDMEClaim();
    ArgumentCaptor<DmeTag> captor = ArgumentCaptor.forClass(DmeTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<DmeTag> tags = captor.getAllValues();
    assertEquals(2, tags.stream().filter(t -> t.getClaim().equals(claim.getClaimId())).count());
    assertEquals(5, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveDMETag() {
    DMEClaim claim = getNonSamhsaDMEClaim();
    ArgumentCaptor<DmeTag> captor = ArgumentCaptor.forClass(DmeTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create HHA Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveHHATag() {
    HHAClaim claim = getSamhsaHHAClaim();
    ArgumentCaptor<HhaTag> captor = ArgumentCaptor.forClass(HhaTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<HhaTag> tags = captor.getAllValues();
    assertEquals(2, tags.stream().filter(t -> t.getClaim().equals(claim.getClaimId())).count());
    assertEquals(2, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveHHATag() {
    HHAClaim claim = getNonSamhsaHHAClaim();
    ArgumentCaptor<HhaTag> captor = ArgumentCaptor.forClass(HhaTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create Hospice Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveHospiceTag() {
    HospiceClaim claim = getSamhsaHospiceClaim();
    ArgumentCaptor<HospiceTag> captor = ArgumentCaptor.forClass(HospiceTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<HospiceTag> tags = captor.getAllValues();
    assertEquals(2, tags.stream().filter(t -> t.getClaim().equals(claim.getClaimId())).count());
    assertEquals(2, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveHospiceTag() {
    HospiceClaim claim = getNonSamhsaHospiceClaim();
    ArgumentCaptor<HospiceTag> captor = ArgumentCaptor.forClass(HospiceTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create SNF Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveSNFTag() {
    SNFClaim claim = getSamhsaSNFClaim();
    ArgumentCaptor<SnfTag> captor = ArgumentCaptor.forClass(SnfTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<SnfTag> tags = captor.getAllValues();
    assertEquals(2, tags.stream().filter(t -> t.getClaim().equals(claim.getClaimId())).count());
    assertEquals(2, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveSNFTag() {
    SNFClaim claim = getNonSamhsaSNFClaim();
    ArgumentCaptor<SnfTag> captor = ArgumentCaptor.forClass(SnfTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create Inpatient Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveInpatientTag() {
    InpatientClaim claim = getSamhsaInpatientlaim();
    ArgumentCaptor<InpatientTag> captor = ArgumentCaptor.forClass(InpatientTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<InpatientTag> tags = captor.getAllValues();
    assertEquals(2, tags.stream().filter(t -> t.getClaim().equals(claim.getClaimId())).count());
    assertEquals(2, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveInpatientTag() {
    InpatientClaim claim = getNonSamhsaInpatientlaim();
    ArgumentCaptor<InpatientTag> captor = ArgumentCaptor.forClass(InpatientTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create Outpatient Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveOutpatientTag() {
    OutpatientClaim claim = getSamhsaOutpatientClaim();
    ArgumentCaptor<OutpatientTag> captor = ArgumentCaptor.forClass(OutpatientTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<OutpatientTag> tags = captor.getAllValues();
    assertEquals(2, tags.stream().filter(t -> t.getClaim().equals(claim.getClaimId())).count());
    assertEquals(2, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveOutpatientTag() {
    OutpatientClaim claim = getNonSamhsaOutpatientClaim();
    ArgumentCaptor<OutpatientTag> captor = ArgumentCaptor.forClass(OutpatientTag.class);
    samhsaUtil.processCcwClaim(claim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /**
   * Gets a fake Samhsa MCS claim.
   *
   * @return a fake Samhsa MCS claim.
   */
  private RdaMcsClaim getSAMHSAMcsClaim() {
    RdaMcsClaim mcsClaim =
        RdaMcsClaim.builder()
            .idrClmHdIcn("456ABC")
            .lastUpdated(Instant.now())
            .diagCodes(
                Set.of(
                    RdaMcsDiagnosisCode.builder()
                        .idrClmHdIcn("456ABC")
                        .idrDiagCode("F1026")
                        .build()))
            .details(
                Set.of(
                    RdaMcsDetail.builder()
                        .idrDtlFromDate(LocalDate.parse("1970-01-01"))
                        .idrClmHdIcn("456ABC")
                        .idrProcCode("H0006")
                        .build()))
            .build();
    return mcsClaim;
  }

  /**
   * Gets a fake Non-Samhsa MCS claim.
   *
   * @return a fake Non-Samhsa MCS claim.
   */
  private RdaMcsClaim getNonSAMHSAMcsClaim() {
    RdaMcsClaim mcsClaim =
        RdaMcsClaim.builder()
            .idrClmHdIcn("456ABC")
            .lastUpdated(Instant.now())
            .diagCodes(
                Set.of(
                    RdaMcsDiagnosisCode.builder()
                        .idrClmHdIcn("456ABC")
                        .idrDiagCode("NONSAMHSA")
                        .build()))
            .details(
                Set.of(
                    RdaMcsDetail.builder()
                        .idrDtlFromDate(LocalDate.parse("1970-01-01"))
                        .idrClmHdIcn("456ABC")
                        .idrProcCode("NONSAMHSA")
                        .build()))
            .build();
    return mcsClaim;
  }

  /**
   * Gets a fake Samhsa FISS claim.
   *
   * @return A fake Samhsa FISS claim.
   */
  private RdaFissClaim getSAMHSAFissClaim() {
    RdaFissClaim fissClaim =
        RdaFissClaim.builder()
            .claimId("XYZ123")
            .lastUpdated(Instant.now())
            .admitDiagCode("F10.21")
            .revenueLines(
                Set.of(
                    RdaFissRevenueLine.builder()
                        .claimId("XYZ123")
                        .serviceDate(LocalDate.parse("1970-01-01"))
                        .hcpcCd("H0007")
                        .rdaPosition((short) 1)
                        .build()))
            .diagCodes(
                Set.of(
                    RdaFissDiagnosisCode.builder()
                        .claimId("XYZ123")
                        .rdaPosition((short) 1)
                        .build()))
            .procCodes(
                Set.of(RdaFissProcCode.builder().claimId("XYZ123").rdaPosition((short) 1).build()))
            .build();
    return fissClaim;
  }

  /**
   * Gets a fake Non-Samhsa FISS claim.
   *
   * @return A fake Non-Samhsa FISS claim.
   */
  private RdaFissClaim getNonSAMHSAFissClaim() {
    RdaFissClaim fissClaim =
        RdaFissClaim.builder()
            .claimId("XYZ123")
            .lastUpdated(Instant.now())
            .admitDiagCode("NONSAMHSA")
            .revenueLines(
                Set.of(
                    RdaFissRevenueLine.builder()
                        .claimId("XYZ123")
                        .serviceDate(LocalDate.parse("1970-01-01"))
                        .hcpcCd("NONSAMHSA")
                        .rdaPosition((short) 1)
                        .build()))
            .diagCodes(
                Set.of(
                    RdaFissDiagnosisCode.builder()
                        .claimId("XYZ123")
                        .rdaPosition((short) 1)
                        .build()))
            .procCodes(
                Set.of(RdaFissProcCode.builder().claimId("XYZ123").rdaPosition((short) 1).build()))
            .build();
    return fissClaim;
  }

  /**
   * Gets a fake Samhsa Carrier claim.
   *
   * @return A fake Samhsa Carrier claim.
   */
  public CarrierClaim getSamhsaCarrierClaim() {
    CarrierClaim claim =
        CarrierClaim.builder()
            .claimId(1234567890)
            .diagnosisPrincipalCode("F10.26")
            .diagnosis1Code("F1029")
            .lines(
                List.of(
                    CarrierClaimLine.builder()
                        .diagnosisCode("F10.27")
                        .lineNumber((short) 1)
                        .hcpcsCode("H0006")
                        .build(),
                    CarrierClaimLine.builder()
                        .diagnosisCode("F10.27")
                        .lineNumber((short) 2)
                        .build()))
            .dateFrom(LocalDate.parse("1970-01-01"))
            .dateThrough(LocalDate.now())
            .build();
    return claim;
  }

  /**
   * Gets a fake Non-Samhsa Carrier claim.
   *
   * @return A fake Non-Samhsa Carrier claim.
   */
  public CarrierClaim getNonSamhsaCarrierClaim() {
    CarrierClaim claim =
        CarrierClaim.builder()
            .claimId(1234567890)
            .diagnosisPrincipalCode("NONSAMHSA")
            .diagnosis1Code("NONSAMHSA")
            .lines(
                List.of(
                    CarrierClaimLine.builder()
                        .diagnosisCode("NONSAMHSA")
                        .lineNumber((short) 1)
                        .hcpcsCode("NONSAMHSA")
                        .build()))
            .dateFrom(LocalDate.parse("1970-01-01"))
            .dateThrough(LocalDate.now())
            .build();
    return claim;
  }

  /**
   * Gets a fake Samhsa DME claim.
   *
   * @return A fake Samhsa DME claim.
   */
  public DMEClaim getSamhsaDMEClaim() {
    DMEClaim claim =
        DMEClaim.builder()
            .claimId(1234567890)
            .diagnosisPrincipalCode("F1026")
            .diagnosis1Code("F1029")
            .lines(
                List.of(
                    DMEClaimLine.builder()
                        .diagnosisCode("F1027")
                        .lineNumber((short) 1)
                        .hcpcsCode("H0006")
                        .build(),
                    DMEClaimLine.builder().diagnosisCode("F1027").lineNumber((short) 2).build()))
            .dateFrom(LocalDate.parse("1970-01-01"))
            .dateThrough(LocalDate.now())
            .build();
    return claim;
  }

  /**
   * Gets a fake Non-Samhsa DME claim.
   *
   * @return A fake Non-Samhsa DME claim.
   */
  public DMEClaim getNonSamhsaDMEClaim() {
    DMEClaim claim =
        DMEClaim.builder()
            .claimId(1234567890)
            .diagnosisPrincipalCode("NONSAMHSA")
            .diagnosis1Code("NONSAMHSA")
            .lines(
                List.of(
                    DMEClaimLine.builder()
                        .diagnosisCode("NONSAMHSA")
                        .lineNumber((short) 1)
                        .hcpcsCode("NONSAMHSA")
                        .build()))
            .dateFrom(LocalDate.parse("1970-01-01"))
            .dateThrough(LocalDate.now())
            .build();
    return claim;
  }

  /**
   * Gets a fake Non-Samhsa HHA claim.
   *
   * @return A fake Non-Samhsa HHA claim.
   */
  public HHAClaim getNonSamhsaHHAClaim() {
    HHAClaim claim = new HHAClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("NONSAMHSA"));
    claim.setDiagnosis1Code(Optional.of("NONSAMHSA"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake Samhsa HHA claim.
   *
   * @return A fake Samhsa HHA claim.
   */
  public HHAClaim getSamhsaHHAClaim() {
    HHAClaim claim = new HHAClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("F10.26"));
    claim.setDiagnosis1Code(Optional.of("F1026"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake Samhsa Hospice claim.
   *
   * @return A fake Samhsa Hospice claim.
   */
  public HospiceClaim getSamhsaHospiceClaim() {
    HospiceClaim claim = new HospiceClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("F10.26"));
    claim.setDiagnosis1Code(Optional.of("F1026"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake non-Samhsa Hospice claim.
   *
   * @return A fake non-Samhsa Hospice claim.
   */
  public HospiceClaim getNonSamhsaHospiceClaim() {
    HospiceClaim claim = new HospiceClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("NONSAMHSA"));
    claim.setDiagnosis1Code(Optional.of("NONSAMHSA"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake Samhsa SNF claim.
   *
   * @return A fake Samhsa SNF claim.
   */
  public SNFClaim getSamhsaSNFClaim() {
    SNFClaim claim = new SNFClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("F1026"));
    claim.setDiagnosis1Code(Optional.of("F1026"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake non-Samhsa SNF claim.
   *
   * @return A fake non-Samhsa SNF claim.
   */
  public SNFClaim getNonSamhsaSNFClaim() {
    SNFClaim claim = new SNFClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("NONSAMHSA"));
    claim.setDiagnosis1Code(Optional.of("NONSAMHSA"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake Samhsa Outpatient claim.
   *
   * @return A fake Samhsa Outpatient claim.
   */
  public OutpatientClaim getSamhsaOutpatientClaim() {
    OutpatientClaim claim = new OutpatientClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("F1026"));
    claim.setDiagnosis1Code(Optional.of("F1026"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake non-Samhsa Outpatient claim.
   *
   * @return A fake non-Samhsa Outpatient claim.
   */
  public OutpatientClaim getNonSamhsaOutpatientClaim() {
    OutpatientClaim claim = new OutpatientClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("NONSAMHSA"));
    claim.setDiagnosis1Code(Optional.of("NONSAMHSA"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake Samhsa Inpatient claim.
   *
   * @return A fake Samhsa Inpatient claim.
   */
  public InpatientClaim getSamhsaInpatientlaim() {
    InpatientClaim claim = new InpatientClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("F1026"));
    claim.setDiagnosis1Code(Optional.of("F1026"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }

  /**
   * Gets a fake non-Samhsa Inpatient claim.
   *
   * @return A fake non-Samhsa Inpatient claim.
   */
  public InpatientClaim getNonSamhsaInpatientlaim() {
    InpatientClaim claim = new InpatientClaim();
    claim.setClaimId(1234567890);
    claim.setDiagnosisPrincipalCode(Optional.of("NONSAMHSA"));
    claim.setDiagnosis1Code(Optional.of("NONSAMHSA"));
    claim.setDateFrom(LocalDate.parse("1970-01-01"));
    claim.setDateThrough(LocalDate.now());

    return claim;
  }
}
