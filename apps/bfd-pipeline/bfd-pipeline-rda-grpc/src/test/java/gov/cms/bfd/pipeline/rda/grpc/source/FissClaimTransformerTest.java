package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
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
  private FissClaimChange.Builder changeBuilder;
  private FissClaim.Builder claimBuilder;
  private PreAdjFissClaim claim;

  @Before
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

  @Test
  public void allFields() {
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
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
        .setClaim(claimBuilder.build());
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
        .setSeq(MIN_SEQUENCE_NUM)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setClaim(claimBuilder.build());
    PreAdjFissClaim transformed = transformer.transformClaim(changeBuilder.build()).getClaim();
    assertThat(transformed, samePropertyValuesAs(claim));
    TransformerTestUtils.assertListContentsHaveSamePropertyValues(
        claim.getDiagCodes(), transformed.getDiagCodes(), PreAdjFissDiagnosisCode::getPriority);
  }

  @Test
  public void insuredPayer() {
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

  @Test
  public void benezPayer() {
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
                        .setBeneSexEnum(FissBeneficiarySex.BENEFIXIARY_SEX_FEMALE)
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

  @Test
  public void auditTrail() {
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
  public void requiredFieldsMissing() {
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
  public void testBadDcn() {
    assertClaimTransformationError(
        () -> claimBuilder.setDcn("123456789012345678901234"),
        new DataTransformer.ErrorMessage("dcn", "invalid length: expected=[1,23] actual=24"));
  }

  @Test
  public void testBadHicNo() {
    assertClaimTransformationError(
        () -> claimBuilder.setHicNo("1234567890123"),
        new DataTransformer.ErrorMessage("hicNo", "invalid length: expected=[1,12] actual=13"));
  }

  @Test
  public void testBadCurrStatus() {
    assertClaimTransformationError(
        () -> claimBuilder.setCurrStatusUnrecognized("ZZZ"),
        new DataTransformer.ErrorMessage("currStatus", "unsupported enum value"));
  }

  @Test
  public void testBadCurrLoc1() {
    assertClaimTransformationError(
        () -> claimBuilder.setCurrLoc1Unrecognized("ZZ"),
        new DataTransformer.ErrorMessage("currLoc1", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadCurrLoc2() {
    assertClaimTransformationError(
        () -> claimBuilder.setCurrLoc2Unrecognized("123456"),
        new DataTransformer.ErrorMessage("currLoc2", "invalid length: expected=[1,5] actual=6"));
  }

  @Test
  public void testBadMedaProvId() {
    assertClaimTransformationError(
        () -> claimBuilder.setMedaProvId("12345678901234"),
        new DataTransformer.ErrorMessage(
            "medaProvId", "invalid length: expected=[1,13] actual=14"));
  }

  @Test
  public void testBadMedaProv6() {
    assertClaimTransformationError(
        () -> claimBuilder.setMedaProv6("1234567"),
        new DataTransformer.ErrorMessage("medaProv_6", "invalid length: expected=[1,6] actual=7"));
  }

  @Test
  public void testBadTotalChargeAmount() {
    assertClaimTransformationError(
        () -> claimBuilder.setTotalChargeAmount("not-a-number"),
        new DataTransformer.ErrorMessage("totalChargeAmount", "invalid amount"));
  }

  @Test
  public void testBadRecdDtCymd() {
    assertClaimTransformationError(
        () -> claimBuilder.setRecdDtCymd("not-a-date"),
        new DataTransformer.ErrorMessage("receivedDate", "invalid date"));
  }

  @Test
  public void testBadCurrTranDtCymd() {
    assertClaimTransformationError(
        () -> claimBuilder.setCurrTranDtCymd("not-a-date"),
        new DataTransformer.ErrorMessage("currTranDate", "invalid date"));
  }

  @Test
  public void testBadAdmDiagCode() {
    assertClaimTransformationError(
        () -> claimBuilder.setAdmDiagCode("12345678"),
        new DataTransformer.ErrorMessage(
            "admitDiagCode", "invalid length: expected=[1,7] actual=8"));
  }

  @Test
  public void testBadPrincipleDiag() {
    assertClaimTransformationError(
        () -> claimBuilder.setPrincipleDiag("12345678"),
        new DataTransformer.ErrorMessage(
            "principleDiag", "invalid length: expected=[1,7] actual=8"));
  }

  @Test
  public void testBadNpiNumber() {
    assertClaimTransformationError(
        () -> claimBuilder.setNpiNumber("12345678901"),
        new DataTransformer.ErrorMessage("npiNumber", "invalid length: expected=[1,10] actual=11"));
  }

  @Test
  public void testBadMbi() {
    assertClaimTransformationError(
        () -> claimBuilder.setMbi("12345678901234"),
        new DataTransformer.ErrorMessage("mbi", "invalid length: expected=[1,13] actual=14"));
  }

  @Test
  public void testBadFedTaxNb() {
    assertClaimTransformationError(
        () -> claimBuilder.setFedTaxNb("12345678901"),
        new DataTransformer.ErrorMessage(
            "fedTaxNumber", "invalid length: expected=[1,10] actual=11"));
  }

  @Test
  public void testBadPracLocAddr1() {
    assertClaimTransformationError(
        () -> claimBuilder.setPracLocAddr1(""),
        new DataTransformer.ErrorMessage(
            "pracLocAddr1", "invalid length: expected=[1,2147483647] actual=0"));
  }

  @Test
  public void testBadPracLocAddr2() {
    assertClaimTransformationError(
        () -> claimBuilder.setPracLocAddr2(""),
        new DataTransformer.ErrorMessage(
            "pracLocAddr2", "invalid length: expected=[1,2147483647] actual=0"));
  }

  @Test
  public void testBadPracLocCity() {
    assertClaimTransformationError(
        () -> claimBuilder.setPracLocCity(""),
        new DataTransformer.ErrorMessage(
            "pracLocCity", "invalid length: expected=[1,2147483647] actual=0"));
  }

  @Test
  public void testBadPracLocState() {
    assertClaimTransformationError(
        () -> claimBuilder.setPracLocState("123"),
        new DataTransformer.ErrorMessage(
            "pracLocState", "invalid length: expected=[1,2] actual=3"));
  }

  @Test
  public void testBadPracLocZip() {
    assertClaimTransformationError(
        () -> claimBuilder.setPracLocZip("1234567890123456"),
        new DataTransformer.ErrorMessage(
            "pracLocZip", "invalid length: expected=[1,15] actual=16"));
  }

  @Test
  public void testBadStmtCovFromCymd() {
    assertClaimTransformationError(
        () -> claimBuilder.setStmtCovFromCymd("not-a-date"),
        new DataTransformer.ErrorMessage("stmtCovFromDate", "invalid date"));
  }

  @Test
  public void testBadStmtCovToCymd() {
    assertClaimTransformationError(
        () -> claimBuilder.setStmtCovToCymd("not-a-date"),
        new DataTransformer.ErrorMessage("stmtCovToDate", "invalid date"));
  }

  @Test
  public void testBadLobCd() {
    assertClaimTransformationError(
        () -> claimBuilder.setLobCdUnrecognized("12"),
        new DataTransformer.ErrorMessage("lobCd", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadServTypCd() {
    assertClaimTransformationError(
        () -> claimBuilder.setServTypCdUnrecognized("12"),
        new DataTransformer.ErrorMessage("servTypeCd", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadFreqCd() {
    assertClaimTransformationError(
        () -> claimBuilder.setFreqCdUnrecognized("12345"),
        new DataTransformer.ErrorMessage("freqCd", "invalid length: expected=[1,1] actual=5"));
  }

  @Test
  public void testBadBillTypCd() {
    assertClaimTransformationError(
        () -> claimBuilder.setBillTypCd("1234"),
        new DataTransformer.ErrorMessage("billTypCd", "invalid length: expected=[1,3] actual=4"));
  }

  @Test
  public void testFissClaimRejectCd() {
    new ClaimFieldTester()
        .stringField(FissClaim.Builder::setRejectCd, PreAdjFissClaim::getRejectCd, "rejectCd", 5);
  }

  @Test
  public void testFissClaimFullPartDenInd() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setFullPartDenInd,
            PreAdjFissClaim::getFullPartDenInd,
            "fullPartDenInd",
            1);
  }

  @Test
  public void testFissClaimNonPayInd() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setNonPayInd, PreAdjFissClaim::getNonPayInd, "nonPayInd", 2);
  }

  @Test
  public void testFissClaimXrefDcnNbr() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setXrefDcnNbr, PreAdjFissClaim::getXrefDcnNbr, "xrefDcnNbr", 23);
  }

  @Test
  public void testFissClaimAdjReqCd() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setAdjReqCdEnum,
            PreAdjFissClaim::getAdjReqCd,
            FissAdjustmentRequestorCode.ADJUSTMENT_REQUESTOR_CODE_FISCAL_INTERMEDIARY,
            "F")
        .stringField(
            FissClaim.Builder::setAdjReqCdUnrecognized,
            PreAdjFissClaim::getAdjReqCd,
            "adjReqCd",
            1);
  }

  @Test
  public void testFissClaimAdjReasCd() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setAdjReasCd, PreAdjFissClaim::getAdjReasCd, "adjReasCd", 2);
  }

  @Test
  public void testFissClaimCancelXrefDcn() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setCancelXrefDcn,
            PreAdjFissClaim::getCancelXrefDcn,
            "cancelXrefDcn",
            23);
  }

  @Test
  public void testFissClaimCancelDate() {
    new ClaimFieldTester()
        .dateField(
            FissClaim.Builder::setCancelDateCymd, PreAdjFissClaim::getCancelDate, "cancelDate");
  }

  @Test
  public void testFissClaimCancAdjCd() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setCancAdjCdEnum,
            PreAdjFissClaim::getCancAdjCd,
            FissCancelAdjustmentCode.CANCEL_ADJUSTMENT_CODE_COVERAGE,
            "C")
        .stringField(
            FissClaim.Builder::setCancAdjCdUnrecognized,
            PreAdjFissClaim::getCancAdjCd,
            "cancAdjCd",
            1);
  }

  @Test
  public void testFissClaimOriginalXrefDcn() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOriginalXrefDcn,
            PreAdjFissClaim::getOriginalXrefDcn,
            "originalXrefDcn",
            23);
  }

  @Test
  public void testFissClaimPaidDt() {
    new ClaimFieldTester()
        .dateField(FissClaim.Builder::setPaidDtCymd, PreAdjFissClaim::getPaidDt, "paidDt");
  }

  @Test
  public void testFissClaimAdmDate() {
    new ClaimFieldTester()
        .dateField(FissClaim.Builder::setAdmDateCymd, PreAdjFissClaim::getAdmDate, "admDate");
  }

  @Test
  public void testFissClaimAdmSource() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setAdmSourceEnum,
            PreAdjFissClaim::getAdmSource,
            FissSourceOfAdmission.SOURCE_OF_ADMISSION_CLINIC_REFERRAL,
            "2")
        .stringField(
            FissClaim.Builder::setAdmSourceUnrecognized,
            PreAdjFissClaim::getAdmSource,
            "admSource",
            1);
  }

  @Test
  public void testFissClaimPrimaryPayerCode() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setPrimaryPayerCodeEnum,
            PreAdjFissClaim::getPrimaryPayerCode,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .stringField(
            FissClaim.Builder::setPrimaryPayerCodeUnrecognized,
            PreAdjFissClaim::getPrimaryPayerCode,
            "primaryPayerCode",
            1);
  }

  @Test
  public void testFissClaimAttendPhysId() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setAttendPhysId,
            PreAdjFissClaim::getAttendPhysId,
            "attendPhysId",
            16);
  }

  @Test
  public void testFissClaimAttendPhysLname() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setAttendPhysLname,
            PreAdjFissClaim::getAttendPhysLname,
            "attendPhysLname",
            17);
  }

  @Test
  public void testFissClaimAttendPhysFname() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setAttendPhysFname,
            PreAdjFissClaim::getAttendPhysFname,
            "attendPhysFname",
            18);
  }

  @Test
  public void testFissClaimAttendPhysMint() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setAttendPhysMint,
            PreAdjFissClaim::getAttendPhysMint,
            "attendPhysMint",
            1);
  }

  @Test
  public void testFissClaimAttendPhysFlag() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setAttendPhysFlagEnum,
            PreAdjFissClaim::getAttendPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .stringField(
            FissClaim.Builder::setAttendPhysFlagUnrecognized,
            PreAdjFissClaim::getAttendPhysFlag,
            "attendPhysFlag",
            1);
  }

  @Test
  public void testFissClaimOperatingPhysId() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOperatingPhysId,
            PreAdjFissClaim::getOperatingPhysId,
            "operatingPhysId",
            16);
  }

  @Test
  public void testFissClaimOperPhysLname() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOperPhysLname,
            PreAdjFissClaim::getOperPhysLname,
            "operPhysLname",
            17);
  }

  @Test
  public void testFissClaimOperPhysFname() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOperPhysFname,
            PreAdjFissClaim::getOperPhysFname,
            "operPhysFname",
            18);
  }

  @Test
  public void testFissClaimOperPhysMint() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOperPhysMint,
            PreAdjFissClaim::getOperPhysMint,
            "operPhysMint",
            1);
  }

  @Test
  public void testFissClaimOperPhysFlag() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setOperPhysFlagEnum,
            PreAdjFissClaim::getOperPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .stringField(
            FissClaim.Builder::setOperPhysFlagUnrecognized,
            PreAdjFissClaim::getOperPhysFlag,
            "operPhysFlag",
            1);
  }

  @Test
  public void testFissClaimOthPhysId() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOthPhysId, PreAdjFissClaim::getOthPhysId, "othPhysId", 16);
  }

  @Test
  public void testFissClaimOthPhysLname() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOthPhysLname,
            PreAdjFissClaim::getOthPhysLname,
            "othPhysLname",
            17);
  }

  @Test
  public void testFissClaimOthPhysFname() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOthPhysFname,
            PreAdjFissClaim::getOthPhysFname,
            "othPhysFname",
            18);
  }

  @Test
  public void testFissClaimOthPhysMint() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setOthPhysMint, PreAdjFissClaim::getOthPhysMint, "othPhysMint", 1);
  }

  @Test
  public void testFissClaimOthPhysFlag() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setOthPhysFlagEnum,
            PreAdjFissClaim::getOthPhysFlag,
            FissPhysicianFlag.PHYSICIAN_FLAG_NO,
            "N")
        .stringField(
            FissClaim.Builder::setOthPhysFlagUnrecognized,
            PreAdjFissClaim::getOthPhysFlag,
            "othPhysFlag",
            1);
  }

  @Test
  public void testFissClaimXrefHicNbr() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setXrefHicNbr, PreAdjFissClaim::getXrefHicNbr, "xrefHicNbr", 12);
  }

  @Test
  public void testFissClaimProcNewHicInd() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setProcNewHicIndEnum,
            PreAdjFissClaim::getProcNewHicInd,
            FissProcessNewHealthInsuranceClaimNumberIndicator.PROCESS_NEW_HIC_INDICATOR_Y,
            "Y")
        .stringField(
            FissClaim.Builder::setProcNewHicIndUnrecognized,
            PreAdjFissClaim::getProcNewHicInd,
            "procNewHicInd",
            1);
  }

  @Test
  public void testFissClaimNewHic() {
    new ClaimFieldTester()
        .stringField(FissClaim.Builder::setNewHic, PreAdjFissClaim::getNewHic, "newHic", 12);
  }

  @Test
  public void testFissClaimReposInd() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setReposIndEnum,
            PreAdjFissClaim::getReposInd,
            FissRepositoryIndicator.REPOSITORY_INDICATOR_HIC_HAS_BEEN_MOVED,
            "Y")
        .stringField(
            FissClaim.Builder::setReposIndUnrecognized,
            PreAdjFissClaim::getReposInd,
            "reposInd",
            1);
  }

  @Test
  public void testFissClaimReposHic() {
    new ClaimFieldTester()
        .stringField(FissClaim.Builder::setReposHic, PreAdjFissClaim::getReposHic, "reposHic", 12);
  }

  @Test
  public void testFissClaimMbiSubmBeneInd() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setMbiSubmBeneIndEnum,
            PreAdjFissClaim::getMbiSubmBeneInd,
            FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier.FISS_HIC_OR_MBI_IS_HIC,
            "H")
        .stringField(
            FissClaim.Builder::setMbiSubmBeneIndUnrecognized,
            PreAdjFissClaim::getMbiSubmBeneInd,
            "mbiSubmBeneInd",
            1);
  }

  @Test
  public void testFissClaimAdjMbiInd() {
    new ClaimFieldTester()
        .enumField(
            FissClaim.Builder::setAdjMbiIndEnum,
            PreAdjFissClaim::getAdjMbiInd,
            FissAdjustmentMedicareBeneficiaryIdentifierIndicator
                .ADJUSTMENT_MBI_INDICATOR_HIC_SUBMITTED_ON_ADJUSTMENT_OR_CANCEL_CLAIM,
            "H")
        .stringField(
            FissClaim.Builder::setAdjMbiIndUnrecognized,
            PreAdjFissClaim::getAdjMbiInd,
            "adjMbiInd",
            1);
  }

  @Test
  public void testFissClaimAdjMbi() {
    new ClaimFieldTester()
        .stringField(FissClaim.Builder::setAdjMbi, PreAdjFissClaim::getAdjMbi, "adjMbi", 11);
  }

  @Test
  public void testFissClaimMedicalRecordNo() {
    new ClaimFieldTester()
        .stringField(
            FissClaim.Builder::setMedicalRecordNo,
            PreAdjFissClaim::getMedicalRecordNo,
            "medicalRecordNo",
            17);
  }

  // endregion Claim tests
  // region ProcCode tests

  @Test
  public void testBadProcCodeProcCd() {
    assertProcCodeTransformationError(
        codeBuilder -> codeBuilder.setProcCd("12345678901"),
        new DataTransformer.ErrorMessage(
            "procCode-0-procCode", "invalid length: expected=[1,10] actual=11"));
  }

  @Test
  public void testBadProcCodeProcFlag() {
    assertProcCodeTransformationError(
        codeBuilder -> codeBuilder.setProcFlag("12345"),
        new DataTransformer.ErrorMessage(
            "procCode-0-procFlag", "invalid length: expected=[1,4] actual=5"));
  }

  @Test
  public void testBadProcCodeProcDt() {
    assertProcCodeTransformationError(
        codeBuilder -> codeBuilder.setProcDt("not-a-date"),
        new DataTransformer.ErrorMessage("procCode-0-procDate", "invalid date"));
  }

  // endregion ProcCode tests
  // region BeneZPayer tests

  @Test
  public void testBadBeneZPayerPayersId() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setPayersIdUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-payersId", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadBeneZPayerPayersName() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setPayersName("123456789012345678901234567890123"),
        new DataTransformer.ErrorMessage(
            "payer-0-payersName", "invalid length: expected=[1,32] actual=33"));
  }

  @Test
  public void testBadBeneZPayerRelInd() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setRelIndUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-relInd", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadBeneZPayerAssignInd() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setAssignIndUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-assignInd", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadBeneZPayerProviderNumber() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setProviderNumber("12345678901234"),
        new DataTransformer.ErrorMessage(
            "payer-0-providerNumber", "invalid length: expected=[1,13] actual=14"));
  }

  @Test
  public void testBadBeneZPayerAdjDcnIcn() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setAdjDcnIcn("123456789012345678901234"),
        new DataTransformer.ErrorMessage(
            "payer-0-adjDcnIcn", "invalid length: expected=[1,23] actual=24"));
  }

  @Test
  public void testBadBeneZPayerPriorPmt() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setPriorPmt("not-a-number"),
        new DataTransformer.ErrorMessage("payer-0-priorPmt", "invalid amount"));
  }

  @Test
  public void testBadBeneZPayerEstAmtDue() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setEstAmtDue("not-a-number"),
        new DataTransformer.ErrorMessage("payer-0-estAmtDue", "invalid amount"));
  }

  @Test
  public void testBadBeneZPayerBeneRel() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setBeneRelUnrecognized("123"),
        new DataTransformer.ErrorMessage(
            "payer-0-beneRel", "invalid length: expected=[1,2] actual=3"));
  }

  @Test
  public void testBadBeneZPayerBeneLastName() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setBeneLastName("1234567890123456"),
        new DataTransformer.ErrorMessage(
            "payer-0-beneLastName", "invalid length: expected=[1,15] actual=16"));
  }

  @Test
  public void testBadBeneZPayerBeneFirstName() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setBeneFirstName("12345678901"),
        new DataTransformer.ErrorMessage(
            "payer-0-beneFirstName", "invalid length: expected=[1,10] actual=11"));
  }

  @Test
  public void testBadBeneZPayerBeneMidInit() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setBeneMidInit("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-beneMidInit", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadBeneZPayerBeneSsnHic() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setBeneSsnHic("12345678901234567890"),
        new DataTransformer.ErrorMessage(
            "payer-0-beneSsnHic", "invalid length: expected=[1,19] actual=20"));
  }

  @Test
  public void testBadBeneZPayerInsuredGroupName() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredGroupName("123456789012345678"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredGroupName", "invalid length: expected=[1,17] actual=18"));
  }

  @Test
  public void testBadBeneZPayerBeneDob() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setBeneDob("not-a-date"),
        new DataTransformer.ErrorMessage("payer-0-beneDob", "invalid date"));
  }

  @Test
  public void testBadBeneZPayerBeneSex() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setBeneSexUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-beneSex", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadBeneZPayerTreatAuthCd() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setTreatAuthCd("1234567890123456789"),
        new DataTransformer.ErrorMessage(
            "payer-0-treatAuthCd", "invalid length: expected=[1,18] actual=19"));
  }

  @Test
  public void testBadBeneZPayerInsuredSex() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredSexUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredSex", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadBeneZPayerInsuredRelX12() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredRelX12Unrecognized("123"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredRelX12", "invalid length: expected=[1,2] actual=3"));
  }

  // endregion BeneZPayer tests
  // region InsuredPayer tests

  @Test
  public void testBadInsuredPayerPayersId() {
    new InsuredPayerFieldTester()
        .enumField(
            FissInsuredPayer.Builder::setPayersIdEnum,
            PreAdjFissPayer::getPayersId,
            FissPayersCode.PAYERS_CODE_AUTO_NO_FAULT,
            "D")
        .stringField(
            FissInsuredPayer.Builder::setPayersIdUnrecognized,
            PreAdjFissPayer::getPayersId,
            "payersId",
            1);
  }

  @Test
  public void testBadInsuredPayerPayersName() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setPayersName,
            PreAdjFissPayer::getPayersName,
            "payersName",
            32);
  }

  @Test
  public void testBadInsuredPayerRelInd() {
    new InsuredPayerFieldTester()
        .enumField(
            FissInsuredPayer.Builder::setRelIndEnum,
            PreAdjFissPayer::getRelInd,
            FissReleaseOfInformation.RELEASE_OF_INFORMATION_NO_RELEASE_ON_FILE,
            "N")
        .stringField(
            FissInsuredPayer.Builder::setRelIndUnrecognized,
            PreAdjFissPayer::getRelInd,
            "relInd",
            1);
  }

  @Test
  public void testBadInsuredPayerAssignInd() {
    new InsuredPayerFieldTester()
        .enumField(
            FissInsuredPayer.Builder::setAssignIndEnum,
            PreAdjFissPayer::getAssignInd,
            FissAssignmentOfBenefitsIndicator.ASSIGNMENT_OF_BENEFITS_INDICATOR_BENEFITS_ASSIGNED,
            "Y")
        .stringField(
            FissInsuredPayer.Builder::setAssignIndUnrecognized,
            PreAdjFissPayer::getAssignInd,
            "assignInd",
            1);
  }

  @Test
  public void testBadInsuredPayerProviderNumber() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setProviderNumber,
            PreAdjFissPayer::getProviderNumber,
            "providerNumber",
            13);
  }

  @Test
  public void testBadInsuredPayerAdjDcnIcn() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setAdjDcnIcn, PreAdjFissPayer::getAdjDcnIcn, "adjDcnIcn", 23);
  }

  @Test
  public void testBadInsuredPayerPriorPmt() {
    new InsuredPayerFieldTester()
        .amountField(
            FissInsuredPayer.Builder::setPriorPmt, PreAdjFissPayer::getPriorPmt, "priorPmt");
  }

  @Test
  public void testBadInsuredPayerEstAmtDue() {
    new InsuredPayerFieldTester()
        .amountField(
            FissInsuredPayer.Builder::setEstAmtDue, PreAdjFissPayer::getEstAmtDue, "estAmtDue");
  }

  @Test
  public void testBadInsuredPayerInsuredRel() {
    new InsuredPayerFieldTester()
        .enumField(
            FissInsuredPayer.Builder::setInsuredRelEnum,
            PreAdjFissPayer::getInsuredRel,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_EMPLOYEE,
            "08")
        .stringField(
            FissInsuredPayer.Builder::setInsuredRelUnrecognized,
            PreAdjFissPayer::getInsuredRel,
            "insuredRel",
            2);
  }

  @Test
  public void testBadInsuredPayerInsuredName() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setInsuredName,
            PreAdjFissPayer::getInsuredName,
            "insuredName",
            25);
  }

  @Test
  public void testBadInsuredPayerInsuredSsnHic() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setInsuredSsnHic,
            PreAdjFissPayer::getInsuredSsnHic,
            "insuredSsnHic",
            19);
  }

  @Test
  public void testBadInsuredPayerInsuredGroupName() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setInsuredGroupName,
            PreAdjFissPayer::getInsuredGroupName,
            "insuredGroupName",
            17);
  }

  @Test
  public void testBadInsuredPayerInsuredGroupNbr() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setInsuredGroupNbr,
            PreAdjFissPayer::getInsuredGroupNbr,
            "insuredGroupNbr",
            20);
  }

  @Test
  public void testBadInsuredPayerTreatAuthCd() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setTreatAuthCd,
            PreAdjFissPayer::getTreatAuthCd,
            "treatAuthCd",
            18);
  }

  @Test
  public void testBadInsuredPayerInsuredSex() {
    new InsuredPayerFieldTester()
        .enumField(
            FissInsuredPayer.Builder::setInsuredSexEnum,
            PreAdjFissPayer::getInsuredSex,
            FissBeneficiarySex.BENEFICIARY_SEX_UNKNOWN,
            "U")
        .stringField(
            FissInsuredPayer.Builder::setInsuredSexUnrecognized,
            PreAdjFissPayer::getInsuredSex,
            "insuredSex",
            1);
  }

  @Test
  public void testBadInsuredPayerInsuredRelX12() {
    new InsuredPayerFieldTester()
        .enumField(
            FissInsuredPayer.Builder::setInsuredRelX12Enum,
            PreAdjFissPayer::getInsuredRelX12,
            FissPatientRelationshipCode.PATIENT_RELATIONSHIP_CODE_DEFAULT,
            "00")
        .stringField(
            FissInsuredPayer.Builder::setInsuredRelX12Unrecognized,
            PreAdjFissPayer::getInsuredRelX12,
            "insuredRelX12",
            2);
  }

  @Test
  public void testBadInsuredPayerInsuredDob() {
    new InsuredPayerFieldTester()
        .dateField(
            FissInsuredPayer.Builder::setInsuredDob, PreAdjFissPayer::getInsuredDob, "insuredDob");
  }

  @Test
  public void testBadInsuredPayerInsuredDobText() {
    new InsuredPayerFieldTester()
        .stringField(
            FissInsuredPayer.Builder::setInsuredDobText,
            PreAdjFissPayer::getInsuredDobText,
            "insuredDobText",
            9);
  }

  // endregion InsuredPayer tests
  // region AuditTrail tests

  @Test
  public void testFissAuditTrailBadtStatus() {
    new AuditTrailFieldTester()
        .enumField(
            FissAuditTrail.Builder::setBadtStatusEnum,
            PreAdjFissAuditTrail::getBadtStatus,
            FissClaimStatus.CLAIM_STATUS_BLANK,
            " ");
  }

  @Test
  public void testFissAudiTrailBadtLoc() {
    new AuditTrailFieldTester()
        .stringField(
            FissAuditTrail.Builder::setBadtLoc, PreAdjFissAuditTrail::getBadtLoc, "badtLoc", 5);
  }

  @Test
  public void testFissAudiTrailBadtOperId() {
    new AuditTrailFieldTester()
        .stringField(
            FissAuditTrail.Builder::setBadtOperId,
            PreAdjFissAuditTrail::getBadtOperId,
            "badtOperId",
            9);
  }

  @Test
  public void testFissAudiTrailBadtReas() {
    new AuditTrailFieldTester()
        .stringField(
            FissAuditTrail.Builder::setBadtReas, PreAdjFissAuditTrail::getBadtReas, "badtReas", 5);
  }

  @Test
  public void testFissAudiTrailBadtCurrDate() {
    new AuditTrailFieldTester()
        .dateField(
            FissAuditTrail.Builder::setBadtCurrDateCymd,
            PreAdjFissAuditTrail::getBadtCurrDate,
            "badtCurrDate");
  }

  // endregion AuditTrail tests

  private void assertClaimTransformationError(
      Runnable claimUpdate, DataTransformer.ErrorMessage... expectedErrors) {
    try {
      // these required fields must always be present - they can be changed by test lambda
      claimBuilder
          .setDcn("dcn")
          .setHicNo("hicn")
          .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP)
          .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
          .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_CABLE);
      claimUpdate.run();
      changeBuilder
          .setSeq(MIN_SEQUENCE_NUM)
          .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
          .setClaim(claimBuilder.build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(ImmutableList.copyOf(expectedErrors), ex.getErrors());
    }
  }

  private void assertProcCodeTransformationError(
      Consumer<FissProcedureCode.Builder> updater, DataTransformer.ErrorMessage... expectedErrors) {
    assertClaimTransformationError(
        () -> {
          final FissProcedureCode.Builder codeBuilder = FissProcedureCode.newBuilder();
          // these required field must always be present - it can be changed by test lambda
          codeBuilder.setProcCd("1234567890");
          updater.accept(codeBuilder);
          claimBuilder.addFissProcCodes(codeBuilder.build());
        },
        expectedErrors);
  }

  private void assertBeneZPayerTransformationError(
      Consumer<FissBeneZPayer.Builder> updater, DataTransformer.ErrorMessage... expectedErrors) {
    assertClaimTransformationError(
        () -> {
          final FissBeneZPayer.Builder codeBuilder = FissBeneZPayer.newBuilder();
          updater.accept(codeBuilder);
          claimBuilder.addFissPayers(FissPayer.newBuilder().setBeneZPayer(codeBuilder.build()));
        },
        expectedErrors);
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
      changeBuilder.setChangeType(ChangeType.CHANGE_TYPE_UPDATE).setClaim(claimBuilder.build());
      transformer.transformClaim(changeBuilder.build());
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals(
          ImmutableList.of(
              new DataTransformer.ErrorMessage("currStatus", "unsupported enum value")),
          ex.getErrors());
    }
  }

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

  @CanIgnoreReturnValue
  abstract class AbstractFieldTester<TBuilder, TEntity> {
    AbstractFieldTester<TBuilder, TEntity> stringField(
        BiConsumer<TBuilder, String> setter,
        Function<TEntity, String> getter,
        String fieldLabel,
        int maxLength) {
      BiConsumer<FissClaim.Builder, String> setter1 =
          (claimBuilder, value) -> setter.accept(getBuilder(claimBuilder), value);
      String fieldLabel1 = getLabel(fieldLabel);
      verifyStringFieldTransformationCorrect(
          setter1, claim1 -> getter.apply(getEntity(claim1)), maxLength);
      verifyStringFieldLengthLimitsEnforced(setter1, fieldLabel1, maxLength, 0);
      verifyStringFieldLengthLimitsEnforced(setter1, fieldLabel1, maxLength, maxLength + 1);
      return this;
    }

    @CanIgnoreReturnValue
    AbstractFieldTester<TBuilder, TEntity> dateField(
        BiConsumer<TBuilder, String> setter,
        Function<TEntity, LocalDate> getter,
        String fieldLabel) {
      BiConsumer<FissClaim.Builder, String> setter1 =
          (claimBuilder, value) -> setter.accept(getBuilder(claimBuilder), value);
      verifyFieldTransformationSucceeds(
          claimBuilder1 -> setter1.accept(claimBuilder1, "2021-12-01"),
          claim1 -> getter.apply(getEntity(claim1)),
          LocalDate.of(2021, 12, 1));
      verifyFieldTransformationFails(
          claimBuilder1 -> setter1.accept(claimBuilder1, "not-a-date"),
          getLabel(fieldLabel),
          "invalid date");
      return this;
    }

    @CanIgnoreReturnValue
    AbstractFieldTester<TBuilder, TEntity> amountField(
        BiConsumer<TBuilder, String> setter,
        Function<TEntity, BigDecimal> getter,
        String fieldLabel) {
      BiConsumer<FissClaim.Builder, String> setter1 =
          (claimBuilder, value) -> setter.accept(getBuilder(claimBuilder), value);
      verifyFieldTransformationSucceeds(
          claimBuilder1 -> setter1.accept(claimBuilder1, "1234.50"),
          claim1 -> getter.apply(getEntity(claim1)),
          new BigDecimal("1234.50"));
      verifyFieldTransformationFails(
          claimBuilder1 -> setter1.accept(claimBuilder1, "not-a-number"),
          getLabel(fieldLabel),
          "invalid amount");
      return this;
    }

    @CanIgnoreReturnValue
    <TEnum extends Enum<?>> AbstractFieldTester<TBuilder, TEntity> enumField(
        BiConsumer<TBuilder, TEnum> setter,
        Function<TEntity, String> getter,
        TEnum enumValue,
        String stringValue) {
      verifyFieldTransformationSucceeds(
          claimBuilder1 ->
              ((BiConsumer<FissClaim.Builder, TEnum>)
                      (claimBuilder2, value) -> setter.accept(getBuilder(claimBuilder2), value))
                  .accept(claimBuilder1, enumValue),
          claim1 -> getter.apply(getEntity(claim1)),
          stringValue);
      return this;
    }

    abstract TBuilder getBuilder(FissClaim.Builder claimBuilder);

    abstract TEntity getEntity(PreAdjFissClaim claim);

    abstract String getLabel(String basicLabel);

    FissClaim.Builder createFissClaimBuilderWithRequiredFields() {
      return FissClaim.newBuilder()
          .setDcn("dcn")
          .setHicNo("hicn")
          .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP)
          .setCurrLoc1Enum(FissProcessingType.PROCESSING_TYPE_MANUAL)
          .setCurrLoc2Enum(FissCurrentLocation2.CURRENT_LOCATION_2_CABLE);
    }

    RdaChange<PreAdjFissClaim> transformClaim(FissClaim claim) {
      var changeBuilder =
          FissClaimChange.newBuilder()
              .setSeq(MIN_SEQUENCE_NUM)
              .setChangeType(ChangeType.CHANGE_TYPE_INSERT)
              .setClaim(claim);
      return transformer.transformClaim(changeBuilder.build());
    }

    private void verifyStringFieldTransformationCorrect(
        BiConsumer<FissClaim.Builder, String> setter,
        Function<PreAdjFissClaim, String> getter,
        int maxLength) {
      final var value = createString(maxLength);
      verifyFieldTransformationSucceeds(
          claimBuilder -> setter.accept(claimBuilder, value), getter, value);
    }

    private void verifyStringFieldLengthLimitsEnforced(
        BiConsumer<FissClaim.Builder, String> setter,
        String fieldLabel,
        int maxLength,
        int length) {
      verifyFieldTransformationFails(
          claimBuilder -> setter.accept(claimBuilder, createString(length)),
          fieldLabel,
          String.format("invalid length: expected=[1,%d] actual=%d", maxLength, length));
    }

    private <T> void verifyFieldTransformationSucceeds(
        Consumer<FissClaim.Builder> setter, Function<PreAdjFissClaim, T> getter, T expectedValue) {
      var claimBuilder = createFissClaimBuilderWithRequiredFields();

      setter.accept(claimBuilder);

      final var change = transformClaim(claimBuilder.build());
      assertEquals(expectedValue, getter.apply(change.getClaim()));
    }

    private void verifyFieldTransformationFails(
        Consumer<FissClaim.Builder> setter, String fieldLabel, String... errorMessages) {
      try {
        var claimBuilder = createFissClaimBuilderWithRequiredFields();

        setter.accept(claimBuilder);

        transformClaim(claimBuilder.build());
        fail("should have thrown");
      } catch (DataTransformer.TransformationException ex) {
        var errors = ImmutableList.builder();
        for (String errorMessage : errorMessages) {
          errors.add(new DataTransformer.ErrorMessage(fieldLabel, errorMessage));
        }
        assertEquals(errors.build(), ex.getErrors());
      }
    }

    private String createString(int length) {
      StringBuilder sb = new StringBuilder();
      var digit = 1;
      while (sb.length() < length) {
        sb.append(digit);
        digit = (digit + 1) % 10;
      }
      return sb.toString();
    }
  }

  class ClaimFieldTester extends AbstractFieldTester<FissClaim.Builder, PreAdjFissClaim> {
    @Override
    FissClaim.Builder getBuilder(FissClaim.Builder claimBuilder) {
      return claimBuilder;
    }

    @Override
    PreAdjFissClaim getEntity(PreAdjFissClaim claim) {
      return claim;
    }

    @Override
    String getLabel(String basicLabel) {
      return basicLabel;
    }
  }

  class AuditTrailFieldTester
      extends AbstractFieldTester<FissAuditTrail.Builder, PreAdjFissAuditTrail> {
    @Override
    FissAuditTrail.Builder getBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissAuditTrailBuilderList().isEmpty()) {
        claimBuilder.addFissAuditTrailBuilder();
      }
      return claimBuilder.getFissAuditTrailBuilder(0);
    }

    @Override
    PreAdjFissAuditTrail getEntity(PreAdjFissClaim claim) {
      assertEquals(1, claim.getAuditTrail().size());
      return claim.getAuditTrail().iterator().next();
    }

    @Override
    String getLabel(String basicLabel) {
      return "auditTrail-0-" + basicLabel;
    }
  }

  class InsuredPayerFieldTester
      extends AbstractFieldTester<FissInsuredPayer.Builder, PreAdjFissPayer> {
    @Override
    FissInsuredPayer.Builder getBuilder(FissClaim.Builder claimBuilder) {
      if (claimBuilder.getFissPayersBuilderList().isEmpty()) {
        claimBuilder.addFissPayersBuilder();
      }
      return claimBuilder.getFissPayersBuilder(0).getInsuredPayerBuilder();
    }

    @Override
    PreAdjFissPayer getEntity(PreAdjFissClaim claim) {
      assertEquals(1, claim.getPayers().size());
      return claim.getPayers().iterator().next();
    }

    @Override
    String getLabel(String basicLabel) {
      return "payer-0-" + basicLabel;
    }
  }
}
