package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static gov.cms.bfd.pipeline.rda.grpc.source.TransformerTestUtils.assertListContentsHaveSamePropertyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.PreAdjMcsAdjustment;
import gov.cms.bfd.model.rda.PreAdjMcsAudit;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjMcsLocation;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsAdjustment;
import gov.cms.mpsm.rda.v1.mcs.McsAudit;
import gov.cms.mpsm.rda.v1.mcs.McsAuditIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsBeneficiarySex;
import gov.cms.mpsm.rda.v1.mcs.McsBillingProviderIndicator;
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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class McsClaimTransformerTest {
  // using a fixed Clock ensures our timestamp is predictable
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);
  private final IdHasher idHasher =
      new IdHasher(new IdHasher.Config(10, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));
  private final McsClaimTransformer transformer =
      new McsClaimTransformer(clock, MbiCache.computedCache(idHasher.getConfig()));
  private McsClaimChange.Builder changeBuilder;
  private McsClaim.Builder claimBuilder;
  private PreAdjMcsClaim claim;

  @BeforeEach
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

  /**
   * Basic smoke test for transformation of claim objects prior to all of the individual field
   * tests.
   */
  @Test
  public void basicFieldsTestForClaimObjectTransformation() {
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
    claim.setMbiRecord(
        new Mbi(
            1L, "54678912456", "717ac79ed263a61100f92f7ca67df9249501d52ee4d1af49ea43b457fcabf0d1"));
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
        .setIdrClaimMbi("54678912456")
        .setIdrHdrFromDos("2020-01-07")
        .setIdrHdrToDos("2020-01-14");
    changeBuilder
        .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
        .setSeq(42)
        .setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  /**
   * Basic smoke test for transformation of detail objects prior to all of the individual field
   * tests.
   */
  @Test
  public void basicFieldsTestForDetailObjectTransformation() {
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

  /**
   * Basic smoke test for transformation of diagnosis code objects prior to all of the individual
   * field tests.
   */
  @Test
  public void basicFieldsTestForDiagCodeObjectTransformation() {
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
  public void testMissingRequiredFieldsGenerateErrors() {
    final long SEQUENCE_NUM = 37;

    try {
      changeBuilder
          .setSeq(SEQUENCE_NUM)
          .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
          .setClaim(
              claimBuilder
                  .addMcsDetails(McsDetail.newBuilder().build())
                  .addMcsDiagnosisCodes(McsDiagnosisCode.newBuilder().build())
                  .build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      List<DataTransformer.ErrorMessage> expectedErrors =
          List.of(
              new DataTransformer.ErrorMessage(
                  "idrClmHdIcn", "invalid length: expected=[1,15] actual=0"),
              new DataTransformer.ErrorMessage(
                  "idrContrId", "invalid length: expected=[1,5] actual=0"),
              new DataTransformer.ErrorMessage("idrClaimType", "no value set"),
              new DataTransformer.ErrorMessage(
                  "diagCode-0-idrDiagCode", "invalid length: expected=[1,7] actual=0"));

      String expectedMessage =
          String.format(
              "failed with %d errors: seq=%d clmHdIcn= errors=[%s]",
              expectedErrors.size(),
              SEQUENCE_NUM,
              expectedErrors.stream()
                  .map(DataTransformer.ErrorMessage::toString)
                  .collect(Collectors.joining(", ")));

      assertEquals(expectedMessage, ex.getMessage());
      assertEquals(expectedErrors, ex.getErrors());
    }
  }

  // region McsClaim

  @Test
  public void testClaimIdrClmHdIcn() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClmHdIcn,
            PreAdjMcsClaim::getIdrClmHdIcn,
            PreAdjMcsClaim.Fields.idrClmHdIcn,
            15);
  }

  @Test
  public void testClaimIdrContrId() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrContrId,
            PreAdjMcsClaim::getIdrContrId,
            PreAdjMcsClaim.Fields.idrContrId,
            5);
  }

  @Test
  public void testClaimIdrHic() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrHic,
            PreAdjMcsClaim::getIdrHic,
            PreAdjMcsClaim.Fields.idrHic,
            12);
  }

  @Test
  public void testClaimIdrClaimType() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrClaimTypeEnum,
            PreAdjMcsClaim::getIdrClaimType,
            McsClaimType.CLAIM_TYPE_MEDICAL,
            "3")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrClaimTypeUnrecognized,
            PreAdjMcsClaim::getIdrClaimType,
            PreAdjMcsClaim.Fields.idrClaimType,
            1);
  }

  @Test
  public void testClaimIdrBeneLast16() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneLast16,
            PreAdjMcsClaim::getIdrBeneLast_1_6,
            PreAdjMcsClaim.Fields.idrBeneLast_1_6,
            6);
  }

  @Test
  public void testClaimIdrBeneFirstInit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneFirstInit,
            PreAdjMcsClaim::getIdrBeneFirstInit,
            PreAdjMcsClaim.Fields.idrBeneFirstInit,
            1);
  }

  @Test
  public void testClaimIdrBeneMidInit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneMidInit,
            PreAdjMcsClaim::getIdrBeneMidInit,
            PreAdjMcsClaim.Fields.idrBeneMidInit,
            1);
  }

  @Test
  public void testClaimIdrBeneSex() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBeneSexEnum,
            PreAdjMcsClaim::getIdrBeneSex,
            McsBeneficiarySex.BENEFICIARY_SEX_MALE,
            "M")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrBeneSexUnrecognized,
            PreAdjMcsClaim::getIdrBeneSex,
            PreAdjMcsClaim.Fields.idrBeneSex,
            1);
  }

  @Test
  public void testClaimIdrStatusCode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrStatusCodeEnum,
            PreAdjMcsClaim::getIdrStatusCode,
            McsStatusCode.STATUS_CODE_DENIED_E,
            "E")
        .verifyEnumFieldTransformationRejectsUnrecognizedValue(
            McsClaim.Builder::setIdrStatusCodeUnrecognized,
            PreAdjMcsClaim.Fields.idrStatusCode,
            "ZZZ")
        .verifyEnumFieldTransformationRejectsSpecificValues(
            McsClaim.Builder::setIdrStatusCodeEnum,
            PreAdjMcsClaim.Fields.idrStatusCode,
            McsStatusCode.STATUS_CODE_NOT_USED);
  }

  @Test
  public void testClaimIdrStatusDate() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrStatusDate,
            PreAdjMcsClaim::getIdrStatusDate,
            PreAdjMcsClaim.Fields.idrStatusDate);
  }

  @Test
  public void testClaimIdrBillProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvNpi,
            PreAdjMcsClaim::getIdrBillProvNpi,
            PreAdjMcsClaim.Fields.idrBillProvNpi,
            10);
  }

  @Test
  public void testClaimIdrBillProvNum() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvNum,
            PreAdjMcsClaim::getIdrBillProvNum,
            PreAdjMcsClaim.Fields.idrBillProvNum,
            10);
  }

  @Test
  public void testClaimIdrBillProvEin() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvEin,
            PreAdjMcsClaim::getIdrBillProvEin,
            PreAdjMcsClaim.Fields.idrBillProvEin,
            10);
  }

  @Test
  public void testClaimIdrBillProvType() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvType,
            PreAdjMcsClaim::getIdrBillProvType,
            PreAdjMcsClaim.Fields.idrBillProvType,
            2);
  }

  @Test
  public void testClaimIdrBillProvSpec() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvSpec,
            PreAdjMcsClaim::getIdrBillProvSpec,
            PreAdjMcsClaim.Fields.idrBillProvSpec,
            2);
  }

  @Test
  public void testClaimIdrBillProvGroupInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBillProvGroupIndEnum,
            PreAdjMcsClaim::getIdrBillProvGroupInd,
            McsBillingProviderIndicator.BILLING_PROVIDER_INDICATOR_GROUP,
            "G")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrBillProvGroupIndUnrecognized,
            PreAdjMcsClaim::getIdrBillProvGroupInd,
            PreAdjMcsClaim.Fields.idrBillProvGroupInd,
            1);
  }

  @Test
  public void testClaimIdrBillProvPriceSpec() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvPriceSpec,
            PreAdjMcsClaim::getIdrBillProvPriceSpec,
            PreAdjMcsClaim.Fields.idrBillProvPriceSpec,
            2);
  }

  @Test
  public void testClaimIdrBillProvCounty() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvCounty,
            PreAdjMcsClaim::getIdrBillProvCounty,
            PreAdjMcsClaim.Fields.idrBillProvCounty,
            2);
  }

  @Test
  public void testClaimIdrBillProvLoc() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvLoc,
            PreAdjMcsClaim::getIdrBillProvLoc,
            PreAdjMcsClaim.Fields.idrBillProvLoc,
            2);
  }

  @Test
  public void testClaimIdrTotAllowed() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrTotAllowed,
            PreAdjMcsClaim::getIdrTotAllowed,
            PreAdjMcsClaim.Fields.idrTotAllowed);
  }

  @Test
  public void testClaimIdrCoinsurance() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrCoinsurance,
            PreAdjMcsClaim::getIdrCoinsurance,
            PreAdjMcsClaim.Fields.idrCoinsurance);
  }

  @Test
  public void testClaimIdrDeductible() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrDeductible,
            PreAdjMcsClaim::getIdrDeductible,
            PreAdjMcsClaim.Fields.idrDeductible);
  }

  @Test
  public void testClaimIdrBillProvStatusCd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBillProvStatusCdEnum,
            PreAdjMcsClaim::getIdrBillProvStatusCd,
            McsBillingProviderStatusCode.BILLING_PROVIDER_STATUS_CODE_NON_PARTICIPATING,
            "N")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrBillProvStatusCdUnrecognized,
            PreAdjMcsClaim::getIdrBillProvStatusCd,
            PreAdjMcsClaim.Fields.idrBillProvStatusCd,
            1);
  }

  @Test
  public void testClaimIdrBilledAmt() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrTotBilledAmt,
            PreAdjMcsClaim::getIdrTotBilledAmt,
            PreAdjMcsClaim.Fields.idrTotBilledAmt);
  }

  @Test
  public void testClaimIdrClaimReceiptDate() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrClaimReceiptDate,
            PreAdjMcsClaim::getIdrClaimReceiptDate,
            PreAdjMcsClaim.Fields.idrClaimReceiptDate);
  }

  @Test
  public void testClaimIdrClaimMbi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClaimMbi,
            PreAdjMcsClaim::getIdrClaimMbi,
            PreAdjMcsClaim.Fields.idrClaimMbi,
            11)
        .verifyIdHashFieldPopulatedCorrectly(
            McsClaim.Builder::setIdrClaimMbi, PreAdjMcsClaim::getIdrClaimMbiHash, 11, idHasher);
  }

  @Test
  public void testClaimIdrHdrFromDos() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrHdrFromDos,
            PreAdjMcsClaim::getIdrHdrFromDateOfSvc,
            PreAdjMcsClaim.Fields.idrHdrFromDateOfSvc);
  }

  @Test
  public void testClaimIdrHdrToDos() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrHdrToDos,
            PreAdjMcsClaim::getIdrHdrToDateOfSvc,
            PreAdjMcsClaim.Fields.idrHdrToDateOfSvc);
  }

  @Test
  public void testClaimIdrAssignment() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrAssignmentEnum,
            PreAdjMcsClaim::getIdrAssignment,
            McsClaimAssignmentCode.CLAIM_ASSIGNMENT_CODE,
            "A")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrAssignmentUnrecognized,
            PreAdjMcsClaim::getIdrAssignment,
            PreAdjMcsClaim.Fields.idrAssignment,
            1);
  }

  @Test
  public void testClaimIdrClmLevelInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrClmLevelIndEnum,
            PreAdjMcsClaim::getIdrClmLevelInd,
            McsClaimLevelIndicator.CLAIM_LEVEL_INDICATOR_ORIGINAL,
            "O")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrClmLevelIndUnrecognized,
            PreAdjMcsClaim::getIdrClmLevelInd,
            PreAdjMcsClaim.Fields.idrClmLevelInd,
            1);
  }

  @Test
  public void testClaimIdrHdrAudit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyIntFieldCopiedCorrectly(
            McsClaim.Builder::setIdrHdrAudit, PreAdjMcsClaim::getIdrHdrAudit);
  }

  @Test
  public void testClaimIdrHdrAuditInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrHdrAuditIndEnum,
            PreAdjMcsClaim::getIdrHdrAuditInd,
            McsAuditIndicator.AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrHdrAuditIndUnrecognized,
            PreAdjMcsClaim::getIdrHdrAuditInd,
            PreAdjMcsClaim.Fields.idrHdrAuditInd,
            1);
  }

  @Test
  public void testClaimIdrUSplitReason() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrUSplitReasonEnum,
            PreAdjMcsClaim::getIdrUSplitReason,
            McsSplitReasonCode.SPLIT_REASON_CODE_GHI_SPLIT,
            "4")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrUSplitReasonUnrecognized,
            PreAdjMcsClaim::getIdrUSplitReason,
            PreAdjMcsClaim.Fields.idrUSplitReason,
            1);
  }

  @Test
  public void testClaimIdrJReferringProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrJReferringProvNpi,
            PreAdjMcsClaim::getIdrJReferringProvNpi,
            PreAdjMcsClaim.Fields.idrJReferringProvNpi,
            10);
  }

  @Test
  public void testClaimIdrJFacProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrJFacProvNpi,
            PreAdjMcsClaim::getIdrJFacProvNpi,
            PreAdjMcsClaim.Fields.idrJFacProvNpi,
            10);
  }

  @Test
  public void testClaimIdrUDemoProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUDemoProvNpi,
            PreAdjMcsClaim::getIdrUDemoProvNpi,
            PreAdjMcsClaim.Fields.idrUDemoProvNpi,
            10);
  }

  @Test
  public void testClaimIdrUSuperNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUSuperNpi,
            PreAdjMcsClaim::getIdrUSuperNpi,
            PreAdjMcsClaim.Fields.idrUSuperNpi,
            10);
  }

  @Test
  public void testClaimIdrUFcadjBilNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUFcadjBilNpi,
            PreAdjMcsClaim::getIdrUFcadjBilNpi,
            PreAdjMcsClaim.Fields.idrUFcadjBilNpi,
            10);
  }

  @Test
  public void testClaimIdrAmbPickupAddresLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupAddresLine1,
            PreAdjMcsClaim::getIdrAmbPickupAddresLine1,
            PreAdjMcsClaim.Fields.idrAmbPickupAddresLine1,
            25);
  }

  @Test
  public void testClaimIdrAmbPickupAddresLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupAddresLine2,
            PreAdjMcsClaim::getIdrAmbPickupAddresLine2,
            PreAdjMcsClaim.Fields.idrAmbPickupAddresLine2,
            20);
  }

  @Test
  public void testClaimIdrAmbPickupCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupCity,
            PreAdjMcsClaim::getIdrAmbPickupCity,
            PreAdjMcsClaim.Fields.idrAmbPickupCity,
            20);
  }

  @Test
  public void testClaimIdrAmbPickupState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupState,
            PreAdjMcsClaim::getIdrAmbPickupState,
            PreAdjMcsClaim.Fields.idrAmbPickupState,
            2);
  }

  @Test
  public void testClaimIdrAmbPickupZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupZipcode,
            PreAdjMcsClaim::getIdrAmbPickupZipcode,
            PreAdjMcsClaim.Fields.idrAmbPickupZipcode,
            9);
  }

  @Test
  public void testClaimIdrAmbDropoffName() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffName,
            PreAdjMcsClaim::getIdrAmbDropoffName,
            PreAdjMcsClaim.Fields.idrAmbDropoffName,
            24);
  }

  @Test
  public void testClaimIdrAmbDropoffAddrLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffAddrLine1,
            PreAdjMcsClaim::getIdrAmbDropoffAddrLine1,
            PreAdjMcsClaim.Fields.idrAmbDropoffAddrLine1,
            25);
  }

  @Test
  public void testClaimIdrAmbDropoffAddrLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffAddrLine2,
            PreAdjMcsClaim::getIdrAmbDropoffAddrLine2,
            PreAdjMcsClaim.Fields.idrAmbDropoffAddrLine2,
            20);
  }

  @Test
  public void testClaimIdrAmbDropoffCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffCity,
            PreAdjMcsClaim::getIdrAmbDropoffCity,
            PreAdjMcsClaim.Fields.idrAmbDropoffCity,
            20);
  }

  @Test
  public void testClaimIdrAmbDropoffState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffState,
            PreAdjMcsClaim::getIdrAmbDropoffState,
            PreAdjMcsClaim.Fields.idrAmbDropoffState,
            2);
  }

  @Test
  public void testClaimIdrAmbDropoffZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffZipcode,
            PreAdjMcsClaim::getIdrAmbDropoffZipcode,
            PreAdjMcsClaim.Fields.idrAmbDropoffZipcode,
            9);
  }

  // endregion McsClaim

  // region McsAdjustments

  @Test
  public void testAdjustmentIdrAdjDate() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjDate,
            PreAdjMcsAdjustment::getIdrAdjDate,
            PreAdjMcsAdjustment.Fields.idrAdjDate);
  }

  @Test
  public void testAdjustmentIdrXrefIcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrXrefIcn,
            PreAdjMcsAdjustment::getIdrXrefIcn,
            PreAdjMcsAdjustment.Fields.idrXrefIcn,
            15);
  }

  @Test
  public void testAdjustmentIdrAdjClerk() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrAdjClerk,
            PreAdjMcsAdjustment::getIdrAdjClerk,
            PreAdjMcsAdjustment.Fields.idrAdjClerk,
            4);
  }

  @Test
  public void testAdjustmentIdrInitCcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrInitCcn,
            PreAdjMcsAdjustment::getIdrInitCcn,
            PreAdjMcsAdjustment.Fields.idrInitCcn,
            15);
  }

  @Test
  public void testAdjustmentIdrAdjChkWrtDt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjChkWrtDt,
            PreAdjMcsAdjustment::getIdrAdjChkWrtDt,
            PreAdjMcsAdjustment.Fields.idrAdjChkWrtDt);
  }

  @Test
  public void testAdjustmentIdrAdjBEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjBEombAmt,
            PreAdjMcsAdjustment::getIdrAdjBEombAmt,
            PreAdjMcsAdjustment.Fields.idrAdjBEombAmt);
  }

  @Test
  public void testAdjustmentIdrAdjPEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjPEombAmt,
            PreAdjMcsAdjustment::getIdrAdjPEombAmt,
            PreAdjMcsAdjustment.Fields.idrAdjPEombAmt);
  }

  // endregion McsAdjustments

  // region McsAudit

  @Test
  public void testAuditIdrJAuditNum() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyIntFieldCopiedCorrectly(
            McsAudit.Builder::setIdrJAuditNum, PreAdjMcsAudit::getIdrJAuditNum);
  }

  @Test
  public void testAuditIdrJAuditInd() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsAudit.Builder::setIdrJAuditIndEnum,
            PreAdjMcsAudit::getIdrJAuditInd,
            McsCutbackAuditIndicator.CUTBACK_AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsAudit.Builder::setIdrJAuditIndUnrecognized,
            PreAdjMcsAudit::getIdrJAuditInd,
            PreAdjMcsAudit.Fields.idrJAuditInd,
            1);
  }

  @Test
  public void testAuditIdrJAuditDisp() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsAudit.Builder::setIdrJAuditDispEnum,
            PreAdjMcsAudit::getIdrJAuditDisp,
            McsCutbackAuditDisposition.CUTBACK_AUDIT_DISPOSITION_ADS_LETTER,
            "S")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsAudit.Builder::setIdrJAuditDispUnrecognized,
            PreAdjMcsAudit::getIdrJAuditDisp,
            PreAdjMcsAudit.Fields.idrJAuditDisp,
            1);
  }

  // endregion McsAudit

  // region McsDiagnosisCode

  @Test
  public void testDiagnosisCodeIdrDiagCode() {
    new McsClaimTransformerTest.DiagCodeFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDiagnosisCode.Builder::setIdrDiagCode,
            PreAdjMcsDiagnosisCode::getIdrDiagCode,
            PreAdjMcsDiagnosisCode.Fields.idrDiagCode,
            7);
  }

  @Test
  public void testDiagnosisCodeIdrDiagIcdType() {
    new McsClaimTransformerTest.DiagCodeFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDiagnosisCode.Builder::setIdrDiagIcdTypeEnum,
            PreAdjMcsDiagnosisCode::getIdrDiagIcdType,
            McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD9,
            "9")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDiagnosisCode.Builder::setIdrDiagIcdTypeUnrecognized,
            PreAdjMcsDiagnosisCode::getIdrDiagIcdType,
            PreAdjMcsDiagnosisCode.Fields.idrDiagIcdType,
            1);
  }

  // endregion McsDiagnosisCode

  // region McsDetail
  @Test
  public void testDetailIdrDtlStatus() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrDtlStatusEnum,
            PreAdjMcsDetail::getIdrDtlStatus,
            McsDetailStatus.DETAIL_STATUS_FINAL,
            "F")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrDtlStatusUnrecognized,
            PreAdjMcsDetail::getIdrDtlStatus,
            PreAdjMcsDetail.Fields.idrDtlStatus,
            1);
  }

  @Test
  public void testDetailIdrDtlFromDate() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsDetail.Builder::setIdrDtlFromDate,
            PreAdjMcsDetail::getIdrDtlFromDate,
            PreAdjMcsDetail.Fields.idrDtlFromDate);
  }

  @Test
  public void testDetailIdrDtlToDate() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsDetail.Builder::setIdrDtlToDate,
            PreAdjMcsDetail::getIdrDtlToDate,
            PreAdjMcsDetail.Fields.idrDtlToDate);
  }

  @Test
  public void testDetailIdrProcCode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrProcCode,
            PreAdjMcsDetail::getIdrProcCode,
            PreAdjMcsDetail.Fields.idrProcCode,
            5);
  }

  @Test
  public void testDetailIdrModOne() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModOne,
            PreAdjMcsDetail::getIdrModOne,
            PreAdjMcsDetail.Fields.idrModOne,
            2);
  }

  @Test
  public void testDetailIdrModTwo() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModTwo,
            PreAdjMcsDetail::getIdrModTwo,
            PreAdjMcsDetail.Fields.idrModTwo,
            2);
  }

  @Test
  public void testDetailIdrModThree() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModThree,
            PreAdjMcsDetail::getIdrModThree,
            PreAdjMcsDetail.Fields.idrModThree,
            2);
  }

  @Test
  public void testDetailIdrModFour() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModFour,
            PreAdjMcsDetail::getIdrModFour,
            PreAdjMcsDetail.Fields.idrModFour,
            2);
  }

  @Test
  public void testDetailIdrDtlDiagIcdType() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrDtlDiagIcdTypeEnum,
            PreAdjMcsDetail::getIdrDtlDiagIcdType,
            McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10,
            "0")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrDtlDiagIcdTypeUnrecognized,
            PreAdjMcsDetail::getIdrDtlDiagIcdType,
            PreAdjMcsDetail.Fields.idrDtlDiagIcdType,
            1);
  }

  @Test
  public void testDetailIdrDtlPrimaryDiagCode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlPrimaryDiagCode,
            PreAdjMcsDetail::getIdrDtlPrimaryDiagCode,
            PreAdjMcsDetail.Fields.idrDtlPrimaryDiagCode,
            7);
  }

  @Test
  public void testDetailIdrKPosLnameOrg() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosLnameOrg,
            PreAdjMcsDetail::getIdrKPosLnameOrg,
            PreAdjMcsDetail.Fields.idrKPosLnameOrg,
            60);
  }

  @Test
  public void testDetailIdrKPosFname() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosFname,
            PreAdjMcsDetail::getIdrKPosFname,
            PreAdjMcsDetail.Fields.idrKPosFname,
            35);
  }

  @Test
  public void testDetailIdrKPosMname() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosMname,
            PreAdjMcsDetail::getIdrKPosMname,
            PreAdjMcsDetail.Fields.idrKPosMname,
            25);
  }

  @Test
  public void testDetailIdrKPosAddr1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr1,
            PreAdjMcsDetail::getIdrKPosAddr1,
            PreAdjMcsDetail.Fields.idrKPosAddr1,
            55);
  }

  @Test
  public void testDetailIdrKPosAddr21St() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr21St,
            PreAdjMcsDetail::getIdrKPosAddr2_1st,
            PreAdjMcsDetail.Fields.idrKPosAddr2_1st,
            30);
  }

  @Test
  public void testDetailIdrKPosAddr22Nd() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr22Nd,
            PreAdjMcsDetail::getIdrKPosAddr2_2nd,
            PreAdjMcsDetail.Fields.idrKPosAddr2_2nd,
            25);
  }

  @Test
  public void testDetailIdrKPosCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosCity,
            PreAdjMcsDetail::getIdrKPosCity,
            PreAdjMcsDetail.Fields.idrKPosCity,
            30);
  }

  @Test
  public void testDetailIdrKPosState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosState,
            PreAdjMcsDetail::getIdrKPosState,
            PreAdjMcsDetail.Fields.idrKPosState,
            2);
  }

  @Test
  public void testDetailIdrKPosZip() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosZip,
            PreAdjMcsDetail::getIdrKPosZip,
            PreAdjMcsDetail.Fields.idrKPosZip,
            15);
  }

  @Test
  public void testDetailIdrTos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrTosEnum,
            PreAdjMcsDetail::getIdrTos,
            McsTypeOfService.TYPE_OF_SERVICE_ANESTHESIA,
            "7")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrTosUnrecognized,
            PreAdjMcsDetail::getIdrTos,
            PreAdjMcsDetail.Fields.idrTos,
            1);
  }

  @Test
  public void testDetailIdrTwoDigitPos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrTwoDigitPosEnum,
            PreAdjMcsDetail::getIdrTwoDigitPos,
            McsTwoDigitPlanOfService.TWO_DIGIT_PLAN_OF_SERVICE_AMBULANCE_LAND,
            "41")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrTwoDigitPosUnrecognized,
            PreAdjMcsDetail::getIdrTwoDigitPos,
            PreAdjMcsDetail.Fields.idrTwoDigitPos,
            2);
  }

  @Test
  public void testDetailIdrDtlRendType() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendType,
            PreAdjMcsDetail::getIdrDtlRendType,
            PreAdjMcsDetail.Fields.idrDtlRendType,
            2);
  }

  @Test
  public void testDetailIdrDtlRendSpec() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendSpec,
            PreAdjMcsDetail::getIdrDtlRendSpec,
            PreAdjMcsDetail.Fields.idrDtlRendSpec,
            2);
  }

  @Test
  public void testDetailIdrDtlRendNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendNpi,
            PreAdjMcsDetail::getIdrDtlRendNpi,
            PreAdjMcsDetail.Fields.idrDtlRendNpi,
            10);
  }

  @Test
  public void testDetailIdrDtlRendProv() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendProv,
            PreAdjMcsDetail::getIdrDtlRendProv,
            PreAdjMcsDetail.Fields.idrDtlRendProv,
            10);
  }

  @Test
  public void testDetailIdrKDtlFacProvNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKDtlFacProvNpi,
            PreAdjMcsDetail::getIdrKDtlFacProvNpi,
            PreAdjMcsDetail.Fields.idrKDtlFacProvNpi,
            10);
  }

  @Test
  public void testDetailIdrDtlAmbPickupAddres1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupAddres1,
            PreAdjMcsDetail::getIdrDtlAmbPickupAddres1,
            PreAdjMcsDetail.Fields.idrDtlAmbPickupAddres1,
            25);
  }

  @Test
  public void testDetailIdrDtlAmbPickupAddres2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupAddres2,
            PreAdjMcsDetail::getIdrDtlAmbPickupAddres2,
            PreAdjMcsDetail.Fields.idrDtlAmbPickupAddres2,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbPickupCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupCity,
            PreAdjMcsDetail::getIdrDtlAmbPickupCity,
            PreAdjMcsDetail.Fields.idrDtlAmbPickupCity,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbPickupState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupState,
            PreAdjMcsDetail::getIdrDtlAmbPickupState,
            PreAdjMcsDetail.Fields.idrDtlAmbPickupState,
            2);
  }

  @Test
  public void testDetailIdrDtlAmbPickupZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupZipcode,
            PreAdjMcsDetail::getIdrDtlAmbPickupZipcode,
            PreAdjMcsDetail.Fields.idrDtlAmbPickupZipcode,
            9);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffName() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffName,
            PreAdjMcsDetail::getIdrDtlAmbDropoffName,
            PreAdjMcsDetail.Fields.idrDtlAmbDropoffName,
            24);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffAddrL1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL1,
            PreAdjMcsDetail::getIdrDtlAmbDropoffAddrL1,
            PreAdjMcsDetail.Fields.idrDtlAmbDropoffAddrL1,
            25);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffAddrL2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL2,
            PreAdjMcsDetail::getIdrDtlAmbDropoffAddrL2,
            PreAdjMcsDetail.Fields.idrDtlAmbDropoffAddrL2,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffCity,
            PreAdjMcsDetail::getIdrDtlAmbDropoffCity,
            PreAdjMcsDetail.Fields.idrDtlAmbDropoffCity,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffState,
            PreAdjMcsDetail::getIdrDtlAmbDropoffState,
            PreAdjMcsDetail.Fields.idrDtlAmbDropoffState,
            2);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffZipcode,
            PreAdjMcsDetail::getIdrDtlAmbDropoffZipcode,
            PreAdjMcsDetail.Fields.idrDtlAmbDropoffZipcode,
            9);
  }

  // endregion McsDetail

  // region McsLocation

  @Test
  public void testLocationIdrLocClerk() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsLocation.Builder::setIdrLocClerk,
            PreAdjMcsLocation::getIdrLocClerk,
            PreAdjMcsLocation.Fields.idrLocClerk,
            4);
  }

  @Test
  public void testLocationIdrLocCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsLocation.Builder::setIdrLocCode,
            PreAdjMcsLocation::getIdrLocCode,
            PreAdjMcsLocation.Fields.idrLocCode,
            3);
  }

  @Test
  public void testLocationIdrLocDate() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsLocation.Builder::setIdrLocDate,
            PreAdjMcsLocation::getIdrLocDate,
            PreAdjMcsLocation.Fields.idrLocDate);
  }

  @Test
  public void testLocationIdrLocActvCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsLocation.Builder::setIdrLocActvCodeEnum,
            PreAdjMcsLocation::getIdrLocActvCode,
            McsLocationActivityCode.LOCATION_ACTIVITY_CODE_CAS_ACTIVITY,
            "Q")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsLocation.Builder::setIdrLocActvCodeUnrecognized,
            PreAdjMcsLocation::getIdrLocActvCode,
            PreAdjMcsLocation.Fields.idrLocActvCode,
            1);
  }

  // endregion McsLocation

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
          .setIdrClmHdIcn("idrClmHdIcn")
          .setIdrContrId("contr")
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
      PreAdjMcsAdjustment answer = claim.getAdjustments().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
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
      PreAdjMcsAudit answer = claim.getAudits().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
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
      PreAdjMcsDetail answer = claim.getDetails().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "detail-0-" + basicLabel;
    }
  }

  class DiagCodeFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<
          McsDiagnosisCode.Builder, PreAdjMcsDiagnosisCode> {
    @Override
    McsDiagnosisCode.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsDiagnosisCodesBuilderList().isEmpty()) {
        claimBuilder.addMcsDiagnosisCodesBuilder();
        claimBuilder.getMcsDiagnosisCodesBuilder(0).setIdrDiagCode("DC");
      }
      return claimBuilder.getMcsDiagnosisCodesBuilder(0);
    }

    @Override
    PreAdjMcsDiagnosisCode getTestEntity(PreAdjMcsClaim claim) {
      assertEquals(1, claim.getDiagCodes().size());
      PreAdjMcsDiagnosisCode answer = claim.getDiagCodes().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "diagCode-0-" + basicLabel;
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
      PreAdjMcsLocation answer = claim.getLocations().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "location-0-" + basicLabel;
    }
  }

  // endregion Field Tester Classes
}
