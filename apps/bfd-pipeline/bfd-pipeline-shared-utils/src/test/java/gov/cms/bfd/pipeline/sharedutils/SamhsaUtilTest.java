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
import gov.cms.bfd.model.rda.samhsa.SamhsaEntry;
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

  /** Test Fiss Claim. */
  RdaFissClaim fissClaim;

  /** Test MCS Claim. */
  RdaMcsClaim mcsClaim;

  /** Entity manager that will be mocked. */
  EntityManager entityManager;

  /** A SAMHSA code to use in the tests. */
  private static final String TEST_SAMHSA_CODE = "H0005";

  /** Test setup. */
  @BeforeEach
  void setup() {
    samhsaUtil = SamhsaUtil.getSamhsaUtil();
    entityManager = mock(EntityManager.class);
    fissClaim = getSAMHSAFissClaim();
    mcsClaim = getSAMHSAMcsClaim();
  }

  /** This test should return a SAMHSA code entry for the given code. */
  @Test
  public void shouldReturnSamhsaEntry() {
    Optional<SamhsaEntry> entry = samhsaUtil.isSamhsaCode(Optional.of(TEST_SAMHSA_CODE));
    assertTrue(entry.isPresent());
  }

  /** This test should create FISS Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveFissTags() {
    ArgumentCaptor<FissTag> captor = ArgumentCaptor.forClass(FissTag.class);
    samhsaUtil.processClaim(fissClaim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<FissTag> tags = captor.getAllValues();
    assertEquals(tags.stream().filter(t -> t.getClaim().equals(fissClaim.getClaimId())).count(), 2);
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveFissTag() {
    fissClaim.setAdmitDiagCode("NONSAMHSA");
    for (RdaFissRevenueLine revenueLine : fissClaim.getRevenueLines()) {
      revenueLine.setHcpcCd("NONSAMHSA");
    }
    ArgumentCaptor<FissTag> captor = ArgumentCaptor.forClass(FissTag.class);
    samhsaUtil.processClaim(fissClaim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /** This test should create MCS Tags and attempt to save them to the database. */
  @Test
  public void shouldSaveMcsTag() {
    ArgumentCaptor<McsTag> captor = ArgumentCaptor.forClass(McsTag.class);
    samhsaUtil.processClaim(mcsClaim, entityManager);
    verify(entityManager, times(2)).merge(captor.capture());
    List<McsTag> tags = captor.getAllValues();
    assertEquals(
        tags.stream().filter(t -> t.getClaim().equals(mcsClaim.getIdrClmHdIcn())).count(), 2);
  }

  /** This test should not try to save a tag. */
  @Test
  public void shouldNotSaveMcsTag() {
    for (RdaMcsDiagnosisCode diagCode : mcsClaim.getDiagCodes()) {
      diagCode.setIdrDiagCode("NONSAMHSA");
    }
    for (RdaMcsDetail detail : mcsClaim.getDetails()) {
      detail.setIdrProcCode("NONSAMHSA");
    }

    ArgumentCaptor<McsTag> captor = ArgumentCaptor.forClass(McsTag.class);
    samhsaUtil.processClaim(mcsClaim, entityManager);
    verify(entityManager, times(0)).merge(captor.capture());
  }

  /**
   * Gets a fake MCS claim.
   *
   * @return a fake MCS claim.
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
   * Gets a fake FISS claim.
   *
   * @return A fake FISS claim.
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
}
