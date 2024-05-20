package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static gov.cms.bfd.pipeline.rda.grpc.source.TransformerTestUtils.assertListContentsHaveSamePropertyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.entities.RdaMcsAdjustment;
import gov.cms.bfd.model.rda.entities.RdaMcsAudit;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaMcsLocation;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RecordSource;
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

/**
 * Unit tests for the {@link McsClaimTransformer}. Unless otherwise stated on a method every test
 * verifies that one or a set of fields within a source grpc message object for a claim have been
 * correctly transformed into appropriate values and copied into a new {@link RdaMcsClaim} JPA
 * entity object or one of its child objects.
 *
 * <p>Field tests are performed using an adaptor object appropriate for each type of grpc/jpa object
 * pair. These adaptor objects ({@link ClaimFieldTester}, {@link AdjustmentFieldTester}, {@link
 * AuditFieldTester}, {@link DetailFieldTester}, and {@link DiagCodeFieldTester}) extend the {@link
 * LocationFieldTester} class and provide class specific implementations of the methods used to
 * construct and transform objects under test.
 *
 * <p>Each individual field test is named after the field it tests and calls appropriate
 * verification methods for that field. {@see ClaimTransformerFieldTester} for documentation of each
 * of the verification methods.
 */
public class McsClaimTransformerTest {
  /** Clock for making timestamps. using a fixed Clock ensures our timestamp is predictable. */
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);

  /** The test hasher. */
  private final IdHasher idHasher =
      new IdHasher(new IdHasher.Config(10, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));

  /** The transformer under test. */
  private final McsClaimTransformer transformer =
      new McsClaimTransformer(clock, MbiCache.computedCache(idHasher.getConfig()));

  /** Creates MCS claim changes for testing changes are as expected. */
  private McsClaimChange.Builder changeBuilder;

  /** Creates a claim for testing in changes and transformations. */
  private McsClaim.Builder claimBuilder;

  /** A claim object used for validation (the expected value of the transformation/change). */
  private RdaMcsClaim claim;

  /** Resets test objects and sets up shared resources between each test. */
  @BeforeEach
  public void setUp() {
    changeBuilder = McsClaimChange.newBuilder();
    claimBuilder = McsClaim.newBuilder();
    claim = new RdaMcsClaim();
    claim.setSequenceNumber(0L);
  }

  /**
   * Tests the minimum valid claim and a change built from a minimum valid claim builder result in
   * the same final claim properties.
   */
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
        .setClaim(claimBuilder.build())
        .setSource(
            RecordSource.newBuilder()
                .setPhase("P1")
                .setPhaseSeqNum(0)
                .setExtractDate("1970-01-01")
                .setTransmissionTimestamp("1970-01-01T00:00:00.000001Z")
                .build());
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
    final RdaMcsDetail detail = new RdaMcsDetail();
    detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    detail.setIdrDtlNumber((short) 0);
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
    detail.setIdrDtlNdc("00002060440");
    detail.setIdrDtlNdcUnitCount("1");
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
                .setIdrDtlNdc("00002060440")
                .setIdrDtlNdcUnitCount("1")
                .build());
    changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
    RdaMcsClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertListContentsHaveSamePropertyValues(
        claim.getDetails(), transformed.getDetails(), RdaMcsDetail::getIdrDtlNumber);
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
    RdaMcsDiagnosisCode diagCode = new RdaMcsDiagnosisCode();
    diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    diagCode.setRdaPosition((short) 1);
    diagCode.setIdrDiagIcdType("9");
    diagCode.setIdrDiagCode("1234567");
    claim.getDiagCodes().add(diagCode);
    diagCode = new RdaMcsDiagnosisCode();
    diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    diagCode.setRdaPosition((short) 2);
    diagCode.setIdrDiagIcdType("0");
    diagCode.setIdrDiagCode("jdsyejs");
    claim.getDiagCodes().add(diagCode);
    claimBuilder
        .setIdrClmHdIcn("123456789012345")
        .setIdrContrId("12345")
        .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL)
        .addMcsDiagnosisCodes(
            McsDiagnosisCode.newBuilder()
                .setIdrClmHdIcn("123456789012345")
                .setRdaPosition(1)
                .setIdrDiagCode("1234567")
                .setIdrDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD9)
                .build())
        .addMcsDiagnosisCodes(
            McsDiagnosisCode.newBuilder()
                .setIdrClmHdIcn("123456789012345")
                .setRdaPosition(2)
                .setIdrDiagCode("jdsyejs")
                .setIdrDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10)
                .build());
    changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_INSERT).setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
    RdaMcsClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertListContentsHaveSamePropertyValues(
        claim.getDetails(), transformed.getDetails(), RdaMcsDetail::getIdrDtlNumber);
  }

  /**
   * Validates that missing fields in a change generate the expected error messages when
   * transforming that change via the {@link McsClaimTransformer}.
   */
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
              new DataTransformer.ErrorMessage("idrClaimType", "no value set"));

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

  /**
   * Tests the idrClmHdIcn field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrClmHdIcn() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClmHdIcn,
            RdaMcsClaim::getIdrClmHdIcn,
            RdaMcsClaim.Fields.idrClmHdIcn,
            15);
  }

  /**
   * Tests the idrContrId field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrContrId() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrContrId,
            RdaMcsClaim::getIdrContrId,
            RdaMcsClaim.Fields.idrContrId,
            5);
  }

  /**
   * Tests the idrHic field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrHic() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrHic, RdaMcsClaim::getIdrHic, RdaMcsClaim.Fields.idrHic, 12);
  }

  /**
   * Tests the idrClaimType field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrClaimType() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrClaimTypeEnum,
            RdaMcsClaim::getIdrClaimType,
            McsClaimType.CLAIM_TYPE_MEDICAL,
            "3")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrClaimTypeUnrecognized,
            RdaMcsClaim::getIdrClaimType,
            RdaMcsClaim.Fields.idrClaimType,
            1);
  }

  /**
   * Tests the idrBeneLast_1_6 field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBeneLast16() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneLast16,
            RdaMcsClaim::getIdrBeneLast_1_6,
            RdaMcsClaim.Fields.idrBeneLast_1_6,
            6);
  }

  /**
   * Tests the idrBeneFirstInit field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBeneFirstInit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneFirstInit,
            RdaMcsClaim::getIdrBeneFirstInit,
            RdaMcsClaim.Fields.idrBeneFirstInit,
            1);
  }

  /**
   * Tests the idrBeneMidInit field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBeneMidInit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBeneMidInit,
            RdaMcsClaim::getIdrBeneMidInit,
            RdaMcsClaim.Fields.idrBeneMidInit,
            1);
  }

  /**
   * Tests the idrBeneSex field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrBeneSex() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBeneSexEnum,
            RdaMcsClaim::getIdrBeneSex,
            McsBeneficiarySex.BENEFICIARY_SEX_MALE,
            "M")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrBeneSexUnrecognized,
            RdaMcsClaim::getIdrBeneSex,
            RdaMcsClaim.Fields.idrBeneSex,
            1);
  }

  /**
   * Tests the idrStatusCode field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrStatusCode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrStatusCodeEnum,
            RdaMcsClaim::getIdrStatusCode,
            McsStatusCode.STATUS_CODE_DENIED_E,
            "E")
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrStatusCodeUnrecognized,
            claim -> String.valueOf(claim.getIdrStatusCode()),
            RdaMcsClaim.Fields.idrStatusCode,
            1);
  }

  /**
   * Tests the idrStatusDate field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrStatusDate() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrStatusDate,
            RdaMcsClaim::getIdrStatusDate,
            RdaMcsClaim.Fields.idrStatusDate);
  }

  /**
   * Tests the idrBillProvNpi field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBillProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvNpi,
            RdaMcsClaim::getIdrBillProvNpi,
            RdaMcsClaim.Fields.idrBillProvNpi,
            10);
  }

  /**
   * Tests the idrBillProvNum field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBillProvNum() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvNum,
            RdaMcsClaim::getIdrBillProvNum,
            RdaMcsClaim.Fields.idrBillProvNum,
            10);
  }

  /**
   * Tests the idrBillProvEin field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBillProvEin() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvEin,
            RdaMcsClaim::getIdrBillProvEin,
            RdaMcsClaim.Fields.idrBillProvEin,
            10);
  }

  /**
   * Tests the idrBillProvType field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBillProvType() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvType,
            RdaMcsClaim::getIdrBillProvType,
            RdaMcsClaim.Fields.idrBillProvType,
            2);
  }

  /**
   * Tests the idrBillProvSpec field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBillProvSpec() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvSpec,
            RdaMcsClaim::getIdrBillProvSpec,
            RdaMcsClaim.Fields.idrBillProvSpec,
            2);
  }

  /**
   * Tests the idrBillProvGroupInd field is properly parsed and copied when a message object is
   * passed through the transformer.
   */
  @Test
  public void testClaimIdrBillProvGroupInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBillProvGroupIndEnum,
            RdaMcsClaim::getIdrBillProvGroupInd,
            McsBillingProviderIndicator.BILLING_PROVIDER_INDICATOR_GROUP,
            "G")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrBillProvGroupIndUnrecognized,
            RdaMcsClaim::getIdrBillProvGroupInd,
            RdaMcsClaim.Fields.idrBillProvGroupInd,
            1);
  }

  /**
   * Tests the idrBillProvPriceSpec field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrBillProvPriceSpec() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvPriceSpec,
            RdaMcsClaim::getIdrBillProvPriceSpec,
            RdaMcsClaim.Fields.idrBillProvPriceSpec,
            2);
  }

  /**
   * Tests the idrBillProvCounty field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrBillProvCounty() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvCounty,
            RdaMcsClaim::getIdrBillProvCounty,
            RdaMcsClaim.Fields.idrBillProvCounty,
            2);
  }

  /**
   * Tests the idrBillProvLoc field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBillProvLoc() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrBillProvLoc,
            RdaMcsClaim::getIdrBillProvLoc,
            RdaMcsClaim.Fields.idrBillProvLoc,
            2);
  }

  /**
   * Tests the idrTotAllowed field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrTotAllowed() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrTotAllowed,
            RdaMcsClaim::getIdrTotAllowed,
            RdaMcsClaim.Fields.idrTotAllowed);
  }

  /**
   * Tests the idrCoinsurance field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrCoinsurance() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrCoinsurance,
            RdaMcsClaim::getIdrCoinsurance,
            RdaMcsClaim.Fields.idrCoinsurance);
  }

  /**
   * Tests the idrDeductible field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrDeductible() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrDeductible,
            RdaMcsClaim::getIdrDeductible,
            RdaMcsClaim.Fields.idrDeductible);
  }

  /**
   * Tests the idrBillProvStatusCd field is properly parsed and copied when a message object is
   * passed through the transformer.
   */
  @Test
  public void testClaimIdrBillProvStatusCd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrBillProvStatusCdEnum,
            RdaMcsClaim::getIdrBillProvStatusCd,
            McsBillingProviderStatusCode.BILLING_PROVIDER_STATUS_CODE_NON_PARTICIPATING,
            "N")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrBillProvStatusCdUnrecognized,
            RdaMcsClaim::getIdrBillProvStatusCd,
            RdaMcsClaim.Fields.idrBillProvStatusCd,
            1);
  }

  /**
   * Tests the idrTotBilledAmt field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrBilledAmt() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrTotBilledAmt,
            RdaMcsClaim::getIdrTotBilledAmt,
            RdaMcsClaim.Fields.idrTotBilledAmt);
  }

  /**
   * Tests the idrClaimReceiptDate field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrClaimReceiptDate() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrClaimReceiptDate,
            RdaMcsClaim::getIdrClaimReceiptDate,
            RdaMcsClaim.Fields.idrClaimReceiptDate);
  }

  /**
   * Tests the idrClaimMbi field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrClaimMbi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrClaimMbi,
            RdaMcsClaim::getIdrClaimMbi,
            RdaMcsClaim.Fields.idrClaimMbi,
            11)
        .verifyIdHashFieldPopulatedCorrectly(
            McsClaim.Builder::setIdrClaimMbi, RdaMcsClaim::getIdrClaimMbiHash, 11, idHasher);
  }

  /**
   * Tests the idrHdrFromDateOfSvc field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrHdrFromDos() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrHdrFromDos,
            RdaMcsClaim::getIdrHdrFromDateOfSvc,
            RdaMcsClaim.Fields.idrHdrFromDateOfSvc);
  }

  /**
   * Tests the idrHdrToDateOfSvc field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrHdrToDos() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsClaim.Builder::setIdrHdrToDos,
            RdaMcsClaim::getIdrHdrToDateOfSvc,
            RdaMcsClaim.Fields.idrHdrToDateOfSvc);
  }

  /**
   * Tests the idrAssignment field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrAssignment() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrAssignmentEnum,
            RdaMcsClaim::getIdrAssignment,
            McsClaimAssignmentCode.CLAIM_ASSIGNMENT_CODE,
            "A")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrAssignmentUnrecognized,
            RdaMcsClaim::getIdrAssignment,
            RdaMcsClaim.Fields.idrAssignment,
            1);
  }

  /**
   * Tests the idrClmLevelInd field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrClmLevelInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrClmLevelIndEnum,
            RdaMcsClaim::getIdrClmLevelInd,
            McsClaimLevelIndicator.CLAIM_LEVEL_INDICATOR_ORIGINAL,
            "O")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrClmLevelIndUnrecognized,
            RdaMcsClaim::getIdrClmLevelInd,
            RdaMcsClaim.Fields.idrClmLevelInd,
            1);
  }

  /**
   * Tests the idrHdrAudit field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrHdrAudit() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyIntFieldCopiedCorrectly(
            McsClaim.Builder::setIdrHdrAudit, RdaMcsClaim::getIdrHdrAudit);
  }

  /**
   * Tests the idrHdrAuditInd field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrHdrAuditInd() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrHdrAuditIndEnum,
            RdaMcsClaim::getIdrHdrAuditInd,
            McsAuditIndicator.AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrHdrAuditIndUnrecognized,
            RdaMcsClaim::getIdrHdrAuditInd,
            RdaMcsClaim.Fields.idrHdrAuditInd,
            1);
  }

  /**
   * Tests the idrUSplitReason field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrUSplitReason() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsClaim.Builder::setIdrUSplitReasonEnum,
            RdaMcsClaim::getIdrUSplitReason,
            McsSplitReasonCode.SPLIT_REASON_CODE_GHI_SPLIT,
            "4")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsClaim.Builder::setIdrUSplitReasonUnrecognized,
            RdaMcsClaim::getIdrUSplitReason,
            RdaMcsClaim.Fields.idrUSplitReason,
            1);
  }

  /**
   * Tests the idrJReferringProvNpi field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrJReferringProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrJReferringProvNpi,
            RdaMcsClaim::getIdrJReferringProvNpi,
            RdaMcsClaim.Fields.idrJReferringProvNpi,
            10);
  }

  /**
   * Tests the idrJFacProvNpi field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrJFacProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrJFacProvNpi,
            RdaMcsClaim::getIdrJFacProvNpi,
            RdaMcsClaim.Fields.idrJFacProvNpi,
            10);
  }

  /**
   * Tests the idrUDemoProvNpi field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrUDemoProvNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUDemoProvNpi,
            RdaMcsClaim::getIdrUDemoProvNpi,
            RdaMcsClaim.Fields.idrUDemoProvNpi,
            10);
  }

  /**
   * Tests the idrUSuperNpi field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrUSuperNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUSuperNpi,
            RdaMcsClaim::getIdrUSuperNpi,
            RdaMcsClaim.Fields.idrUSuperNpi,
            10);
  }

  /**
   * Tests the idrUFcadjBilNpi field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrUFcadjBilNpi() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrUFcadjBilNpi,
            RdaMcsClaim::getIdrUFcadjBilNpi,
            RdaMcsClaim.Fields.idrUFcadjBilNpi,
            10);
  }

  /**
   * Tests the idrAmbPickupAddresLine1 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrAmbPickupAddresLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupAddresLine1,
            RdaMcsClaim::getIdrAmbPickupAddresLine1,
            RdaMcsClaim.Fields.idrAmbPickupAddresLine1,
            25);
  }

  /**
   * Tests the idrAmbPickupAddresLine2 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrAmbPickupAddresLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupAddresLine2,
            RdaMcsClaim::getIdrAmbPickupAddresLine2,
            RdaMcsClaim.Fields.idrAmbPickupAddresLine2,
            20);
  }

  /**
   * Tests the idrAmbPickupCity field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testClaimIdrAmbPickupCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupCity,
            RdaMcsClaim::getIdrAmbPickupCity,
            RdaMcsClaim.Fields.idrAmbPickupCity,
            20);
  }

  /**
   * Tests the idrAmbPickupState field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrAmbPickupState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupState,
            RdaMcsClaim::getIdrAmbPickupState,
            RdaMcsClaim.Fields.idrAmbPickupState,
            2);
  }

  /**
   * Tests the idrAmbPickupZipcode field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrAmbPickupZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbPickupZipcode,
            RdaMcsClaim::getIdrAmbPickupZipcode,
            RdaMcsClaim.Fields.idrAmbPickupZipcode,
            9);
  }

  /**
   * Tests the idrAmbDropoffName field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrAmbDropoffName() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffName,
            RdaMcsClaim::getIdrAmbDropoffName,
            RdaMcsClaim.Fields.idrAmbDropoffName,
            24);
  }

  /**
   * Tests the idrAmbDropoffAddrLine1 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrAmbDropoffAddrLine1() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffAddrLine1,
            RdaMcsClaim::getIdrAmbDropoffAddrLine1,
            RdaMcsClaim.Fields.idrAmbDropoffAddrLine1,
            25);
  }

  /**
   * Tests the idrAmbDropoffAddrLine2 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testClaimIdrAmbDropoffAddrLine2() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffAddrLine2,
            RdaMcsClaim::getIdrAmbDropoffAddrLine2,
            RdaMcsClaim.Fields.idrAmbDropoffAddrLine2,
            20);
  }

  /**
   * Tests the idrAmbDropoffCity field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrAmbDropoffCity() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffCity,
            RdaMcsClaim::getIdrAmbDropoffCity,
            RdaMcsClaim.Fields.idrAmbDropoffCity,
            20);
  }

  /**
   * Tests the idrAmbDropoffState field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrAmbDropoffState() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffState,
            RdaMcsClaim::getIdrAmbDropoffState,
            RdaMcsClaim.Fields.idrAmbDropoffState,
            2);
  }

  /**
   * Tests the idrAmbDropoffZipcode field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testClaimIdrAmbDropoffZipcode() {
    new McsClaimTransformerTest.ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsClaim.Builder::setIdrAmbDropoffZipcode,
            RdaMcsClaim::getIdrAmbDropoffZipcode,
            RdaMcsClaim.Fields.idrAmbDropoffZipcode,
            9);
  }

  // endregion McsClaim

  // region McsAdjustments

  /**
   * Tests the idrAdjDate field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentIdrAdjDate() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjDate,
            RdaMcsAdjustment::getIdrAdjDate,
            RdaMcsAdjustment.Fields.idrAdjDate);
  }

  /**
   * Tests the idrXrefIcn field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentIdrXrefIcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrXrefIcn,
            RdaMcsAdjustment::getIdrXrefIcn,
            RdaMcsAdjustment.Fields.idrXrefIcn,
            15);
  }

  /**
   * Tests the idrAdjClerk field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentIdrAdjClerk() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrAdjClerk,
            RdaMcsAdjustment::getIdrAdjClerk,
            RdaMcsAdjustment.Fields.idrAdjClerk,
            4);
  }

  /**
   * Tests the idrInitCcn field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentIdrInitCcn() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsAdjustment.Builder::setIdrInitCcn,
            RdaMcsAdjustment::getIdrInitCcn,
            RdaMcsAdjustment.Fields.idrInitCcn,
            15);
  }

  /**
   * Tests the idrAdjChkWrtDt field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentIdrAdjChkWrtDt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjChkWrtDt,
            RdaMcsAdjustment::getIdrAdjChkWrtDt,
            RdaMcsAdjustment.Fields.idrAdjChkWrtDt);
  }

  /**
   * Tests the idrAdjBEombAmt field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentIdrAdjBEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjBEombAmt,
            RdaMcsAdjustment::getIdrAdjBEombAmt,
            RdaMcsAdjustment.Fields.idrAdjBEombAmt);
  }

  /**
   * Tests the idrAdjPEombAmt field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentIdrAdjPEombAmt() {
    new McsClaimTransformerTest.AdjustmentFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            McsAdjustment.Builder::setIdrAdjPEombAmt,
            RdaMcsAdjustment::getIdrAdjPEombAmt,
            RdaMcsAdjustment.Fields.idrAdjPEombAmt);
  }

  /**
   * Tests the rdaPosition field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAdjustmentRdaPosition() {
    new AdjustmentFieldTester()
        .verifyUIntFieldToShortFieldCopiedCorrectly(
            McsAdjustment.Builder::setRdaPosition,
            RdaMcsAdjustment::getRdaPosition,
            RdaMcsAdjustment.Fields.rdaPosition);
  }

  // endregion McsAdjustments

  // region McsAudit

  /**
   * Tests the idrJAuditNum field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAuditIdrJAuditNum() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyIntFieldCopiedCorrectly(
            McsAudit.Builder::setIdrJAuditNum, RdaMcsAudit::getIdrJAuditNum);
  }

  /**
   * Tests the idrJAuditInd field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testAuditIdrJAuditInd() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsAudit.Builder::setIdrJAuditIndEnum,
            RdaMcsAudit::getIdrJAuditInd,
            McsCutbackAuditIndicator.CUTBACK_AUDIT_INDICATOR_AUDIT_NUMBER,
            "A")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsAudit.Builder::setIdrJAuditIndUnrecognized,
            RdaMcsAudit::getIdrJAuditInd,
            RdaMcsAudit.Fields.idrJAuditInd,
            1);
  }

  /**
   * Tests the idrJAuditDisp field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testAuditIdrJAuditDisp() {
    new McsClaimTransformerTest.AuditFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsAudit.Builder::setIdrJAuditDispEnum,
            RdaMcsAudit::getIdrJAuditDisp,
            McsCutbackAuditDisposition.CUTBACK_AUDIT_DISPOSITION_ADS_LETTER,
            "S")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsAudit.Builder::setIdrJAuditDispUnrecognized,
            RdaMcsAudit::getIdrJAuditDisp,
            RdaMcsAudit.Fields.idrJAuditDisp,
            1);
  }

  /**
   * Tests the rdaPosition field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testAuditRdaPosition() {
    new AuditFieldTester()
        .verifyUIntFieldToShortFieldCopiedCorrectly(
            McsAudit.Builder::setRdaPosition,
            RdaMcsAudit::getRdaPosition,
            RdaMcsAudit.Fields.rdaPosition);
  }

  // endregion McsAudit

  // region McsDiagnosisCode

  /**
   * Tests the idrDiagCode field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDiagnosisCodeIdrDiagCode() {
    new McsClaimTransformerTest.DiagCodeFieldTester()
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDiagnosisCode.Builder::setIdrDiagCode,
            RdaMcsDiagnosisCode::getIdrDiagCode,
            RdaMcsDiagnosisCode.Fields.idrDiagCode,
            7);
  }

  /**
   * Tests the idrDiagIcdType field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDiagnosisCodeIdrDiagIcdType() {
    new McsClaimTransformerTest.DiagCodeFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDiagnosisCode.Builder::setIdrDiagIcdTypeEnum,
            RdaMcsDiagnosisCode::getIdrDiagIcdType,
            McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD9,
            "9")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDiagnosisCode.Builder::setIdrDiagIcdTypeUnrecognized,
            RdaMcsDiagnosisCode::getIdrDiagIcdType,
            RdaMcsDiagnosisCode.Fields.idrDiagIcdType,
            1);
  }

  /**
   * Tests the rdaPosition field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDiagnosisCodeRdaPosition() {
    new DiagCodeFieldTester()
        .verifyUIntFieldToShortFieldCopiedCorrectly(
            McsDiagnosisCode.Builder::setRdaPosition,
            RdaMcsDiagnosisCode::getRdaPosition,
            RdaMcsDiagnosisCode.Fields.rdaPosition);
  }

  // endregion McsDiagnosisCode

  // region McsDetail

  /**
   * Tests the idrDtlNumber field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlNumber() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyUIntFieldToShortFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlNumber,
            RdaMcsDetail::getIdrDtlNumber,
            RdaMcsDetail.Fields.idrDtlNumber);
  }

  /**
   * Tests the idrDtlStatus field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlStatus() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrDtlStatusEnum,
            RdaMcsDetail::getIdrDtlStatus,
            McsDetailStatus.DETAIL_STATUS_FINAL,
            "F")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrDtlStatusUnrecognized,
            RdaMcsDetail::getIdrDtlStatus,
            RdaMcsDetail.Fields.idrDtlStatus,
            1);
  }

  /**
   * Tests the idrDtlFromDate field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlFromDate() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsDetail.Builder::setIdrDtlFromDate,
            RdaMcsDetail::getIdrDtlFromDate,
            RdaMcsDetail.Fields.idrDtlFromDate);
  }

  /**
   * Tests the idrDtlToDate field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlToDate() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsDetail.Builder::setIdrDtlToDate,
            RdaMcsDetail::getIdrDtlToDate,
            RdaMcsDetail.Fields.idrDtlToDate);
  }

  /**
   * Tests the idrProcCode field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrProcCode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectlyEmptyIgnored(
            McsDetail.Builder::setIdrProcCode,
            RdaMcsDetail::getIdrProcCode,
            RdaMcsDetail.Fields.idrProcCode,
            5);
  }

  /**
   * Tests the idrModOne field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrModOne() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModOne,
            RdaMcsDetail::getIdrModOne,
            RdaMcsDetail.Fields.idrModOne,
            2);
  }

  /**
   * Tests the idrModTwo field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrModTwo() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModTwo,
            RdaMcsDetail::getIdrModTwo,
            RdaMcsDetail.Fields.idrModTwo,
            2);
  }

  /**
   * Tests the idrModThree field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrModThree() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModThree,
            RdaMcsDetail::getIdrModThree,
            RdaMcsDetail.Fields.idrModThree,
            2);
  }

  /**
   * Tests the idrModFour field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrModFour() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrModFour,
            RdaMcsDetail::getIdrModFour,
            RdaMcsDetail.Fields.idrModFour,
            2);
  }

  /**
   * Tests the idrDtlDiagIcdType field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlDiagIcdType() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrDtlDiagIcdTypeEnum,
            RdaMcsDetail::getIdrDtlDiagIcdType,
            McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10,
            "0")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrDtlDiagIcdTypeUnrecognized,
            RdaMcsDetail::getIdrDtlDiagIcdType,
            RdaMcsDetail.Fields.idrDtlDiagIcdType,
            1);
  }

  /**
   * Tests the idrDtlPrimaryDiagCode field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlPrimaryDiagCode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlPrimaryDiagCode,
            RdaMcsDetail::getIdrDtlPrimaryDiagCode,
            RdaMcsDetail.Fields.idrDtlPrimaryDiagCode,
            7);
  }

  /**
   * Tests the idrKPosLnameOrg field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosLnameOrg() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosLnameOrg,
            RdaMcsDetail::getIdrKPosLnameOrg,
            RdaMcsDetail.Fields.idrKPosLnameOrg,
            60);
  }

  /**
   * Tests the idrKPosFname field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosFname() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosFname,
            RdaMcsDetail::getIdrKPosFname,
            RdaMcsDetail.Fields.idrKPosFname,
            35);
  }

  /**
   * Tests the idrKPosMname field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosMname() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosMname,
            RdaMcsDetail::getIdrKPosMname,
            RdaMcsDetail.Fields.idrKPosMname,
            25);
  }

  /**
   * Tests the idrKPosAddr1 field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosAddr1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr1,
            RdaMcsDetail::getIdrKPosAddr1,
            RdaMcsDetail.Fields.idrKPosAddr1,
            55);
  }

  /**
   * Tests the idrKPosAddr2_1st field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosAddr21St() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr21St,
            RdaMcsDetail::getIdrKPosAddr2_1st,
            RdaMcsDetail.Fields.idrKPosAddr2_1st,
            30);
  }

  /**
   * Tests the idrKPosAddr2_2nd field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosAddr22Nd() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosAddr22Nd,
            RdaMcsDetail::getIdrKPosAddr2_2nd,
            RdaMcsDetail.Fields.idrKPosAddr2_2nd,
            25);
  }

  /**
   * Tests the idrKPosCity field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosCity,
            RdaMcsDetail::getIdrKPosCity,
            RdaMcsDetail.Fields.idrKPosCity,
            30);
  }

  /**
   * Tests the idrKPosState field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosState,
            RdaMcsDetail::getIdrKPosState,
            RdaMcsDetail.Fields.idrKPosState,
            2);
  }

  /**
   * Tests the idrKPosZip field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrKPosZip() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKPosZip,
            RdaMcsDetail::getIdrKPosZip,
            RdaMcsDetail.Fields.idrKPosZip,
            15);
  }

  /**
   * Tests the idrTos field is properly parsed and copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testDetailIdrTos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrTosEnum,
            RdaMcsDetail::getIdrTos,
            McsTypeOfService.TYPE_OF_SERVICE_ANESTHESIA,
            "7")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrTosUnrecognized,
            RdaMcsDetail::getIdrTos,
            RdaMcsDetail.Fields.idrTos,
            1);
  }

  /**
   * Tests the idrTwoDigitPos field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrTwoDigitPos() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsDetail.Builder::setIdrTwoDigitPosEnum,
            RdaMcsDetail::getIdrTwoDigitPos,
            McsTwoDigitPlanOfService.TWO_DIGIT_PLAN_OF_SERVICE_AMBULANCE_LAND,
            "41")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsDetail.Builder::setIdrTwoDigitPosUnrecognized,
            RdaMcsDetail::getIdrTwoDigitPos,
            RdaMcsDetail.Fields.idrTwoDigitPos,
            2);
  }

  /**
   * Tests the idrDtlRendType field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlRendType() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendType,
            RdaMcsDetail::getIdrDtlRendType,
            RdaMcsDetail.Fields.idrDtlRendType,
            2);
  }

  /**
   * Tests the idrDtlRendSpec field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlRendSpec() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendSpec,
            RdaMcsDetail::getIdrDtlRendSpec,
            RdaMcsDetail.Fields.idrDtlRendSpec,
            2);
  }

  /**
   * Tests the idrDtlRendNpi field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlRendNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendNpi,
            RdaMcsDetail::getIdrDtlRendNpi,
            RdaMcsDetail.Fields.idrDtlRendNpi,
            10);
  }

  /**
   * Tests the idrDtlRendProv field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlRendProv() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlRendProv,
            RdaMcsDetail::getIdrDtlRendProv,
            RdaMcsDetail.Fields.idrDtlRendProv,
            10);
  }

  /**
   * Tests the idrKDtlFacProvNpi field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testDetailIdrKDtlFacProvNpi() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrKDtlFacProvNpi,
            RdaMcsDetail::getIdrKDtlFacProvNpi,
            RdaMcsDetail.Fields.idrKDtlFacProvNpi,
            10);
  }

  /**
   * Tests the idrDtlAmbPickupAddres1 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbPickupAddres1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupAddres1,
            RdaMcsDetail::getIdrDtlAmbPickupAddres1,
            RdaMcsDetail.Fields.idrDtlAmbPickupAddres1,
            25);
  }

  /**
   * Tests the idrDtlAmbPickupAddres2 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbPickupAddres2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupAddres2,
            RdaMcsDetail::getIdrDtlAmbPickupAddres2,
            RdaMcsDetail.Fields.idrDtlAmbPickupAddres2,
            20);
  }

  /**
   * Tests the idrDtlAmbPickupCity field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbPickupCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupCity,
            RdaMcsDetail::getIdrDtlAmbPickupCity,
            RdaMcsDetail.Fields.idrDtlAmbPickupCity,
            20);
  }

  /**
   * Tests the idrDtlAmbPickupState field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbPickupState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupState,
            RdaMcsDetail::getIdrDtlAmbPickupState,
            RdaMcsDetail.Fields.idrDtlAmbPickupState,
            2);
  }

  /**
   * Tests the idrDtlAmbPickupZipcode field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbPickupZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbPickupZipcode,
            RdaMcsDetail::getIdrDtlAmbPickupZipcode,
            RdaMcsDetail.Fields.idrDtlAmbPickupZipcode,
            9);
  }

  /**
   * Tests the idrDtlAmbDropoffName field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbDropoffName() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffName,
            RdaMcsDetail::getIdrDtlAmbDropoffName,
            RdaMcsDetail.Fields.idrDtlAmbDropoffName,
            24);
  }

  /**
   * Tests the idrDtlAmbDropoffAddrL1 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbDropoffAddrL1() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL1,
            RdaMcsDetail::getIdrDtlAmbDropoffAddrL1,
            RdaMcsDetail.Fields.idrDtlAmbDropoffAddrL1,
            25);
  }

  /**
   * Tests the idrDtlAmbDropoffAddrL2 field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbDropoffAddrL2() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffAddrL2,
            RdaMcsDetail::getIdrDtlAmbDropoffAddrL2,
            RdaMcsDetail.Fields.idrDtlAmbDropoffAddrL2,
            20);
  }

  /**
   * Tests the idrDtlAmbDropoffCity field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbDropoffCity() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffCity,
            RdaMcsDetail::getIdrDtlAmbDropoffCity,
            RdaMcsDetail.Fields.idrDtlAmbDropoffCity,
            20);
  }

  /**
   * Tests the idrDtlAmbDropoffState field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbDropoffState() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffState,
            RdaMcsDetail::getIdrDtlAmbDropoffState,
            RdaMcsDetail.Fields.idrDtlAmbDropoffState,
            2);
  }

  /**
   * Tests the idrDtlAmbDropoffZipcode field is properly copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testDetailIdrDtlAmbDropoffZipcode() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlAmbDropoffZipcode,
            RdaMcsDetail::getIdrDtlAmbDropoffZipcode,
            RdaMcsDetail.Fields.idrDtlAmbDropoffZipcode,
            9);
  }

  // endregion McsDetail

  // region McsLocation

  /**
   * Tests the idrLocClerk field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testLocationIdrLocClerk() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsLocation.Builder::setIdrLocClerk,
            RdaMcsLocation::getIdrLocClerk,
            RdaMcsLocation.Fields.idrLocClerk,
            4);
  }

  /**
   * Tests the idrLocCode field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testLocationIdrLocCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsLocation.Builder::setIdrLocCode,
            RdaMcsLocation::getIdrLocCode,
            RdaMcsLocation.Fields.idrLocCode,
            3);
  }

  /**
   * Tests the idrLocDate field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testLocationIdrLocDate() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            McsLocation.Builder::setIdrLocDate,
            RdaMcsLocation::getIdrLocDate,
            RdaMcsLocation.Fields.idrLocDate);
  }

  /**
   * Tests the idrLocActvCode field is properly parsed and copied when a message object is passed
   * through the transformer.
   */
  @Test
  public void testLocationIdrLocActvCode() {
    new McsClaimTransformerTest.LocationFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            McsLocation.Builder::setIdrLocActvCodeEnum,
            RdaMcsLocation::getIdrLocActvCode,
            McsLocationActivityCode.LOCATION_ACTIVITY_CODE_CAS_ACTIVITY,
            "Q")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            McsLocation.Builder::setIdrLocActvCodeUnrecognized,
            RdaMcsLocation::getIdrLocActvCode,
            RdaMcsLocation.Fields.idrLocActvCode,
            1);
  }

  /**
   * Tests the rdaPosition field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testLocationRdaPosition() {
    new LocationFieldTester()
        .verifyUIntFieldToShortFieldCopiedCorrectly(
            McsLocation.Builder::setRdaPosition,
            RdaMcsLocation::getRdaPosition,
            RdaMcsLocation.Fields.rdaPosition);
  }

  /**
   * Tests the idrDtlNdc field is properly copied when a message object is passed through the
   * transformer.
   */
  @Test
  public void testDetailIdrDtlNdc() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlNdc,
            RdaMcsDetail::getIdrDtlNdc,
            RdaMcsDetail.Fields.idrDtlNdc,
            48);
  }

  /**
   * Tests the idrDtlNdcUnitCount field is properly copied when a message object is passed through
   * the transformer.
   */
  @Test
  public void testDetailIdrDtlNdcUnitCount() {
    new McsClaimTransformerTest.DetailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            McsDetail.Builder::setIdrDtlNdcUnitCount,
            RdaMcsDetail::getIdrDtlNdcUnitCount,
            RdaMcsDetail.Fields.idrDtlNdcUnitCount,
            15);
  }

  // endregion McsLocation

  /**
   * Asserts that a transformed change matches the change type and property values of an expected
   * claim.
   *
   * @param changeType the change type to validate
   */
  private void assertChangeMatches(RdaChange.Type changeType) {
    RdaChange<RdaMcsClaim> changed = transformer.transformClaim(changeBuilder.build());
    assertEquals(changeType, changed.getType());
    assertThat(changed.getClaim(), samePropertyValuesAs(claim));
  }

  // region Field Tester Classes

  /**
   * Adaptor class extending the {@link ClaimTransformerFieldTester} class that can be used to
   * create {@link McsClaim.Builder} instances and to trigger a transformation of a claim. Serves as
   * a base class for specific field tester classes that share the same implementations of {@code
   * createClaimBuilder()} and {@code transformClaim()}.
   *
   * @param <TBuilder> the claim builder class created by this adaptor
   * @param <TEntity> the entity class created by this adaptor
   */
  private abstract class AbstractFieldTester<TBuilder, TEntity>
      extends ClaimTransformerFieldTester<
          McsClaim.Builder, McsClaim, RdaMcsClaim, TBuilder, TEntity> {
    /** {@inheritDoc} */
    @Override
    McsClaim.Builder createClaimBuilder() {
      return McsClaim.newBuilder()
          .setIdrClmHdIcn("idrClmHdIcn")
          .setIdrContrId("contr")
          .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL);
    }

    /** {@inheritDoc} */
    @Override
    RdaChange<RdaMcsClaim> transformClaim(McsClaim claim) {
      var changeBuilder =
          McsClaimChange.newBuilder()
              .setSeq(MIN_SEQUENCE_NUM)
              .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
              .setClaim(claim);
      return transformer.transformClaim(changeBuilder.build());
    }

    /** {@inheritDoc} */
    @Override
    McsClaim buildClaim(McsClaim.Builder builder) {
      return builder.build();
    }
  }

  /**
   * Adaptor class extending the {@link ClaimTransformerFieldTester} class that can be used to
   * create {@link McsClaim.Builder} instances and to trigger a transformation of a claim. Used for
   * tests that operator on {@link McsClaim} and {@link RdaMcsClaim} instances.
   */
  private class ClaimFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsClaim.Builder, RdaMcsClaim> {
    /** {@inheritDoc} */
    @Override
    McsClaim.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      return claimBuilder;
    }

    /** {@inheritDoc} */
    @Override
    RdaMcsClaim getTestEntity(RdaMcsClaim claim) {
      return claim;
    }
  }

  /**
   * Adaptor class extending the {@link ClaimTransformerFieldTester} class that can be used to
   * create {@link McsAdjustment.Builder} instances and to trigger a transformation of a claim. Used
   * for tests that operate on {@link McsAdjustment} and {@link RdaMcsAdjustment} instances.
   */
  class AdjustmentFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsAdjustment.Builder, RdaMcsAdjustment> {
    /** {@inheritDoc} */
    @Override
    McsAdjustment.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsAdjustmentsBuilderList().isEmpty()) {
        claimBuilder.addMcsAdjustmentsBuilder();
      }
      return claimBuilder.getMcsAdjustmentsBuilder(0);
    }

    /** {@inheritDoc} */
    @Override
    RdaMcsAdjustment getTestEntity(RdaMcsClaim claim) {
      assertEquals(1, claim.getAdjustments().size());
      RdaMcsAdjustment answer = claim.getAdjustments().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      return answer;
    }

    /** {@inheritDoc} */
    @Override
    String getLabel(String basicLabel) {
      return "adjustments-0-" + basicLabel;
    }
  }

  /**
   * Adaptor class extending the {@link ClaimTransformerFieldTester} class that can be used to
   * create {@link McsAudit.Builder} instances and to trigger a transformation of a claim. Used for
   * tests that operator on {@link McsAudit} and {@link RdaMcsAudit} instances.
   */
  class AuditFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsAudit.Builder, RdaMcsAudit> {
    /** {@inheritDoc} */
    @Override
    McsAudit.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsAuditsBuilderList().isEmpty()) {
        claimBuilder.addMcsAuditsBuilder();
      }
      return claimBuilder.getMcsAuditsBuilder(0);
    }

    /** {@inheritDoc} */
    @Override
    RdaMcsAudit getTestEntity(RdaMcsClaim claim) {
      assertEquals(1, claim.getAudits().size());
      RdaMcsAudit answer = claim.getAudits().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      return answer;
    }

    /** {@inheritDoc} */
    @Override
    String getLabel(String basicLabel) {
      return "audits-0-" + basicLabel;
    }
  }

  /**
   * Adaptor class extending the {@link ClaimTransformerFieldTester} class that can be used to
   * create {@link McsDetail.Builder} instances and to trigger a transformation of a claim. Used for
   * tests that operator on {@link McsDetail} and {@link RdaMcsDetail} instances.
   */
  class DetailFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsDetail.Builder, RdaMcsDetail> {
    /** {@inheritDoc} */
    @Override
    McsDetail.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsDetailsBuilderList().isEmpty()) {
        claimBuilder.addMcsDetailsBuilder();
      }
      return claimBuilder.getMcsDetailsBuilder(0);
    }

    /** {@inheritDoc} */
    @Override
    RdaMcsDetail getTestEntity(RdaMcsClaim claim) {
      assertEquals(1, claim.getDetails().size());
      RdaMcsDetail answer = claim.getDetails().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      return answer;
    }

    /** {@inheritDoc} */
    @Override
    String getLabel(String basicLabel) {
      return "details-0-" + basicLabel;
    }
  }

  /**
   * Adaptor class extending the {@link ClaimTransformerFieldTester} class that can be used to
   * create {@link McsDiagnosisCode.Builder} instances and to trigger a transformation of a claim.
   * Used for tests that operate on {@link McsDiagnosisCode} and {@link RdaMcsDiagnosisCode}
   * instances.
   */
  class DiagCodeFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<
          McsDiagnosisCode.Builder, RdaMcsDiagnosisCode> {
    /** {@inheritDoc} */
    @Override
    McsDiagnosisCode.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsDiagnosisCodesBuilderList().isEmpty()) {
        claimBuilder.addMcsDiagnosisCodesBuilder();
        claimBuilder.getMcsDiagnosisCodesBuilder(0).setIdrDiagCode("DC");
      }
      return claimBuilder.getMcsDiagnosisCodesBuilder(0);
    }

    /** {@inheritDoc} */
    @Override
    RdaMcsDiagnosisCode getTestEntity(RdaMcsClaim claim) {
      assertEquals(1, claim.getDiagCodes().size());
      RdaMcsDiagnosisCode answer = claim.getDiagCodes().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      return answer;
    }

    /** {@inheritDoc} */
    @Override
    String getLabel(String basicLabel) {
      return "diagCodes-0-" + basicLabel;
    }
  }

  /**
   * Adaptor class extending the {@link ClaimTransformerFieldTester} class that can be used to
   * create {@link McsLocation.Builder} instances and to trigger a transformation of a claim. Used
   * for tests that operate on {@link McsLocation} and {@link RdaMcsLocation} instances.
   */
  class LocationFieldTester
      extends McsClaimTransformerTest.AbstractFieldTester<McsLocation.Builder, RdaMcsLocation> {
    /** {@inheritDoc} */
    @Override
    McsLocation.Builder getTestEntityBuilder(McsClaim.Builder claimBuilder) {
      if (claimBuilder.getMcsLocationsBuilderList().isEmpty()) {
        claimBuilder.addMcsLocationsBuilder();
      }
      return claimBuilder.getMcsLocationsBuilder(0);
    }

    /** {@inheritDoc} */
    @Override
    RdaMcsLocation getTestEntity(RdaMcsClaim claim) {
      assertEquals(1, claim.getLocations().size());
      RdaMcsLocation answer = claim.getLocations().iterator().next();
      assertEquals("idrClmHdIcn", answer.getIdrClmHdIcn());
      return answer;
    }

    /** {@inheritDoc} */
    @Override
    String getLabel(String basicLabel) {
      return "locations-0-" + basicLabel;
    }
  }

  // endregion Field Tester Classes
}
