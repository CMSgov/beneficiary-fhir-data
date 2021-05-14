package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.InsuranceComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.v4.providers.DMEClaimTransformerV2}. */
public final class DMEClaimTransformerV2Test {
  DMEClaim claim;
  ExplanationOfBenefit eob;
  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return
   * @throws FHIRException
   */
  public DMEClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    DMEClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(new Date());

    return claim;
  }

  @Before
  public void before() {
    claim = generateClaim();
    eob = DMEClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  @Test
  public void shouldSetID() {
    Assert.assertEquals("dme-" + claim.getClaimId(), eob.getId());
  }

  @Test
  public void shouldSetLastUpdated() {
    Assert.assertNotNull(eob.getMeta().getLastUpdated());
  }

  @Test
  public void shouldSetCorrectProfile() {
    // The base CanonicalType doesn't seem to compare correctly so lets convert it
    // to a string
    Assert.assertTrue(
        eob.getMeta().getProfile().stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_EOB_INPATIENT_PROFILE_URL)));
  }

  @Test
  public void shouldSetUse() {
    Assert.assertEquals(Use.CLAIM, eob.getUse());
  }

  @Test
  public void shouldSetFinalAction() {
    Assert.assertEquals(ExplanationOfBenefitStatus.ACTIVE, eob.getStatus());
  }

  @Test
  public void shouldSetBillablePeriod() throws Exception {
    // We just want to make sure it is set
    Assert.assertNotNull(eob.getBillablePeriod());
    Assert.assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-02-03"),
        eob.getBillablePeriod().getStart());
    Assert.assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-02-03"), eob.getBillablePeriod().getEnd());
  }

  @Test
  public void shouldReferencePatient() {
    Assert.assertNotNull(eob.getPatient());
    Assert.assertEquals("Patient/567834", eob.getPatient().getReference());
  }

  @Test
  public void shouldHaveCreatedDate() {
    Assert.assertNotNull(eob.getCreated());
  }

  /**
   * CareTeam list
   *
   * <p>Based on how the code currently works, we can assume that the same CareTeam members always
   * are added in the same order. This means we can look them up by sequence number.
   */
  @Test
  public void shouldHaveCareTeamList() {
    Assert.assertEquals(2, eob.getCareTeam().size());
  }

  /**
   * Testing all of these in one test, just because there isn't a distinct identifier really for
   * each
   */
  @Test
  public void shouldHaveCareTeamMembers() {
    // First member
    CareTeamComponent member1 = TransformerTestUtilsV2.findCareTeamBySequence(1, eob.getCareTeam());
    Assert.assertEquals("1306849450", member1.getProvider().getIdentifier().getValue());

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());
    Assert.assertEquals("1244444444", member2.getProvider().getIdentifier().getValue());
  }

  /** SupportingInfo items */
  @Test
  public void shouldHaveSupportingInfoList() {
    Assert.assertEquals(2, eob.getSupportingInfo().size());
  }

  @Test
  public void shouldHaveClaimReceivedDateSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("clmrecvddate", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
                // We don't care what the sequence number is here
                sic.getSequence(),
                // Category
                Arrays.asList(
                    new Coding(
                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                        "clmrecvddate",
                        "Claim Received Date"),
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/information",
                        "https://bluebutton.cms.gov/resources/variables/nch_wkly_proc_dt",
                        "NCH Weekly Claim Processing Date")))
            // timingDate
            .setTiming(new DateType("2014-02-14"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveLineHctHgbRsltNumSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/line_hct_hgb_rslt_num",
            eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://terminology.hl7.org/CodeSystem/claiminformationcategory",
                    "info",
                    "Information"),
                new Coding(
                    "https://bluebutton.cms.gov/resources/codesystem/information",
                    "https://bluebutton.cms.gov/resources/variables/line_hct_hgb_rslt_num",
                    "Hematocrit / Hemoglobin Test Results")));

    compare.setValue(new Reference("#line-observation-1"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  /** Diagnosis elements */
  @Test
  public void shouldHaveDiagnosesList() {
    Assert.assertEquals(3, eob.getDiagnosis().size());
  }

  @Test
  public void shouldHaveDiagnosesMembers() {
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("R5555", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "R5555", null),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                "principal",
                "Principal Diagnosis"),
            null,
            null);

    Assert.assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("R6666", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "R6666", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            null,
            null);

    Assert.assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("G6666", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "G6666", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    Assert.assertTrue(cmp3.equalsDeep(diag3));
  }

  /** Insurance */
  @Test
  public void shouldReferenceCoverageInInsurance() {
    // Only one insurance object
    Assert.assertEquals(1, eob.getInsurance().size());

    InsuranceComponent insurance = eob.getInsuranceFirstRep();

    InsuranceComponent compare =
        new InsuranceComponent()
            .setCoverage(new Reference().setReference("Coverage/part-a-567834"));

    Assert.assertTrue(compare.equalsDeep(insurance));
  }

  /** Line Items */
  @Test
  public void shouldHaveLineItems() {
    Assert.assertEquals(1, eob.getItem().size());
  }

  @Test
  public void shouldHaveLineItemSequence() {
    Assert.assertEquals(1, eob.getItemFirstRep().getSequence());
  }

  @Test
  public void shouldHaveLineItemCareTeamRef() {
    // The order isn't important but this should reference a care team member
    Assert.assertNotNull(eob.getItemFirstRep().getCareTeamSequence());
    Assert.assertEquals(1, eob.getItemFirstRep().getCareTeamSequence().size());
  }

  @Test
  public void shouldHaveLineItemDiagnosisRef() {
    Assert.assertNotNull(eob.getItemFirstRep().getDiagnosisSequence());
    Assert.assertEquals(1, eob.getItemFirstRep().getDiagnosisSequence().size());
  }

  @Test
  public void shouldHaveLineItemInformationRef() {
    Assert.assertNotNull(eob.getItemFirstRep().getInformationSequence());
    Assert.assertEquals(1, eob.getItemFirstRep().getInformationSequence().size());
  }

  @Test
  public void shouldHaveLineItemCategory() {
    CodeableConcept category = eob.getItemFirstRep().getCategory();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/line_cms_type_srvc_cd",
                        "P",
                        "Lump sum purchase of DME, prosthetics orthotics")));

    Assert.assertTrue(compare.equalsDeep(category));
  }

  @Test
  public void shouldHaveLineItemProductOrServiceExtension() {
    Assert.assertNotNull(eob.getItemFirstRep().getProductOrService());
    Assert.assertEquals(1, eob.getItemFirstRep().getProductOrService().getExtension().size());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "http://hl7.org/fhir/sid/ndc",
            eob.getItemFirstRep().getProductOrService().getExtension());

    Extension compare =
        new Extension(
            "http://hl7.org/fhir/sid/ndc",
            new Coding(
                "http://hl7.org/fhir/sid/ndc",
                "667159747",
                "TYLENOL EXTRA STRENGTH - ACETAMINOPHEN"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  /**
   * Serializes the EOB and prints to the command line
   *
   * @throws FHIRException
   */
  @Ignore
  @Test
  public void serializeSampleARecord() throws FHIRException {
    ExplanationOfBenefit eob =
        DMEClaimTransformerV2.transform(new MetricRegistry(), generateClaim(), Optional.of(false));
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link DMEClaim}.
   *
   * @param claim the {@link DMEClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     DMEClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(DMEClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match

    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimTypeV2.DME,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // TODO - fix the following tests for V2
    /*

    // Test to ensure common group fields between Carrier and DME match
    TransformerTestUtilsV2.assertEobCommonGroupCarrierDMEEquals(
        eob,
        claim.getBeneficiaryId(),
        claim.getCarrierNumber(),
        claim.getClinicalTrialNumber(),
        claim.getBeneficiaryPartBDeductAmount(),
        claim.getPaymentDenialCode(),
        claim.getReferringPhysicianNpi(),
        Optional.of(claim.getProviderAssignmentIndicator()),
        claim.getProviderPaymentAmount(),
        claim.getBeneficiaryPaymentAmount(),
        claim.getSubmittedChargeAmount(),
        claim.getAllowedChargeAmount());

    TransformerTestUtilsV2.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.PRPAYAMT, claim.getPrimaryPayerPaidAmount(), eob);

    // Assert.assertEquals(1, eob.getItem().size());
    // ItemComponent eobItem0 = eob.getItem().get(0);
    // DMEClaimLine claimLine1 = claim.getLines().get(0);
    // Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

    TransformerTestUtilsV2.assertExtensionIdentifierEquals(
        CcwCodebookVariable.SUPLRNUM, claimLine1.getProviderBillingNumber(), eobItem0);

    // TransformerTestUtilsV2.assertCareTeamEquals(
    //    claimLine1.getProviderNPI().get(), ClaimCareteamrole.PRIMARY, eob);

    CareTeamComponent performingCareTeamEntry =
        TransformerTestUtilsV2.findCareTeamEntryForProviderIdentifier(
            claimLine1.getProviderNPI().get(), eob.getCareTeam());
    TransformerTestUtilsV2.assertHasCoding(
        CcwCodebookVariable.PRVDR_SPCLTY,
        claimLine1.getProviderSpecialityCode(),
        performingCareTeamEntry.getQualification());
    TransformerTestUtilsV2.assertExtensionCodingEquals(
        CcwCodebookVariable.PRTCPTNG_IND_CD,
        claimLine1.getProviderParticipatingIndCode(),
        performingCareTeamEntry);

    TransformerTestUtilsV2.assertExtensionCodingEquals(
        CcwCodebookVariable.PRVDR_STATE_CD,
        claimLine1.getProviderStateCode(),
        eobItem0.getLocation());

    TransformerTestUtilsV2.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        claimLine1.getHcpcsInitialModifierCode(),
        claimLine1.getHcpcsSecondModifierCode(),
        claim.getHcpcsYearCode(),
        0);     // TODO - replace w/index as needed

        TransformerTestUtilsV2.assertHasCoding(
            TransformerConstants.CODING_SYSTEM_HCPCS,
            "" + claim.getHcpcsYearCode().get(),
            null,
            claimLine1.getHcpcsCode().get(),
            eobItem0.getService().getCoding());

        TransformerTestUtilsV2.assertAdjudicationAmountEquals(
            CcwCodebookVariable.LINE_PRMRY_ALOWD_CHRG_AMT,
            claimLine1.getPrimaryPayerAllowedChargeAmount(),
            eobItem0.getAdjudication());

        TransformerTestUtilsV2.assertAdjudicationAmountEquals(
            CcwCodebookVariable.LINE_DME_PRCHS_PRICE_AMT,
            claimLine1.getPurchasePriceAmount(),
            eobItem0.getAdjudication());

        TransformerTestUtilsV2.assertExtensionCodingEquals(
            CcwCodebookVariable.DMERC_LINE_PRCNG_STATE_CD,
            claimLine1.getPricingStateCode(),
            eobItem0.getLocation());

        TransformerTestUtilsV2.assertExtensionCodingEquals(
            CcwCodebookVariable.DMERC_LINE_SUPPLR_TYPE_CD,
            claimLine1.getSupplierTypeCode(),
            eobItem0.getLocation());

        TransformerTestUtilsV2.assertExtensionQuantityEquals(
            CcwCodebookVariable.DMERC_LINE_SCRN_SVGS_AMT,
            claimLine1.getScreenSavingsAmount(),
            eobItem0);

        TransformerTestUtilsV2.assertQuantityUnitInfoEquals(
            CcwCodebookVariable.DMERC_LINE_MTUS_CNT,
            CcwCodebookVariable.DMERC_LINE_MTUS_CD,
            claimLine1.getMtusCode(),
            eobItem0);

        TransformerTestUtilsV2.assertExtensionQuantityEquals(
            CcwCodebookVariable.DMERC_LINE_MTUS_CNT, claimLine1.getMtusCount(), eobItem0);

        TransformerTestUtilsV2.assertExtensionCodingEquals(
            eobItem0,
            TransformerConstants.CODING_NDC,
            TransformerConstants.CODING_NDC,
            claimLine1.getNationalDrugCode().get());

        // verify {@link
        // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
        // method worked as expected for this claim type
        TransformerTestUtilsV2.assertMapEobType(
            eob.getType(),
            ClaimType.DME,
            // FUTURE there currently is not an equivalent CODING_FHIR_CLAIM_TYPE mapping
            // for this claim type. If added then the Optional empty parameter below should
            // be updated to match expected result.
            Optional.empty(),
            Optional.of(claim.getNearLineRecordIdCode()),
            Optional.of(claim.getClaimTypeCode()));

    // Test to ensure common item fields between Carrier and DME match
    TransformerTestUtilsV2.assertEobCommonItemCarrierDMEEquals(
        eobItem0,
        eob,
        claimLine1.getServiceCount(),
        claimLine1.getPlaceOfServiceCode(),
        claimLine1.getFirstExpenseDate(),
        claimLine1.getLastExpenseDate(),
        claimLine1.getBeneficiaryPaymentAmount(),
        claimLine1.getProviderPaymentAmount(),
        claimLine1.getBeneficiaryPartBDeductAmount(),
        claimLine1.getPrimaryPayerCode(),
        claimLine1.getPrimaryPayerPaidAmount(),
        claimLine1.getBetosCode(),
        claimLine1.getPaymentAmount(),
        claimLine1.getPaymentCode(),
        claimLine1.getCoinsuranceAmount(),
        claimLine1.getSubmittedChargeAmount(),
        claimLine1.getAllowedChargeAmount(),
        claimLine1.getProcessingIndicatorCode(),
        claimLine1.getServiceDeductibleCode(),
        claimLine1.getDiagnosisCode(),
        claimLine1.getDiagnosisCodeVersion(),
        claimLine1.getHctHgbTestTypeCode(),
        claimLine1.getHctHgbTestResult(),
        claimLine1.getCmsServiceTypeCode(),
        claimLine1.getNationalDrugCode());
    */

    // Test lastUpdated
    TransformerTestUtilsV2.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
