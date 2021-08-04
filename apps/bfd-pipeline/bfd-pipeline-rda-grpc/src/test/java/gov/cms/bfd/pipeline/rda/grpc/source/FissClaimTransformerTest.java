package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class FissClaimTransformerTest {
  // using a fixed Clock ensures our timestamp is predictable
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(
          clock,
          new IdHasher(
              new IdHasher.Config(1000, "nottherealpepper".getBytes(StandardCharsets.UTF_8))));
  private ClaimChange.Builder changeBuilder;
  private FissClaim.Builder claimBuilder;
  private PreAdjFissClaim claim;

  @Before
  public void setUp() {
    changeBuilder = ClaimChange.newBuilder();
    claimBuilder = FissClaim.newBuilder();
    claim = new PreAdjFissClaim();
  }

  @Test
  public void minimumValidClaim() {
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('T');
    claim.setCurrLoc1('M');
    claim.setCurrLoc2("9000");
    claim.setLastUpdated(clock.instant());
    claimBuilder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP)
        .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
        .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_CABLE);
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_INSERT)
        .setFissClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  @Test
  public void allFields() {
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('M');
    claim.setCurrLoc1('M');
    claim.setCurrLoc2("9000");
    claim.setMedaProvId("mpi");
    claim.setMedaProv_6("mp_6");
    claim.setTotalChargeAmount(new BigDecimal("1002.54"));
    claim.setReceivedDate(LocalDate.of(2020, 1, 2));
    claim.setCurrTranDate(LocalDate.of(2021, 3, 4));
    claim.setAdmitDiagCode("1234567");
    claim.setPrincipleDiag("7654321");
    claim.setNpiNumber("npi-123456");
    claim.setMbi("1234567890123");
    claim.setMbiHash("d51b083f91c62eff93b6245bc8203bafa566f41b3553314d049059b8e55eea0d");
    claim.setFedTaxNumber("1234567890");
    claim.setPracLocAddr1("loc-address-1");
    claim.setPracLocAddr2("loc-address-2");
    claim.setPracLocCity("loc-city");
    claim.setPracLocState("ls");
    claim.setPracLocZip("123456789012345");
    claim.setStmtCovFromDate(LocalDate.of(2020, 2, 3));
    claim.setStmtCovToDate(LocalDate.of(2021, 4, 5));
    claim.setLastUpdated(clock.instant());
    claimBuilder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_MOVE)
        .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
        .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_CABLE)
        .setMedaProvId("mpi")
        .setMedaProv6("mp_6")
        .setTotalChargeAmount("1002.54")
        .setRecdDtCymd("2020-01-02")
        .setCurrTranDtCymd("2021-03-04")
        .setAdmDiagCode("1234567")
        .setPrincipleDiag("7654321")
        .setNpiNumber("npi-123456")
        .setMbi("1234567890123")
        .setFedTaxNb("1234567890")
        .setPracLocAddr1("loc-address-1")
        .setPracLocAddr2("loc-address-2")
        .setPracLocCity("loc-city")
        .setPracLocState("ls")
        .setPracLocZip("123456789012345")
        .setStmtCovFromCymd("2020-02-03")
        .setStmtCovToCymd("2021-04-05");
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
        .setFissClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.UPDATE);
  }

  @Test
  public void procCodes() {
    claimBuilder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_MOVE)
        .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
        .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_FINAL)
        .addFissProcCodes(
            FissProcedureCode.newBuilder().setProcCd("code-1").setProcFlag("fl-1").build())
        .addFissProcCodes(
            FissProcedureCode.newBuilder()
                .setProcCd("code-2")
                .setProcFlag("fl-2")
                .setProcDt("2021-07-06")
                .build());
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('M');
    claim.setCurrLoc1('M');
    claim.setCurrLoc2("2");
    claim.setLastUpdated(clock.instant());
    PreAdjFissProcCode code = new PreAdjFissProcCode();
    code.setDcn("dcn");
    code.setPriority((short) 0);
    code.setProcCode("code-1");
    code.setProcFlag("fl-1");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getProcCodes().add(code);
    code = new PreAdjFissProcCode();
    code.setDcn("dcn");
    code.setPriority((short) 1);
    code.setProcCode("code-2");
    code.setProcFlag("fl-2");
    code.setProcDate(LocalDate.of(2021, 7, 6));
    code.setLastUpdated(claim.getLastUpdated());
    claim.getProcCodes().add(code);
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
        .setFissClaim(claimBuilder.build());
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getProcCodes(), transformed.getProcCodes(), PreAdjFissProcCode::getPriority);
  }

  @Test
  public void diagCodes() {
    claimBuilder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_MOVE)
        .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
        .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_FINAL)
        .addFissDiagCodes(
            FissDiagnosisCode.newBuilder()
                .setDiagCd2("code-1")
                .setDiagPoaIndEnum(
                    FissDiagnosisPresentOnAdmissionIndicator
                        .DIAGNOSIS_PRESENT_ON_ADMISSION_INDICATOR_CLINICALLY_UNDETERMINED)
                .setBitFlags("1234")
                .build())
        .addFissDiagCodes(
            FissDiagnosisCode.newBuilder()
                .setDiagCd2("code-2")
                .setDiagPoaIndEnum(
                    FissDiagnosisPresentOnAdmissionIndicator
                        .DIAGNOSIS_PRESENT_ON_ADMISSION_INDICATOR_NO)
                .setBitFlags("4321")
                .build());
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('M');
    claim.setCurrLoc1('M');
    claim.setCurrLoc2("9997");
    claim.setLastUpdated(clock.instant());
    PreAdjFissDiagnosisCode code = new PreAdjFissDiagnosisCode();
    code.setDcn("dcn");
    code.setPriority((short) 0);
    code.setDiagCd2("code-1");
    code.setDiagPoaInd("W");
    code.setBitFlags("1234");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getDiagCodes().add(code);
    code = new PreAdjFissDiagnosisCode();
    code.setDcn("dcn");
    code.setPriority((short) 1);
    code.setDiagCd2("code-2");
    code.setDiagPoaInd("N");
    code.setBitFlags("4321");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getDiagCodes().add(code);
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
        .setFissClaim(claimBuilder.build());
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getDiagCodes(), transformed.getDiagCodes(), PreAdjFissDiagnosisCode::getPriority);
  }

  @Test
  public void requiredFieldsMissing() {
    try {
      changeBuilder
          .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
          .setFissClaim(claimBuilder.build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          Arrays.asList(
              new DataTransformer.ErrorMessage("dcn", "invalid length: expected=[1,23] actual=0"),
              new DataTransformer.ErrorMessage("hicNo", "invalid length: expected=[1,12] actual=0"),
              new DataTransformer.ErrorMessage("currStatus", "no value set"),
              new DataTransformer.ErrorMessage("currLoc1", "no value set"),
              new DataTransformer.ErrorMessage("currLoc2", "no value set")),
          ex.getErrors());
    }
  }

  @Test
  public void allBadFields() {
    try {
      claimBuilder
          .setDcn("123456789012345678901234")
          .setHicNo("1234567890123")
          .setCurrStatusEnumValue(-1)
          .setCurrLoc1EnumValue(-1)
          .setCurrLoc2Unrecognized("123456")
          .setMedaProvId("12345678901234")
          .setMedaProv6("1234567")
          .setTotalChargeAmount("not-a-number")
          .setRecdDtCymd("not-a-date")
          .setCurrTranDtCymd("not-a-date")
          .setAdmDiagCode("12345678")
          .setPrincipleDiag("12345678")
          .setNpiNumber("12345678901")
          .setMbi("12345678901234")
          .setFedTaxNb("12345678901")
          .setPracLocAddr1("")
          .setPracLocAddr2("")
          .setPracLocCity("")
          .setPracLocState("123")
          .setPracLocZip("1234567890123456")
          .setStmtCovFromCymd("not-a-date")
          .setStmtCovToCymd("not-a-date")
          .addFissProcCodes(
              FissProcedureCode.newBuilder()
                  .setProcCd("12345678901")
                  .setProcFlag("12345")
                  .setProcDt("not-a-date")
                  .build());
      changeBuilder
          .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
          .setFissClaim(claimBuilder.build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          Arrays.asList(
              new DataTransformer.ErrorMessage("dcn", "invalid length: expected=[1,23] actual=24"),
              new DataTransformer.ErrorMessage(
                  "hicNo", "invalid length: expected=[1,12] actual=13"),
              new DataTransformer.ErrorMessage("currStatus", "unrecognized enum value"),
              new DataTransformer.ErrorMessage("currLoc1", "unrecognized enum value"),
              new DataTransformer.ErrorMessage(
                  "currLoc2", "invalid length: expected=[1,5] actual=6"),
              new DataTransformer.ErrorMessage(
                  "medaProvId", "invalid length: expected=[1,13] actual=14"),
              new DataTransformer.ErrorMessage(
                  "medaProv_6", "invalid length: expected=[1,6] actual=7"),
              new DataTransformer.ErrorMessage("totalChargeAmount", "invalid amount"),
              new DataTransformer.ErrorMessage("receivedDate", "invalid date"),
              new DataTransformer.ErrorMessage("currTranDate", "invalid date"),
              new DataTransformer.ErrorMessage(
                  "admitDiagCode", "invalid length: expected=[1,7] actual=8"),
              new DataTransformer.ErrorMessage(
                  "principleDiag", "invalid length: expected=[1,7] actual=8"),
              new DataTransformer.ErrorMessage(
                  "npiNumber", "invalid length: expected=[1,10] actual=11"),
              new DataTransformer.ErrorMessage(
                  "pracLocAddr1", "invalid length: expected=[1,2147483647] actual=0"),
              new DataTransformer.ErrorMessage(
                  "pracLocAddr2", "invalid length: expected=[1,2147483647] actual=0"),
              new DataTransformer.ErrorMessage(
                  "pracLocCity", "invalid length: expected=[1,2147483647] actual=0"),
              new DataTransformer.ErrorMessage(
                  "pracLocState", "invalid length: expected=[1,2] actual=3"),
              new DataTransformer.ErrorMessage(
                  "pracLocZip", "invalid length: expected=[1,15] actual=16"),
              new DataTransformer.ErrorMessage("mbi", "invalid length: expected=[1,13] actual=14"),
              new DataTransformer.ErrorMessage(
                  "fedTaxNumber", "invalid length: expected=[1,10] actual=11"),
              new DataTransformer.ErrorMessage("stmtCovFromDate", "invalid date"),
              new DataTransformer.ErrorMessage("stmtCovToDate", "invalid date"),
              new DataTransformer.ErrorMessage(
                  "procCode-0-procCode", "invalid length: expected=[1,10] actual=11"),
              new DataTransformer.ErrorMessage(
                  "procCode-0-procFlag", "invalid length: expected=[1,4] actual=5"),
              new DataTransformer.ErrorMessage("procCode-0-procDate", "invalid date")),
          ex.getErrors());
    }
  }

  @Test
  public void unrecognizedCurrStatus() {
    try {
      claimBuilder
          .setDcn("dcn")
          .setHicNo("hicn")
          .setCurrStatusUnrecognized("X")
          .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
          .setCurrLoc2Unrecognized("9000");
      changeBuilder
          .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
          .setFissClaim(claimBuilder.build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          ImmutableList.of(
              new DataTransformer.ErrorMessage("currStatus", "unsupported enum value")),
          ex.getErrors());
    }
  }

  private void assertChangeMatches(RdaChange.Type changeType) {
    RdaChange<PreAdjFissClaim> changed = transformer.transformClaim(changeBuilder.build());
    assertEquals(changeType, changed.getType());
    assertThat(changed.getClaim(), samePropertyValuesAs(claim));
  }
}
