package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static gov.cms.bfd.pipeline.rda.grpc.source.TransformerTestUtils.assertListContentsHaveSamePropertyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.model.rda.PreAdjMcsAdjustment;
import gov.cms.bfd.model.rda.PreAdjMcsAudit;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjMcsLocation;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsAdjustment;
import gov.cms.mpsm.rda.v1.mcs.McsAudit;
import gov.cms.mpsm.rda.v1.mcs.McsAuditIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsBeneficiarySex;
import gov.cms.mpsm.rda.v1.mcs.McsBillingProviderStatusCode;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaimAssignmentCode;
import gov.cms.mpsm.rda.v1.mcs.McsClaimLevelIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsClaimType;
import gov.cms.mpsm.rda.v1.mcs.McsCutbackAuditDisposition;
import gov.cms.mpsm.rda.v1.mcs.McsCutbackAuditIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDetailStatus;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisIcdType;
import gov.cms.mpsm.rda.v1.mcs.McsLocation;
import gov.cms.mpsm.rda.v1.mcs.McsLocationActivityCode;
import gov.cms.mpsm.rda.v1.mcs.McsSplitReasonCode;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import gov.cms.mpsm.rda.v1.mcs.McsTwoDigitPlanOfService;
import gov.cms.mpsm.rda.v1.mcs.McsTypeOfService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;

public class McsClaimTransformerTest {
  // using a fixed Clock ensures our timestamp is predictable
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);
  private final McsClaimTransformer transformer =
      new McsClaimTransformer(
          clock,
          new IdHasher(
              new IdHasher.Config(1000, "nottherealpepper".getBytes(StandardCharsets.UTF_8))));
  private McsClaimChange.Builder changeBuilder;
  private McsClaim.Builder claimBuilder;
  private PreAdjMcsClaim claim;

  @Before
  public void setUp() {
    changeBuilder = McsClaimChange.newBuilder();
    claimBuilder = McsClaim.newBuilder();
    claim = new PreAdjMcsClaim();
    claim.setSequenceNumber(0L);
  }

  @Test
  public void minimumValidClaim() {
    claim.setIdrClmHdIcn("123456789012345");
    claim.setIdrContrId("12345");
    claim.setIdrClaimType("3");
    claim.setLastUpdated(clock.instant());
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL);
    changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  @Test
  public void allFields() {
    claim.setSequenceNumber(42L);
    claim.setIdrClmHdIcn("123456789012345");
    claim.setIdrContrId("12345");
    claim.setIdrHic("123456789012");
    claim.setIdrClaimType("3");
    claim.setIdrDtlCnt(0);
    claim.setIdrBeneLast_1_6("123456");
    claim.setIdrBeneFirstInit("7");
    claim.setIdrBeneMidInit("8");
    claim.setIdrBeneSex("F");
    claim.setIdrStatusCode("A");
    claim.setIdrStatusDate(LocalDate.of(2020, 2, 3));
    claim.setIdrBillProvNpi("CDEFGHIJKL");
    claim.setIdrBillProvNum("MNOPQRSTUV");
    claim.setIdrBillProvEin("WXYZabcdef");
    claim.setIdrBillProvType("RS");
    claim.setIdrBillProvSpec("tu");
    claim.setIdrBillProvGroupInd("v");
    claim.setIdrBillProvPriceSpec("rw");
    claim.setIdrBillProvCounty("34");
    claim.setIdrBillProvLoc("43");
    claim.setIdrTotAllowed(new BigDecimal("12345.42"));
    claim.setIdrCoinsurance(new BigDecimal("67890.94"));
    claim.setIdrDeductible(new BigDecimal("87945.28"));
    claim.setIdrBillProvStatusCd("P");
    claim.setIdrTotBilledAmt(new BigDecimal("67591.96"));
    claim.setIdrClaimReceiptDate(LocalDate.of(2020, 2, 1));
    claim.setIdrClaimMbi("5467891245678");
    claim.setIdrClaimMbiHash("8033928eb4cf902474141065280c51791663e86d760da5a0fadf354daffb4b01");
    claim.setIdrHdrFromDateOfSvc(LocalDate.of(2020, 1, 7));
    claim.setIdrHdrToDateOfSvc(LocalDate.of(2020, 1, 14));
    claim.setLastUpdated(clock.instant());
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrHic("123456789012")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL)
        .setIdrDtlCnt(0)
        .setIdrBeneLast16("123456")
        .setIdrBeneFirstInit("7")
        .setIdrBeneMidInit("8")
        .setIdrBeneSexEnum(McsBeneficiarySex.BENEFICIARY_SEX_FEMALE)
        .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A)
        .setIdrStatusDate("2020-02-03")
        .setIdrBillProvNpi("CDEFGHIJKL")
        .setIdrBillProvNum("MNOPQRSTUV")
        .setIdrBillProvEin("WXYZabcdef")
        .setIdrBillProvType("RS")
        .setIdrBillProvSpec("tu")
        .setIdrBillProvGroupIndUnrecognized("v")
        .setIdrBillProvPriceSpec("rw")
        .setIdrBillProvCounty("34")
        .setIdrBillProvLoc("43")
        .setIdrTotAllowed("12345.42")
        .setIdrCoinsurance("67890.94")
        .setIdrDeductible("87945.28")
        .setIdrBillProvStatusCdEnum(
            McsBillingProviderStatusCode.BILLING_PROVIDER_STATUS_CODE_PARTICIPATING)
        .setIdrTotBilledAmt("67591.96")
        .setIdrClaimReceiptDate("2020-02-01")
        .setIdrClaimMbi("5467891245678")
        .setIdrHdrFromDos("2020-01-07")
        .setIdrHdrToDos("2020-01-14");
    changeBuilder
        .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
        .setSeq(42)
        .setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  @Test
  public void details() {
    claim.setIdrClmHdIcn("123456789012345");
    claim.setIdrContrId("12345");
    claim.setIdrClaimType("3");
    claim.setLastUpdated(clock.instant());
    final PreAdjMcsDetail detail = new PreAdjMcsDetail();
    detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    detail.setPriority((short) 0);
    detail.setIdrDtlStatus("F");
    detail.setIdrDtlFromDate(LocalDate.of(2020, 1, 9));
    detail.setIdrDtlToDate(LocalDate.of(2020, 1, 10));
    detail.setIdrProcCode("abCDe");
    detail.setIdrModOne("aB");
    detail.setIdrModTwo("Cd");
    detail.setIdrModThree("EF");
    detail.setIdrModFour("gh");
    detail.setIdrDtlDiagIcdType("9");
    detail.setIdrDtlPrimaryDiagCode("hetwpqj");
    detail.setIdrKPosLnameOrg("123456789012345678901234567890123456789012345678901234567890");
    detail.setIdrKPosFname("12345678901234567890123456789012345");
    detail.setIdrKPosMname("1234567890123456789012345");
    detail.setIdrKPosAddr1("1234567890123456789012345678901234567890123456789012345");
    detail.setIdrKPosAddr2_1st("123456789012345678901234567890");
    detail.setIdrKPosAddr2_2nd("1234567890123456789012345");
    detail.setIdrKPosCity("123456789012345678901234567890");
    detail.setIdrKPosState("12");
    detail.setIdrKPosZip("123456789012345");
    detail.setLastUpdated(clock.instant());
    claim.getDetails().add(detail);
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL)
        .addMcsDetails(
            McsDetail.newBuilder()
                .setIdrDtlStatusEnum(McsDetailStatus.DETAIL_STATUS_FINAL)
                .setIdrDtlFromDate("2020-01-09")
                .setIdrDtlToDate("2020-01-10")
                .setIdrProcCode("abCDe")
                .setIdrModOne("aB")
                .setIdrModTwo("Cd")
                .setIdrModThree("EF")
                .setIdrModFour("gh")
                .setIdrDtlDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD9)
                .setIdrDtlPrimaryDiagCode("hetwpqj")
                .setIdrKPosLnameOrg("123456789012345678901234567890123456789012345678901234567890")
                .setIdrKPosFname("12345678901234567890123456789012345")
                .setIdrKPosMname("1234567890123456789012345")
                .setIdrKPosAddr1("1234567890123456789012345678901234567890123456789012345")
                .setIdrKPosAddr21St("123456789012345678901234567890")
                .setIdrKPosAddr22Nd("1234567890123456789012345")
                .setIdrKPosCity("123456789012345678901234567890")
                .setIdrKPosState("12")
                .setIdrKPosZip("123456789012345")
                .build());
    changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
    PreAdjMcsClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertListContentsHaveSamePropertyValues(
        claim.getDetails(), transformed.getDetails(), PreAdjMcsDetail::getPriority);
  }

  @Test
  public void diagnosisCodes() {
    claim.setIdrClmHdIcn("123456789012345");
    claim.setIdrContrId("12345");
    claim.setIdrClaimType("3");
    claim.setLastUpdated(clock.instant());
    PreAdjMcsDiagnosisCode diagCode = new PreAdjMcsDiagnosisCode();
    diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    diagCode.setPriority((short) 0);
    diagCode.setIdrDiagIcdType("9");
    diagCode.setIdrDiagCode("1234567");
    diagCode.setLastUpdated(clock.instant());
    claim.getDiagCodes().add(diagCode);
    diagCode = new PreAdjMcsDiagnosisCode();
    diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    diagCode.setPriority((short) 1);
    diagCode.setIdrDiagIcdType("0");
    diagCode.setIdrDiagCode("jdsyejs");
    diagCode.setLastUpdated(clock.instant());
    claim.getDiagCodes().add(diagCode);
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL)
        .addMcsDiagnosisCodes(
            McsDiagnosisCode.newBuilder()
                .setIdrClmHdIcn("123456789012345")
                .setIdrDiagCode("1234567")
                .setIdrDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD9)
                .build())
        .addMcsDiagnosisCodes(
            McsDiagnosisCode.newBuilder()
                .setIdrClmHdIcn("123456789012345")
                .setIdrDiagCode("jdsyejs")
                .setIdrDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10)
                .build());
    changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
    PreAdjMcsClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertListContentsHaveSamePropertyValues(
        claim.getDetails(), transformed.getDetails(), PreAdjMcsDetail::getPriority);
  }

  @Test
  public void requiredFieldsMissing() {
    try {
      changeBuilder
          .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
          .setClaim(
              claimBuilder
                  .addMcsDetails(McsDetail.newBuilder().build())
                  .addMcsDiagnosisCodes(McsDiagnosisCode.newBuilder().build())
                  .build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          ImmutableList.of(
              new DataTransformer.ErrorMessage(
                  "idrClmHdIcn", "invalid length: expected=[1,15] actual=0"),
              new DataTransformer.ErrorMessage(
                  "idrContrId", "invalid length: expected=[1,5] actual=0"),
              new DataTransformer.ErrorMessage("idrClaimType", "no value set"),
              new DataTransformer.ErrorMessage(
                  "diagCode-0-idrDiagCode", "invalid length: expected=[1,7] actual=0")),
          ex.getErrors());
    }
  }

  // region McsClaim

  @Test
  public void testBadIdrClmHdIcn() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrClmHdIcn("123456789012345---"),
        new DataTransformer.ErrorMessage(
            "idrClmHdIcn", "invalid length: expected=[1,15] actual=18"));
  }

  @Test
  public void testBadIdrContrId() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrContrId("12345---"),
        new DataTransformer.ErrorMessage("idrContrId", "invalid length: expected=[1,5] actual=8"));
  }

  @Test
  public void testBadIdrHic() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrHic("123456789012---"),
        new DataTransformer.ErrorMessage("idrHic", "invalid length: expected=[1,12] actual=15"));
  }

  @Test
  public void testBadIdrClaimType() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrClaimTypeUnrecognized("55558873478237821782317821782317823783287"),
        new DataTransformer.ErrorMessage(
            "idrClaimType", "invalid length: expected=[1,1] actual=41"));
  }

  @Test
  public void testBadIdrBeneLast16() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBeneLast16("123456---"),
        new DataTransformer.ErrorMessage(
            "idrBeneLast_1_6", "invalid length: expected=[1,6] actual=9"));
  }

  @Test
  public void testBadIdrBeneFirstInit() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBeneFirstInit("7---"),
        new DataTransformer.ErrorMessage(
            "idrBeneFirstInit", "invalid length: expected=[1,1] actual=4"));
  }

  @Test
  public void testBadIdrBeneMidInit() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBeneMidInit("8---"),
        new DataTransformer.ErrorMessage(
            "idrBeneMidInit", "invalid length: expected=[1,1] actual=4"));
  }

  @Test
  public void testBadIdrBeneSex() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBeneSexUnrecognized("ZZZ"),
        new DataTransformer.ErrorMessage("idrBeneSex", "invalid length: expected=[1,1] actual=3"));
  }

  @Test
  public void testBadIdrStatusCode() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrStatusCodeUnrecognized("ZZZ"),
        new DataTransformer.ErrorMessage("idrStatusCode", "unsupported enum value"));
  }

  @Test
  public void testBadIdrStatusDate() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrStatusDate("2020-02-03---"),
        new DataTransformer.ErrorMessage("idrStatusDate", "invalid date"));
  }

  @Test
  public void testBadIdrBillProvNpi() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvNpi("CDEFGHIJKL---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvNpi", "invalid length: expected=[1,10] actual=13"));
  }

  @Test
  public void testBadIdrBillProvNum() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvNum("MNOPQRSTUV---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvNum", "invalid length: expected=[1,10] actual=13"));
  }

  @Test
  public void testBadIdrBillProvEin() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvEin("WXYZabcdef---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvEin", "invalid length: expected=[1,10] actual=13"));
  }

  @Test
  public void testBadIdrBillProvType() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvType("RS---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvType", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadIdrBillProvSpec() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvSpec("tu---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvSpec", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadIdrBillProvGroup() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvGroupIndUnrecognized("v---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvGroupInd", "invalid length: expected=[1,1] actual=4"));
  }

  @Test
  public void testBadIdrBillProvPriceSpec() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvPriceSpec("rw---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvPriceSpec", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadIdrBillProvCounty() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvCounty("34---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvCounty", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadIdrBillProvLoc() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvLoc("43---"),
        new DataTransformer.ErrorMessage(
            "idrBillProvLoc", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadIdrTotAllowed() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrTotAllowed("12345.42---"),
        new DataTransformer.ErrorMessage("idrTotAllowed", "invalid amount"));
  }

  @Test
  public void testBadIdrCoinsurance() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrCoinsurance("67890.94---"),
        new DataTransformer.ErrorMessage("idrCoinsurance", "invalid amount"));
  }

  @Test
  public void testBadIdrDeductible() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrDeductible("87945.28---"),
        new DataTransformer.ErrorMessage("idrDeductible", "invalid amount"));
  }

  @Test
  public void testBadIdrBillProvStatusCd() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrBillProvStatusCdUnrecognized("123"),
        new DataTransformer.ErrorMessage(
            "idrBillProvStatusCd", "invalid length: expected=[1,1] actual=3"));
  }

  @Test
  public void testBadIdrTotBilledAmt() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrTotBilledAmt("67591.96---"),
        new DataTransformer.ErrorMessage("idrTotBilledAmt", "invalid amount"));
  }

  @Test
  public void testBadIdrClaimReceiptDate() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrClaimReceiptDate("2020-02-01---"),
        new DataTransformer.ErrorMessage("idrClaimReceiptDate", "invalid date"));
  }

  @Test
  public void testBadIdrClaimMbi() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrClaimMbi("5467891245678---"),
        new DataTransformer.ErrorMessage(
            "idrClaimMbi", "invalid length: expected=[1,13] actual=16"));
  }

  @Test
  public void testBadIdrHdrFromDos() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrHdrFromDos("2020-01-07---"),
        new DataTransformer.ErrorMessage("idrHdrFromDateOfSvc", "invalid date"));
  }

  @Test
  public void testBadIdrHdrToDos() {
    assertClaimTransformationError(
        () -> claimBuilder.setIdrHdrToDos("2020-01-14---"),
        new DataTransformer.ErrorMessage("idrHdrToDateOfSvc", "invalid date"));
  }

  @Test
  public void testClaimIdrAssignment() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .enumField(
            McsClaim.Builder::setIdrAssignmentEnum,
            PreAdjMcsClaim::getIdrAssignment,
            McsClaimAssignmentCode.CLAIM_ASSIGNMENT_CODE,
            "A")
        .stringField(
            McsClaim.Builder::setIdrAssignmentUnrecognized,
            PreAdjMcsClaim::getIdrAssignment,
            "idrAssignment",
            1);
  }

  @Test
  public void testClaimIdrClmLevelInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .enumField(
            McsClaim.Builder::setIdrClmLevelIndEnum,
            PreAdjMcsClaim::getIdrClmLevelInd,
            McsClaimLevelIndicator.CLAIM_LEVEL_INDICATOR_ORIGINAL,
            "O")
        .stringField(
            McsClaim.Builder::setIdrClmLevelIndUnrecognized,
            PreAdjMcsClaim::getIdrClmLevelInd,
            "idrClmLevelInd",
            1);
  }

  @Test
  public void testClaimIdrHdrAudit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .intField(McsClaim.Builder::setIdrHdrAudit, PreAdjMcsClaim::getIdrHdrAudit);
  }

  @Test
  public void testClaimIdrHdrAuditInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .enumField(
            McsClaim.Builder::setIdrHdrAuditIndEnum,
            PreAdjMcsClaim::getIdrHdrAuditInd,
            McsAuditIndicator.AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .stringField(
            McsClaim.Builder::setIdrHdrAuditIndUnrecognized,
            PreAdjMcsClaim::getIdrHdrAuditInd,
            "idrHdrAuditInd",
            1);
  }

  @Test
  public void testClaimIdrUSplitReason() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .enumField(
            McsClaim.Builder::setIdrUSplitReasonEnum,
            PreAdjMcsClaim::getIdrUSplitReason,
            McsSplitReasonCode.SPLIT_REASON_CODE_GHI_SPLIT,
            "4")
        .stringField(
            McsClaim.Builder::setIdrUSplitReasonUnrecognized,
            PreAdjMcsClaim::getIdrUSplitReason,
            "idrUSplitReason",
            1);
  }

  @Test
  public void testClaimIdrJReferringProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrJReferringProvNpi,
            PreAdjMcsClaim::getIdrJReferringProvNpi,
            "idrJReferringProvNpi",
            10);
  }

  @Test
  public void testClaimIdrJFacProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrJFacProvNpi,
            PreAdjMcsClaim::getIdrJFacProvNpi,
            "idrJFacProvNpi",
            10);
  }

  @Test
  public void testClaimIdrUDemoProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrUDemoProvNpi,
            PreAdjMcsClaim::getIdrUDemoProvNpi,
            "idrUDemoProvNpi",
            10);
  }

  @Test
  public void testClaimIdrUSuperNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrUSuperNpi, PreAdjMcsClaim::getIdrUSuperNpi, "idrUSuperNpi", 10);
  }

  @Test
  public void testClaimIdrUFcadjBilNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrUFcadjBilNpi,
            PreAdjMcsClaim::getIdrUFcadjBilNpi,
            "idrUFcadjBilNpi",
            10);
  }

  @Test
  public void testClaimIdrAmbPickupAddresLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbPickupAddresLine1,
            PreAdjMcsClaim::getIdrAmbPickupAddresLine1,
            "idrAmbPickupAddresLine1",
            25);
  }

  @Test
  public void testClaimIdrAmbPickupAddresLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbPickupAddresLine2,
            PreAdjMcsClaim::getIdrAmbPickupAddresLine2,
            "idrAmbPickupAddresLine2",
            20);
  }

  @Test
  public void testClaimIdrAmbPickupCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbPickupCity,
            PreAdjMcsClaim::getIdrAmbPickupCity,
            "idrAmbPickupCity",
            20);
  }

  @Test
  public void testClaimIdrAmbPickupState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbPickupState,
            PreAdjMcsClaim::getIdrAmbPickupState,
            "idrAmbPickupState",
            2);
  }

  @Test
  public void testClaimIdrAmbPickupZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbPickupZipcode,
            PreAdjMcsClaim::getIdrAmbPickupZipcode,
            "idrAmbPickupZipcode",
            9);
  }

  @Test
  public void testClaimIdrAmbDropoffName() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbDropoffName,
            PreAdjMcsClaim::getIdrAmbDropoffName,
            "idrAmbDropoffName",
            24);
  }

  @Test
  public void testClaimIdrAmbDropoffAddrLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbDropoffAddrLine1,
            PreAdjMcsClaim::getIdrAmbDropoffAddrLine1,
            "idrAmbDropoffAddrLine1",
            25);
  }

  @Test
  public void testClaimIdrAmbDropoffAddrLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbDropoffAddrLine2,
            PreAdjMcsClaim::getIdrAmbDropoffAddrLine2,
            "idrAmbDropoffAddrLine2",
            20);
  }

  @Test
  public void testClaimIdrAmbDropoffCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbDropoffCity,
            PreAdjMcsClaim::getIdrAmbDropoffCity,
            "idrAmbDropoffCity",
            20);
  }

  @Test
  public void testClaimIdrAmbDropoffState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbDropoffState,
            PreAdjMcsClaim::getIdrAmbDropoffState,
            "idrAmbDropoffState",
            2);
  }

  @Test
  public void testClaimIdrAmbDropoffZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .stringField(
            McsClaim.Builder::setIdrAmbDropoffZipcode,
            PreAdjMcsClaim::getIdrAmbDropoffZipcode,
            "idrAmbDropoffZipcode",
            9);
  }

  // endregion McsClaim

  // region McsAdjustments

  @Test
  public void testAdjustmentIdrAdjDate() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .dateField(
            McsAdjustment.Builder::setIdrAdjDate, PreAdjMcsAdjustment::getIdrAdjDate, "idrAdjDate");
  }

  @Test
  public void testAdjustmentIdrXrefIcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .stringField(
            McsAdjustment.Builder::setIdrXrefIcn,
            PreAdjMcsAdjustment::getIdrXrefIcn,
            "idrXrefIcn",
            15);
  }

  @Test
  public void testAdjustmentIdrAdjClerk() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .stringField(
            McsAdjustment.Builder::setIdrAdjClerk,
            PreAdjMcsAdjustment::getIdrAdjClerk,
            "idrAdjClerk",
            4);
  }

  @Test
  public void testAdjustmentIdrInitCcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .stringField(
            McsAdjustment.Builder::setIdrInitCcn,
            PreAdjMcsAdjustment::getIdrInitCcn,
            "idrInitCcn",
            15);
  }

  @Test
  public void testAdjustmentIdrAdjChkWrtDt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .dateField(
            McsAdjustment.Builder::setIdrAdjChkWrtDt,
            PreAdjMcsAdjustment::getIdrAdjChkWrtDt,
            "idrAdjChkWrtDt");
  }

  @Test
  public void testAdjustmentIdrAdjBEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .amountField(
            McsAdjustment.Builder::setIdrAdjBEombAmt,
            PreAdjMcsAdjustment::getIdrAdjBEombAmt,
            "idrAdjBEombAmt");
  }

  @Test
  public void testAdjustmentIdrAdjPEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .amountField(
            McsAdjustment.Builder::setIdrAdjPEombAmt,
            PreAdjMcsAdjustment::getIdrAdjPEombAmt,
            "idrAdjPEombAmt");
  }

  // endregion McsAdjustments

  // region McsAudit

  @Test
  public void testAuditIdrJAuditNum() {
    new McsClaimTransformerTest.AuditFieldTester()
        .intField(McsAudit.Builder::setIdrJAuditNum, PreAdjMcsAudit::getIdrJAuditNum);
  }

  @Test
  public void testAuditIdrJAuditInd() {
    new McsClaimTransformerTest.AuditFieldTester()
        .enumField(
            McsAudit.Builder::setIdrJAuditIndEnum,
            PreAdjMcsAudit::getIdrJAuditInd,
            McsCutbackAuditIndicator.CUTBACK_AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .stringField(
            McsAudit.Builder::setIdrJAuditIndUnrecognized,
            PreAdjMcsAudit::getIdrJAuditInd,
            "idrJAuditInd",
            1);
  }

  @Test
  public void testAuditIdrJAuditDisp() {
    new McsClaimTransformerTest.AuditFieldTester()
        .enumField(
            McsAudit.Builder::setIdrJAuditDispEnum,
            PreAdjMcsAudit::getIdrJAuditDisp,
            McsCutbackAuditDisposition.CUTBACK_AUDIT_DISPOSITION_ADS_LETTER,
            "S")
        .stringField(
            McsAudit.Builder::setIdrJAuditDispUnrecognized,
            PreAdjMcsAudit::getIdrJAuditDisp,
            "idrJAuditDisp",
            1);
  }

  // endregion McsAudit

  // region McsDiagnosisCode

  @Test
  public void testBadDiagnosisCodeIdrDiagCode() {
    assertDiagnosisCodeTransformationError(
        codeBuilder -> codeBuilder.setIdrDiagCode("1234567---"),
        new DataTransformer.ErrorMessage(
            "diagCode-0-idrDiagCode", "invalid length: expected=[1,7] actual=10"));
  }

  @Test
  public void testBadDiagnosisCodeIdrDiagIcdType() {
    assertDiagnosisCodeTransformationError(
        codeBuilder -> codeBuilder.setIdrDiagIcdTypeUnrecognized("sdjbfdskjbdfskjbsdf---"),
        new DataTransformer.ErrorMessage(
            "diagCode-0-idrDiagIcdType", "invalid length: expected=[1,1] actual=22"));
  }

  @Test
  public void testBadDetailIdrDtlStatus() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrDtlStatusUnrecognized("ZZZ"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrDtlStatus", "invalid length: expected=[1,1] actual=3"));
  }
  // endregion McsDiagnosisCode

  // region McsDetail
  @Test
  public void testBadDetailIdrDtlFromDate() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrDtlFromDate("--not-a-date--"),
        new DataTransformer.ErrorMessage("detail-0-idrDtlFromDate", "invalid date"));
  }

  @Test
  public void testBadDetailIdrDtlToDate() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrDtlToDate("--not-a-date--"),
        new DataTransformer.ErrorMessage("detail-0-idrDtlToDate", "invalid date"));
  }

  @Test
  public void testBadDetailIdrProcCode() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrProcCode("abCDe---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrProcCode", "invalid length: expected=[1,5] actual=8"));
  }

  @Test
  public void testBadDetailIdrModOne() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrModOne("aB---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrModOne", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadDetailIdrModTwo() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrModTwo("Cd---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrModTwo", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadDetailIdrModThree() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrModThree("EF---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrModThree", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadDetailIdrModFour() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrModFour("gh---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrModFour", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadDetailIdrDtlDiagIcdType() {
    assertDetailTransformationError(
        detailBuilder ->
            detailBuilder.setIdrDtlDiagIcdTypeUnrecognized(
                "jbkasjdbkjadsfbflasdbglbasdfljbfdsaj---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrDtlDiagIcdType", "invalid length: expected=[1,1] actual=39"));
  }

  @Test
  public void testBadDetailIdrDtlPrimaryDiagCode() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrDtlPrimaryDiagCode("hetwpqj---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrDtlPrimaryDiagCode", "invalid length: expected=[1,7] actual=10"));
  }

  @Test
  public void testBadDetailIdrKPosLnameOrg() {
    assertDetailTransformationError(
        detailBuilder ->
            detailBuilder.setIdrKPosLnameOrg(
                "123456789012345678901234567890123456789012345678901234567890---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosLnameOrg", "invalid length: expected=[1,60] actual=63"));
  }

  @Test
  public void testBadDetailIdrKPosFname() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrKPosFname("12345678901234567890123456789012345---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosFname", "invalid length: expected=[1,35] actual=38"));
  }

  @Test
  public void testBadDetailIdrKPosMname() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrKPosMname("1234567890123456789012345---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosMname", "invalid length: expected=[1,25] actual=28"));
  }

  @Test
  public void testBadDetailIdrKPosAddr1() {
    assertDetailTransformationError(
        detailBuilder ->
            detailBuilder.setIdrKPosAddr1(
                "1234567890123456789012345678901234567890123456789012345---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosAddr1", "invalid length: expected=[1,55] actual=58"));
  }

  @Test
  public void testBadDetailIdrKPosAddr21St() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrKPosAddr21St("123456789012345678901234567890---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosAddr2_1st", "invalid length: expected=[1,30] actual=33"));
  }

  @Test
  public void testBadDetailIdrKPosAddr22Nd() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrKPosAddr22Nd("1234567890123456789012345---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosAddr2_2nd", "invalid length: expected=[1,25] actual=28"));
  }

  @Test
  public void testBadDetailIdrKPosCity() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrKPosCity("123456789012345678901234567890---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosCity", "invalid length: expected=[1,30] actual=33"));
  }

  @Test
  public void testBadDetailIdrKPosState() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrKPosState("12---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosState", "invalid length: expected=[1,2] actual=5"));
  }

  @Test
  public void testBadDetailIdrKPosZip() {
    assertDetailTransformationError(
        detailBuilder -> detailBuilder.setIdrKPosZip("123456789012345---"),
        new DataTransformer.ErrorMessage(
            "detail-0-idrKPosZip", "invalid length: expected=[1,15] actual=18"));
  }

  @Test
  public void testDetailIdrTos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .enumField(
            McsDetail.Builder::setIdrTosEnum,
            PreAdjMcsDetail::getIdrTos,
            McsTypeOfService.TYPE_OF_SERVICE_ANESTHESIA,
            "7")
        .stringField(
            McsDetail.Builder::setIdrTosUnrecognized, PreAdjMcsDetail::getIdrTos, "idrTos", 1);
  }

  @Test
  public void testDetailIdrTwoDigitPos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .enumField(
            McsDetail.Builder::setIdrTwoDigitPosEnum,
            PreAdjMcsDetail::getIdrTwoDigitPos,
            McsTwoDigitPlanOfService.TWO_DIGIT_PLAN_OF_SERVICE_AMBULANCE_LAND,
            "41")
        .stringField(
            McsDetail.Builder::setIdrTwoDigitPosUnrecognized,
            PreAdjMcsDetail::getIdrTwoDigitPos,
            "idrTwoDigitPos",
            2);
  }

  @Test
  public void testDetailIdrDtlRendType() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlRendType,
            PreAdjMcsDetail::getIdrDtlRendType,
            "idrDtlRendType",
            2);
  }

  @Test
  public void testDetailIdrDtlRendSpec() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlRendSpec,
            PreAdjMcsDetail::getIdrDtlRendSpec,
            "idrDtlRendSpec",
            2);
  }

  @Test
  public void testDetailIdrDtlRendNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlRendNpi,
            PreAdjMcsDetail::getIdrDtlRendNpi,
            "idrDtlRendNpi",
            10);
  }

  @Test
  public void testDetailIdrDtlRendProv() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlRendProv,
            PreAdjMcsDetail::getIdrDtlRendProv,
            "idrDtlRendProv",
            10);
  }

  @Test
  public void testDetailIdrKDtlFacProvNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrKDtlFacProvNpi,
            PreAdjMcsDetail::getIdrKDtlFacProvNpi,
            "idrKDtlFacProvNpi",
            10);
  }

  @Test
  public void testDetailIdrDtlAmbPickupAddres1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbPickupAddres1,
            PreAdjMcsDetail::getIdrDtlAmbPickupAddres1,
            "idrDtlAmbPickupAddres1",
            25);
  }

  @Test
  public void testDetailIdrDtlAmbPickupAddres2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbPickupAddres2,
            PreAdjMcsDetail::getIdrDtlAmbPickupAddres2,
            "idrDtlAmbPickupAddres2",
            20);
  }

  @Test
  public void testDetailIdrDtlAmbPickupCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbPickupCity,
            PreAdjMcsDetail::getIdrDtlAmbPickupCity,
            "idrDtlAmbPickupCity",
            20);
  }

  @Test
  public void testDetailIdrDtlAmbPickupState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbPickupState,
            PreAdjMcsDetail::getIdrDtlAmbPickupState,
            "idrDtlAmbPickupState",
            2);
  }

  @Test
  public void testDetailIdrDtlAmbPickupZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbPickupZipcode,
            PreAdjMcsDetail::getIdrDtlAmbPickupZipcode,
            "idrDtlAmbPickupZipcode",
            9);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffName() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbDropoffName,
            PreAdjMcsDetail::getIdrDtlAmbDropoffName,
            "idrDtlAmbDropoffName",
            24);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffAddrL1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL1,
            PreAdjMcsDetail::getIdrDtlAmbDropoffAddrL1,
            "idrDtlAmbDropoffAddrL1",
            25);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffAddrL2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL2,
            PreAdjMcsDetail::getIdrDtlAmbDropoffAddrL2,
            "idrDtlAmbDropoffAddrL2",
            20);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbDropoffCity,
            PreAdjMcsDetail::getIdrDtlAmbDropoffCity,
            "idrDtlAmbDropoffCity",
            20);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbDropoffState,
            PreAdjMcsDetail::getIdrDtlAmbDropoffState,
            "idrDtlAmbDropoffState",
            2);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .stringField(
            McsDetail.Builder::setIdrDtlAmbDropoffZipcode,
            PreAdjMcsDetail::getIdrDtlAmbDropoffZipcode,
            "idrDtlAmbDropoffZipcode",
            9);
  }

  // endregion McsDetail

  // region McsLocation

  @Test
  public void testLocationIdrLocClerk() {
    new McsClaimTransformerTest.LocationFieldTester()
        .stringField(
            McsLocation.Builder::setIdrLocClerk,
            PreAdjMcsLocation::getIdrLocClerk,
            "idrLocClerk",
            4);
  }

  @Test
  public void testLocationIdrLocCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .stringField(
            McsLocation.Builder::setIdrLocCode, PreAdjMcsLocation::getIdrLocCode, "idrLocCode", 3);
  }

  @Test
  public void testLocationIdrLocDate() {
    new McsClaimTransformerTest.LocationFieldTester()
        .dateField(
            McsLocation.Builder::setIdrLocDate, PreAdjMcsLocation::getIdrLocDate, "idrLocDate");
  }

  @Test
  public void testLocationIdrLocActvCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .enumField(
            McsLocation.Builder::setIdrLocActvCodeEnum,
            PreAdjMcsLocation::getIdrLocActvCode,
            McsLocationActivityCode.LOCATION_ACTIVITY_CODE_CAS_ACTIVITY,
            "Q")
        .stringField(
            McsLocation.Builder::setIdrLocActvCodeUnrecognized,
            PreAdjMcsLocation::getIdrLocActvCode,
            "idrLocActvCode",
            1);
  }

  // endregion McsLocation

  private void assertClaimTransformationError(
      Runnable claimUpdate, DataTransformer.ErrorMessage... expectedErrors) {
    try {
      claimBuilder
          .setIdrClmHdIcn("123456789012345")
          .setIdrContrId("12345")
          .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL);
      claimUpdate.run();
      changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(ImmutableList.copyOf(expectedErrors), ex.getErrors());
    }
  }

  private void assertDiagnosisCodeTransformationError(
      Consumer<McsDiagnosisCode.Builder> updater, DataTransformer.ErrorMessage... expectedErrors) {
    assertClaimTransformationError(
        () -> {
          final McsDiagnosisCode.Builder codeBuilder = McsDiagnosisCode.newBuilder();
          codeBuilder.setIdrClmHdIcn("123456789012345");
          codeBuilder.setIdrDiagCode("7777777");
          updater.accept(codeBuilder);
          claimBuilder.addMcsDiagnosisCodes(codeBuilder.build());
        },
        expectedErrors);
  }

  private void assertDetailTransformationError(
      Consumer<McsDetail.Builder> updater, DataTransformer.ErrorMessage... expectedErrors) {
    assertClaimTransformationError(
        () -> {
          final McsDetail.Builder detailBuilder = McsDetail.newBuilder();
          updater.accept(detailBuilder);
          claimBuilder.addMcsDetails(detailBuilder.build());
        },
        expectedErrors);
  }

  @Test
  public void unrecognizedStatusCode() {
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL)
        .setIdrStatusCodeUnrecognized("X");
    changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
    try {
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          ImmutableList.of(
              new DataTransformer.ErrorMessage("idrStatusCode", "unsupported enum value")),
          ex.getErrors());
    }
  }

  @Test
  public void rejectedStatusCodeEnum() {
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL)
        .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_NOT_USED);
    changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
    try {
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          ImmutableList.of(
              new DataTransformer.ErrorMessage("idrStatusCode", "unsupported enum value")),
          ex.getErrors());
    }
  }

  private void assertChangeMatches(RdaChange.Type changeType) {
    RdaChange<PreAdjMcsClaim> changed = transformer.transformClaim(changeBuilder.build());
    assertEquals(changeType, changed.getType());
    assertThat(changed.getClaim(), samePropertyValuesAs(claim));
  }

  // region Field Tester Classes

  private abstract class AbstractFieldTester<TBuilder, TEntity>
      extends ClaimTransformerFieldTester<
          McsClaim.Builder, McsClaim, PreAdjMcsClaim, TBuilder, TEntity> {
    @Override
    McsClaim.Builder createClaimBuilder() {
      return McsClaim.newBuilder()
          .setIdrClmHdIcn("123456789012345")
          .setIdrContrId("12345")
          .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL);
    }

    @Override
    RdaChange<PreAdjMcsClaim> transformClaim(McsClaim claim) {
      var changeBuilder =
          McsClaimChange.newBuilder()
              .setSeq(MIN_SEQUENCE_NUM)
              .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
              .setClaim(claim);
      return transformer.transformClaim(changeBuilder.build());
    }

    @Override
    McsClaim buildClaim(McsClaim.Builder builder) {
      return builder.build();
    }
  }

  private class ClaimFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsClaim.Builder, PreAdjMcsClaim> {
    @Override
    McsClaim.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      return claimBuilder;
    }

    @Override
    PreAdjMcsClaim getTestEntity(PreAdjMcsClaim claim) {
      return claim;
    }
  }

  class AdjustmentFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<
          McsAdjustment.Builder, PreAdjMcsAdjustment> {
    @Override
    McsAdjustment.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsAdjustmentsBuilderList().isEmpty()) {
        claimBuilder.addMcsAdjustmentsBuilder();
      }
      return claimBuilder.getMcsAdjustmentsBuilder(0);
    }

    @Override
    PreAdjMcsAdjustment getTestEntity(PreAdjMcsClaim claim) {
      assertEquals(1, claim.getAdjustments().size());
      return claim.getAdjustments().iterator().next();
    }

    @Override
    String getLabel(String basicLabel) {
      return "adjustment-0-" + basicLabel;
    }
  }

  class AuditFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsAudit.Builder, PreAdjMcsAudit> {
    @Override
    McsAudit.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsAuditsBuilderList().isEmpty()) {
        claimBuilder.addMcsAuditsBuilder();
      }
      return claimBuilder.getMcsAuditsBuilder(0);
    }

    @Override
    PreAdjMcsAudit getTestEntity(PreAdjMcsClaim claim) {
      assertEquals(1, claim.getAudits().size());
      return claim.getAudits().iterator().next();
    }

    @Override
    String getLabel(String basicLabel) {
      return "audit-0-" + basicLabel;
    }
  }

  class DetailFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsDetail.Builder, PreAdjMcsDetail> {
    @Override
    McsDetail.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsDetailsBuilderList().isEmpty()) {
        claimBuilder.addMcsDetailsBuilder();
      }
      return claimBuilder.getMcsDetailsBuilder(0);
    }

    @Override
    PreAdjMcsDetail getTestEntity(PreAdjMcsClaim claim) {
      assertEquals(1, claim.getDetails().size());
      return claim.getDetails().iterator().next();
    }

    @Override
    String getLabel(String basicLabel) {
      return "detail-0-" + basicLabel;
    }
  }

  class LocationFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsLocation.Builder, PreAdjMcsLocation> {
    @Override
    McsLocation.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsLocationsBuilderList().isEmpty()) {
        claimBuilder.addMcsLocationsBuilder();
      }
      return claimBuilder.getMcsLocationsBuilder(0);
    }

    @Override
    PreAdjMcsLocation getTestEntity(PreAdjMcsClaim claim) {
      assertEquals(1, claim.getLocations().size());
      return claim.getLocations().iterator().next();
    }

    @Override
    String getLabel(String basicLabel) {
      return "location-0-" + basicLabel;
    }
  }

  // endregion Field Tester Classes
}
