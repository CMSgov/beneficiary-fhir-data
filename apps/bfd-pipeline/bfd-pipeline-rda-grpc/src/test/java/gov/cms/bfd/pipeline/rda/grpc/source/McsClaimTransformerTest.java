package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static gov.cms.bfd.pipeline.rda.grpc.source.TransformerTestUtils.assertListContentsHaveSamePropertyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.model.rda.PartAdjMcsAdjustment;
import gov.cms.bfd.model.rda.PartAdjMcsAudit;
import gov.cms.bfd.model.rda.PartAdjMcsClaim;
import gov.cms.bfd.model.rda.PartAdjMcsDetail;
import gov.cms.bfd.model.rda.PartAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rda.PartAdjMcsLocation;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
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
import org.junit.Before;
import org.junit.Test;

public class McsClaimTransformerTest {
  // using a fixed Clock ensures our timestamp is predictable
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);
  private final IdHasher idHasher =
      new IdHasher(new IdHasher.Config(10, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));
  private final McsClaimTransformer transformer = new McsClaimTransformer(clock, idHasher);
  private McsClaimChange.Builder changeBuilder;
  private McsClaim.Builder claimBuilder;
  private PartAdjMcsClaim claim;

  @Before
  public void setUp() {
    changeBuilder = McsClaimChange.newBuilder();
    claimBuilder = McsClaim.newBuilder();
    claim = new PartAdjMcsClaim();
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
    claim.setIdrClaimMbi("5467891245678");
    claim.setIdrClaimMbiHash("c0755c7a103d9d8556778f64cc45766686d6c02151ebfcc4639dcaeedbf00ca1");
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
    final PartAdjMcsDetail detail = new PartAdjMcsDetail();
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
    PartAdjMcsClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertListContentsHaveSamePropertyValues(
        claim.getDetails(), transformed.getDetails(), PartAdjMcsDetail::getPriority);
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
    PartAdjMcsDiagnosisCode diagCode = new PartAdjMcsDiagnosisCode();
    diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    diagCode.setPriority((short) 0);
    diagCode.setIdrDiagIcdType("9");
    diagCode.setIdrDiagCode("1234567");
    diagCode.setLastUpdated(clock.instant());
    claim.getDiagCodes().add(diagCode);
    diagCode = new PartAdjMcsDiagnosisCode();
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
    PartAdjMcsClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertListContentsHaveSamePropertyValues(
        claim.getDetails(), transformed.getDetails(), PartAdjMcsDetail::getPriority);
  }

  @Test
  public void testMissingRequiredFieldsGenerateErrors() {
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
  public void testClaimIdrClmHdIcn() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClmHdIcn,
            PartAdjMcsClaim::getIdrClmHdIcn,
            PartAdjMcsClaim.Fields.idrClmHdIcn,
            15);
  }

  @Test
  public void testClaimIdrContrId() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrContrId,
            PartAdjMcsClaim::getIdrContrId,
            PartAdjMcsClaim.Fields.idrContrId,
            5);
  }

  @Test
  public void testClaimIdrHic() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrHic,
            PartAdjMcsClaim::getIdrHic,
            PartAdjMcsClaim.Fields.idrHic,
            12);
  }

  @Test
  public void testClaimIdrClaimType() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrClaimTypeEnum,
            PartAdjMcsClaim::getIdrClaimType,
            McsClaimType.CLAIM_TYPE_MEDICAL,
            "3")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClaimTypeUnrecognized,
            PartAdjMcsClaim::getIdrClaimType,
            PartAdjMcsClaim.Fields.idrClaimType,
            1);
  }

  @Test
  public void testClaimIdrBeneLast16() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneLast16,
            PartAdjMcsClaim::getIdrBeneLast_1_6,
            PartAdjMcsClaim.Fields.idrBeneLast_1_6,
            6);
  }

  @Test
  public void testClaimIdrBeneFirstInit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneFirstInit,
            PartAdjMcsClaim::getIdrBeneFirstInit,
            PartAdjMcsClaim.Fields.idrBeneFirstInit,
            1);
  }

  @Test
  public void testClaimIdrBeneMidInit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneMidInit,
            PartAdjMcsClaim::getIdrBeneMidInit,
            PartAdjMcsClaim.Fields.idrBeneMidInit,
            1);
  }

  @Test
  public void testClaimIdrBeneSex() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBeneSexEnum,
            PartAdjMcsClaim::getIdrBeneSex,
            McsBeneficiarySex.BENEFICIARY_SEX_MALE,
            "M")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneSexUnrecognized,
            PartAdjMcsClaim::getIdrBeneSex,
            PartAdjMcsClaim.Fields.idrBeneSex,
            1);
  }

  @Test
  public void testClaimIdrStatusCode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrStatusCodeEnum,
            PartAdjMcsClaim::getIdrStatusCode,
            McsStatusCode.STATUS_CODE_DENIED_E,
            "E")
        .verifyEnumFieldTransformationRejectsUnrecognizedValue(
            McsClaim.Builder::setIdrStatusCodeUnrecognized,
            PartAdjMcsClaim.Fields.idrStatusCode,
            "ZZZ")
        .verifyEnumFieldTransformationRejectsSpecificValues(
            McsClaim.Builder::setIdrStatusCodeEnum,
            PartAdjMcsClaim.Fields.idrStatusCode,
            McsStatusCode.STATUS_CODE_NOT_USED);
  }

  @Test
  public void testClaimIdrStatusDate() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrStatusDate,
            PartAdjMcsClaim::getIdrStatusDate,
            PartAdjMcsClaim.Fields.idrStatusDate);
  }

  @Test
  public void testClaimIdrBillProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvNpi,
            PartAdjMcsClaim::getIdrBillProvNpi,
            PartAdjMcsClaim.Fields.idrBillProvNpi,
            10);
  }

  @Test
  public void testClaimIdrBillProvNum() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvNum,
            PartAdjMcsClaim::getIdrBillProvNum,
            PartAdjMcsClaim.Fields.idrBillProvNum,
            10);
  }

  @Test
  public void testClaimIdrBillProvEin() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvEin,
            PartAdjMcsClaim::getIdrBillProvEin,
            PartAdjMcsClaim.Fields.idrBillProvEin,
            10);
  }

  @Test
  public void testClaimIdrBillProvType() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvType,
            PartAdjMcsClaim::getIdrBillProvType,
            PartAdjMcsClaim.Fields.idrBillProvType,
            2);
  }

  @Test
  public void testClaimIdrBillProvSpec() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvSpec,
            PartAdjMcsClaim::getIdrBillProvSpec,
            PartAdjMcsClaim.Fields.idrBillProvSpec,
            2);
  }

  @Test
  public void testClaimIdrBillProvGroupInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBillProvGroupIndEnum,
            PartAdjMcsClaim::getIdrBillProvGroupInd,
            McsBillingProviderIndicator.BILLING_PROVIDER_INDICATOR_GROUP,
            "G")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvGroupIndUnrecognized,
            PartAdjMcsClaim::getIdrBillProvGroupInd,
            PartAdjMcsClaim.Fields.idrBillProvGroupInd,
            1);
  }

  @Test
  public void testClaimIdrBillProvPriceSpec() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvPriceSpec,
            PartAdjMcsClaim::getIdrBillProvPriceSpec,
            PartAdjMcsClaim.Fields.idrBillProvPriceSpec,
            2);
  }

  @Test
  public void testClaimIdrBillProvCounty() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvCounty,
            PartAdjMcsClaim::getIdrBillProvCounty,
            PartAdjMcsClaim.Fields.idrBillProvCounty,
            2);
  }

  @Test
  public void testClaimIdrBillProvLoc() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvLoc,
            PartAdjMcsClaim::getIdrBillProvLoc,
            PartAdjMcsClaim.Fields.idrBillProvLoc,
            2);
  }

  @Test
  public void testClaimIdrTotAllowed() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrTotAllowed,
            PartAdjMcsClaim::getIdrTotAllowed,
            PartAdjMcsClaim.Fields.idrTotAllowed);
  }

  @Test
  public void testClaimIdrCoinsurance() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrCoinsurance,
            PartAdjMcsClaim::getIdrCoinsurance,
            PartAdjMcsClaim.Fields.idrCoinsurance);
  }

  @Test
  public void testClaimIdrDeductible() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrDeductible,
            PartAdjMcsClaim::getIdrDeductible,
            PartAdjMcsClaim.Fields.idrDeductible);
  }

  @Test
  public void testClaimIdrBillProvStatusCd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBillProvStatusCdEnum,
            PartAdjMcsClaim::getIdrBillProvStatusCd,
            McsBillingProviderStatusCode.BILLING_PROVIDER_STATUS_CODE_NON_PARTICIPATING,
            "N")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvStatusCdUnrecognized,
            PartAdjMcsClaim::getIdrBillProvStatusCd,
            PartAdjMcsClaim.Fields.idrBillProvStatusCd,
            1);
  }

  @Test
  public void testClaimIdrBilledAmt() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrTotBilledAmt,
            PartAdjMcsClaim::getIdrTotBilledAmt,
            PartAdjMcsClaim.Fields.idrTotBilledAmt);
  }

  @Test
  public void testClaimIdrClaimReceiptDate() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrClaimReceiptDate,
            PartAdjMcsClaim::getIdrClaimReceiptDate,
            PartAdjMcsClaim.Fields.idrClaimReceiptDate);
  }

  @Test
  public void testClaimIdrClaimMbi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClaimMbi,
            PartAdjMcsClaim::getIdrClaimMbi,
            PartAdjMcsClaim.Fields.idrClaimMbi,
            13)
        .verifyIdHashFieldPopulatedCorrectly(
            McsClaim.Builder::setIdrClaimMbi, PartAdjMcsClaim::getIdrClaimMbiHash, 13, idHasher);
  }

  @Test
  public void testClaimIdrHdrFromDos() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrHdrFromDos,
            PartAdjMcsClaim::getIdrHdrFromDateOfSvc,
            PartAdjMcsClaim.Fields.idrHdrFromDateOfSvc);
  }

  @Test
  public void testClaimIdrHdrToDos() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrHdrToDos,
            PartAdjMcsClaim::getIdrHdrToDateOfSvc,
            PartAdjMcsClaim.Fields.idrHdrToDateOfSvc);
  }

  @Test
  public void testClaimIdrAssignment() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrAssignmentEnum,
            PartAdjMcsClaim::getIdrAssignment,
            McsClaimAssignmentCode.CLAIM_ASSIGNMENT_CODE,
            "A")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAssignmentUnrecognized,
            PartAdjMcsClaim::getIdrAssignment,
            PartAdjMcsClaim.Fields.idrAssignment,
            1);
  }

  @Test
  public void testClaimIdrClmLevelInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrClmLevelIndEnum,
            PartAdjMcsClaim::getIdrClmLevelInd,
            McsClaimLevelIndicator.CLAIM_LEVEL_INDICATOR_ORIGINAL,
            "O")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClmLevelIndUnrecognized,
            PartAdjMcsClaim::getIdrClmLevelInd,
            PartAdjMcsClaim.Fields.idrClmLevelInd,
            1);
  }

  @Test
  public void testClaimIdrHdrAudit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyIntFieldCopiedCorrectly(
            McsClaim.Builder::setIdrHdrAudit, PartAdjMcsClaim::getIdrHdrAudit);
  }

  @Test
  public void testClaimIdrHdrAuditInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrHdrAuditIndEnum,
            PartAdjMcsClaim::getIdrHdrAuditInd,
            McsAuditIndicator.AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrHdrAuditIndUnrecognized,
            PartAdjMcsClaim::getIdrHdrAuditInd,
            PartAdjMcsClaim.Fields.idrHdrAuditInd,
            1);
  }

  @Test
  public void testClaimIdrUSplitReason() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrUSplitReasonEnum,
            PartAdjMcsClaim::getIdrUSplitReason,
            McsSplitReasonCode.SPLIT_REASON_CODE_GHI_SPLIT,
            "4")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUSplitReasonUnrecognized,
            PartAdjMcsClaim::getIdrUSplitReason,
            PartAdjMcsClaim.Fields.idrUSplitReason,
            1);
  }

  @Test
  public void testClaimIdrJReferringProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrJReferringProvNpi,
            PartAdjMcsClaim::getIdrJReferringProvNpi,
            PartAdjMcsClaim.Fields.idrJReferringProvNpi,
            10);
  }

  @Test
  public void testClaimIdrJFacProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrJFacProvNpi,
            PartAdjMcsClaim::getIdrJFacProvNpi,
            PartAdjMcsClaim.Fields.idrJFacProvNpi,
            10);
  }

  @Test
  public void testClaimIdrUDemoProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUDemoProvNpi,
            PartAdjMcsClaim::getIdrUDemoProvNpi,
            PartAdjMcsClaim.Fields.idrUDemoProvNpi,
            10);
  }

  @Test
  public void testClaimIdrUSuperNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUSuperNpi,
            PartAdjMcsClaim::getIdrUSuperNpi,
            PartAdjMcsClaim.Fields.idrUSuperNpi,
            10);
  }

  @Test
  public void testClaimIdrUFcadjBilNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUFcadjBilNpi,
            PartAdjMcsClaim::getIdrUFcadjBilNpi,
            PartAdjMcsClaim.Fields.idrUFcadjBilNpi,
            10);
  }

  @Test
  public void testClaimIdrAmbPickupAddresLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupAddresLine1,
            PartAdjMcsClaim::getIdrAmbPickupAddresLine1,
            PartAdjMcsClaim.Fields.idrAmbPickupAddresLine1,
            25);
  }

  @Test
  public void testClaimIdrAmbPickupAddresLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupAddresLine2,
            PartAdjMcsClaim::getIdrAmbPickupAddresLine2,
            PartAdjMcsClaim.Fields.idrAmbPickupAddresLine2,
            20);
  }

  @Test
  public void testClaimIdrAmbPickupCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupCity,
            PartAdjMcsClaim::getIdrAmbPickupCity,
            PartAdjMcsClaim.Fields.idrAmbPickupCity,
            20);
  }

  @Test
  public void testClaimIdrAmbPickupState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupState,
            PartAdjMcsClaim::getIdrAmbPickupState,
            PartAdjMcsClaim.Fields.idrAmbPickupState,
            2);
  }

  @Test
  public void testClaimIdrAmbPickupZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupZipcode,
            PartAdjMcsClaim::getIdrAmbPickupZipcode,
            PartAdjMcsClaim.Fields.idrAmbPickupZipcode,
            9);
  }

  @Test
  public void testClaimIdrAmbDropoffName() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffName,
            PartAdjMcsClaim::getIdrAmbDropoffName,
            PartAdjMcsClaim.Fields.idrAmbDropoffName,
            24);
  }

  @Test
  public void testClaimIdrAmbDropoffAddrLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffAddrLine1,
            PartAdjMcsClaim::getIdrAmbDropoffAddrLine1,
            PartAdjMcsClaim.Fields.idrAmbDropoffAddrLine1,
            25);
  }

  @Test
  public void testClaimIdrAmbDropoffAddrLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffAddrLine2,
            PartAdjMcsClaim::getIdrAmbDropoffAddrLine2,
            PartAdjMcsClaim.Fields.idrAmbDropoffAddrLine2,
            20);
  }

  @Test
  public void testClaimIdrAmbDropoffCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffCity,
            PartAdjMcsClaim::getIdrAmbDropoffCity,
            PartAdjMcsClaim.Fields.idrAmbDropoffCity,
            20);
  }

  @Test
  public void testClaimIdrAmbDropoffState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffState,
            PartAdjMcsClaim::getIdrAmbDropoffState,
            PartAdjMcsClaim.Fields.idrAmbDropoffState,
            2);
  }

  @Test
  public void testClaimIdrAmbDropoffZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffZipcode,
            PartAdjMcsClaim::getIdrAmbDropoffZipcode,
            PartAdjMcsClaim.Fields.idrAmbDropoffZipcode,
            9);
  }

  // endregion McsClaim

  // region McsAdjustments

  @Test
  public void testAdjustmentIdrAdjDate() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjDate,
            PartAdjMcsAdjustment::getIdrAdjDate,
            PartAdjMcsAdjustment.Fields.idrAdjDate);
  }

  @Test
  public void testAdjustmentIdrXrefIcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrXrefIcn,
            PartAdjMcsAdjustment::getIdrXrefIcn,
            PartAdjMcsAdjustment.Fields.idrXrefIcn,
            15);
  }

  @Test
  public void testAdjustmentIdrAdjClerk() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrAdjClerk,
            PartAdjMcsAdjustment::getIdrAdjClerk,
            PartAdjMcsAdjustment.Fields.idrAdjClerk,
            4);
  }

  @Test
  public void testAdjustmentIdrInitCcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrInitCcn,
            PartAdjMcsAdjustment::getIdrInitCcn,
            PartAdjMcsAdjustment.Fields.idrInitCcn,
            15);
  }

  @Test
  public void testAdjustmentIdrAdjChkWrtDt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjChkWrtDt,
            PartAdjMcsAdjustment::getIdrAdjChkWrtDt,
            PartAdjMcsAdjustment.Fields.idrAdjChkWrtDt);
  }

  @Test
  public void testAdjustmentIdrAdjBEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjBEombAmt,
            PartAdjMcsAdjustment::getIdrAdjBEombAmt,
            PartAdjMcsAdjustment.Fields.idrAdjBEombAmt);
  }

  @Test
  public void testAdjustmentIdrAdjPEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjPEombAmt,
            PartAdjMcsAdjustment::getIdrAdjPEombAmt,
            PartAdjMcsAdjustment.Fields.idrAdjPEombAmt);
  }

  // endregion McsAdjustments

  // region McsAudit

  @Test
  public void testAuditIdrJAuditNum() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyIntFieldCopiedCorrectly(
            McsAudit.Builder::setIdrJAuditNum, PartAdjMcsAudit::getIdrJAuditNum);
  }

  @Test
  public void testAuditIdrJAuditInd() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsAudit.Builder::setIdrJAuditIndEnum,
            PartAdjMcsAudit::getIdrJAuditInd,
            McsCutbackAuditIndicator.CUTBACK_AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .verifyStringFieldCopiedCorrectly(
            McsAudit.Builder::setIdrJAuditIndUnrecognized,
            PartAdjMcsAudit::getIdrJAuditInd,
            PartAdjMcsAudit.Fields.idrJAuditInd,
            1);
  }

  @Test
  public void testAuditIdrJAuditDisp() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsAudit.Builder::setIdrJAuditDispEnum,
            PartAdjMcsAudit::getIdrJAuditDisp,
            McsCutbackAuditDisposition.CUTBACK_AUDIT_DISPOSITION_ADS_LETTER,
            "S")
        .verifyStringFieldCopiedCorrectly(
            McsAudit.Builder::setIdrJAuditDispUnrecognized,
            PartAdjMcsAudit::getIdrJAuditDisp,
            PartAdjMcsAudit.Fields.idrJAuditDisp,
            1);
  }

  // endregion McsAudit

  // region McsDiagnosisCode

  @Test
  public void testDiagnosisCodeIdrDiagCode() {
    new McsClaimTransformerTest.DiagCodeFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDiagnosisCode.Builder::setIdrDiagCode,
            PartAdjMcsDiagnosisCode::getIdrDiagCode,
            PartAdjMcsDiagnosisCode.Fields.idrDiagCode,
            7);
  }

  @Test
  public void testDiagnosisCodeIdrDiagIcdType() {
    new McsClaimTransformerTest.DiagCodeFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDiagnosisCode.Builder::setIdrDiagIcdTypeEnum,
            PartAdjMcsDiagnosisCode::getIdrDiagIcdType,
            McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD9,
            "9")
        .verifyStringFieldCopiedCorrectly(
            McsDiagnosisCode.Builder::setIdrDiagIcdTypeUnrecognized,
            PartAdjMcsDiagnosisCode::getIdrDiagIcdType,
            PartAdjMcsDiagnosisCode.Fields.idrDiagIcdType,
            1);
  }

  // endregion McsDiagnosisCode

  // region McsDetail
  @Test
  public void testDetailIdrDtlStatus() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrDtlStatusEnum,
            PartAdjMcsDetail::getIdrDtlStatus,
            McsDetailStatus.DETAIL_STATUS_FINAL,
            "F")
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlStatusUnrecognized,
            PartAdjMcsDetail::getIdrDtlStatus,
            PartAdjMcsDetail.Fields.idrDtlStatus,
            1);
  }

  @Test
  public void testDetailIdrDtlFromDate() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsDetail.Builder::setIdrDtlFromDate,
            PartAdjMcsDetail::getIdrDtlFromDate,
            PartAdjMcsDetail.Fields.idrDtlFromDate);
  }

  @Test
  public void testDetailIdrDtlToDate() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsDetail.Builder::setIdrDtlToDate,
            PartAdjMcsDetail::getIdrDtlToDate,
            PartAdjMcsDetail.Fields.idrDtlToDate);
  }

  @Test
  public void testDetailIdrProcCode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrProcCode,
            PartAdjMcsDetail::getIdrProcCode,
            PartAdjMcsDetail.Fields.idrProcCode,
            5);
  }

  @Test
  public void testDetailIdrModOne() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModOne,
            PartAdjMcsDetail::getIdrModOne,
            PartAdjMcsDetail.Fields.idrModOne,
            2);
  }

  @Test
  public void testDetailIdrModTwo() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModTwo,
            PartAdjMcsDetail::getIdrModTwo,
            PartAdjMcsDetail.Fields.idrModTwo,
            2);
  }

  @Test
  public void testDetailIdrModThree() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModThree,
            PartAdjMcsDetail::getIdrModThree,
            PartAdjMcsDetail.Fields.idrModThree,
            2);
  }

  @Test
  public void testDetailIdrModFour() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModFour,
            PartAdjMcsDetail::getIdrModFour,
            PartAdjMcsDetail.Fields.idrModFour,
            2);
  }

  @Test
  public void testDetailIdrDtlDiagIcdType() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrDtlDiagIcdTypeEnum,
            PartAdjMcsDetail::getIdrDtlDiagIcdType,
            McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10,
            "0")
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlDiagIcdTypeUnrecognized,
            PartAdjMcsDetail::getIdrDtlDiagIcdType,
            PartAdjMcsDetail.Fields.idrDtlDiagIcdType,
            1);
  }

  @Test
  public void testDetailIdrDtlPrimaryDiagCode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlPrimaryDiagCode,
            PartAdjMcsDetail::getIdrDtlPrimaryDiagCode,
            PartAdjMcsDetail.Fields.idrDtlPrimaryDiagCode,
            7);
  }

  @Test
  public void testDetailIdrKPosLnameOrg() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosLnameOrg,
            PartAdjMcsDetail::getIdrKPosLnameOrg,
            PartAdjMcsDetail.Fields.idrKPosLnameOrg,
            60);
  }

  @Test
  public void testDetailIdrKPosFname() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosFname,
            PartAdjMcsDetail::getIdrKPosFname,
            PartAdjMcsDetail.Fields.idrKPosFname,
            35);
  }

  @Test
  public void testDetailIdrKPosMname() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosMname,
            PartAdjMcsDetail::getIdrKPosMname,
            PartAdjMcsDetail.Fields.idrKPosMname,
            25);
  }

  @Test
  public void testDetailIdrKPosAddr1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr1,
            PartAdjMcsDetail::getIdrKPosAddr1,
            PartAdjMcsDetail.Fields.idrKPosAddr1,
            55);
  }

  @Test
  public void testDetailIdrKPosAddr21St() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr21St,
            PartAdjMcsDetail::getIdrKPosAddr2_1st,
            PartAdjMcsDetail.Fields.idrKPosAddr2_1st,
            30);
  }

  @Test
  public void testDetailIdrKPosAddr22Nd() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr22Nd,
            PartAdjMcsDetail::getIdrKPosAddr2_2nd,
            PartAdjMcsDetail.Fields.idrKPosAddr2_2nd,
            25);
  }

  @Test
  public void testDetailIdrKPosCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosCity,
            PartAdjMcsDetail::getIdrKPosCity,
            PartAdjMcsDetail.Fields.idrKPosCity,
            30);
  }

  @Test
  public void testDetailIdrKPosState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosState,
            PartAdjMcsDetail::getIdrKPosState,
            PartAdjMcsDetail.Fields.idrKPosState,
            2);
  }

  @Test
  public void testDetailIdrKPosZip() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosZip,
            PartAdjMcsDetail::getIdrKPosZip,
            PartAdjMcsDetail.Fields.idrKPosZip,
            15);
  }

  @Test
  public void testDetailIdrTos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrTosEnum,
            PartAdjMcsDetail::getIdrTos,
            McsTypeOfService.TYPE_OF_SERVICE_ANESTHESIA,
            "7")
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrTosUnrecognized,
            PartAdjMcsDetail::getIdrTos,
            PartAdjMcsDetail.Fields.idrTos,
            1);
  }

  @Test
  public void testDetailIdrTwoDigitPos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrTwoDigitPosEnum,
            PartAdjMcsDetail::getIdrTwoDigitPos,
            McsTwoDigitPlanOfService.TWO_DIGIT_PLAN_OF_SERVICE_AMBULANCE_LAND,
            "41")
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrTwoDigitPosUnrecognized,
            PartAdjMcsDetail::getIdrTwoDigitPos,
            PartAdjMcsDetail.Fields.idrTwoDigitPos,
            2);
  }

  @Test
  public void testDetailIdrDtlRendType() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendType,
            PartAdjMcsDetail::getIdrDtlRendType,
            PartAdjMcsDetail.Fields.idrDtlRendType,
            2);
  }

  @Test
  public void testDetailIdrDtlRendSpec() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendSpec,
            PartAdjMcsDetail::getIdrDtlRendSpec,
            PartAdjMcsDetail.Fields.idrDtlRendSpec,
            2);
  }

  @Test
  public void testDetailIdrDtlRendNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendNpi,
            PartAdjMcsDetail::getIdrDtlRendNpi,
            PartAdjMcsDetail.Fields.idrDtlRendNpi,
            10);
  }

  @Test
  public void testDetailIdrDtlRendProv() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendProv,
            PartAdjMcsDetail::getIdrDtlRendProv,
            PartAdjMcsDetail.Fields.idrDtlRendProv,
            10);
  }

  @Test
  public void testDetailIdrKDtlFacProvNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKDtlFacProvNpi,
            PartAdjMcsDetail::getIdrKDtlFacProvNpi,
            PartAdjMcsDetail.Fields.idrKDtlFacProvNpi,
            10);
  }

  @Test
  public void testDetailIdrDtlAmbPickupAddres1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupAddres1,
            PartAdjMcsDetail::getIdrDtlAmbPickupAddres1,
            PartAdjMcsDetail.Fields.idrDtlAmbPickupAddres1,
            25);
  }

  @Test
  public void testDetailIdrDtlAmbPickupAddres2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupAddres2,
            PartAdjMcsDetail::getIdrDtlAmbPickupAddres2,
            PartAdjMcsDetail.Fields.idrDtlAmbPickupAddres2,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbPickupCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupCity,
            PartAdjMcsDetail::getIdrDtlAmbPickupCity,
            PartAdjMcsDetail.Fields.idrDtlAmbPickupCity,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbPickupState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupState,
            PartAdjMcsDetail::getIdrDtlAmbPickupState,
            PartAdjMcsDetail.Fields.idrDtlAmbPickupState,
            2);
  }

  @Test
  public void testDetailIdrDtlAmbPickupZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupZipcode,
            PartAdjMcsDetail::getIdrDtlAmbPickupZipcode,
            PartAdjMcsDetail.Fields.idrDtlAmbPickupZipcode,
            9);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffName() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffName,
            PartAdjMcsDetail::getIdrDtlAmbDropoffName,
            PartAdjMcsDetail.Fields.idrDtlAmbDropoffName,
            24);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffAddrL1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL1,
            PartAdjMcsDetail::getIdrDtlAmbDropoffAddrL1,
            PartAdjMcsDetail.Fields.idrDtlAmbDropoffAddrL1,
            25);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffAddrL2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL2,
            PartAdjMcsDetail::getIdrDtlAmbDropoffAddrL2,
            PartAdjMcsDetail.Fields.idrDtlAmbDropoffAddrL2,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffCity,
            PartAdjMcsDetail::getIdrDtlAmbDropoffCity,
            PartAdjMcsDetail.Fields.idrDtlAmbDropoffCity,
            20);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffState,
            PartAdjMcsDetail::getIdrDtlAmbDropoffState,
            PartAdjMcsDetail.Fields.idrDtlAmbDropoffState,
            2);
  }

  @Test
  public void testDetailIdrDtlAmbDropoffZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffZipcode,
            PartAdjMcsDetail::getIdrDtlAmbDropoffZipcode,
            PartAdjMcsDetail.Fields.idrDtlAmbDropoffZipcode,
            9);
  }

  // endregion McsDetail

  // region McsLocation

  @Test
  public void testLocationIdrLocClerk() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsLocation.Builder::setIdrLocClerk,
            PartAdjMcsLocation::getIdrLocClerk,
            PartAdjMcsLocation.Fields.idrLocClerk,
            4);
  }

  @Test
  public void testLocationIdrLocCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsLocation.Builder::setIdrLocCode,
            PartAdjMcsLocation::getIdrLocCode,
            PartAdjMcsLocation.Fields.idrLocCode,
            3);
  }

  @Test
  public void testLocationIdrLocDate() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsLocation.Builder::setIdrLocDate,
            PartAdjMcsLocation::getIdrLocDate,
            PartAdjMcsLocation.Fields.idrLocDate);
  }

  @Test
  public void testLocationIdrLocActvCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsLocation.Builder::setIdrLocActvCodeEnum,
            PartAdjMcsLocation::getIdrLocActvCode,
            McsLocationActivityCode.LOCATION_ACTIVITY_CODE_CAS_ACTIVITY,
            "Q")
        .verifyStringFieldCopiedCorrectly(
            McsLocation.Builder::setIdrLocActvCodeUnrecognized,
            PartAdjMcsLocation::getIdrLocActvCode,
            PartAdjMcsLocation.Fields.idrLocActvCode,
            1);
  }

  // endregion McsLocation

  private void assertChangeMatches(RdaChange.Type changeType) {
    RdaChange<PartAdjMcsClaim> changed = transformer.transformClaim(changeBuilder.build());
    assertEquals(changeType, changed.getType());
    assertThat(changed.getClaim(), samePropertyValuesAs(claim));
  }

  // region Field Tester Classes

  private abstract class AbstractFieldTester<TBuilder, TEntity>
      extends ClaimTransformerFieldTester<
          McsClaim.Builder, McsClaim, PartAdjMcsClaim, TBuilder, TEntity> {
    @Override
    McsClaim.Builder createClaimBuilder() {
      return McsClaim.newBuilder()
          .setIdrClmHdIcn("idrClmHdIcn")
          .setIdrContrId("contr")
          .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL);
    }

    @Override
    RdaChange<PartAdjMcsClaim> transformClaim(McsClaim claim) {
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
      extends McsClaimTransformerTest.AbstractFieldTester<McsClaim.Builder, PartAdjMcsClaim> {
    @Override
    McsClaim.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      return claimBuilder;
    }

    @Override
    PartAdjMcsClaim getTestEntity(PartAdjMcsClaim claim) {
      return claim;
    }
  }

  class AdjustmentFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<
          McsAdjustment.Builder, PartAdjMcsAdjustment> {
    @Override
    McsAdjustment.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsAdjustmentsBuilderList().isEmpty()) {
        claimBuilder.addMcsAdjustmentsBuilder();
      }
      return claimBuilder.getMcsAdjustmentsBuilder(0);
    }

    @Override
    PartAdjMcsAdjustment getTestEntity(PartAdjMcsClaim claim) {
      assertEquals(1, claim.getAdjustments().size());
      PartAdjMcsAdjustment answer = claim.getAdjustments().iterator().next();
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
      extends McsClaimTransformerTest.AbstractFieldTester<McsAudit.Builder, PartAdjMcsAudit> {
    @Override
    McsAudit.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsAuditsBuilderList().isEmpty()) {
        claimBuilder.addMcsAuditsBuilder();
      }
      return claimBuilder.getMcsAuditsBuilder(0);
    }

    @Override
    PartAdjMcsAudit getTestEntity(PartAdjMcsClaim claim) {
      assertEquals(1, claim.getAudits().size());
      PartAdjMcsAudit answer = claim.getAudits().iterator().next();
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
      extends McsClaimTransformerTest.AbstractFieldTester<McsDetail.Builder, PartAdjMcsDetail> {
    @Override
    McsDetail.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsDetailsBuilderList().isEmpty()) {
        claimBuilder.addMcsDetailsBuilder();
      }
      return claimBuilder.getMcsDetailsBuilder(0);
    }

    @Override
    PartAdjMcsDetail getTestEntity(PartAdjMcsClaim claim) {
      assertEquals(1, claim.getDetails().size());
      PartAdjMcsDetail answer = claim.getDetails().iterator().next();
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
          McsDiagnosisCode.Builder, PartAdjMcsDiagnosisCode> {
    @Override
    McsDiagnosisCode.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsDiagnosisCodesBuilderList().isEmpty()) {
        claimBuilder.addMcsDiagnosisCodesBuilder();
        claimBuilder.getMcsDiagnosisCodesBuilder(0).setIdrDiagCode("DC");
      }
      return claimBuilder.getMcsDiagnosisCodesBuilder(0);
    }

    @Override
    PartAdjMcsDiagnosisCode getTestEntity(PartAdjMcsClaim claim) {
      assertEquals(1, claim.getDiagCodes().size());
      PartAdjMcsDiagnosisCode answer = claim.getDiagCodes().iterator().next();
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
      extends McsClaimTransformerTest.AbstractFieldTester<McsLocation.Builder, PartAdjMcsLocation> {
    @Override
    McsLocation.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsLocationsBuilderList().isEmpty()) {
        claimBuilder.addMcsLocationsBuilder();
      }
      return claimBuilder.getMcsLocationsBuilder(0);
    }

    @Override
    PartAdjMcsLocation getTestEntity(PartAdjMcsClaim claim) {
      assertEquals(1, claim.getLocations().size());
      PartAdjMcsLocation answer = claim.getLocations().iterator().next();
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
