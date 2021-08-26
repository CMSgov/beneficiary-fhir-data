package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissPayer;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissAssignmentOfBenefitsIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassification;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForClinics;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForSpecialFacilities;
import gov.cms.mpsm.rda.v1.fiss.FissBillFacilityType;
import gov.cms.mpsm.rda.v1.fiss.FissBillFrequency;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissInsuredPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPatientRelationshipCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import gov.cms.mpsm.rda.v1.fiss.FissReleaseOfInformation;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.function.Consumer;
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
    code.setProcCode("code-1");
    code.setProcFlag("fl-1");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getProcCodes().add(code);
    code = new PreAdjFissProcCode();
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
        claim.getProcCodes(), transformed.getProcCodes());
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
    code.setDiagCd2("code-1");
    code.setDiagPoaInd("W");
    code.setBitFlags("1234");
    code.setLastUpdated(claim.getLastUpdated());
    claim.getDiagCodes().add(code);
    code = new PreAdjFissDiagnosisCode();
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
        claim.getDiagCodes(), transformed.getDiagCodes());
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
        claim.getPayers(), transformed.getPayers());
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
        claim.getPayers(), transformed.getPayers());
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
        new DataTransformer.ErrorMessage("priorPmt", "invalid amount"));
  }

  @Test
  public void testBadBeneZPayerEstAmtDue() {
    assertBeneZPayerTransformationError(
        codeBuilder -> codeBuilder.setEstAmtDue("not-a-number"),
        new DataTransformer.ErrorMessage("estAmtDue", "invalid amount"));
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
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setPayersIdUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-payersId", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadInsuredPayerPayersName() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setPayersName("123456789012345678901234567890123"),
        new DataTransformer.ErrorMessage(
            "payer-0-payersName", "invalid length: expected=[1,32] actual=33"));
  }

  @Test
  public void testBadInsuredPayerRelInd() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setRelIndUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-relInd", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadInsuredPayerAssignInd() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setAssignIndUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-assignInd", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadInsuredPayerProviderNumber() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setProviderNumber("12345678901234"),
        new DataTransformer.ErrorMessage(
            "payer-0-providerNumber", "invalid length: expected=[1,13] actual=14"));
  }

  @Test
  public void testBadInsuredPayerAdjDcnIcn() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setAdjDcnIcn("123456789012345678901234"),
        new DataTransformer.ErrorMessage(
            "payer-0-adjDcnIcn", "invalid length: expected=[1,23] actual=24"));
  }

  @Test
  public void testBadInsuredPayerPriorPmt() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setPriorPmt("not-a-number"),
        new DataTransformer.ErrorMessage("priorPmt", "invalid amount"));
  }

  @Test
  public void testBadInsuredPayerEstAmtDue() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setEstAmtDue("not-a-number"),
        new DataTransformer.ErrorMessage("estAmtDue", "invalid amount"));
  }

  @Test
  public void testBadInsuredPayerInsuredRel() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredRelUnrecognized("123"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredRel", "invalid length: expected=[1,2] actual=3"));
  }

  @Test
  public void testBadInsuredPayerInsuredName() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredName("12345678901234567890123456"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredName", "invalid length: expected=[1,25] actual=26"));
  }

  @Test
  public void testBadInsuredPayerInsuredSsnHic() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredSsnHic("12345678901234567890"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredSsnHic", "invalid length: expected=[1,19] actual=20"));
  }

  @Test
  public void testBadInsuredPayerInsuredGroupName() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredGroupName("123456789012345678"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredGroupName", "invalid length: expected=[1,17] actual=18"));
  }

  @Test
  public void testBadInsuredPayerInsuredGroupNbr() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredGroupNbr("123456789012345678901"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredGroupNbr", "invalid length: expected=[1,20] actual=21"));
  }

  @Test
  public void testBadInsuredPayerTreatAuthCd() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setTreatAuthCd("1234567890123456789"),
        new DataTransformer.ErrorMessage(
            "payer-0-treatAuthCd", "invalid length: expected=[1,18] actual=19"));
  }

  @Test
  public void testBadInsuredPayerInsuredSex() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredSexUnrecognized("12"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredSex", "invalid length: expected=[1,1] actual=2"));
  }

  @Test
  public void testBadInsuredPayerInsuredRelX12() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredRelX12Unrecognized("123"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredRelX12", "invalid length: expected=[1,2] actual=3"));
  }

  @Test
  public void testBadInsuredPayerInsuredDob() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredDob("not-a-date"),
        new DataTransformer.ErrorMessage("payer-0-insuredDob", "invalid date"));
  }

  @Test
  public void testBadInsuredPayerInsuredDobText() {
    assertInsuredPayerTransformationError(
        codeBuilder -> codeBuilder.setInsuredDobText("1234567890"),
        new DataTransformer.ErrorMessage(
            "payer-0-insuredDobText", "invalid length: expected=[1,9] actual=10"));
  }

  // endregion InsuredPayer tests

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

  private void assertInsuredPayerTransformationError(
      Consumer<FissInsuredPayer.Builder> updater, DataTransformer.ErrorMessage... expectedErrors) {
    assertClaimTransformationError(
        () -> {
          final FissInsuredPayer.Builder codeBuilder = FissInsuredPayer.newBuilder();
          updater.accept(codeBuilder);
          claimBuilder.addFissPayers(FissPayer.newBuilder().setInsuredPayer(codeBuilder.build()));
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
}
