package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
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

  @Test
  public void shouldHaveReferral() {
    Assert.assertNotNull(eob.getReferral());
    Assert.assertEquals("1306849450", eob.getReferral().getIdentifier().getValue());
  }

  @Test
  public void shouldHaveDisposition() {
    Assert.assertEquals("01", claim.getClaimDispositionCode());
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
  public void shouldHaveLineItemExtension() {
    Assert.assertNotNull(eob.getItemFirstRep().getExtension());
    Assert.assertEquals(10, eob.getItemFirstRep().getExtension().size());

    Extension ex1 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/suplrnum",
            eob.getItemFirstRep().getExtension());

    Extension compare1 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/suplrnum",
            new Identifier()
                .setSystem("https://bluebutton.cms.gov/resources/variables/suplrnum")
                .setValue("1219966666"));

    Assert.assertTrue(compare1.equalsDeep(ex1));

    Extension ex2 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_scrn_svgs_amt",
            eob.getItemFirstRep().getExtension());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt2 = new BigDecimal(0.00);
    amt2 = amt2.setScale(2, RoundingMode.HALF_DOWN);

    Extension compare2 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_scrn_svgs_amt",
            new Quantity().setValue(amt2));

    Assert.assertTrue(compare2.equalsDeep(ex2));

    Extension ex3 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_mtus_cnt",
            eob.getItemFirstRep().getExtension());

    Extension compare3 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_mtus_cnt",
            new Quantity()
                .setUnit("Number of services")
                .setValue(60)
                .setSystem("https://bluebutton.cms.gov/resources/variables/dmerc_line_mtus_cd")
                .setCode("3"));

    Assert.assertTrue(compare3.equalsDeep(ex3));

    Extension ex4 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_prcng_state_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare4 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_prcng_state_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/dmerc_line_prcng_state_cd",
                "AL",
                null));

    Assert.assertTrue(compare4.equalsDeep(ex4));

    Extension ex5 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_supplr_type_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare5 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_supplr_type_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/dmerc_line_supplr_type_cd",
                "3",
                "Suppliers (other than sole proprietorship) for whom employer identification (EI) numbers are used in coding the ID field."));

    Assert.assertTrue(compare5.equalsDeep(ex5));

    Extension ex6 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/betos_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare6 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/betos_cd",
            new Coding("https://bluebutton.cms.gov/resources/variables/betos_cd", "D9Z", null));

    Assert.assertTrue(compare6.equalsDeep(ex6));

    Extension ex7 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare7 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
                "E",
                "Workers' compensation"));

    Assert.assertTrue(compare7.equalsDeep(ex7));

    Extension ex8 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/line_prcsg_ind_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare8 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/line_prcsg_ind_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/line_prcsg_ind_cd",
                "A",
                "Allowed"));

    Assert.assertTrue(compare8.equalsDeep(ex8));

    Extension ex9 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/line_service_deductible",
            eob.getItemFirstRep().getExtension());

    Extension compare9 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/line_service_deductible",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/line_service_deductible",
                "0",
                "Service Subject to Deductible"));

    Assert.assertTrue(compare9.equalsDeep(ex9));

    Extension ex10 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare10 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
                "E",
                "Workers' compensation"));

    Assert.assertTrue(compare10.equalsDeep(ex10));
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
                "495800192",
                "Day Time Cold Multi-Symptom Cool Blast - ACETAMINOPHEN; GUAIFENESIN; DEXTROMETHORPHAN HYDROBROMIDE; PHENYLEPHRINE HYDROCHLORIDE"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldHaveLineItemModifier() {
    Assert.assertEquals(1, eob.getItemFirstRep().getModifier().size());

    CodeableConcept modifier = eob.getItemFirstRep().getModifierFirstRep();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding("https://bluebutton.cms.gov/resources/codesystem/hcpcs", "YY", null)
                        .setVersion("3")));

    Assert.assertTrue(compare.equalsDeep(modifier));
  }

  @Test
  public void shouldHaveLineItemServicedPeriod() throws Exception {
    Date serviceStart = eob.getItemFirstRep().getServicedPeriod().getStart();
    Date serviceEnd = eob.getItemFirstRep().getServicedPeriod().getEnd();

    Period compare = new Period();
    compare.setStart(
        new SimpleDateFormat("yyy-MM-dd").parse("2014-02-03"), TemporalPrecisionEnum.DAY);
    compare.setEnd(
        new SimpleDateFormat("yyy-MM-dd").parse("2014-02-03"), TemporalPrecisionEnum.DAY);

    Assert.assertEquals(compare.getStart().toString(), serviceStart.toString());
    Assert.assertEquals(compare.getEnd().toString(), serviceEnd.toString());
  }

  @Test
  public void shouldHaveLineItemLocationCodeableConcept() {
    CodeableConcept location = eob.getItemFirstRep().getLocationCodeableConcept();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/line_place_of_srvc_cd",
                        "12",
                        "Home. Location, other than a hospital or other facility, where the patient receives care in a private residence.")));

    compare.setExtension(
        Arrays.asList(
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/prvdr_state_cd",
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/prvdr_state_cd", "MO", null))));

    Assert.assertTrue(compare.equalsDeep(location));
  }

  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();

    Quantity compare = new Quantity(60);

    Assert.assertTrue(compare.equalsDeep(quantity));
  }

  @Test
  public void shouldHaveLineItemAdjudications() {
    Assert.assertEquals(13, eob.getItemFirstRep().getAdjudication().size());
  }

  @Test
  public void shouldHaveLineItemAdjudicationRevCntrPrvdrPmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(120.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLinePrmryAlowdChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_prmry_alowd_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/line_prmry_alowd_chrg_amt",
                                "Line Primary Payer Allowed Charge Amount"))))
            .setAmount(
                new Money().setValue(20.29).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineDmePrchsPriceAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_dme_prchs_price_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/line_dme_prchs_price_amt",
                                "Line DME Purchase Price Amount"))))
            .setAmount(
                new Money().setValue(82.29).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineNchPmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_nch_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

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
                                "Line NCH Medicare Payment Amount"))));

    compare.setExtension(
        Arrays.asList(
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/line_pmt_80_100_cd",
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/line_pmt_80_100_cd",
                    "0",
                    "80%"))));

    compare.setAmount(
        new Money().setValue(123.45).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineBenePmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_bene_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(11.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLinePrvdrPmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_prvdr_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(120.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineBenePtbDdctblAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_bene_ptb_ddctbl_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(18.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineBenePrmryPyrPdAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_pd_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(11.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineCoinsrncAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_coinsrnc_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(20.20);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineSubmtdChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_sbmtd_chrg_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/line_sbmtd_chrg_amt",
                                "Line Submitted Charge Amount"))))
            .setAmount(
                new Money().setValue(130.45).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineAlowdChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_alowd_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

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
            .setAmount(
                new Money().setValue(130.45).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineBenePmtAmt2() {
    BigDecimal amt = new BigDecimal(82.29);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategoryAndAmount(
            "https://bluebutton.cms.gov/resources/variables/line_bene_pmt_amt",
            amt,
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/line_bene_pmt_amt",
                                "Line Payment Amount to Beneficiary"))))
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(adjudication));
  }

  @Test
  public void shouldHaveLineItemAdjudicationLineDmePrchsPriceAmt2() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/line_dme_prchs_price_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/line_dme_prchs_price_amt",
                                "Line DME Purchase Price Amount"))))
            .setAmount(
                new Money().setValue(82.29).setCurrency(TransformerConstants.CODED_MONEY_USD));

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

  /** Payment */
  @Test
  public void shouldHavePayment() {
    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(
                new Money().setValue(777.75).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
    Assert.assertEquals(5, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  @Test
  public void shouldHaveCarrClmCashDdctblAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/carr_clm_cash_ddctbl_apld_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(777.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

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
            .setUsed(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveNchClmPrvdrPmtAmtFinancial() {
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
                new Money().setValue(666.75).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveNchClmBenePmtAmtFinancial() {
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
                new Money().setValue(666.66).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveNchCarrClmSubmtdChrgAmtFinancial() {
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
                new Money().setValue(1752.75).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(benefit));
  }

  @Test
  public void shouldHaveNchCarrClmAlwdAmtFinancial() {
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
                new Money().setValue(754.79).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
        DMEClaimTransformerV2.transform(new MetricRegistry(), generateClaim(), Optional.of(false));
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }
}
