package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CarrierClaimTransformerV2Test {
  static CarrierClaim claim;
  ExplanationOfBenefit eob;
  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return claim object
   * @throws FHIRException
   */
  public CarrierClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());

    return claim;
  }

  @Before
  public void before() {
    claim = generateClaim();
    ExplanationOfBenefit genEob =
        CarrierClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.of(false));
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.CarrierClaimTransformerV2#transform(MetricRegistry, Object,
   * Optional<Boolean>)} works as expected when run against the {@link
   * StaticRifResource#SAMPLE_A_INPATIENT} {@link InpatientClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    assertMatches(
        claim,
        CarrierClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.of(false)),
        false);
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  /**
   * Serializes the EOB and prints to the command line
   *
   * @throws FHIRException
   */
  @Ignore
  @Test
  public void serializeSampleARecord() throws FHIRException {
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  @Test
  public void shouldSetBillablePeriod() throws Exception {
    // We just want to make sure it is set
    Assert.assertNotNull(eob.getBillablePeriod());
    Assert.assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("1999-10-27"),
        eob.getBillablePeriod().getStart());
    Assert.assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("1999-10-27"), eob.getBillablePeriod().getEnd());
  }

  @Test
  public void shouldHaveIdentifiers() {
    Assert.assertEquals(2, eob.getIdentifier().size());
    Identifier clmGrp1 =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "https://bluebutton.cms.gov/resources/variables/clm_id", eob.getIdentifier());

    Identifier compare1 =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/variables/clm_id",
            "9991831999",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
            "uc",
            "Unique Claim ID");

    Assert.assertTrue(compare1.equalsDeep(clmGrp1));

    Identifier clmGrp2 =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "https://bluebutton.cms.gov/resources/identifier/claim-group", eob.getIdentifier());

    Identifier compare2 =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/identifier/claim-group",
            "900",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
            "uc",
            "Unique Claim ID");

    Assert.assertTrue(compare2.equalsDeep(clmGrp2));
  }

  @Test
  public void shouldHaveEstensions() {
    Assert.assertEquals(7, eob.getExtension().size());
  }

  @Test
  public void shouldHaveExtensionsWithNearLineCode() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
                "O",
                "Part B physician/supplier claim record (processed by local carriers; can include DMEPOS services)"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveExtensionsWithCarrierNumber() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/carr_num", eob.getExtension());

    Identifier identifier =
        new Identifier()
            .setSystem("https://bluebutton.cms.gov/resources/variables/carr_num")
            .setValue("61026");

    Extension compare =
        new Extension("https://bluebutton.cms.gov/resources/variables/carr_num", identifier);

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveExtensionsWithCarrierClaimControlNumber() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/carr_clm_cntl_num", eob.getExtension());

    Identifier identifier =
        new Identifier()
            .setSystem("https://bluebutton.cms.gov/resources/variables/carr_clm_cntl_num")
            .setValue("74655592568216");

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/carr_clm_cntl_num", identifier);

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveExtensionsWithCarrierClaimPaymentDownloadCode() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/carr_clm_pmt_dnl_cd",
            eob.getExtension());

    Coding coding =
        new Coding()
            .setSystem("https://bluebutton.cms.gov/resources/variables/carr_clm_pmt_dnl_cd")
            .setDisplay("Physician/supplier")
            .setCode("1");

    Extension compare =
        new Extension("https://bluebutton.cms.gov/resources/variables/carr_clm_pmt_dnl_cd", coding);

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveExtensionsWithCarrierAssignedClaim() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/asgmntcd", eob.getExtension());

    Coding coding =
        new Coding()
            .setSystem("https://bluebutton.cms.gov/resources/variables/asgmntcd")
            .setDisplay("Assigned claim")
            .setCode("A");

    Extension compare =
        new Extension("https://bluebutton.cms.gov/resources/variables/asgmntcd", coding);

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveExtensionsWithClaimClinicalTrailNumber() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_clncl_tril_num",
            eob.getExtension());

    Identifier identifier =
        new Identifier()
            .setSystem("https://bluebutton.cms.gov/resources/variables/clm_clncl_tril_num")
            .setValue("0");

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_clncl_tril_num", identifier);

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveExtensionsWithClaimEntryCodeNumber() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/carr_clm_entry_cd", eob.getExtension());

    Coding coding =
        new Coding()
            .setSystem("https://bluebutton.cms.gov/resources/variables/carr_clm_entry_cd")
            .setDisplay(
                "Original debit; void of original debit (If CLM_DISP_CD = 3, code 1 means voided original debit)")
            .setCode("1");

    Extension compare =
        new Extension("https://bluebutton.cms.gov/resources/variables/carr_clm_entry_cd", coding);

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  /** SupportingInfo items */
  @Test
  public void shouldHaveSupportingInfoList() {
    Assert.assertEquals(2, eob.getSupportingInfo().size());
  }

  @Test
  public void shouldHaveSupportingInfoListForClaimReceivedDate() {
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
                    "NCH Weekly Claim Processing Date")));

    compare.setTiming(new DateType("1999-11-06"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveSupportingInfoListForClaimReceivedDate2() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("info", eob.getSupportingInfo());

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

    compare.setValue(new Reference("#line-observation-6"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveCreatedDate() {
    Assert.assertNotNull(eob.getCreated());
  }

  @Test
  public void shouldReferencePatient() {
    Assert.assertNotNull(eob.getPatient());
    Assert.assertEquals("Patient/567834", eob.getPatient().getReference());
  }

  @Test
  public void shouldInsuranceCoverage() {
    Assert.assertNotNull(eob.getInsurance());
    Assert.assertEquals(
        "Coverage/part-b-567834", eob.getInsurance().get(0).getCoverage().getReference());
  }

  @Test
  public void shouldSetFinalAction() {
    Assert.assertEquals(ExplanationOfBenefitStatus.ACTIVE, eob.getStatus());
  }

  @Test
  public void shouldSetUse() {
    Assert.assertEquals(Use.CLAIM, eob.getUse());
  }

  @Test
  public void shouldSetID() {
    Assert.assertEquals("ExplanationOfBenefit/carrier-" + claim.getClaimId(), eob.getId());
  }

  @Test
  public void shouldSetLastUpdated() {
    Assert.assertNotNull(eob.getMeta().getLastUpdated());
  }

  @Test
  public void shouldHaveLineItemProductOrServiceCoding() {
    CodeableConcept pos = eob.getItemFirstRep().getProductOrService();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "92999", null)));

    compare.setExtension(
        Arrays.asList(
            new Extension(
                "http://hl7.org/fhir/sid/ndc",
                new Coding("http://hl7.org/fhir/sid/ndc", "49035044700", null))));

    Assert.assertTrue(compare.equalsDeep(pos));
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
            .setTiming(new DateType("1999-11-06"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  /** Diagnosis elements */
  @Test
  public void shouldHaveDiagnosesList() {
    Assert.assertEquals(5, eob.getDiagnosis().size());
  }

  @Test
  public void shouldHaveDiagnosesMembers() {
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("H5555", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "H5555", null),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype", "principal", "principal"),
            null,
            null);

    Assert.assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("H8888", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "H8888", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    Assert.assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("H66666", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "H66666", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    Assert.assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("H77777", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "H77777", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    Assert.assertTrue(cmp4.equalsDeep(diag4));

    DiagnosisComponent diag5 =
        TransformerTestUtilsV2.findDiagnosisByCode("H12345", eob.getDiagnosis());

    DiagnosisComponent cmp5 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag5.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "H12345", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    Assert.assertTrue(cmp5.equalsDeep(diag5));
  }

  /** Top level Type */
  @Test
  public void shouldHaveExpectedTypeCoding() {
    Assert.assertEquals(3, eob.getType().getCoding().size());
  }

  @Test
  public void shouldHaveExpectedCodingValues() {
    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/nch_clm_type_cd",
                        "71",
                        "Local carrier non-durable medical equipment, prosthetics, orthotics, and supplies (DMEPOS) claim"),
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/eob-type",
                        "CARRIER",
                        null),
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/claim-type",
                        "professional",
                        "Professional")));

    Assert.assertTrue(compare.equalsDeep(eob.getType()));
  }

  /**
   * CareTeam list
   *
   * <p>Based on how the code currently works, we can assume that the same CareTeam members always
   * are added in the same order. This means we can look them up by sequence number.
   */
  @Test
  public void shouldHaveCareTeamList() {
    Assert.assertEquals(5, eob.getCareTeam().size());
  }

  @Test
  public void shouldHaveCareTeamMembers() {
    // First member
    CareTeamComponent member1 = TransformerTestUtilsV2.findCareTeamBySequence(1, eob.getCareTeam());
    CareTeamComponent compare1 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            1,
            "8765676",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "referring",
            "Referring");

    Assert.assertTrue(compare1.equalsDeep(member1));

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());
    CareTeamComponent compare2 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            2,
            "K25852",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "referring",
            "Referring");

    Assert.assertTrue(compare2.equalsDeep(member2));

    // Third member
    CareTeamComponent member3 = TransformerTestUtilsV2.findCareTeamBySequence(3, eob.getCareTeam());
    Resource resource = eob.getContained().get(0);
    CareTeamComponent compare3 = TransformerTestUtilsV2.createCareTeamMember(3, resource);
    /*
    compare3.setRole(
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
                        "performing",
                        "Performing provider"))));
    compare3.setResponsible(true);
    compare3.setQualification(
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding()
                        .setSystem("https://bluebutton.cms.gov/resources/variables/prvdr_spclty")
                        .setDisplay("Optometrist")
                        .setCode("41"))));
    compare3.addExtension(
        "https://bluebutton.cms.gov/resources/variables/carr_line_prvdr_type_cd",
        new Coding()
            .setSystem("https://bluebutton.cms.gov/resources/variables/carr_line_prvdr_type_cd")
            .setCode("0"));

    compare3.addExtension(
        "https://bluebutton.cms.gov/resources/variables/prtcptng_ind_cd",
        new Coding()
            .setSystem("https://bluebutton.cms.gov/resources/variables/prtcptng_ind_cd")
            .setCode("1")
            .setDisplay("Participating"));

    Assert.assertTrue(compare3.equalsDeep(member3));
    */
    // Fourth member
    CareTeamComponent member4 = TransformerTestUtilsV2.findCareTeamBySequence(5, eob.getCareTeam());
    CareTeamComponent compare4 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            5,
            "1497758544",
            "http://terminology.hl7.org/CodeSystem/claimcareteamrole",
            "primary",
            "Primary provider");
    compare4.getProvider().setDisplay("CUMBERLAND COUNTY HOSPITAL SYSTEM, INC");

    Assert.assertTrue(compare4.equalsDeep(member4));
  }

  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();

    Quantity compare = new Quantity(1);

    Assert.assertTrue(compare.equalsDeep(quantity));
  }

  @Test
  public void shouldHaveLineItemAdjudications() {
    Assert.assertEquals(9, eob.getItemFirstRep().getAdjudication().size());
  }

  @Test
  public void shouldHaveLineItemDenialReasonAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "denialreason", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudicationDiscriminator",
                                "denialreason",
                                "Denial Reason"))))
            .setReason(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/variables/carr_line_rdcd_pmt_phys_astn_c",
                                "0",
                                "N/A"))));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemPaidToPatientAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "paidtopatient", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "paidtopatient",
                                "Paid to patient"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_bene_pmt_amt",
                                "Line Payment Amount to Beneficiary"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemBenefitAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "benefit", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://terminology.hl7.org/CodeSystem/adjudication",
                                "benefit",
                                "Benefit Amount"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_nch_pmt_amt",
                                "Line NCH Medicare Payment Amount"))))
            .setAmount(
                new Money().setValue(37.5).setCurrency(TransformerConstants.CODED_MONEY_USD));
    compare.setExtension(
        Arrays.asList(
            new Extension("https://bluebutton.cms.gov/resources/variables/line_pmt_80_100_cd")
                .setValue(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/line_pmt_80_100_cd",
                        "0",
                        "80%"))));
    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemPaidToProviderAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "paidtoprovider", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "paidtoprovider",
                                "Paid to provider"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_prvdr_pmt_amt",
                                "Line Provider Payment Amount"))))
            .setAmount(
                new Money().setValue(37.5).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemDeductibleAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "deductible", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://terminology.hl7.org/CodeSystem/adjudication",
                                "deductible",
                                "Deductible"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_bene_ptb_ddctbl_amt",
                                "Line Beneficiary Part B Deductible Amount"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemPriorPayerPaidAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "priorpayerpaid", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "priorpayerpaid",
                                "Prior payer paid"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_pd_amt",
                                "Line Primary Payer (if not Medicare) Paid Amount"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemCoInsuranceAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "coinsurance", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "coinsurance",
                                "Co-insurance"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_coinsrnc_amt",
                                "Line Beneficiary Coinsurance Amount"))))
            .setAmount(
                new Money().setValue(9.57).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemSubmittedAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "submitted", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://terminology.hl7.org/CodeSystem/adjudication",
                                "submitted",
                                "Submitted Amount"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_sbmtd_chrg_amt",
                                "Line Submitted Charge Amount"))))
            .setAmount(new Money().setValue(75).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemEligibleAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "eligible", eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://terminology.hl7.org/CodeSystem/adjudication",
                                "eligible",
                                "Eligible Amount"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/line_alowd_chrg_amt",
                                "Line Allowed Charge Amount"))))
            .setAmount(new Money().setValue(75).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveBenefitBalanceFinancial() {
    Assert.assertEquals(5, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  @Test
  public void shouldHaveClmPassThruCashDeductibleFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/carr_clm_cash_ddctbl_apld_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/carr_clm_cash_ddctbl_apld_amt",
                                "Carrier Claim Cash Deductible Applied Amount (sum of all line-level deductible amounts)"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("777.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveClmPassThruClaimProviderPaymentAmountFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_clm_prvdr_pmt_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_clm_prvdr_pmt_amt",
                                "NCH Claim Provider Payment Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("123.45"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveClmPassThruClaimProviderPaymentAmountToBeneficiaryFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_clm_bene_pmt_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_clm_bene_pmt_amt",
                                "NCH Claim Payment Amount to Beneficiary"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("888.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveClmPassThruClaimSubmittedChargeFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_carr_clm_sbmtd_chrg_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_carr_clm_sbmtd_chrg_amt",
                                "NCH Carrier Claim Submitted Charge Amount (sum of all line-level submitted charges)"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("245.04"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveClmPassThruClaimAllowedChargeFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_carr_clm_alowd_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_carr_clm_alowd_amt",
                                "NCH Carrier Claim Allowed Charge Amount (sum of all line-level allowed charges)"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("166.23"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveClmTotChrgAmtTotal() {
    // Only one so just pull it directly and compare
    TotalComponent total = eob.getTotalFirstRep();

    TotalComponent compare =
        new TotalComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "priorpayerpaid",
                                "Prior payer paid"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/clm_tot_chrg_amt",
                                "Claim Total Charge Amount"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(total));
  }

  /** Procedures */
  @Test
  public void shouldHaveProcedureList() {
    Assert.assertEquals(0, eob.getProcedure().size());
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link InpatientClaim}.
   *
   * @param claim the {@link InpatientClaim} that the {@link ExplanationOfBenefit} was generated
   *     from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     InpatientClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(CarrierClaim claim, ExplanationOfBenefit eob, Boolean includeTaxNumbers)
      throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimTypeV2.CARRIER,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());
  }
}
