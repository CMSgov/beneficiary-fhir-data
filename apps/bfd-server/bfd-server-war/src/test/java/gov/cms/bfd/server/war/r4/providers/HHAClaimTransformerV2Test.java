package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Address;
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
import org.hl7.fhir.r4.model.ExplanationOfBenefit.InsuranceComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.PaymentComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HHAClaimTransformerV2Test {
  HHAClaim claim;
  ExplanationOfBenefit eob;

  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return
   * @throws FHIRException
   */
  public HHAClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    HHAClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(r -> (HHAClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(Instant.now());

    return claim;
  }

  @Before
  public void before() {
    claim = generateClaim();
    eob = HHAClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  @Test
  public void shouldSetID() {
    Assert.assertEquals("hha-" + claim.getClaimId(), eob.getId());
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
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_EOB_NONCLINICIAN_PROFILE_URL)));
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
        (new SimpleDateFormat("yyy-MM-dd")).parse("2015-06-23"),
        eob.getBillablePeriod().getStart());
    Assert.assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2015-06-23"), eob.getBillablePeriod().getEnd());
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
                "3",
                "Home Health Agency (HHA)"));

    Assert.assertTrue(compare.equalsDeep(ex));
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
    CareTeamComponent compare1 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            1,
            "2222222222",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "attending",
            "Attending");

    Assert.assertTrue(compare1.equalsDeep(member1));

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());
    CareTeamComponent compare2 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            2,
            "345345345",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "performing",
            "Performing provider");

    Assert.assertTrue(compare2.equalsDeep(member2));
  }

  /** SupportingInfo items */
  @Test
  public void shouldHaveSupportingInfoList() {
    Assert.assertEquals(9, eob.getSupportingInfo().size());
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
            .setTiming(new DateType("2015-11-06"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveTypeOfBillSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("typeofbill", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                    "typeofbill",
                    "Type of Bill")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_freq_cd",
                "1",
                "Admit thru discharge claim"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveDischargeStatusSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "discharge-status", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                    "discharge-status",
                    "Discharge Status")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/ptnt_dschrg_stus_cd", "1", null));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveNchPrmryPyrCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nch_prmry_pyr_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/nch_prmry_pyr_cd",
                    "NCH Primary Payer Code (if not Medicare)")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/nch_prmry_pyr_cd",
                "A",
                "Employer group health plan (EGHP) insurance for an aged beneficiary"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveClmMcoPdSwSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_ind_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/clm_pps_ind_cd",
                    "Claim PPS Indicator Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_pps_ind_cd",
                "2",
                "PPS bill; claim contains PPS indicator"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveClmHhaLupaIndCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_hha_lupa_ind_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/clm_hha_lupa_ind_cd",
                    "Claim HHA Low Utilization Payment Adjustment (LUPA) Indicator Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_hha_lupa_ind_cd",
                "L",
                "Low utilization payment adjustment (LUPA) claim"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveClmHhaRfrlCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_hha_rfrl_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/clm_hha_rfrl_cd",
                    "Claim HHA Referral Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_hha_rfrl_cd",
                "1",
                "Physician referral - The patient was admitted upon the recommendation of a personal physician."));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveClmHhaTotVisitCntSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_hha_tot_visit_cnt",
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
                        "https://bluebutton.cms.gov/resources/variables/clm_hha_tot_visit_cnt",
                        "Claim HHA Total Visit Count")))
            // valueQuantity
            .setValue(new Quantity().setValue(3));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveAdmissionPeriodSupInfo() throws Exception {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("admissionperiod", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                    "admissionperiod",
                    "Admission Period")));
    // timingPeriod
    Period period = new Period();
    period.setStart(
        new SimpleDateFormat("yyy-MM-dd").parse("2015-06-23"), TemporalPrecisionEnum.DAY);

    compare.setTiming(period);

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  /** Diagnosis elements */
  @Test
  public void shouldHaveDiagnosesList() {
    Assert.assertEquals(4, eob.getDiagnosis().size());
  }

  @Test
  public void shouldHaveDiagnosesMembers() {
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("H5555", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-9-cm", "H5555", null),
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
        TransformerTestUtilsV2.findDiagnosisByCode("R2222", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            new Coding("http://hl7.org/fhir/sid/icd-10", "R2222", null),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
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
                "secondary",
                "Secondary"),
            null,
            null);

    Assert.assertTrue(cmp4.equalsDeep(diag4));
  }

  /** Insurance */
  @Test
  public void shouldReferenceCoverageInInsurance() {
    // Only one insurance object
    Assert.assertEquals(1, eob.getInsurance().size());

    InsuranceComponent insurance = eob.getInsuranceFirstRep();

    InsuranceComponent compare =
        new InsuranceComponent()
            .setCoverage(new Reference().setReference("Coverage/part-b-567834"));

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
  public void shouldHaveLineItemRevenue() {
    CodeableConcept revenue = eob.getItemFirstRep().getRevenue();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/rev_cntr",
                        "0023",
                        "Home Health services paid under PPS submitted as TOB 32X and 33X, effective 10/00. This code may appear multiple times on a claim to identify different HIPPS/Home Health Resource Groups (HRG)."),
                    new Coding("https://www.nubc.org/CodeSystem/RevenueCodes", "4", null),
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/rev_cntr_ddctbl_coinsrnc_cd",
                        "4",
                        "No charge or units associated with this revenue center code. (For multiple HCPCS per single revenue center code) For revenue center code 0001, the following MSP override values may be present:")));

    Assert.assertTrue(compare.equalsDeep(revenue));
  }

  @Test
  public void shouldHaveLineItemProductOrServiceCoding() {
    CodeableConcept pos = eob.getItemFirstRep().getProductOrService();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "2GGGG", null)));

    Assert.assertTrue(compare.equalsDeep(pos));
  }

  @Test
  public void shouldHaveLineItemModifier() {
    Assert.assertEquals(2, eob.getItemFirstRep().getModifier().size());

    CodeableConcept modifier = eob.getItemFirstRep().getModifierFirstRep();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "KO", null)));

    Assert.assertTrue(compare.equalsDeep(modifier));
  }

  @Test
  public void shouldHaveLineItemServicedDate() {
    DateType servicedDate = eob.getItemFirstRep().getServicedDateType();

    DateType compare = new DateType("2015-06-23");

    Assert.assertEquals(servicedDate.toString(), compare.toString());
  }

  @Test
  public void shouldHaveLineItemLocation() {
    Address address = eob.getItemFirstRep().getLocationAddress();

    Address compare = new Address().setState("UT");

    Assert.assertTrue(compare.equalsDeep(address));
  }

  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();

    Quantity compare = new Quantity(666);

    Assert.assertTrue(compare.equalsDeep(quantity));
  }

  @Test
  public void shouldHaveLineItemAdjudications() {
    Assert.assertEquals(5, eob.getItemFirstRep().getAdjudication().size());
  }

  @Test
  public void shouldHaveLineItemAdjudicationRevCntr1stAnsiCd() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByReason(
            "CO120", eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_1st_ansi_cd",
                                "CO120",
                                null))));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationRevCntrRateAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_rate_amt",
            eob.getItemFirstRep().getAdjudication());

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
  public void shouldHaveLineItemAdjudicationRevCntrTotChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_tot_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(25.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationRevCntrNcrvdChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_ncvrd_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(24.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationRevCntrPmtAmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_pmt_amt_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(26.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
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
                                "http://terminology.hl7.org/CodeSystem/adjudication",
                                "submitted",
                                "Submitted Amount"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/clm_tot_chrg_amt",
                                "Claim Total Charge Amount"))))
            .setAmount(
                new Money().setValue(199.99).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(total));
  }
  /** Payment */
  @Test
  public void shouldHavePayment() {
    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(188.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(eob.getPayment()));
  }

  /** Total */
  @Test
  public void shouldHaveTotal() {
    Assert.assertEquals(1, eob.getTotal().size());
  }

  /** Benefit Balance */
  @Test
  public void shouldHaveBenefitBalance() {
    Assert.assertEquals(1, eob.getBenefitBalance().size());

    // Test Category here
    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/ex-benefitcategory",
                        "1",
                        "Medical Care")));

    Assert.assertTrue(compare.equalsDeep(eob.getBenefitBalanceFirstRep().getCategory()));
  }

  @Test
  public void shouldHaveBenefitBalanceFinancial() {
    Assert.assertEquals(1, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  @Test
  public void shouldHavePrpayAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/prpayamt",
            eob.getBenefitBalanceFirstRep().getFinancial());

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
                    .setValueElement(new DecimalType("11.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
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
        HHAClaimTransformerV2.transform(new MetricRegistry(), generateClaim(), Optional.of(false));
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }
}
