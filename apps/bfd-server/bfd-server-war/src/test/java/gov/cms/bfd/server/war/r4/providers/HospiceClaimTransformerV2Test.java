package gov.cms.bfd.server.war.r4.providers;

import static org.hamcrest.CoreMatchers.*;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.text.SimpleDateFormat;
import java.util.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.InsuranceComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.PaymentComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public final class HospiceClaimTransformerV2Test {

  private static final FhirContext fhirContext = FhirContext.forR4();
  private ExplanationOfBenefit eob = null;
  private HospiceClaim claim = null;
  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return
   * @throws FHIRException
   */
  @Before
  public void generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(new Date());
    createEOB(Optional.of(false));
  }

  private void createEOB(Optional<Boolean> includeTaxNumber) {
    eob = HospiceClaimTransformerV2.transform(new MetricRegistry(), claim, includeTaxNumber);
  }

  /**
   * Serializes the EOB and prints to the command line
   *
   * @throws FHIRException
   */
  @Ignore
  @Test
  public void shouldOutputJSON() throws FHIRException {
    Assert.assertNotNull(eob);
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SNFClaimTransformer#transform(Object)} works as expected
   * when run against the {@link StaticRifResource#SAMPLE_A_SNF} {@link SNFClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    assertMatches(
        claim,
        HospiceClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.of(false)));
  }

  /** Common top level ExplanationOfBenefit values */
  @Test
  public void shouldSetId() {
    Assert.assertEquals("hospice-" + claim.getClaimId(), eob.getId());
  }

  @Test
  public void shouldSetLastUpdated() {
    Assert.assertNotNull(eob.getMeta().getLastUpdated());
  }

  @Test
  public void shouldSetCorrectProfile() {
    // The base CanonicalType doesn't seem to compare correctly so lets convert it to a string
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
  public void shouldSetOutcomeStatus() {
    Assert.assertEquals(ExplanationOfBenefit.RemittanceOutcome.COMPLETE, eob.getOutcome());
  }

  /** Provider Local Reference */
  @Test
  public void shouldHaveProviderReference() {
    List<Resource> containEntries = eob.getContained();
    Assert.assertEquals(1, containEntries.size());
    Assert.assertNotNull(eob.getProvider());
    Assert.assertEquals("#provider-org", eob.getProvider().getReference());
  }

  /** Patient Reference */
  @Test
  public void shouldHavePatientReference() {
    Assert.assertNotNull(eob.getPatient());
    Assert.assertEquals("Patient/567834", eob.getPatient().getReference());
  }

  @Test
  public void shouldHaveCreatedDate() {
    Assert.assertNotNull(eob.getCreated());
  }

  /** Provider Identifier(s) */
  @Test
  public void shouldHaveContainedIdentifier() {
    List<Resource> actuals = eob.getContained();

    Assert.assertEquals(1, actuals.size());
    Organization org = (Organization) actuals.get(0);
    List<Identifier> expected = org.getIdentifier();
    Assert.assertEquals(2, expected.size());

    List<Identifier> compare = new ArrayList<Identifier>();
    Identifier ident = new Identifier();
    ident
        .setValue("12345")
        .getType()
        .addCoding()
        .setCode("PRN")
        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203");

    compare.add(ident);

    ident = new Identifier();
    ident
        .setValue("999999999")
        .setSystem("http://hl7.org/fhir/sid/us-npi")
        .getType()
        .addCoding()
        .setCode("npi")
        .setSystem("http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType");

    compare.add(ident);

    for (int i = 0; i < compare.size(); i++) {
      Assert.assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
  }

  @Test
  public void shouldHaveIdentifiers() {
    List<Identifier> expected = eob.getIdentifier();
    Assert.assertEquals(2, expected.size());

    List<Identifier> compare =
        Arrays.asList(
            TransformerTestUtilsV2.createIdentifier(
                "https://bluebutton.cms.gov/resources/variables/clm_id",
                "9992223422",
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
                "uc",
                "Unique Claim ID"),
            TransformerTestUtilsV2.createIdentifier(
                "https://bluebutton.cms.gov/resources/identifier/claim-group",
                "900",
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
                "uc",
                "Unique Claim ID"));

    for (int i = 0; i < expected.size(); i++) {
      Assert.assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
  }

  @Test
  public void shouldHaveExtensions() {
    List<Extension> expected = eob.getExtension();
    Assert.assertEquals(3, expected.size());

    Assert.assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            eob.getExtension()));

    Assert.assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
            eob.getExtension()));

    Assert.assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
            eob.getExtension()));

    List<Extension> compare =
        Arrays.asList(
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
                    "V",
                    "Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice [HOS], or home health agency [HHA])")),
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
                    "P",
                    "Payment requested")),
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
                    "1",
                    null)));

    for (int i = 0; i < expected.size(); i++) {
      Assert.assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
  }

  @Test
  public void shouldHaveTypeCodings() {
    CodeableConcept cc = eob.getType();
    Assert.assertNotNull(cc);
    List<Coding> expected = cc.getCoding();
    Assert.assertEquals(3, expected.size());

    List<Coding> compare = new ArrayList<Coding>();
    compare.add(
        new Coding()
            .setCode("50")
            .setSystem("https://bluebutton.cms.gov/resources/variables/nch_clm_type_cd")
            .setDisplay("Hospice claim"));
    compare.add(
        new Coding()
            .setCode("HOSPICE")
            .setSystem("https://bluebutton.cms.gov/resources/codesystem/eob-type"));
    compare.add(
        new Coding()
            .setCode("institutional")
            .setSystem("http://terminology.hl7.org/CodeSystem/claim-type")
            .setDisplay("Institutional"));

    for (int i = 0; i < expected.size(); i++) {
      Assert.assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
  }

  @Test
  public void shouldSetBillablePeriod() throws Exception {
    // We just want to make sure it is set
    Assert.assertNotNull(eob.getBillablePeriod());
    Assert.assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-01-01"),
        eob.getBillablePeriod().getStart());
    Assert.assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-01-30"), eob.getBillablePeriod().getEnd());
  }

  @Test
  public void shouldSetInsurer() throws Exception {
    Reference expected = eob.getInsurer();
    Assert.assertNotNull(expected);
    Reference compare = new Reference();
    compare.getIdentifier().setValue("CMS");
    Assert.assertTrue(compare.equalsDeep(expected));
  }

  @Test
  public void shouldHaveFacilityTypeExtension() {
    Assert.assertNotNull(eob.getFacility());
    Assert.assertEquals(1, eob.getFacility().getExtension().size());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
            eob.getFacility().getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
                "8",
                "Ambulatory Surgery Center (ASC) or other special facility (e.g. hospice)"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveCareTeamMembers() {
    // First member
    CareTeamComponent member1 = TransformerTestUtilsV2.findCareTeamBySequence(1, eob.getCareTeam());
    CareTeamComponent compare1 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            1,
            "8888888888",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "attending",
            "Attending");

    Assert.assertTrue(compare1.equalsDeep(member1));

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());

    CareTeamComponent compare2 =
        new CareTeamComponent()
            .setSequence(2)
            .setProvider(
                new Reference()
                    .setIdentifier(
                        TransformerTestUtilsV2.createIdentifier(
                            null,
                            "0",
                            "http://terminology.hl7.org/CodeSystem/v2-0203",
                            "UPIN",
                            "Medicare/CMS (formerly HCFA)'s Universal Physician Identification numbers")))
            .setRole(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
                                "performing",
                                "Performing provider"))));

    Assert.assertTrue(compare2.equalsDeep(member2));

    //     // Third member
    CareTeamComponent member3 = TransformerTestUtilsV2.findCareTeamBySequence(3, eob.getCareTeam());

    CareTeamComponent compare3 =
        new CareTeamComponent()
            .setSequence(3)
            .setProvider(
                new Reference()
                    .setIdentifier(
                        TransformerTestUtilsV2.createIdentifier(
                            null,
                            "345345345",
                            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
                            "npi",
                            "National Provider Identifier")))
            .setRole(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
                                "performing",
                                "Performing provider"))));

    Assert.assertTrue(compare3.equalsDeep(member3));
  }

  @Test
  public void shouldHaveAllDiagnosis() {
    List<DiagnosisComponent> expected = eob.getDiagnosis();
    Assert.assertEquals(4, expected.size());
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("R5555", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-9-cm", "R5555", null),
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
        TransformerTestUtilsV2.findDiagnosisByCode("R2222", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "R2222", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            null,
            null);

    Assert.assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("R3333", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "R3333", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
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

  @Test
  public void shouldHaveLineItemRevCenterRateAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_rate_amt",
            eob.getItemFirstRep().getAdjudication());

    Assert.assertNotNull(adjudication);

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_rate_amt",
                                "Revenue Center Rate Amount"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemRevCenterTotalChargeAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_tot_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

    Assert.assertNotNull(adjudication);

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_tot_chrg_amt",
                                "Revenue Center Total Charge Amount"))))
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("2555.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemRevCenterNonRecoverdChargeAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_ncvrd_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

    Assert.assertNotNull(adjudication);

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "noncovered",
                                "Noncovered"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_ncvrd_chrg_amt",
                                "Revenue Center Non-Covered Charge Amount"))))
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("300.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemRevCenterMedicareProviderAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    Assert.assertNotNull(adjudication);

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
                                "Revenue Center (Medicare) Provider Payment Amount"))))
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("29.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemRevCenterPaidToProviderAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    Assert.assertNotNull(adjudication);

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
                                "Revenue Center (Medicare) Provider Payment Amount"))))
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("29.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemRevCenterBenePaymentAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_bene_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    Assert.assertNotNull(adjudication);

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_bene_pmt_amt",
                                "Revenue Center Payment Amount to Beneficiary"))))
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("28.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemRevCenterMedicarePaymentAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_pmt_amt_amt",
            eob.getItemFirstRep().getAdjudication());

    Assert.assertNotNull(adjudication);

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_pmt_amt_amt",
                                "Revenue Center (Medicare) Payment Amount"))))
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("26.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveTotalChargeAmtAdjudication() {
    // Only one so just pull it directly and compare
    TotalComponent total = eob.getTotalFirstRep();

    TotalComponent compare =
        new TotalComponent()
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
                                "https://bluebutton.cms.gov/resources/variables/clm_tot_chrg_amt",
                                "Claim Total Charge Amount"))))
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("199.99"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(total));
  }

  @Test
  public void shouldHavePaymentTotal() {
    PaymentComponent expected = eob.getPayment();
    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("130.32"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(expected));
  }

  @Test
  public void shouldHaveCBenefitDayCnt() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_utlztn_day_cnt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    Assert.assertNotNull(benefit);

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_utlztn_day_cnt",
                                "Claim Medicare Utilization Day Count"))))
            .setUsed(new UnsignedIntType(30));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveCBenefitClaimPaidAmt() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/prpayamt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    Assert.assertNotNull(benefit);

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/prpayamt",
                                "NCH Primary Payer (if not Medicare) Claim Paid Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType(0))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
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
  static void assertMatches(HospiceClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimTypeV2.HOSPICE,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());
  }
}
