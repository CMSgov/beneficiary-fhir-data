package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.source.TransformerTestUtils.assertListContentsHaveSamePropertyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsBeneficiarySex;
import gov.cms.mpsm.rda.v1.mcs.McsBillingProviderStatusCode;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaimType;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDetailStatus;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisIcdType;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
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
    assertListContentsHaveSamePropertyValues(claim.getDetails(), transformed.getDetails());
  }

  @Test
  public void diagnosisCodes() {
    claim.setIdrClmHdIcn("123456789012345");
    claim.setIdrContrId("12345");
    claim.setIdrClaimType("3");
    claim.setLastUpdated(clock.instant());
    PreAdjMcsDiagnosisCode diagCode = new PreAdjMcsDiagnosisCode();
    diagCode.setIdrDiagIcdType("9");
    diagCode.setIdrDiagCode("1234567");
    diagCode.setLastUpdated(clock.instant());
    claim.getDiagCodes().add(diagCode);
    diagCode = new PreAdjMcsDiagnosisCode();
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
    assertListContentsHaveSamePropertyValues(claim.getDetails(), transformed.getDetails());
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
              new DataTransformer.ErrorMessage("idrClaimType", "no value set")),
          ex.getErrors());
    }
  }

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
  public void testBadDiagnosisCodeIdrDiagCode() {
    assertDiagnosisCodeTransformationError(
        codeBuilder -> codeBuilder.setIdrDiagCode("1234567---"),
        new DataTransformer.ErrorMessage(
            "diagCode-0-idrDiagCode", "invalid length: expected=[1,7] actual=10"));
  }

  @Test
  public void testBadDiagnosisCodeIdrDiagIcdType() {
    assertDiagnosisCodeTransformationError(
        codeBuilder -> codeBuilder.setIdrDiagIcdTypeEnumUnrecognized("sdjbfdskjbdfskjbsdf---"),
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
}
