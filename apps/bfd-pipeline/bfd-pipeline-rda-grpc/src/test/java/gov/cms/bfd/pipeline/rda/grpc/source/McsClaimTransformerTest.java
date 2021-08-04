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
import gov.cms.mpsm.rda.v1.ClaimChange;
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
  private ClaimChange.Builder changeBuilder;
  private McsClaim.Builder claimBuilder;
  private PreAdjMcsClaim claim;

  @Before
  public void setUp() {
    changeBuilder = ClaimChange.newBuilder();
    claimBuilder = McsClaim.newBuilder();
    claim = new PreAdjMcsClaim();
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
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_INSERT)
        .setMcsClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  @Test
  public void allFields() {
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
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_INSERT)
        .setMcsClaim(claimBuilder.build());
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
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_INSERT)
        .setMcsClaim(claimBuilder.build());
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
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_INSERT)
        .setMcsClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
    PreAdjMcsClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertListContentsHaveSamePropertyValues(
        claim.getDetails(), transformed.getDetails(), PreAdjMcsDetail::getPriority);
  }

  @Test
  public void requiredFieldsMissing() {
    try {
      changeBuilder
          .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
          .setMcsClaim(
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
                  "diagCode-0-idrClmHdIcn", "invalid length: expected=[1,15] actual=0")),
          ex.getErrors());
    }
  }

  @Test
  public void allBadFields() {
    try {
      claimBuilder
          .setIdrClmHdIcn("123456789012345---")
          .setIdrContrId("12345---")
          .setIdrHic("123456789012---")
          .setIdrClaimTypeUnrecognized("55558873478237821782317821782317823783287")
          .setIdrDtlCnt(0)
          .setIdrBeneLast16("123456---")
          .setIdrBeneFirstInit("7---")
          .setIdrBeneMidInit("8---")
          .setIdrBeneSexEnum(McsBeneficiarySex.BENEFICIARY_SEX_FEMALE)
          .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A)
          .setIdrStatusDate("2020-02-03---")
          .setIdrBillProvNpi("CDEFGHIJKL---")
          .setIdrBillProvNum("MNOPQRSTUV---")
          .setIdrBillProvEin("WXYZabcdef---")
          .setIdrBillProvType("RS---")
          .setIdrBillProvSpec("tu---")
          .setIdrBillProvGroupIndUnrecognized("v---")
          .setIdrBillProvPriceSpec("rw---")
          .setIdrBillProvCounty("34---")
          .setIdrBillProvLoc("43---")
          .setIdrTotAllowed("12345.42---")
          .setIdrCoinsurance("67890.94---")
          .setIdrDeductible("87945.28---")
          .setIdrBillProvStatusCdEnum(
              McsBillingProviderStatusCode.BILLING_PROVIDER_STATUS_CODE_PARTICIPATING)
          .setIdrTotBilledAmt("67591.96---")
          .setIdrClaimReceiptDate("2020-02-01---")
          .setIdrClaimMbi("5467891245678---")
          .setIdrHdrFromDos("2020-01-07---")
          .setIdrHdrToDos("2020-01-14---")
          .addMcsDiagnosisCodes(
              McsDiagnosisCode.newBuilder()
                  .setIdrClmHdIcn("123456789012345---")
                  .setIdrDiagCode("1234567---")
                  .setIdrDiagIcdTypeEnumUnrecognized("sdjbfdskjbdfskjbsdf---")
                  .build())
          .addMcsDetails(
              McsDetail.newBuilder()
                  .setIdrDtlStatusEnum(McsDetailStatus.DETAIL_STATUS_FINAL)
                  .setIdrDtlFromDate("--not-a-date--")
                  .setIdrDtlToDate("--not-a-date--")
                  .setIdrProcCode("abCDe---")
                  .setIdrModOne("aB---")
                  .setIdrModTwo("Cd---")
                  .setIdrModThree("EF---")
                  .setIdrModFour("gh---")
                  .setIdrDtlDiagIcdTypeUnrecognized("jbkasjdbkjadsfbflasdbglbasdfljbfdsaj---")
                  .setIdrDtlPrimaryDiagCode("hetwpqj---")
                  .setIdrKPosLnameOrg(
                      "123456789012345678901234567890123456789012345678901234567890---")
                  .setIdrKPosFname("12345678901234567890123456789012345---")
                  .setIdrKPosMname("1234567890123456789012345---")
                  .setIdrKPosAddr1("1234567890123456789012345678901234567890123456789012345---")
                  .setIdrKPosAddr21St("123456789012345678901234567890---")
                  .setIdrKPosAddr22Nd("1234567890123456789012345---")
                  .setIdrKPosCity("123456789012345678901234567890---")
                  .setIdrKPosState("12---")
                  .setIdrKPosZip("123456789012345---")
                  .build());
      changeBuilder
          .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
          .setMcsClaim(claimBuilder.build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          ImmutableList.of(
              new DataTransformer.ErrorMessage(
                  "idrClmHdIcn", "invalid length: expected=[1,15] actual=18"),
              new DataTransformer.ErrorMessage(
                  "idrContrId", "invalid length: expected=[1,5] actual=8"),
              new DataTransformer.ErrorMessage(
                  "idrHic", "invalid length: expected=[1,12] actual=15"),
              new DataTransformer.ErrorMessage(
                  "idrClaimType", "invalid length: expected=[1,1] actual=41"),
              new DataTransformer.ErrorMessage(
                  "idrBeneLast_1_6", "invalid length: expected=[1,6] actual=9"),
              new DataTransformer.ErrorMessage(
                  "idrBeneFirstInit", "invalid length: expected=[1,1] actual=4"),
              new DataTransformer.ErrorMessage(
                  "idrBeneMidInit", "invalid length: expected=[1,1] actual=4"),
              new DataTransformer.ErrorMessage("idrStatusDate", "invalid date"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvNpi", "invalid length: expected=[1,10] actual=13"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvNum", "invalid length: expected=[1,10] actual=13"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvEin", "invalid length: expected=[1,10] actual=13"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvType", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvSpec", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvGroupInd", "invalid length: expected=[1,1] actual=4"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvType", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvSpec", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvGroupInd", "invalid length: expected=[1,1] actual=4"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvPriceSpec", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvCounty", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "idrBillProvLoc", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage("idrTotAllowed", "invalid amount"),
              new DataTransformer.ErrorMessage("idrCoinsurance", "invalid amount"),
              new DataTransformer.ErrorMessage("idrDeductible", "invalid amount"),
              new DataTransformer.ErrorMessage("idrTotBilledAmt", "invalid amount"),
              new DataTransformer.ErrorMessage("idrClaimReceiptDate", "invalid date"),
              new DataTransformer.ErrorMessage(
                  "idrClaimMbi", "invalid length: expected=[1,13] actual=16"),
              new DataTransformer.ErrorMessage("idrHdrFromDateOfSvc", "invalid date"),
              new DataTransformer.ErrorMessage("idrHdrToDateOfSvc", "invalid date"),
              new DataTransformer.ErrorMessage(
                  "diagCode-0-idrClmHdIcn", "invalid length: expected=[1,15] actual=18"),
              new DataTransformer.ErrorMessage(
                  "diagCode-0-idrDiagIcdType", "invalid length: expected=[1,1] actual=22"),
              new DataTransformer.ErrorMessage(
                  "diagCode-0-idrDiagCode", "invalid length: expected=[1,7] actual=10"),
              new DataTransformer.ErrorMessage("detail-0-idrDtlFromDate", "invalid date"),
              new DataTransformer.ErrorMessage("detail-0-idrDtlToDate", "invalid date"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrProcCode", "invalid length: expected=[1,5] actual=8"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrModOne", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrModTwo", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrModThree", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrModFour", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrDtlDiagIcdType", "invalid length: expected=[1,1] actual=39"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrDtlPrimaryDiagCode", "invalid length: expected=[1,7] actual=10"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosLnameOrg", "invalid length: expected=[1,60] actual=63"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosFname", "invalid length: expected=[1,35] actual=38"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosMname", "invalid length: expected=[1,25] actual=28"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosAddr1", "invalid length: expected=[1,55] actual=58"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosAddr2_1st", "invalid length: expected=[1,30] actual=33"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosAddr2_2nd", "invalid length: expected=[1,25] actual=28"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosCity", "invalid length: expected=[1,30] actual=33"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosState", "invalid length: expected=[1,2] actual=5"),
              new DataTransformer.ErrorMessage(
                  "detail-0-idrKPosZip", "invalid length: expected=[1,15] actual=18")),
          ex.getErrors());
    }
  }

  @Test
  public void unrecognizedStatusCode() {
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL)
        .setIdrStatusCodeUnrecognized("X");
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_INSERT)
        .setMcsClaim(claimBuilder.build());
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
    changeBuilder
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_INSERT)
        .setMcsClaim(claimBuilder.build());
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
