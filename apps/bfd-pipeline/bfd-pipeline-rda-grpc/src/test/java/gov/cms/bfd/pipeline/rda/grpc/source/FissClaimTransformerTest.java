package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.RdaFissAuditTrail;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.RdaFissPayer;
import gov.cms.bfd.model.rda.RdaFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissAdjustmentMedicareBeneficiaryIdentifierIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissAdjustmentRequestorCode;
import gov.cms.mpsm.rda.v1.fiss.FissAssignmentOfBenefitsIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissAuditTrail;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassification;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForClinics;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForSpecialFacilities;
import gov.cms.mpsm.rda.v1.fiss.FissBillFacilityType;
import gov.cms.mpsm.rda.v1.fiss.FissBillFrequency;
import gov.cms.mpsm.rda.v1.fiss.FissCancelAdjustmentCode;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier;
import gov.cms.mpsm.rda.v1.fiss.FissInsuredPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPatientRelationshipCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissPhysicianFlag;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessNewHealthInsuranceClaimNumberIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import gov.cms.mpsm.rda.v1.fiss.FissReleaseOfInformation;
import gov.cms.mpsm.rda.v1.fiss.FissRepositoryIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissSourceOfAdmission;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link FissClaimTransformer}. Unless otherwise stated on a method every test
 * verifies that one or a set of fields within a source grpc message object for a claim have been
 * correctly transformed into appropriate values and copied into a new {@link RdaFissClaim} JPA
 * entity object.
 *
 * <p>Field tests are performed using an adaptor object appropriate for each type of grpc/jpa object
 * pair. These adaptor objects ({@link ClaimFieldTester}, {@link AuditTrailFieldTester}, {@link
 * BeneZPayerFieldTester}, {@link InsuredPayerFieldTester}, and {@link ProcCodeFieldTester}) extend
 * the {@link ClaimTransformerFieldTester} class and provide class specific implementations of the
 * methods used to construct and transform objects under test.
 *
 * <p>Each individual field test is named after the field it tests and calls appropriate
 * verification methods for that field. {@see ClaimTransformerFieldTester} for documentation of each
 * of the verification methods.
 */
public class FissClaimTransformerTest {
  // using a fixed Clock ensures our timestamp is predictable
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);
  private final IdHasher idHasher =
      new IdHasher(new IdHasher.Config(10, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(clock, MbiCache.computedCache(idHasher.getConfig()));
  private FissClaimChange.Builder changeBuilder;
  private FissClaim.Builder claimBuilder;
  private RdaFissClaim claim;

  @BeforeEach
  public void setUp() {
    changeBuilder = FissClaimChange.newBuilder();
    claimBuilder = FissClaim.newBuilder();
    claim = new RdaFissClaim();
    claim.setSequenceNumber(0L);
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
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
        .setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  /**
   * Basic smoke test for transformation of claim objects prior to all of the individual field
   * tests.
   */
  @Test
  public void basicFieldsTestForClaimObjectTransformation() {
    claim.setDcn("dcn");
    claim.setSequenceNumber(42L);
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
    claim.setMbiRecord(
        new Mbi(
            1L, "12345678901", "3cf7b310f8fd6e7b275ddbdc6c3cd5b4eec0ea10bc9a504d471b086bd5d9b888"));
    claim.setFedTaxNumber("1234567890");
    claim.setPracLocAddr1("loc-address-1");
    claim.setPracLocAddr2("loc-address-2");
    claim.setPracLocCity("loc-city");
    claim.setPracLocState("ls");
    claim.setPracLocZip("123456789012345");
    claim.setStmtCovFromDate(LocalDate.of(2020, 2, 3));
    claim.setStmtCovToDate(LocalDate.of(2021, 4, 5));
    claim.setLobCd("1");
    claim.setServTypeCd("6");
    claim.setServTypeCdMapping(RdaFissClaim.ServTypeCdMapping.Clinic);
    claim.setFreqCd("G");
    claim.setBillTypCd("ABC");
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
        .setMbi("12345678901")
        .setFedTaxNb("1234567890")
        .setPracLocAddr1("loc-address-1")
        .setPracLocAddr2("loc-address-2")
        .setPracLocCity("loc-city")
        .setPracLocState("ls")
        .setPracLocZip("123456789012345")
        .setStmtCovFromCymd("2020-02-03")
        .setStmtCovToCymd("2021-04-05")
        .setLobCdEnum(FissBillFacilityType.BILL_FACILITY_TYPE_HOSPITAL)
        .setServTypeCdForClinicsEnum(
            FissBillClassificationForClinics
                .BILL_CLASSIFICATION_FOR_CLINICS_COMMUNITY_MENTAL_HEALTH_CENTER)
        .setFreqCdEnum(FissBillFrequency.BILL_FREQUENCY_ADJUSTMENT_CLAIM_G)
        .setBillTypCd("ABC");
    changeBuilder
        .setSeq(42)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.UPDATE);
  }

  /**
   * Basic smoke test for transformation of procedure code objects prior to all of the individual
   * field tests.
   */
  @Test
  public void basicFieldsTestForProcCodeObjectTransformation() {
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
    RdaFissProcCode code = new RdaFissProcCode();
    code.setDcn("dcn");
    code.setPriority((short) 0);
    code.setProcCode("code-1");
    code.setProcFlag("fl-1");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getProcCodes().add(code);
    code = new RdaFissProcCode();
    code.setDcn("dcn");
    code.setPriority((short) 1);
    code.setProcCode("code-2");
    code.setProcFlag("fl-2");
    code.setProcDate(LocalDate.of(2021, 7, 6));
    code.setLastUpdated(claim.getLastUpdated());
    claim.getProcCodes().add(code);
    changeBuilder
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
        .setClaim(claimBuilder.build());
    RdaFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getProcCodes(), transformed.getProcCodes(), RdaFissProcCode::getPriority);
  }

  /**
   * Basic smoke test for transformation of diagnosis code objects prior to all of the individual
   * field tests.
   */
  @Test
  public void basicFieldsTestForDiagCodeObjectTransformation() {
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
    RdaFissDiagnosisCode code = new RdaFissDiagnosisCode();
    code.setDcn("dcn");
    code.setPriority((short) 0);
    code.setDiagCd2("code-1");
    code.setDiagPoaInd("W");
    code.setBitFlags("1234");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getDiagCodes().add(code);
    code = new RdaFissDiagnosisCode();
    code.setDcn("dcn");
    code.setPriority((short) 1);
    code.setDiagCd2("code-2");
    code.setDiagPoaInd("N");
    code.setBitFlags("4321");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getDiagCodes().add(code);
    changeBuilder
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setClaim(claimBuilder.build());
    RdaFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getDiagCodes(), transformed.getDiagCodes(), RdaFissDiagnosisCode::getPriority);
  }

  /**
   * Basic smoke test for transformation of insured payer objects prior to all of the individual
   * field tests.
   */
  @Test
  public void basicFieldsTestForInsuredPayerObjectTransformation() {
    claimBuilder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_MOVE)
        .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
        .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_FINAL)
        .addFissPayers(
            FissPayer.newBuilder()
                .setInsuredPayer(
                    FissInsuredPayer.newBuilder()
                        .setPayersIdEnum(FissPayersCode.PAYERS_CODE_BLACK_LUNG)
                        .setPayersName("payers-name")
                        .setRelIndEnum(
                            FissReleaseOfInformation.RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE)
                        .setAssignIndEnum(
                            FissAssignmentOfBenefitsIndicator
                                .ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED)
                        .setProviderNumber("provider-num")
                        .setAdjDcnIcn("dcn-icn")
                        .setPriorPmt("123.45")
                        .setEstAmtDue("234.56")
                        .setInsuredRelEnum(
                            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_EMPLOYEE)
                        .setInsuredName("insured-name")
                        .setInsuredSsnHic("ssn-hic")
                        .setInsuredGroupName("group-name")
                        .setInsuredGroupNbr("group-num")
                        .setTreatAuthCd("auth-code")
                        .setInsuredSexEnum(FissBeneficiarySex.BENEFICIARY_SEX_MALE)
                        .setInsuredRelX12Enum(
                            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_GRANDCHILD)
                        .setInsuredDobText("11222021")
                        .setInsuredDob("2021-11-22")
                        .build())
                .build());
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('M');
    claim.setCurrLoc1('M');
    claim.setCurrLoc2("9997");
    claim.setLastUpdated(clock.instant());
    RdaFissPayer payer = new RdaFissPayer();
    payer.setDcn("dcn");
    payer.setPriority((short) 0);
    payer.setPayerType(RdaFissPayer.PayerType.Insured);
    payer.setPayersId("H");
    payer.setPayersName("payers-name");
    payer.setRelInd("N");
    payer.setAssignInd("Y");
    payer.setProviderNumber("provider-num");
    payer.setAdjDcnIcn("dcn-icn");
    payer.setPriorPmt(new BigDecimal("123.45"));
    payer.setEstAmtDue(new BigDecimal("234.56"));
    payer.setInsuredRel("08");
    payer.setInsuredName("insured-name");
    payer.setInsuredSsnHic("ssn-hic");
    payer.setInsuredGroupName("group-name");
    payer.setInsuredGroupNbr("group-num");
    payer.setTreatAuthCd("auth-code");
    payer.setInsuredSex("M");
    payer.setInsuredRelX12("13");
    payer.setInsuredDob(LocalDate.of(2021, 11, 22));
    payer.setInsuredDobText("11222021");
    payer.setLastUpdated(claim.getLastUpdated());
    claim.getPayers().add(payer);
    changeBuilder
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setClaim(claimBuilder.build());
    RdaFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getPayers(), transformed.getPayers(), RdaFissPayer::getPriority);
  }

  /**
   * Basic smoke test for transformation of BeneZ payer objects prior to all of the individual field
   * tests.
   */
  @Test
  public void basicFieldsTestForBeneZPayerObjectTransformation() {
    claimBuilder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_MOVE)
        .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
        .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_FINAL)
        .addFissPayers(
            FissPayer.newBuilder()
                .setBeneZPayer(
                    FissBeneZPayer.newBuilder()
                        .setPayersIdEnum(FissPayersCode.PAYERS_CODE_BLACK_LUNG)
                        .setPayersName("payers-name")
                        .setRelIndEnum(
                            FissReleaseOfInformation.RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE)
                        .setAssignIndEnum(
                            FissAssignmentOfBenefitsIndicator
                                .ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED)
                        .setProviderNumber("provider-num")
                        .setAdjDcnIcn("dcn-icn")
                        .setPriorPmt("123.45")
                        .setEstAmtDue("234.56")
                        .setBeneRelEnum(
                            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_EMPLOYEE)
                        .setBeneLastName("last-name")
                        .setBeneFirstName("first-name")
                        .setBeneMidInit("Z")
                        .setBeneSsnHic("ssn-hic")
                        .setInsuredGroupName("group-name")
                        .setBeneDob("2020-09-10")
                        .setBeneSexEnum(FissBeneficiarySex.BENEFICIARY_SEX_FEMALE)
                        .setTreatAuthCd("auth-code")
                        .setInsuredSexEnum(FissBeneficiarySex.BENEFICIARY_SEX_MALE)
                        .setInsuredRelX12Enum(
                            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_GRANDCHILD)
                        .build())
                .build());
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('M');
    claim.setCurrLoc1('M');
    claim.setCurrLoc2("9997");
    claim.setLastUpdated(clock.instant());
    RdaFissPayer payer = new RdaFissPayer();
    payer.setDcn("dcn");
    payer.setPriority((short) 0);
    payer.setPayerType(RdaFissPayer.PayerType.BeneZ);
    payer.setPayersId("H");
    payer.setPayersName("payers-name");
    payer.setRelInd("N");
    payer.setAssignInd("Y");
    payer.setProviderNumber("provider-num");
    payer.setAdjDcnIcn("dcn-icn");
    payer.setPriorPmt(new BigDecimal("123.45"));
    payer.setEstAmtDue(new BigDecimal("234.56"));
    payer.setBeneRel("08");
    payer.setBeneLastName("last-name");
    payer.setBeneFirstName("first-name");
    payer.setBeneMidInit("Z");
    payer.setBeneSsnHic("ssn-hic");
    payer.setInsuredGroupName("group-name");
    payer.setBeneDob(LocalDate.of(2020, 9, 10));
    payer.setBeneSex("F");
    payer.setTreatAuthCd("auth-code");
    payer.setInsuredSex("M");
    payer.setInsuredRelX12("13");
    payer.setLastUpdated(claim.getLastUpdated());
    claim.getPayers().add(payer);
    changeBuilder
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setClaim(claimBuilder.build());
    RdaFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getPayers(), transformed.getPayers(), RdaFissPayer::getPriority);
  }

  /**
   * Basic smoke test for transformation of audit trail objects prior to all of the individual field
   * tests.
   */
  @Test
  public void basicFieldsTestForAuditTrailObjectTransformation() {
    claimBuilder
        .setDcn("dcn")
        .setHicNo("hicn")
        .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_MOVE)
        .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
        .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_FINAL)
        .addFissAuditTrail(
            FissAuditTrail.newBuilder()
                .setBadtStatusEnum(FissClaimStatus.CLAIM_STATUS_MOVE)
                .setBadtLoc("1")
                .setBadtOperId("2")
                .setBadtReas("3")
                .setBadtCurrDateCymd("2021-12-03")
                .build());
    claim.setDcn("dcn");
    claim.setHicNo("hicn");
    claim.setCurrStatus('M');
    claim.setCurrLoc1('M');
    claim.setCurrLoc2("9997");
    claim.setLastUpdated(clock.instant());
    RdaFissAuditTrail auditTrail = new RdaFissAuditTrail();
    auditTrail.setDcn("dcn");
    auditTrail.setPriority((short) 0);
    auditTrail.setBadtStatus("M");
    auditTrail.setBadtLoc("1");
    auditTrail.setBadtOperId("2");
    auditTrail.setBadtReas("3");
    auditTrail.setBadtCurrDate(LocalDate.of(2021, 12, 3));
    auditTrail.setLastUpdated(claim.getLastUpdated());
    claim.getAuditTrail().add(auditTrail);
    changeBuilder
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setClaim(claimBuilder.build());
    RdaFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getAuditTrail(), transformed.getAuditTrail(), RdaFissAuditTrail::getPriority);
  }

  @Test
  public void testMissingRequiredFieldsGenerateErrors() {
    try {
      changeBuilder
          .setSeq(MIN_SEQUENCE_NUM)
          .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
          .setClaim(claimBuilder.build());
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

  // region Claim tests

  @Test
  public void testClaimDcn() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setDcn, RdaFissClaim::getDcn, "dcn", 23);
  }

  @Test
  public void testClaimHicNo() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setHicNo, RdaFissClaim::getHicNo, "hicNo", 12);
  }

  @Test
  public void testClaimCurrStatus() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setCurrStatusEnum,
            claim -> String.valueOf(claim.getCurrStatus()),
            FissClaimStatus.CLAIM_STATUS_MOVE,
            "M")
        .verifyEnumFieldTransformationRejectsUnrecognizedValue(
            FissClaim.Builder::setCurrStatusUnrecognized, RdaFissClaim.Fields.currStatus, "ZZZ");
  }

  @Test
  public void testClaimCurrLoc1() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setCurrLoc1Enum,
            claim -> String.valueOf(claim.getCurrLoc1()),
            FissProcessingType.PROCESSING_TYPE_BATCH,
            "B")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setCurrLoc1Unrecognized,
            claim -> String.valueOf(claim.getCurrLoc1()),
            RdaFissClaim.Fields.currLoc1,
            1);
  }

  @Test
  public void testClaimCurrLoc2() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setCurrLoc2Enum,
            RdaFissClaim::getCurrLoc2,
            FissCurrentLocation2.CURRENT_LOCATION_2_CABLE,
            "9000")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setCurrLoc2Unrecognized,
            RdaFissClaim::getCurrLoc2,
            RdaFissClaim.Fields.currLoc2,
            5);
  }

  @Test
  public void testClaimProvStateCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setProvStateCd,
            RdaFissClaim::getProvStateCd,
            RdaFissClaim.Fields.provStateCd,
            2);
  }

  @Test
  public void testClaimProvTypFacilCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setProvTypFacilCd,
            RdaFissClaim::getProvTypFacilCd,
            RdaFissClaim.Fields.provTypFacilCd,
            1);
  }

  @Test
  public void testClaimProvEmerInd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setProvEmerInd,
            RdaFissClaim::getProvEmerInd,
            RdaFissClaim.Fields.provEmerInd,
            1);
  }

  @Test
  public void testClaimProvDeptId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setProvDeptId,
            RdaFissClaim::getProvDeptId,
            RdaFissClaim.Fields.provDeptId,
            3);
  }

  @Test
  public void testClaimMedaProvId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMedaProvId,
            RdaFissClaim::getMedaProvId,
            RdaFissClaim.Fields.medaProvId,
            13);
  }

  @Test
  public void testClaimMedaProv6() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMedaProv6,
            RdaFissClaim::getMedaProv_6,
            RdaFissClaim.Fields.medaProv_6,
            6);
  }

  @Test
  public void testClaimTotalChargeAmount() {
    new ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissClaim.Builder::setTotalChargeAmount,
            RdaFissClaim::getTotalChargeAmount,
            RdaFissClaim.Fields.totalChargeAmount);
  }

  @Test
  public void testClaimRecdDtCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setRecdDtCymd,
            RdaFissClaim::getReceivedDate,
            RdaFissClaim.Fields.receivedDate);
  }

  @Test
  public void testClaimCurrTranDtCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setCurrTranDtCymd,
            RdaFissClaim::getCurrTranDate,
            RdaFissClaim.Fields.currTranDate);
  }

  @Test
  public void testClaimAdmDiagCode() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdmDiagCode,
            RdaFissClaim::getAdmitDiagCode,
            RdaFissClaim.Fields.admitDiagCode,
            7);
  }

  @Test
  public void testClaimPrincipleDiag() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPrincipleDiag,
            RdaFissClaim::getPrincipleDiag,
            RdaFissClaim.Fields.principleDiag,
            7);
  }

  @Test
  public void testClaimNpiNumber() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setNpiNumber,
            RdaFissClaim::getNpiNumber,
            RdaFissClaim.Fields.npiNumber,
            10);
  }

  @Test
  public void testClaimMbi() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMbi, RdaFissClaim::getMbi, RdaFissClaim.Fields.mbi, 11)
        .verifyIdHashFieldPopulatedCorrectly(
            FissClaim.Builder::setMbi, RdaFissClaim::getMbiHash, 11, idHasher);
  }

  @Test
  public void testClaimFedTaxNb() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setFedTaxNb,
            RdaFissClaim::getFedTaxNumber,
            RdaFissClaim.Fields.fedTaxNumber,
            10);
  }

  @Test
  public void testClaimPracLocAddr1() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocAddr1,
            RdaFissClaim::getPracLocAddr1,
            RdaFissClaim.Fields.pracLocAddr1,
            2147483647);
  }

  @Test
  public void testClaimPracLocAddr2() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocAddr2,
            RdaFissClaim::getPracLocAddr2,
            RdaFissClaim.Fields.pracLocAddr2,
            2147483647);
  }

  @Test
  public void testClaimPracLocCity() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocCity,
            RdaFissClaim::getPracLocCity,
            RdaFissClaim.Fields.pracLocCity,
            2147483647);
  }

  @Test
  public void testClaimPracLocState() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocState,
            RdaFissClaim::getPracLocState,
            RdaFissClaim.Fields.pracLocState,
            2);
  }

  @Test
  public void testClaimPracLocZip() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocZip,
            RdaFissClaim::getPracLocZip,
            RdaFissClaim.Fields.pracLocZip,
            15);
  }

  @Test
  public void testClaimStmtCovFromCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setStmtCovFromCymd,
            RdaFissClaim::getStmtCovFromDate,
            RdaFissClaim.Fields.stmtCovFromDate);
  }

  @Test
  public void testClaimStmtCovToCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setStmtCovToCymd,
            RdaFissClaim::getStmtCovToDate,
            RdaFissClaim.Fields.stmtCovToDate);
  }

  @Test
  public void testClaimLobCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setLobCdEnum,
            claim -> String.valueOf(claim.getLobCd()),
            FissBillFacilityType.BILL_FACILITY_TYPE_HOME_HEALTH,
            "3")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setLobCdUnrecognized,
            claim -> String.valueOf(claim.getLobCd()),
            RdaFissClaim.Fields.lobCd,
            1);
  }

  @Test
  public void testClaimServTypCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setServTypeCdEnum,
            claim -> String.valueOf(claim.getServTypeCd()),
            FissBillClassification.BILL_CLASSIFICATION_INPATIENT_PART_A,
            "1")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setServTypCdUnrecognized,
            claim -> String.valueOf(claim.getServTypeCd()),
            RdaFissClaim.Fields.servTypeCd,
            1);
  }

  @Test
  public void testClaimFreqCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setFreqCdEnum,
            RdaFissClaim::getFreqCd,
            FissBillFrequency.BILL_FREQUENCY_ADJUSTMENT_CLAIM_F,
            "F")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setFreqCdUnrecognized,
            RdaFissClaim::getFreqCd,
            RdaFissClaim.Fields.freqCd,
            1);
  }

  @Test
  public void testClaimBillTypCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setBillTypCd,
            RdaFissClaim::getBillTypCd,
            RdaFissClaim.Fields.billTypCd,
            3);
  }

  @Test
  public void testClaimRejectCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setRejectCd,
            RdaFissClaim::getRejectCd,
            RdaFissClaim.Fields.rejectCd,
            5);
  }

  @Test
  public void testClaimFullPartDenInd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setFullPartDenInd,
            RdaFissClaim::getFullPartDenInd,
            RdaFissClaim.Fields.fullPartDenInd,
            1);
  }

  @Test
  public void testClaimNonPayInd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setNonPayInd,
            RdaFissClaim::getNonPayInd,
            RdaFissClaim.Fields.nonPayInd,
            2);
  }

  @Test
  public void testClaimXrefDcnNbr() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setXrefDcnNbr,
            RdaFissClaim::getXrefDcnNbr,
            RdaFissClaim.Fields.xrefDcnNbr,
            23);
  }

  @Test
  public void testClaimAdjReqCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAdjReqCdEnum,
            RdaFissClaim::getAdjReqCd,
            FissAdjustmentRequestorCode.ADJUSTMENT_REQUESTOR_CODE_FISCAL_INTERMEDIARY,
            "F")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setAdjReqCdUnrecognized,
            RdaFissClaim::getAdjReqCd,
            RdaFissClaim.Fields.adjReqCd,
            1);
  }

  @Test
  public void testClaimAdjReasCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdjReasCd,
            RdaFissClaim::getAdjReasCd,
            RdaFissClaim.Fields.adjReasCd,
            2);
  }

  @Test
  public void testClaimCancelXrefDcn() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setCancelXrefDcn,
            RdaFissClaim::getCancelXrefDcn,
            RdaFissClaim.Fields.cancelXrefDcn,
            23);
  }

  @Test
  public void testClaimCancelDate() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setCancelDateCymd,
            RdaFissClaim::getCancelDate,
            RdaFissClaim.Fields.cancelDate);
  }

  @Test
  public void testClaimCancAdjCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setCancAdjCdEnum,
            RdaFissClaim::getCancAdjCd,
            FissCancelAdjustmentCode.CANCEL_ADJUSTMENT_CODE_COVERAGE,
            "C")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setCancAdjCdUnrecognized,
            RdaFissClaim::getCancAdjCd,
            RdaFissClaim.Fields.cancAdjCd,
            1);
  }

  @Test
  public void testClaimOriginalXrefDcn() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOriginalXrefDcn,
            RdaFissClaim::getOriginalXrefDcn,
            RdaFissClaim.Fields.originalXrefDcn,
            23);
  }

  @Test
  public void testClaimPaidDt() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setPaidDtCymd, RdaFissClaim::getPaidDt, RdaFissClaim.Fields.paidDt);
  }

  @Test
  public void testClaimAdmDate() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setAdmDateCymd,
            RdaFissClaim::getAdmDate,
            RdaFissClaim.Fields.admDate);
  }

  @Test
  public void testClaimAdmSource() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAdmSourceEnum,
            RdaFissClaim::getAdmSource,
            FissSourceOfAdmission.SOURCE_OF_ADMISSION_CLINIC_REFERRAL,
            "2")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setAdmSourceUnrecognized,
            RdaFissClaim::getAdmSource,
            RdaFissClaim.Fields.admSource,
            1);
  }

  @Test
  public void testClaimPrimaryPayerCode() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setPrimaryPayerCodeEnum,
            RdaFissClaim::getPrimaryPayerCode,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setPrimaryPayerCodeUnrecognized,
            RdaFissClaim::getPrimaryPayerCode,
            RdaFissClaim.Fields.primaryPayerCode,
            1);
  }

  @Test
  public void testClaimAttendPhysId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysId,
            RdaFissClaim::getAttendPhysId,
            RdaFissClaim.Fields.attendPhysId,
            16);
  }

  @Test
  public void testClaimAttendPhysLname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysLname,
            RdaFissClaim::getAttendPhysLname,
            RdaFissClaim.Fields.attendPhysLname,
            17);
  }

  @Test
  public void testClaimAttendPhysFname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysFname,
            RdaFissClaim::getAttendPhysFname,
            RdaFissClaim.Fields.attendPhysFname,
            18);
  }

  @Test
  public void testClaimAttendPhysMint() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysMint,
            RdaFissClaim::getAttendPhysMint,
            RdaFissClaim.Fields.attendPhysMint,
            1);
  }

  @Test
  public void testClaimAttendPhysFlag() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAttendPhysFlagEnum,
            RdaFissClaim::getAttendPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setAttendPhysFlagUnrecognized,
            RdaFissClaim::getAttendPhysFlag,
            RdaFissClaim.Fields.attendPhysFlag,
            1);
  }

  @Test
  public void testClaimOperatingPhysId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperatingPhysId,
            RdaFissClaim::getOperatingPhysId,
            RdaFissClaim.Fields.operatingPhysId,
            16);
  }

  @Test
  public void testClaimOperPhysLname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperPhysLname,
            RdaFissClaim::getOperPhysLname,
            RdaFissClaim.Fields.operPhysLname,
            17);
  }

  @Test
  public void testClaimOperPhysFname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperPhysFname,
            RdaFissClaim::getOperPhysFname,
            RdaFissClaim.Fields.operPhysFname,
            18);
  }

  @Test
  public void testClaimOperPhysMint() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperPhysMint,
            RdaFissClaim::getOperPhysMint,
            RdaFissClaim.Fields.operPhysMint,
            1);
  }

  @Test
  public void testClaimOperPhysFlag() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setOperPhysFlagEnum,
            RdaFissClaim::getOperPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setOperPhysFlagUnrecognized,
            RdaFissClaim::getOperPhysFlag,
            RdaFissClaim.Fields.operPhysFlag,
            1);
  }

  @Test
  public void testClaimOthPhysId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysId,
            RdaFissClaim::getOthPhysId,
            RdaFissClaim.Fields.othPhysId,
            16);
  }

  @Test
  public void testClaimOthPhysLname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysLname,
            RdaFissClaim::getOthPhysLname,
            RdaFissClaim.Fields.othPhysLname,
            17);
  }

  @Test
  public void testClaimOthPhysFname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysFname,
            RdaFissClaim::getOthPhysFname,
            RdaFissClaim.Fields.othPhysFname,
            18);
  }

  @Test
  public void testClaimOthPhysMint() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysMint,
            RdaFissClaim::getOthPhysMint,
            RdaFissClaim.Fields.othPhysMint,
            1);
  }

  @Test
  public void testClaimOthPhysFlag() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setOthPhysFlagEnum,
            RdaFissClaim::getOthPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setOthPhysFlagUnrecognized,
            RdaFissClaim::getOthPhysFlag,
            RdaFissClaim.Fields.othPhysFlag,
            1);
  }

  @Test
  public void testClaimXrefHicNbr() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setXrefHicNbr,
            RdaFissClaim::getXrefHicNbr,
            RdaFissClaim.Fields.xrefHicNbr,
            12);
  }

  @Test
  public void testClaimProcNewHicInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setProcNewHicIndEnum,
            RdaFissClaim::getProcNewHicInd,
            FissProcessNewHealthInsuranceClaimNumberIndicator.PROCESS_NEW_HIC_INDICATOR_Y,
            "Y")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setProcNewHicIndUnrecognized,
            RdaFissClaim::getProcNewHicInd,
            RdaFissClaim.Fields.procNewHicInd,
            1);
  }

  @Test
  public void testClaimNewHic() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setNewHic, RdaFissClaim::getNewHic, RdaFissClaim.Fields.newHic, 12);
  }

  @Test
  public void testClaimReposInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setReposIndEnum,
            RdaFissClaim::getReposInd,
            FissRepositoryIndicator.REPOSITORY_INDICATOR_HIC_HAS_BEEN_MOVED,
            "Y")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setReposIndUnrecognized,
            RdaFissClaim::getReposInd,
            RdaFissClaim.Fields.reposInd,
            1);
  }

  @Test
  public void testClaimReposHic() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setReposHic,
            RdaFissClaim::getReposHic,
            RdaFissClaim.Fields.reposHic,
            12);
  }

  @Test
  public void testClaimMbiSubmBeneInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setMbiSubmBeneIndEnum,
            RdaFissClaim::getMbiSubmBeneInd,
            FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier.FISS_HIC_OR_MBI_IS_HIC,
            "H")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setMbiSubmBeneIndUnrecognized,
            RdaFissClaim::getMbiSubmBeneInd,
            RdaFissClaim.Fields.mbiSubmBeneInd,
            1);
  }

  @Test
  public void testClaimAdjMbiInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAdjMbiIndEnum,
            RdaFissClaim::getAdjMbiInd,
            FissAdjustmentMedicareBeneficiaryIdentifierIndicator
                .ADJUSTMENT_MBI_INDICATOR_HIC_SUBMITTED_ON_ADJUSTMENT_OR_CANCEL_CLAIM,
            "H")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissClaim.Builder::setAdjMbiIndUnrecognized,
            RdaFissClaim::getAdjMbiInd,
            RdaFissClaim.Fields.adjMbiInd,
            1);
  }

  @Test
  public void testClaimAdjMbi() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdjMbi, RdaFissClaim::getAdjMbi, RdaFissClaim.Fields.adjMbi, 11);
  }

  @Test
  public void testClaimMedicalRecordNo() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMedicalRecordNo,
            RdaFissClaim::getMedicalRecordNo,
            RdaFissClaim.Fields.medicalRecordNo,
            17);
  }

  // endregion Claim tests
  // region ProcCode tests

  @Test
  public void testProcCodeProcCd() {
    new ProcCodeFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissProcedureCode.Builder::setProcCd,
            RdaFissProcCode::getProcCode,
            RdaFissProcCode.Fields.procCode,
            10);
  }

  @Test
  public void testProcCodeProcFlag() {
    new ProcCodeFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissProcedureCode.Builder::setProcFlag,
            RdaFissProcCode::getProcFlag,
            RdaFissProcCode.Fields.procFlag,
            4);
  }

  @Test
  public void testProcCodeProcDt() {
    new ProcCodeFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissProcedureCode.Builder::setProcDt,
            RdaFissProcCode::getProcDate,
            RdaFissProcCode.Fields.procDate);
  }

  // endregion ProcCode tests
  // region BeneZPayer tests

  @Test
  public void testBeneZPayerPayersId() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setPayersIdEnum,
            RdaFissPayer::getPayersId,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissBeneZPayer.Builder::setPayersIdUnrecognized,
            RdaFissPayer::getPayersId,
            RdaFissPayer.Fields.payersId,
            1);
  }

  @Test
  public void testBeneZPayerPayersName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setPayersName,
            RdaFissPayer::getPayersName,
            RdaFissPayer.Fields.payersName,
            32);
  }

  @Test
  public void testBeneZPayerRelInd() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setRelIndEnum,
            RdaFissPayer::getRelInd,
            FissReleaseOfInformation.RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE,
            "N")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissBeneZPayer.Builder::setRelIndUnrecognized,
            RdaFissPayer::getRelInd,
            RdaFissPayer.Fields.relInd,
            1);
  }

  @Test
  public void testBeneZPayerAssignInd() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setAssignIndEnum,
            RdaFissPayer::getAssignInd,
            FissAssignmentOfBenefitsIndicator.ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED,
            "Y")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissBeneZPayer.Builder::setAssignIndUnrecognized,
            RdaFissPayer::getAssignInd,
            RdaFissPayer.Fields.assignInd,
            1);
  }

  @Test
  public void testBeneZPayerProviderNumber() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setProviderNumber,
            RdaFissPayer::getProviderNumber,
            RdaFissPayer.Fields.providerNumber,
            13);
  }

  @Test
  public void testBeneZPayerAdjDcnIcn() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setAdjDcnIcn,
            RdaFissPayer::getAdjDcnIcn,
            RdaFissPayer.Fields.adjDcnIcn,
            23);
  }

  @Test
  public void testBeneZPayerPriorPmt() {
    new BeneZPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissBeneZPayer.Builder::setPriorPmt,
            RdaFissPayer::getPriorPmt,
            RdaFissPayer.Fields.priorPmt);
  }

  @Test
  public void testBeneZPayerEstAmtDue() {
    new BeneZPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissBeneZPayer.Builder::setEstAmtDue,
            RdaFissPayer::getEstAmtDue,
            RdaFissPayer.Fields.estAmtDue);
  }

  @Test
  public void testBeneZPayerBeneRel() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setBeneRelEnum,
            RdaFissPayer::getBeneRel,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_DEFAULT,
            "00")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissBeneZPayer.Builder::setBeneRelUnrecognized,
            RdaFissPayer::getBeneRel,
            RdaFissPayer.Fields.beneRel,
            2);
  }

  @Test
  public void testBeneZPayerBeneLastName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneLastName,
            RdaFissPayer::getBeneLastName,
            RdaFissPayer.Fields.beneLastName,
            15);
  }

  @Test
  public void testBeneZPayerBeneFirstName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneFirstName,
            RdaFissPayer::getBeneFirstName,
            RdaFissPayer.Fields.beneFirstName,
            10);
  }

  @Test
  public void testBeneZPayerBeneMidInit() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneMidInit,
            RdaFissPayer::getBeneMidInit,
            RdaFissPayer.Fields.beneMidInit,
            1);
  }

  @Test
  public void testBeneZPayerBeneSsnHic() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneSsnHic,
            RdaFissPayer::getBeneSsnHic,
            RdaFissPayer.Fields.beneSsnHic,
            19);
  }

  @Test
  public void testBeneZPayerInsuredGroupName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setInsuredGroupName,
            RdaFissPayer::getInsuredGroupName,
            RdaFissPayer.Fields.insuredGroupName,
            17);
  }

  @Test
  public void testBeneZPayerBeneDob() {
    new BeneZPayerFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissBeneZPayer.Builder::setBeneDob,
            RdaFissPayer::getBeneDob,
            RdaFissPayer.Fields.beneDob);
  }

  @Test
  public void testBeneZPayerBeneSex() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setBeneSexEnum,
            RdaFissPayer::getBeneSex,
            FissBeneficiarySex.BENEFICIARY_SEX_FEMALE,
            "F")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissBeneZPayer.Builder::setBeneSexUnrecognized,
            RdaFissPayer::getBeneSex,
            RdaFissPayer.Fields.beneSex,
            1);
  }

  @Test
  public void testBeneZPayerTreatAuthCd() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setTreatAuthCd,
            RdaFissPayer::getTreatAuthCd,
            RdaFissPayer.Fields.treatAuthCd,
            18);
  }

  @Test
  public void testBeneZPayerInsuredSex() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setInsuredSexEnum,
            RdaFissPayer::getInsuredSex,
            FissBeneficiarySex.BENEFICIARY_SEX_FEMALE,
            "F")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissBeneZPayer.Builder::setInsuredSexUnrecognized,
            RdaFissPayer::getInsuredSex,
            RdaFissPayer.Fields.insuredSex,
            1);
  }

  @Test
  public void testBeneZPayerInsuredRelX12() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setInsuredRelX12Enum,
            RdaFissPayer::getInsuredRelX12,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_DEFAULT,
            "00")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissBeneZPayer.Builder::setInsuredRelX12Unrecognized,
            RdaFissPayer::getInsuredRelX12,
            RdaFissPayer.Fields.insuredRelX12,
            2);
  }

  // endregion BeneZPayer tests
  // region InsuredPayer tests

  @Test
  public void testInsuredPayerPayersId() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setPayersIdEnum,
            RdaFissPayer::getPayersId,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissInsuredPayer.Builder::setPayersIdUnrecognized,
            RdaFissPayer::getPayersId,
            RdaFissPayer.Fields.payersId,
            1);
  }

  @Test
  public void testInsuredPayerPayersName() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setPayersName,
            RdaFissPayer::getPayersName,
            RdaFissPayer.Fields.payersName,
            32);
  }

  @Test
  public void testInsuredPayerRelInd() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setRelIndEnum,
            RdaFissPayer::getRelInd,
            FissReleaseOfInformation.RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE,
            "N")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissInsuredPayer.Builder::setRelIndUnrecognized,
            RdaFissPayer::getRelInd,
            RdaFissPayer.Fields.relInd,
            1);
  }

  @Test
  public void testInsuredPayerAssignInd() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setAssignIndEnum,
            RdaFissPayer::getAssignInd,
            FissAssignmentOfBenefitsIndicator.ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED,
            "Y")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissInsuredPayer.Builder::setAssignIndUnrecognized,
            RdaFissPayer::getAssignInd,
            RdaFissPayer.Fields.assignInd,
            1);
  }

  @Test
  public void testInsuredPayerProviderNumber() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setProviderNumber,
            RdaFissPayer::getProviderNumber,
            RdaFissPayer.Fields.providerNumber,
            13);
  }

  @Test
  public void testInsuredPayerAdjDcnIcn() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setAdjDcnIcn,
            RdaFissPayer::getAdjDcnIcn,
            RdaFissPayer.Fields.adjDcnIcn,
            23);
  }

  @Test
  public void testInsuredPayerPriorPmt() {
    new InsuredPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissInsuredPayer.Builder::setPriorPmt,
            RdaFissPayer::getPriorPmt,
            RdaFissPayer.Fields.priorPmt);
  }

  @Test
  public void testInsuredPayerEstAmtDue() {
    new InsuredPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissInsuredPayer.Builder::setEstAmtDue,
            RdaFissPayer::getEstAmtDue,
            RdaFissPayer.Fields.estAmtDue);
  }

  @Test
  public void testInsuredPayerInsuredRel() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setInsuredRelEnum,
            RdaFissPayer::getInsuredRel,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_EMPLOYEE,
            "08")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissInsuredPayer.Builder::setInsuredRelUnrecognized,
            RdaFissPayer::getInsuredRel,
            RdaFissPayer.Fields.insuredRel,
            2);
  }

  @Test
  public void testInsuredPayerInsuredName() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredName,
            RdaFissPayer::getInsuredName,
            RdaFissPayer.Fields.insuredName,
            25);
  }

  @Test
  public void testInsuredPayerInsuredSsnHic() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredSsnHic,
            RdaFissPayer::getInsuredSsnHic,
            RdaFissPayer.Fields.insuredSsnHic,
            19);
  }

  @Test
  public void testInsuredPayerInsuredGroupName() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredGroupName,
            RdaFissPayer::getInsuredGroupName,
            RdaFissPayer.Fields.insuredGroupName,
            17);
  }

  @Test
  public void testInsuredPayerInsuredGroupNbr() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredGroupNbr,
            RdaFissPayer::getInsuredGroupNbr,
            RdaFissPayer.Fields.insuredGroupNbr,
            20);
  }

  @Test
  public void testInsuredPayerTreatAuthCd() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setTreatAuthCd,
            RdaFissPayer::getTreatAuthCd,
            RdaFissPayer.Fields.treatAuthCd,
            18);
  }

  @Test
  public void testInsuredPayerInsuredSex() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setInsuredSexEnum,
            RdaFissPayer::getInsuredSex,
            FissBeneficiarySex.BENEFICIARY_SEX_UNKNOWN,
            "U")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissInsuredPayer.Builder::setInsuredSexUnrecognized,
            RdaFissPayer::getInsuredSex,
            RdaFissPayer.Fields.insuredSex,
            1);
  }

  @Test
  public void testInsuredPayerInsuredRelX12() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setInsuredRelX12Enum,
            RdaFissPayer::getInsuredRelX12,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_DEFAULT,
            "00")
        .verifyStringFieldCopiedCorrectlyEmptyOK(
            FissInsuredPayer.Builder::setInsuredRelX12Unrecognized,
            RdaFissPayer::getInsuredRelX12,
            RdaFissPayer.Fields.insuredRelX12,
            2);
  }

  @Test
  public void testInsuredPayerInsuredDob() {
    new InsuredPayerFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissInsuredPayer.Builder::setInsuredDob,
            RdaFissPayer::getInsuredDob,
            RdaFissPayer.Fields.insuredDob);
  }

  @Test
  public void testInsuredPayerInsuredDobText() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredDobText,
            RdaFissPayer::getInsuredDobText,
            RdaFissPayer.Fields.insuredDobText,
            9);
  }

  // endregion InsuredPayer tests
  // region AuditTrail tests

  @Test
  public void testAuditTrailBadtStatus() {
    new AuditTrailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissAuditTrail.Builder::setBadtStatusEnum,
            RdaFissAuditTrail::getBadtStatus,
            FissClaimStatus.CLAIM_STATUS_BLANK,
            " ");
  }

  @Test
  public void testAudiTrailBadtLoc() {
    new AuditTrailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissAuditTrail.Builder::setBadtLoc,
            RdaFissAuditTrail::getBadtLoc,
            RdaFissAuditTrail.Fields.badtLoc,
            5);
  }

  @Test
  public void testAudiTrailBadtOperId() {
    new AuditTrailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissAuditTrail.Builder::setBadtOperId,
            RdaFissAuditTrail::getBadtOperId,
            RdaFissAuditTrail.Fields.badtOperId,
            9);
  }

  @Test
  public void testAudiTrailBadtReas() {
    new AuditTrailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissAuditTrail.Builder::setBadtReas,
            RdaFissAuditTrail::getBadtReas,
            RdaFissAuditTrail.Fields.badtReas,
            5);
  }

  @Test
  public void testAudiTrailBadtCurrDate() {
    new AuditTrailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissAuditTrail.Builder::setBadtCurrDateCymd,
            RdaFissAuditTrail::getBadtCurrDate,
            RdaFissAuditTrail.Fields.badtCurrDate);
  }

  // endregion AuditTrail tests

  /**
   * This test ensures that the special processing for the 4-way enum field ServTypeCd works
   * properly. In addition to the type code being set from the enum an extra enum in the entity
   * servTypeCdMapping will always be set as well.
   */
  @Test
  public void servTypeCdEnums() {
    // these required fields must always be present for the transform to be error free
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
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
        .setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);

    // servTypeCd specific tests begin here

    claim.setServTypeCd("2");
    claim.setServTypeCdMapping(RdaFissClaim.ServTypeCdMapping.Normal);
    claimBuilder.setServTypeCdEnum(
        FissBillClassification.BILL_CLASSIFICATION_HOSPITAL_BASED_OR_INPATIENT_PART_B);
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);

    claim.setServTypeCd("6");
    claim.setServTypeCdMapping(RdaFissClaim.ServTypeCdMapping.Clinic);
    claimBuilder.setServTypeCdForClinicsEnum(
        FissBillClassificationForClinics
            .BILL_CLASSIFICATION_FOR_CLINICS_COMMUNITY_MENTAL_HEALTH_CENTER);
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);

    claim.setServTypeCd("5");
    claim.setServTypeCdMapping(RdaFissClaim.ServTypeCdMapping.SpecialFacility);
    claimBuilder.setServTypeCdForSpecialFacilitiesEnum(
        FissBillClassificationForSpecialFacilities
            .BILL_CLASSIFICATION_FOR_SPECIAL_FACILITIES_CRITICAL_ACCESS_HOSPITALS);
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);

    claim.setServTypeCd("Z");
    claim.setServTypeCdMapping(RdaFissClaim.ServTypeCdMapping.Unrecognized);
    claimBuilder.setServTypCdUnrecognized("Z");
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  private void assertChangeMatches(RdaChange.Type changeType) {
    RdaChange<RdaFissClaim> changed = transformer.transformClaim(changeBuilder.build());
    assertEquals(changeType, changed.getType());
    assertThat(changed.getClaim(), samePropertyValuesAs(claim));
  }

  // region Field Tester Classes

  private abstract class AbstractFieldTester<TBuilder, TEntity>
      extends ClaimTransformerFieldTester<
          FissClaim.Builder, FissClaim, RdaFissClaim, TBuilder, TEntity> {
    @Override
    FissClaim.Builder createClaimBuilder() {
      return FissClaim.newBuilder()
          .setDcn("dcn")
          .setHicNo("hicn")
          .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP)
          .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
          .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_CABLE);
    }

    @Override
    RdaChange<RdaFissClaim> transformClaim(FissClaim claim) {
      var changeBuilder =
          FissClaimChange.newBuilder()
              .setSeq(MIN_SEQUENCE_NUM)
              .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
              .setClaim(claim);
      return transformer.transformClaim(changeBuilder.build());
    }

    @Override
    FissClaim buildClaim(FissClaim.Builder builder) {
      return builder.build();
    }
  }

  class ClaimFieldTester extends AbstractFieldTester<FissClaim.Builder, RdaFissClaim> {
    @Override
    FissClaim.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      return claimBuilder;
    }

    @Override
    RdaFissClaim getTestEntity(RdaFissClaim claim) {
      return claim;
    }
  }

  class AuditTrailFieldTester
      extends AbstractFieldTester<FissAuditTrail.Builder, RdaFissAuditTrail> {
    @Override
    FissAuditTrail.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissAuditTrailBuilderList().isEmpty()) {
        claimBuilder.addFissAuditTrailBuilder();
      }
      return claimBuilder.getFissAuditTrailBuilder(0);
    }

    @Override
    RdaFissAuditTrail getTestEntity(RdaFissClaim claim) {
      assertEquals(1, claim.getAuditTrail().size());
      RdaFissAuditTrail answer = claim.getAuditTrail().iterator().next();
      assertEquals("dcn", answer.getDcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "auditTrail-0-" + basicLabel;
    }
  }

  class BeneZPayerFieldTester extends AbstractFieldTester<FissBeneZPayer.Builder, RdaFissPayer> {
    @Override
    FissBeneZPayer.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissPayersBuilderList().isEmpty()) {
        claimBuilder.addFissPayersBuilder();
      }
      return claimBuilder.getFissPayersBuilder(0).getBeneZPayerBuilder();
    }

    @Override
    RdaFissPayer getTestEntity(RdaFissClaim claim) {
      assertEquals(1, claim.getPayers().size());
      RdaFissPayer answer = claim.getPayers().iterator().next();
      assertEquals("dcn", answer.getDcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "payer-0-" + basicLabel;
    }
  }

  class InsuredPayerFieldTester
      extends AbstractFieldTester<FissInsuredPayer.Builder, RdaFissPayer> {
    @Override
    FissInsuredPayer.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissPayersBuilderList().isEmpty()) {
        claimBuilder.addFissPayersBuilder();
      }
      return claimBuilder.getFissPayersBuilder(0).getInsuredPayerBuilder();
    }

    @Override
    RdaFissPayer getTestEntity(RdaFissClaim claim) {
      assertEquals(1, claim.getPayers().size());
      RdaFissPayer answer = claim.getPayers().iterator().next();
      assertEquals("dcn", answer.getDcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "payer-0-" + basicLabel;
    }
  }

  class ProcCodeFieldTester
      extends AbstractFieldTester<FissProcedureCode.Builder, RdaFissProcCode> {
    @Override
    FissProcedureCode.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissProcCodesBuilderList().isEmpty()) {
        claimBuilder.addFissProcCodesBuilder();
        claimBuilder.getFissProcCodesBuilder(0).setProcCd("procCode");
      }
      return claimBuilder.getFissProcCodesBuilder(0);
    }

    @Override
    RdaFissProcCode getTestEntity(RdaFissClaim claim) {
      assertEquals(1, claim.getProcCodes().size());
      RdaFissProcCode answer = claim.getProcCodes().iterator().next();
      assertEquals("dcn", answer.getDcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "procCode-0-" + basicLabel;
    }
  }

  // endregion Field Tester Classes
}
