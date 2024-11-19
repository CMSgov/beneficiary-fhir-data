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
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
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

  /** A SAMHSA code to use in the tests. */
  private static final String TEST_SAMHSA_CODE = "H0005";

  /** Test setup. */
  @BeforeEach
  void setup() {
    samhsaUtil = SamhsaUtil.getSamhsaUtil();
    entityManager = mock(EntityManager.class);
  }

  /** This test should return a SAMHSA code entry for the given code. */
  @Test
  public void shouldReturnSamhsaEntry() {
    Optional<SamhsaEntry> entry = samhsaUtil.getSamhsaCode(Optional.of(TEST_SAMHSA_CODE));
    assertTrue(entry.isPresent());
  }

  /** This test should create FISS Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveFissTags() {
    ArgumentCaptor<FissTag> captor = ArgumentCaptor.forClass(FissTag.class);
    RdaFissClaim fissClaim = getSAMHSAFissClaim();
    samhsaUtil.processClaim(fissClaim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<FissTag> tags = captor.getAllValues();
    assertEquals(tags.stream().filter(t -> t.getClaim().equals(fissClaim.getClaimId())).count(), 2);
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveFissTag() {
    RdaFissClaim fissClaim = getNonSAMHSAFissClaim();
    ArgumentCaptor<FissTag> captor = ArgumentCaptor.forClass(FissTag.class);
    samhsaUtil.processClaim(fissClaim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create MCS Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveMcsTag() {
    RdaMcsClaim mcsClaim = getSAMHSAMcsClaim();
    ArgumentCaptor<McsTag> captor = ArgumentCaptor.forClass(McsTag.class);
    samhsaUtil.processClaim(mcsClaim, entityManager);
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
    samhsaUtil.processClaim(carrierClaim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<CarrierTag> tags = captor.getAllValues();
    assertEquals(
        2, tags.stream().filter(t -> t.getClaim().equals(carrierClaim.getClaimId())).count());
    assertEquals(4, tags.getFirst().getDetails().size());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveCarrierTag() {
    CarrierClaim carrierClaim = getNonSamhsaCarrierClaim();
    ArgumentCaptor<CarrierTag> captor = ArgumentCaptor.forClass(CarrierTag.class);
    samhsaUtil.processClaim(carrierClaim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveMcsTag() {
    RdaMcsClaim mcsClaim = getNonSAMHSAMcsClaim();
    ArgumentCaptor<McsTag> captor = ArgumentCaptor.forClass(McsTag.class);
    samhsaUtil.processClaim(mcsClaim, entityManager);
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
                        .idrDiagCode("F10.26")
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
            .diagnosis1Code("F10.29")
            .lines(
                List.of(
                    CarrierClaimLine.builder()
                        .diagnosisCode("F10.27")
                        .lineNumber((short) 1)
                        .hcpcsCode("H0006")
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
}
