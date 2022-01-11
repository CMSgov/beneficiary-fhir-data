package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.PreAdjFissAuditTrail;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissPayer;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
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

public class FissClaimTransformerTest {
  // using a fixed Clock ensures our timestamp is predictable
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1621609413832L), ZoneOffset.UTC);
  private final IdHasher idHasher =
      new IdHasher(new IdHasher.Config(10, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));
  private final FissClaimTransformer transformer =
      new FissClaimTransformer(clock, idHasher.getConfig());
  private FissClaimChange.Builder changeBuilder;
  private FissClaim.Builder claimBuilder;
  private PreAdjFissClaim claim;

  @BeforeEach
  public void setUp() {
    changeBuilder = FissClaimChange.newBuilder();
    claimBuilder = FissClaim.newBuilder();
    claim = new PreAdjFissClaim();
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
            1L,
            "1234567890123",
            "50ad6d78d3b8bb1a8195896c7479f04f4af76e8b42011a24146a943ede9321a0"));
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
    claim.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Clinic);
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
        .setMbi("1234567890123")
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
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
        .setClaim(claimBuilder.build());
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getProcCodes(), transformed.getProcCodes(), PreAdjFissProcCode::getPriority);
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
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setClaim(claimBuilder.build());
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getDiagCodes(), transformed.getDiagCodes(), PreAdjFissDiagnosisCode::getPriority);
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
    PreAdjFissPayer payer = new PreAdjFissPayer();
    payer.setDcn("dcn");
    payer.setPriority((short) 0);
    payer.setPayerType(PreAdjFissPayer.PayerType.Insured);
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
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getPayers(), transformed.getPayers(), PreAdjFissPayer::getPriority);
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
    PreAdjFissPayer payer = new PreAdjFissPayer();
    payer.setDcn("dcn");
    payer.setPriority((short) 0);
    payer.setPayerType(PreAdjFissPayer.PayerType.BeneZ);
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
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getPayers(), transformed.getPayers(), PreAdjFissPayer::getPriority);
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
    PreAdjFissAuditTrail auditTrail = new PreAdjFissAuditTrail();
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
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getAuditTrail(), transformed.getAuditTrail(), PreAdjFissAuditTrail::getPriority);
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
            FissClaim.Builder::setDcn, PreAdjFissClaim::getDcn, "dcn", 23);
  }

  @Test
  public void testClaimHicNo() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setHicNo, PreAdjFissClaim::getHicNo, "hicNo", 12);
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
            FissClaim.Builder::setCurrStatusUnrecognized, PreAdjFissClaim.Fields.currStatus, "ZZZ");
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
            PreAdjFissClaim.Fields.currLoc1,
            1);
  }

  @Test
  public void testClaimCurrLoc2() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setCurrLoc2Enum,
            PreAdjFissClaim::getCurrLoc2,
            FissCurrentLocation2.CURRENT_LOCATION_2_CABLE,
            "9000")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setCurrLoc2Unrecognized,
            PreAdjFissClaim::getCurrLoc2,
            PreAdjFissClaim.Fields.currLoc2,
            5);
  }

  @Test
  public void testClaimMedaProvId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMedaProvId,
            PreAdjFissClaim::getMedaProvId,
            PreAdjFissClaim.Fields.medaProvId,
            13);
  }

  @Test
  public void testClaimMedaProv6() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMedaProv6,
            PreAdjFissClaim::getMedaProv_6,
            PreAdjFissClaim.Fields.medaProv_6,
            6);
  }

  @Test
  public void testClaimTotalChargeAmount() {
    new ClaimFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissClaim.Builder::setTotalChargeAmount,
            PreAdjFissClaim::getTotalChargeAmount,
            PreAdjFissClaim.Fields.totalChargeAmount);
  }

  @Test
  public void testClaimRecdDtCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setRecdDtCymd,
            PreAdjFissClaim::getReceivedDate,
            PreAdjFissClaim.Fields.receivedDate);
  }

  @Test
  public void testClaimCurrTranDtCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setCurrTranDtCymd,
            PreAdjFissClaim::getCurrTranDate,
            PreAdjFissClaim.Fields.currTranDate);
  }

  @Test
  public void testClaimAdmDiagCode() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdmDiagCode,
            PreAdjFissClaim::getAdmitDiagCode,
            PreAdjFissClaim.Fields.admitDiagCode,
            7);
  }

  @Test
  public void testClaimPrincipleDiag() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPrincipleDiag,
            PreAdjFissClaim::getPrincipleDiag,
            PreAdjFissClaim.Fields.principleDiag,
            7);
  }

  @Test
  public void testClaimNpiNumber() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setNpiNumber,
            PreAdjFissClaim::getNpiNumber,
            PreAdjFissClaim.Fields.npiNumber,
            10);
  }

  @Test
  public void testClaimMbi() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMbi, PreAdjFissClaim::getMbi, PreAdjFissClaim.Fields.mbi, 13)
        .verifyIdHashFieldPopulatedCorrectly(
            FissClaim.Builder::setMbi, PreAdjFissClaim::getMbiHash, 13, idHasher);
  }

  @Test
  public void testClaimFedTaxNb() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setFedTaxNb,
            PreAdjFissClaim::getFedTaxNumber,
            PreAdjFissClaim.Fields.fedTaxNumber,
            10);
  }

  @Test
  public void testClaimPracLocAddr1() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocAddr1,
            PreAdjFissClaim::getPracLocAddr1,
            PreAdjFissClaim.Fields.pracLocAddr1,
            2147483647);
  }

  @Test
  public void testClaimPracLocAddr2() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocAddr2,
            PreAdjFissClaim::getPracLocAddr2,
            PreAdjFissClaim.Fields.pracLocAddr2,
            2147483647);
  }

  @Test
  public void testClaimPracLocCity() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocCity,
            PreAdjFissClaim::getPracLocCity,
            PreAdjFissClaim.Fields.pracLocCity,
            2147483647);
  }

  @Test
  public void testClaimPracLocState() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocState,
            PreAdjFissClaim::getPracLocState,
            PreAdjFissClaim.Fields.pracLocState,
            2);
  }

  @Test
  public void testClaimPracLocZip() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPracLocZip,
            PreAdjFissClaim::getPracLocZip,
            PreAdjFissClaim.Fields.pracLocZip,
            15);
  }

  @Test
  public void testClaimStmtCovFromCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setStmtCovFromCymd,
            PreAdjFissClaim::getStmtCovFromDate,
            PreAdjFissClaim.Fields.stmtCovFromDate);
  }

  @Test
  public void testClaimStmtCovToCymd() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setStmtCovToCymd,
            PreAdjFissClaim::getStmtCovToDate,
            PreAdjFissClaim.Fields.stmtCovToDate);
  }

  @Test
  public void testClaimLobCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setLobCdEnum,
            claim -> String.valueOf(claim.getLobCd()),
            FissBillFacilityType.BILL_FACILITY_TYPE_HOME_HEALTH,
            "3")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setLobCdUnrecognized,
            claim -> String.valueOf(claim.getLobCd()),
            PreAdjFissClaim.Fields.lobCd,
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
            PreAdjFissClaim.Fields.servTypeCd,
            1);
  }

  @Test
  public void testClaimFreqCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setFreqCdEnum,
            PreAdjFissClaim::getFreqCd,
            FissBillFrequency.BILL_FREQUENCY_ADJUSTMENT_CLAIM_F,
            "F")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setFreqCdUnrecognized,
            PreAdjFissClaim::getFreqCd,
            PreAdjFissClaim.Fields.freqCd,
            1);
  }

  @Test
  public void testClaimBillTypCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setBillTypCd,
            PreAdjFissClaim::getBillTypCd,
            PreAdjFissClaim.Fields.billTypCd,
            3);
  }

  @Test
  public void testClaimRejectCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setRejectCd,
            PreAdjFissClaim::getRejectCd,
            PreAdjFissClaim.Fields.rejectCd,
            5);
  }

  @Test
  public void testClaimFullPartDenInd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setFullPartDenInd,
            PreAdjFissClaim::getFullPartDenInd,
            PreAdjFissClaim.Fields.fullPartDenInd,
            1);
  }

  @Test
  public void testClaimNonPayInd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setNonPayInd,
            PreAdjFissClaim::getNonPayInd,
            PreAdjFissClaim.Fields.nonPayInd,
            2);
  }

  @Test
  public void testClaimXrefDcnNbr() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setXrefDcnNbr,
            PreAdjFissClaim::getXrefDcnNbr,
            PreAdjFissClaim.Fields.xrefDcnNbr,
            23);
  }

  @Test
  public void testClaimAdjReqCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAdjReqCdEnum,
            PreAdjFissClaim::getAdjReqCd,
            FissAdjustmentRequestorCode.ADJUSTMENT_REQUESTOR_CODE_FISCAL_INTERMEDIARY,
            "F")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdjReqCdUnrecognized,
            PreAdjFissClaim::getAdjReqCd,
            PreAdjFissClaim.Fields.adjReqCd,
            1);
  }

  @Test
  public void testClaimAdjReasCd() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdjReasCd,
            PreAdjFissClaim::getAdjReasCd,
            PreAdjFissClaim.Fields.adjReasCd,
            2);
  }

  @Test
  public void testClaimCancelXrefDcn() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setCancelXrefDcn,
            PreAdjFissClaim::getCancelXrefDcn,
            PreAdjFissClaim.Fields.cancelXrefDcn,
            23);
  }

  @Test
  public void testClaimCancelDate() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setCancelDateCymd,
            PreAdjFissClaim::getCancelDate,
            PreAdjFissClaim.Fields.cancelDate);
  }

  @Test
  public void testClaimCancAdjCd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setCancAdjCdEnum,
            PreAdjFissClaim::getCancAdjCd,
            FissCancelAdjustmentCode.CANCEL_ADJUSTMENT_CODE_COVERAGE,
            "C")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setCancAdjCdUnrecognized,
            PreAdjFissClaim::getCancAdjCd,
            PreAdjFissClaim.Fields.cancAdjCd,
            1);
  }

  @Test
  public void testClaimOriginalXrefDcn() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOriginalXrefDcn,
            PreAdjFissClaim::getOriginalXrefDcn,
            PreAdjFissClaim.Fields.originalXrefDcn,
            23);
  }

  @Test
  public void testClaimPaidDt() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setPaidDtCymd,
            PreAdjFissClaim::getPaidDt,
            PreAdjFissClaim.Fields.paidDt);
  }

  @Test
  public void testClaimAdmDate() {
    new ClaimFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissClaim.Builder::setAdmDateCymd,
            PreAdjFissClaim::getAdmDate,
            PreAdjFissClaim.Fields.admDate);
  }

  @Test
  public void testClaimAdmSource() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAdmSourceEnum,
            PreAdjFissClaim::getAdmSource,
            FissSourceOfAdmission.SOURCE_OF_ADMISSION_CLINIC_REFERRAL,
            "2")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdmSourceUnrecognized,
            PreAdjFissClaim::getAdmSource,
            PreAdjFissClaim.Fields.admSource,
            1);
  }

  @Test
  public void testClaimPrimaryPayerCode() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setPrimaryPayerCodeEnum,
            PreAdjFissClaim::getPrimaryPayerCode,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setPrimaryPayerCodeUnrecognized,
            PreAdjFissClaim::getPrimaryPayerCode,
            PreAdjFissClaim.Fields.primaryPayerCode,
            1);
  }

  @Test
  public void testClaimAttendPhysId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysId,
            PreAdjFissClaim::getAttendPhysId,
            PreAdjFissClaim.Fields.attendPhysId,
            16);
  }

  @Test
  public void testClaimAttendPhysLname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysLname,
            PreAdjFissClaim::getAttendPhysLname,
            PreAdjFissClaim.Fields.attendPhysLname,
            17);
  }

  @Test
  public void testClaimAttendPhysFname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysFname,
            PreAdjFissClaim::getAttendPhysFname,
            PreAdjFissClaim.Fields.attendPhysFname,
            18);
  }

  @Test
  public void testClaimAttendPhysMint() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysMint,
            PreAdjFissClaim::getAttendPhysMint,
            PreAdjFissClaim.Fields.attendPhysMint,
            1);
  }

  @Test
  public void testClaimAttendPhysFlag() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAttendPhysFlagEnum,
            PreAdjFissClaim::getAttendPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAttendPhysFlagUnrecognized,
            PreAdjFissClaim::getAttendPhysFlag,
            PreAdjFissClaim.Fields.attendPhysFlag,
            1);
  }

  @Test
  public void testClaimOperatingPhysId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperatingPhysId,
            PreAdjFissClaim::getOperatingPhysId,
            PreAdjFissClaim.Fields.operatingPhysId,
            16);
  }

  @Test
  public void testClaimOperPhysLname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperPhysLname,
            PreAdjFissClaim::getOperPhysLname,
            PreAdjFissClaim.Fields.operPhysLname,
            17);
  }

  @Test
  public void testClaimOperPhysFname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperPhysFname,
            PreAdjFissClaim::getOperPhysFname,
            PreAdjFissClaim.Fields.operPhysFname,
            18);
  }

  @Test
  public void testClaimOperPhysMint() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperPhysMint,
            PreAdjFissClaim::getOperPhysMint,
            PreAdjFissClaim.Fields.operPhysMint,
            1);
  }

  @Test
  public void testClaimOperPhysFlag() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setOperPhysFlagEnum,
            PreAdjFissClaim::getOperPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOperPhysFlagUnrecognized,
            PreAdjFissClaim::getOperPhysFlag,
            PreAdjFissClaim.Fields.operPhysFlag,
            1);
  }

  @Test
  public void testClaimOthPhysId() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysId,
            PreAdjFissClaim::getOthPhysId,
            PreAdjFissClaim.Fields.othPhysId,
            16);
  }

  @Test
  public void testClaimOthPhysLname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysLname,
            PreAdjFissClaim::getOthPhysLname,
            PreAdjFissClaim.Fields.othPhysLname,
            17);
  }

  @Test
  public void testClaimOthPhysFname() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysFname,
            PreAdjFissClaim::getOthPhysFname,
            PreAdjFissClaim.Fields.othPhysFname,
            18);
  }

  @Test
  public void testClaimOthPhysMint() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysMint,
            PreAdjFissClaim::getOthPhysMint,
            PreAdjFissClaim.Fields.othPhysMint,
            1);
  }

  @Test
  public void testClaimOthPhysFlag() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setOthPhysFlagEnum,
            PreAdjFissClaim::getOthPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setOthPhysFlagUnrecognized,
            PreAdjFissClaim::getOthPhysFlag,
            PreAdjFissClaim.Fields.othPhysFlag,
            1);
  }

  @Test
  public void testClaimXrefHicNbr() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setXrefHicNbr,
            PreAdjFissClaim::getXrefHicNbr,
            PreAdjFissClaim.Fields.xrefHicNbr,
            12);
  }

  @Test
  public void testClaimProcNewHicInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setProcNewHicIndEnum,
            PreAdjFissClaim::getProcNewHicInd,
            FissProcessNewHealthInsuranceClaimNumberIndicator.PROCESS_NEW_HIC_INDICATOR_Y,
            "Y")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setProcNewHicIndUnrecognized,
            PreAdjFissClaim::getProcNewHicInd,
            PreAdjFissClaim.Fields.procNewHicInd,
            1);
  }

  @Test
  public void testClaimNewHic() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setNewHic,
            PreAdjFissClaim::getNewHic,
            PreAdjFissClaim.Fields.newHic,
            12);
  }

  @Test
  public void testClaimReposInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setReposIndEnum,
            PreAdjFissClaim::getReposInd,
            FissRepositoryIndicator.REPOSITORY_INDICATOR_HIC_HAS_BEEN_MOVED,
            "Y")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setReposIndUnrecognized,
            PreAdjFissClaim::getReposInd,
            PreAdjFissClaim.Fields.reposInd,
            1);
  }

  @Test
  public void testClaimReposHic() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setReposHic,
            PreAdjFissClaim::getReposHic,
            PreAdjFissClaim.Fields.reposHic,
            12);
  }

  @Test
  public void testClaimMbiSubmBeneInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setMbiSubmBeneIndEnum,
            PreAdjFissClaim::getMbiSubmBeneInd,
            FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier.FISS_HIC_OR_MBI_IS_HIC,
            "H")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMbiSubmBeneIndUnrecognized,
            PreAdjFissClaim::getMbiSubmBeneInd,
            PreAdjFissClaim.Fields.mbiSubmBeneInd,
            1);
  }

  @Test
  public void testClaimAdjMbiInd() {
    new ClaimFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissClaim.Builder::setAdjMbiIndEnum,
            PreAdjFissClaim::getAdjMbiInd,
            FissAdjustmentMedicareBeneficiaryIdentifierIndicator
                .ADJUSTMENT_MBI_INDICATOR_HIC_SUBMITTED_ON_ADJUSTMENT_OR_CANCEL_CLAIM,
            "H")
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdjMbiIndUnrecognized,
            PreAdjFissClaim::getAdjMbiInd,
            PreAdjFissClaim.Fields.adjMbiInd,
            1);
  }

  @Test
  public void testClaimAdjMbi() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setAdjMbi,
            PreAdjFissClaim::getAdjMbi,
            PreAdjFissClaim.Fields.adjMbi,
            11);
  }

  @Test
  public void testClaimMedicalRecordNo() {
    new ClaimFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissClaim.Builder::setMedicalRecordNo,
            PreAdjFissClaim::getMedicalRecordNo,
            PreAdjFissClaim.Fields.medicalRecordNo,
            17);
  }

  // endregion Claim tests
  // region ProcCode tests

  @Test
  public void testProcCodeProcCd() {
    new ProcCodeFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissProcedureCode.Builder::setProcCd,
            PreAdjFissProcCode::getProcCode,
            PreAdjFissProcCode.Fields.procCode,
            10);
  }

  @Test
  public void testProcCodeProcFlag() {
    new ProcCodeFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissProcedureCode.Builder::setProcFlag,
            PreAdjFissProcCode::getProcFlag,
            PreAdjFissProcCode.Fields.procFlag,
            4);
  }

  @Test
  public void testProcCodeProcDt() {
    new ProcCodeFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissProcedureCode.Builder::setProcDt,
            PreAdjFissProcCode::getProcDate,
            PreAdjFissProcCode.Fields.procDate);
  }

  // endregion ProcCode tests
  // region BeneZPayer tests

  @Test
  public void testBeneZPayerPayersId() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setPayersIdEnum,
            PreAdjFissPayer::getPayersId,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setPayersIdUnrecognized,
            PreAdjFissPayer::getPayersId,
            PreAdjFissPayer.Fields.payersId,
            1);
  }

  @Test
  public void testBeneZPayerPayersName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setPayersName,
            PreAdjFissPayer::getPayersName,
            PreAdjFissPayer.Fields.payersName,
            32);
  }

  @Test
  public void testBeneZPayerRelInd() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setRelIndEnum,
            PreAdjFissPayer::getRelInd,
            FissReleaseOfInformation.RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE,
            "N")
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setRelIndUnrecognized,
            PreAdjFissPayer::getRelInd,
            PreAdjFissPayer.Fields.relInd,
            1);
  }

  @Test
  public void testBeneZPayerAssignInd() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setAssignIndEnum,
            PreAdjFissPayer::getAssignInd,
            FissAssignmentOfBenefitsIndicator.ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED,
            "Y")
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setAssignIndUnrecognized,
            PreAdjFissPayer::getAssignInd,
            PreAdjFissPayer.Fields.assignInd,
            1);
  }

  @Test
  public void testBeneZPayerProviderNumber() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setProviderNumber,
            PreAdjFissPayer::getProviderNumber,
            PreAdjFissPayer.Fields.providerNumber,
            13);
  }

  @Test
  public void testBeneZPayerAdjDcnIcn() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setAdjDcnIcn,
            PreAdjFissPayer::getAdjDcnIcn,
            PreAdjFissPayer.Fields.adjDcnIcn,
            23);
  }

  @Test
  public void testBeneZPayerPriorPmt() {
    new BeneZPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissBeneZPayer.Builder::setPriorPmt,
            PreAdjFissPayer::getPriorPmt,
            PreAdjFissPayer.Fields.priorPmt);
  }

  @Test
  public void testBeneZPayerEstAmtDue() {
    new BeneZPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissBeneZPayer.Builder::setEstAmtDue,
            PreAdjFissPayer::getEstAmtDue,
            PreAdjFissPayer.Fields.estAmtDue);
  }

  @Test
  public void testBeneZPayerBeneRel() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setBeneRelEnum,
            PreAdjFissPayer::getBeneRel,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_DEFAULT,
            "00")
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneRelUnrecognized,
            PreAdjFissPayer::getBeneRel,
            PreAdjFissPayer.Fields.beneRel,
            2);
  }

  @Test
  public void testBeneZPayerBeneLastName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneLastName,
            PreAdjFissPayer::getBeneLastName,
            PreAdjFissPayer.Fields.beneLastName,
            15);
  }

  @Test
  public void testBeneZPayerBeneFirstName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneFirstName,
            PreAdjFissPayer::getBeneFirstName,
            PreAdjFissPayer.Fields.beneFirstName,
            10);
  }

  @Test
  public void testBeneZPayerBeneMidInit() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneMidInit,
            PreAdjFissPayer::getBeneMidInit,
            PreAdjFissPayer.Fields.beneMidInit,
            1);
  }

  @Test
  public void testBeneZPayerBeneSsnHic() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneSsnHic,
            PreAdjFissPayer::getBeneSsnHic,
            PreAdjFissPayer.Fields.beneSsnHic,
            19);
  }

  @Test
  public void testBeneZPayerInsuredGroupName() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setInsuredGroupName,
            PreAdjFissPayer::getInsuredGroupName,
            PreAdjFissPayer.Fields.insuredGroupName,
            17);
  }

  @Test
  public void testBeneZPayerBeneDob() {
    new BeneZPayerFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissBeneZPayer.Builder::setBeneDob,
            PreAdjFissPayer::getBeneDob,
            PreAdjFissPayer.Fields.beneDob);
  }

  @Test
  public void testBeneZPayerBeneSex() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setBeneSexEnum,
            PreAdjFissPayer::getBeneSex,
            FissBeneficiarySex.BENEFICIARY_SEX_FEMALE,
            "F")
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setBeneSexUnrecognized,
            PreAdjFissPayer::getBeneSex,
            PreAdjFissPayer.Fields.beneSex,
            1);
  }

  @Test
  public void testBeneZPayerTreatAuthCd() {
    new BeneZPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setTreatAuthCd,
            PreAdjFissPayer::getTreatAuthCd,
            PreAdjFissPayer.Fields.treatAuthCd,
            18);
  }

  @Test
  public void testBeneZPayerInsuredSex() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setInsuredSexEnum,
            PreAdjFissPayer::getInsuredSex,
            FissBeneficiarySex.BENEFICIARY_SEX_FEMALE,
            "F")
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setInsuredSexUnrecognized,
            PreAdjFissPayer::getInsuredSex,
            PreAdjFissPayer.Fields.insuredSex,
            1);
  }

  @Test
  public void testBeneZPayerInsuredRelX12() {
    new BeneZPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissBeneZPayer.Builder::setInsuredRelX12Enum,
            PreAdjFissPayer::getInsuredRelX12,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_DEFAULT,
            "00")
        .verifyStringFieldCopiedCorrectly(
            FissBeneZPayer.Builder::setInsuredRelX12Unrecognized,
            PreAdjFissPayer::getInsuredRelX12,
            PreAdjFissPayer.Fields.insuredRelX12,
            2);
  }

  // endregion BeneZPayer tests
  // region InsuredPayer tests

  @Test
  public void testInsuredPayerPayersId() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setPayersIdEnum,
            PreAdjFissPayer::getPayersId,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setPayersIdUnrecognized,
            PreAdjFissPayer::getPayersId,
            PreAdjFissPayer.Fields.payersId,
            1);
  }

  @Test
  public void testInsuredPayerPayersName() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setPayersName,
            PreAdjFissPayer::getPayersName,
            PreAdjFissPayer.Fields.payersName,
            32);
  }

  @Test
  public void testInsuredPayerRelInd() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setRelIndEnum,
            PreAdjFissPayer::getRelInd,
            FissReleaseOfInformation.RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE,
            "N")
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setRelIndUnrecognized,
            PreAdjFissPayer::getRelInd,
            PreAdjFissPayer.Fields.relInd,
            1);
  }

  @Test
  public void testInsuredPayerAssignInd() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setAssignIndEnum,
            PreAdjFissPayer::getAssignInd,
            FissAssignmentOfBenefitsIndicator.ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED,
            "Y")
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setAssignIndUnrecognized,
            PreAdjFissPayer::getAssignInd,
            PreAdjFissPayer.Fields.assignInd,
            1);
  }

  @Test
  public void testInsuredPayerProviderNumber() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setProviderNumber,
            PreAdjFissPayer::getProviderNumber,
            PreAdjFissPayer.Fields.providerNumber,
            13);
  }

  @Test
  public void testInsuredPayerAdjDcnIcn() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setAdjDcnIcn,
            PreAdjFissPayer::getAdjDcnIcn,
            PreAdjFissPayer.Fields.adjDcnIcn,
            23);
  }

  @Test
  public void testInsuredPayerPriorPmt() {
    new InsuredPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissInsuredPayer.Builder::setPriorPmt,
            PreAdjFissPayer::getPriorPmt,
            PreAdjFissPayer.Fields.priorPmt);
  }

  @Test
  public void testInsuredPayerEstAmtDue() {
    new InsuredPayerFieldTester()
        .verifyAmountStringFieldTransformedCorrectly(
            FissInsuredPayer.Builder::setEstAmtDue,
            PreAdjFissPayer::getEstAmtDue,
            PreAdjFissPayer.Fields.estAmtDue);
  }

  @Test
  public void testInsuredPayerInsuredRel() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setInsuredRelEnum,
            PreAdjFissPayer::getInsuredRel,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_EMPLOYEE,
            "08")
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredRelUnrecognized,
            PreAdjFissPayer::getInsuredRel,
            PreAdjFissPayer.Fields.insuredRel,
            2);
  }

  @Test
  public void testInsuredPayerInsuredName() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredName,
            PreAdjFissPayer::getInsuredName,
            PreAdjFissPayer.Fields.insuredName,
            25);
  }

  @Test
  public void testInsuredPayerInsuredSsnHic() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredSsnHic,
            PreAdjFissPayer::getInsuredSsnHic,
            PreAdjFissPayer.Fields.insuredSsnHic,
            19);
  }

  @Test
  public void testInsuredPayerInsuredGroupName() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredGroupName,
            PreAdjFissPayer::getInsuredGroupName,
            PreAdjFissPayer.Fields.insuredGroupName,
            17);
  }

  @Test
  public void testInsuredPayerInsuredGroupNbr() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredGroupNbr,
            PreAdjFissPayer::getInsuredGroupNbr,
            PreAdjFissPayer.Fields.insuredGroupNbr,
            20);
  }

  @Test
  public void testInsuredPayerTreatAuthCd() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setTreatAuthCd,
            PreAdjFissPayer::getTreatAuthCd,
            PreAdjFissPayer.Fields.treatAuthCd,
            18);
  }

  @Test
  public void testInsuredPayerInsuredSex() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setInsuredSexEnum,
            PreAdjFissPayer::getInsuredSex,
            FissBeneficiarySex.BENEFICIARY_SEX_UNKNOWN,
            "U")
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredSexUnrecognized,
            PreAdjFissPayer::getInsuredSex,
            PreAdjFissPayer.Fields.insuredSex,
            1);
  }

  @Test
  public void testInsuredPayerInsuredRelX12() {
    new InsuredPayerFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissInsuredPayer.Builder::setInsuredRelX12Enum,
            PreAdjFissPayer::getInsuredRelX12,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_DEFAULT,
            "00")
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredRelX12Unrecognized,
            PreAdjFissPayer::getInsuredRelX12,
            PreAdjFissPayer.Fields.insuredRelX12,
            2);
  }

  @Test
  public void testInsuredPayerInsuredDob() {
    new InsuredPayerFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissInsuredPayer.Builder::setInsuredDob,
            PreAdjFissPayer::getInsuredDob,
            PreAdjFissPayer.Fields.insuredDob);
  }

  @Test
  public void testInsuredPayerInsuredDobText() {
    new InsuredPayerFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissInsuredPayer.Builder::setInsuredDobText,
            PreAdjFissPayer::getInsuredDobText,
            PreAdjFissPayer.Fields.insuredDobText,
            9);
  }

  // endregion InsuredPayer tests
  // region AuditTrail tests

  @Test
  public void testAuditTrailBadtStatus() {
    new AuditTrailFieldTester()
        .verifyEnumFieldStringValueExtractedCorrectly(
            FissAuditTrail.Builder::setBadtStatusEnum,
            PreAdjFissAuditTrail::getBadtStatus,
            FissClaimStatus.CLAIM_STATUS_BLANK,
            " ");
  }

  @Test
  public void testAudiTrailBadtLoc() {
    new AuditTrailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissAuditTrail.Builder::setBadtLoc,
            PreAdjFissAuditTrail::getBadtLoc,
            PreAdjFissAuditTrail.Fields.badtLoc,
            5);
  }

  @Test
  public void testAudiTrailBadtOperId() {
    new AuditTrailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissAuditTrail.Builder::setBadtOperId,
            PreAdjFissAuditTrail::getBadtOperId,
            PreAdjFissAuditTrail.Fields.badtOperId,
            9);
  }

  @Test
  public void testAudiTrailBadtReas() {
    new AuditTrailFieldTester()
        .verifyStringFieldCopiedCorrectly(
            FissAuditTrail.Builder::setBadtReas,
            PreAdjFissAuditTrail::getBadtReas,
            PreAdjFissAuditTrail.Fields.badtReas,
            5);
  }

  @Test
  public void testAudiTrailBadtCurrDate() {
    new AuditTrailFieldTester()
        .verifyDateStringFieldTransformedCorrectly(
            FissAuditTrail.Builder::setBadtCurrDateCymd,
            PreAdjFissAuditTrail::getBadtCurrDate,
            PreAdjFissAuditTrail.Fields.badtCurrDate);
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
    claim.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Normal);
    claimBuilder.setServTypeCdEnum(
        FissBillClassification.BILL_CLASSIFICATION_HOSPITAL_BASED_OR_INPATIENT_PART_B);
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);

    claim.setServTypeCd("6");
    claim.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Clinic);
    claimBuilder.setServTypeCdForClinicsEnum(
        FissBillClassificationForClinics
            .BILL_CLASSIFICATION_FOR_CLINICS_COMMUNITY_MENTAL_HEALTH_CENTER);
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);

    claim.setServTypeCd("5");
    claim.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.SpecialFacility);
    claimBuilder.setServTypeCdForSpecialFacilitiesEnum(
        FissBillClassificationForSpecialFacilities
            .BILL_CLASSIFICATION_FOR_SPECIAL_FACILITIES_CRITICAL_ACCESS_HOSPITALS);
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);

    claim.setServTypeCd("Z");
    claim.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Unrecognized);
    claimBuilder.setServTypCdUnrecognized("Z");
    changeBuilder.setClaim(claimBuilder.build());
    assertChangeMatches(RdaChange.Type.INSERT);
  }

  private void assertChangeMatches(RdaChange.Type changeType) {
    RdaChange<PreAdjFissClaim> changed = transformer.transformClaim(changeBuilder.build());
    assertEquals(changeType, changed.getType());
    assertThat(changed.getClaim(), samePropertyValuesAs(claim));
  }

  // region Field Tester Classes

  private abstract class AbstractFieldTester<TBuilder, TEntity>
      extends ClaimTransformerFieldTester<
          FissClaim.Builder, FissClaim, PreAdjFissClaim, TBuilder, TEntity> {
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
    RdaChange<PreAdjFissClaim> transformClaim(FissClaim claim) {
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

  class ClaimFieldTester extends AbstractFieldTester<FissClaim.Builder, PreAdjFissClaim> {
    @Override
    FissClaim.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      return claimBuilder;
    }

    @Override
    PreAdjFissClaim getTestEntity(PreAdjFissClaim claim) {
      return claim;
    }
  }

  class AuditTrailFieldTester
      extends AbstractFieldTester<FissAuditTrail.Builder, PreAdjFissAuditTrail> {
    @Override
    FissAuditTrail.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissAuditTrailBuilderList().isEmpty()) {
        claimBuilder.addFissAuditTrailBuilder();
      }
      return claimBuilder.getFissAuditTrailBuilder(0);
    }

    @Override
    PreAdjFissAuditTrail getTestEntity(PreAdjFissClaim claim) {
      assertEquals(1, claim.getAuditTrail().size());
      PreAdjFissAuditTrail answer = claim.getAuditTrail().iterator().next();
      assertEquals("dcn", answer.getDcn());
      assertEquals((short) 0, answer.getPriority());
      return answer;
    }

    @Override
    String getLabel(String basicLabel) {
      return "auditTrail-0-" + basicLabel;
    }
  }

  class BeneZPayerFieldTester extends AbstractFieldTester<FissBeneZPayer.Builder, PreAdjFissPayer> {
    @Override
    FissBeneZPayer.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissPayersBuilderList().isEmpty()) {
        claimBuilder.addFissPayersBuilder();
      }
      return claimBuilder.getFissPayersBuilder(0).getBeneZPayerBuilder();
    }

    @Override
    PreAdjFissPayer getTestEntity(PreAdjFissClaim claim) {
      assertEquals(1, claim.getPayers().size());
      PreAdjFissPayer answer = claim.getPayers().iterator().next();
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
      extends AbstractFieldTester<FissInsuredPayer.Builder, PreAdjFissPayer> {
    @Override
    FissInsuredPayer.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissPayersBuilderList().isEmpty()) {
        claimBuilder.addFissPayersBuilder();
      }
      return claimBuilder.getFissPayersBuilder(0).getInsuredPayerBuilder();
    }

    @Override
    PreAdjFissPayer getTestEntity(PreAdjFissClaim claim) {
      assertEquals(1, claim.getPayers().size());
      PreAdjFissPayer answer = claim.getPayers().iterator().next();
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
      extends AbstractFieldTester<FissProcedureCode.Builder, PreAdjFissProcCode> {
    @Override
    FissProcedureCode.Builder getTestEntityBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissProcCodesBuilderList().isEmpty()) {
        claimBuilder.addFissProcCodesBuilder();
        claimBuilder.getFissProcCodesBuilder(0).setProcCd("procCode");
      }
      return claimBuilder.getFissProcCodesBuilder(0);
    }

    @Override
    PreAdjFissProcCode getTestEntity(PreAdjFissClaim claim) {
      assertEquals(1, claim.getProcCodes().size());
      PreAdjFissProcCode answer = claim.getProcCodes().iterator().next();
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
