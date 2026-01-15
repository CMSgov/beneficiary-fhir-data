package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookMissingVariable;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link OutpatientClaimTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class OutpatientClaimTransformerV2Test {
  /** The parsed claim used to generate the EOB and for validating with. */
  OutpatientClaim claim;

  /** The EOB under test created from the {@link #claim}. */
  ExplanationOfBenefit eob;

  /** The transformer under test. */
  OutpatientClaimTransformerV2 outpatientClaimTransformer;

  /** The fhir context for parsing the test file. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** The metric registry. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The SamhsaSecurityTag lookup. */
  @Mock SecurityTagManager securityTagManager;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  Set<String> securityTags = new HashSet<>();

  /**
   * Generates the Claim object to be used in multiple tests.
   *
   * @return the claim object
   * @throws FHIRException if there was an issue creating the claim
   */
  public OutpatientClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    OutpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(OutpatientClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());
    return claim;
  }

  /**
   * Sets up the claim and EOB before each test.
   *
   * @throws IOException if there is an issue reading the test file
   */
  @BeforeEach
  public void before() throws IOException {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    outpatientClaimTransformer =
        new OutpatientClaimTransformerV2(metricRegistry, securityTagManager, false);
    claim = generateClaim();
    ExplanationOfBenefit genEob =
        outpatientClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    String expectedTimerName = outpatientClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals("ExplanationOfBenefit/outpatient-" + claim.getClaimId(), eob.getId());
  }

  /** Tests that the transformer sets the expected last updated date in the metadata. */
  @Test
  public void shouldSetLastUpdated() {
    assertNotNull(eob.getMeta().getLastUpdated());
  }

  /** Tests that the transformer sets the expected profile metadata. */
  @Test
  public void shouldSetCorrectProfile() {
    // The base CanonicalType doesn't seem to compare correctly so lets convert it
    // to a string
    assertTrue(
        eob.getMeta().getProfile().stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_EOB_OUTPATIENT_PROFILE_URL)));
  }

  /** Tests that the transformer sets the expected 'nature of request' value. */
  @Test
  public void shouldSetUse() {
    assertEquals(Use.CLAIM, eob.getUse());
  }

  /** Tests that the transformer sets the expected final action status. */
  @Test
  public void shouldSetFinalAction() {
    assertEquals(ExplanationOfBenefitStatus.ACTIVE, eob.getStatus());
  }

  /**
   * Tests that the transformer sets the billable period.
   *
   * @throws Exception should not be thrown
   */
  @Test
  public void shouldSetBillablePeriod() throws Exception {
    // We just want to make sure it is set
    assertNotNull(eob.getBillablePeriod());
    Extension extension =
        eob.getBillablePeriod()
            .getExtensionByUrl("https://bluebutton.cms.gov/resources/variables/claim_query_cd");
    assertNotNull(extension);
    Coding valueCoding = (Coding) extension.getValue();
    assertEquals("Final bill", valueCoding.getDisplay());
    assertEquals("3", valueCoding.getCode());

    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2011-01-24"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2011-01-24"), eob.getBillablePeriod().getEnd());
  }

  /** Tests that the transformer sets the expected patient reference. */
  @Test
  public void shouldReferencePatient() {
    assertNotNull(eob.getPatient());
    assertEquals("Patient/567834", eob.getPatient().getReference());
  }

  /** Tests that the transformer sets the expected creation date. */
  @Test
  public void shouldHaveCreatedDate() {
    assertNotNull(eob.getCreated());
  }

  /**
   * Tests that the transformer sets the expected number of facility type extensions and the correct
   * values.
   */
  @Test
  public void shouldHaveFacilityTypeExtension() {
    assertNotNull(eob.getFacility());
    assertEquals(1, eob.getFacility().getExtension().size());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
            eob.getFacility().getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd", "1", "Hospital"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected number of care team entries. */
  @Test
  public void shouldHaveCareTeamList() {
    assertEquals(4, eob.getCareTeam().size());
  }

  /** Tests that the transformer sets the expected values for the care team member entries. */
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
            "Attending",
            "207R00000X",
            "Internal Medicine Physician");

    assertTrue(compare1.equalsDeep(member1));

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());
    CareTeamComponent compare2 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            2,
            "3333333333",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "operating",
            "Operating",
            "207T00000X",
            "Neurological Surgery Physician");

    assertTrue(compare2.equalsDeep(member2));

    // Third member
    CareTeamComponent member3 = TransformerTestUtilsV2.findCareTeamBySequence(3, eob.getCareTeam());
    CareTeamComponent compare3 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            3,
            "4444",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "otheroperating",
            "Other Operating",
            "204D00000X",
            "Neuromusculoskeletal Medicine & OMM Physician");

    assertTrue(compare3.equalsDeep(member3));

    // Fourth member
    CareTeamComponent member4 = TransformerTestUtilsV2.findCareTeamBySequence(4, eob.getCareTeam());
    CareTeamComponent compare4 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            4,
            "345345345",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "performing",
            "Performing provider",
            "207ZH0000X",
            "Hematology (Pathology) Physician");

    assertTrue(compare4.equalsDeep(member4));
  }

  /** Tests that the transformer sets the expected number of supporting info entries. */
  @Test
  public void shouldHaveSupportingInfoList() {
    assertEquals(5, eob.getSupportingInfo().size());
  }

  /**
   * Tests that the transformer sets the expected Supporting Information for claim received date.
   */
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
            .setTiming(new DateType("2011-02-26"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected claim PPS indicator code supporting info. */
  @Test
  public void shouldHaveClmMcoPdSwSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_mco_pd_sw",
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
                    "https://bluebutton.cms.gov/resources/variables/clm_mco_pd_sw",
                    "Claim MCO Paid Switch")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_mco_pd_sw",
                "0",
                "No managed care organization (MCO) payment"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected type of bill supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected discharge status supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected NCH primary payer code supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected number of diagnosis. */
  @Test
  public void shouldHaveDiagnosesList() {
    assertEquals(5, eob.getDiagnosis().size());
  }

  /** Tests that the transformer sets the expected diagnosis entries. */
  @Test
  public void shouldHaveDiagnosesMembers() {
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("A40", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A40", "STREPTOCOCCAL SEPSIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A40", "STREPTOCOCCAL SEPSIS")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                "principal",
                "Principal Diagnosis"),
            null,
            null);

    assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("A52", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A52", "LATE SYPHILIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A52", "LATE SYPHILIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            null,
            null);

    assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("A06", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A06", "AMEBIASIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A06", "AMEBIASIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            null,
            null);

    assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("A15", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A15", "RESPIRATORY TUBERCULOSIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A15", "RESPIRATORY TUBERCULOSIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            null,
            null);

    assertTrue(cmp4.equalsDeep(diag4));

    DiagnosisComponent diag5 =
        TransformerTestUtilsV2.findDiagnosisByCode("A37", eob.getDiagnosis());

    DiagnosisComponent cmp5 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag5.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A37", "WHOOPING COUGH"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A37", "WHOOPING COUGH")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "patientreasonforvisit",
                "Patient Reason for Visit"),
            null,
            null);

    assertTrue(cmp5.equalsDeep(diag5));
  }

  /** Tests that the transformer sets the expected number of procedure entries. */
  @Test
  public void shouldHaveProcedureList() {
    assertEquals(1, eob.getProcedure().size());
  }

  /** Tests that the transformer sets the expected procedure entries. */
  @Test
  public void shouldHaveProcedureMembers() {
    ProcedureComponent proc1 =
        TransformerTestUtilsV2.findProcedureByCode("CD1YYZZ", eob.getProcedure());

    ProcedureComponent cmp1 =
        TransformerTestUtilsV2.createProcedure(
            proc1.getSequence(),
            List.of(
                new Coding(
                    "http://www.cms.gov/Medicare/Coding/ICD10",
                    "CD1YYZZ",
                    "PLANAR NUCL MED IMAG OF DIGESTIVE SYS USING OTH RADIONUCLIDE"),
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10",
                    "CD1YYZZ",
                    "PLANAR NUCL MED IMAG OF DIGESTIVE SYS USING OTH RADIONUCLIDE")),
            "2016-01-16T00:00:00Z");

    assertTrue(cmp1.equalsDeep(proc1), "Comparing Procedure code CD1YYZZ");
  }

  /**
   * Tests that the transformer sets the expected number of insurance entries with the expected
   * values.
   */
  @Test
  public void shouldReferenceCoverageInInsurance() {
    // // Only one insurance object if there is more than we need to fix the focal
    // set to point
    // to the correct insurance
    assertEquals(false, eob.getInsurance().size() > 1);
    assertEquals(1, eob.getInsurance().size());

    InsuranceComponent insurance = eob.getInsuranceFirstRep();

    InsuranceComponent compare =
        new InsuranceComponent()
            .setFocal(true)
            .setCoverage(new Reference().setReference("Coverage/part-b-567834"));

    assertTrue(compare.equalsDeep(insurance));
  }

  /** Tests that the transformer sets the expected number of line items. */
  @Test
  public void shouldHaveLineItems() {
    assertEquals(1, eob.getItem().size());
  }

  /** Tests that the transformer sets the expected number of line item sequences. */
  @Test
  public void shouldHaveLineItemSequence() {
    assertEquals(25, eob.getItemFirstRep().getSequence());
  }

  /** Tests that the transformer sets the expected line item care team reference. */
  @Test
  public void shouldHaveLineItemCareTeamRef() {
    // The order isn't important but this should reference a care team member
    assertNotNull(eob.getItemFirstRep().getCareTeamSequence());
    assertEquals(1, eob.getItemFirstRep().getCareTeamSequence().size());
  }

  /** Tests that the transformer sets the expected line item revenue codes. */
  @Test
  public void shouldHaveLineItemRevenue() {
    CodeableConcept revenue = eob.getItemFirstRep().getRevenue();

    Coding code1 =
        revenue.getCoding().stream()
            .filter(
                coding ->
                    coding
                        .getSystem()
                        .equals("https://bluebutton.cms.gov/resources/variables/rev_cntr"))
            .findFirst()
            .orElse(null);

    assertNotNull(code1, "Missing expected rev_cntr coding");
    assertEquals("1", code1.getCode());
    assertNull(code1.getDisplay());
  }

  /** Tests that the transformer sets the expected Coding for line item produce/service. */
  @Test
  public void shouldHaveLineItemProductOrServiceCoding() {
    CodeableConcept pos = eob.getItemFirstRep().getProductOrService();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "M99", null)));

    compare.setExtension(
        Arrays.asList(
            new Extension(
                "http://hl7.org/fhir/sid/ndc",
                new Coding("http://hl7.org/fhir/sid/ndc", "000000000", "Fake Diluent - WATER"))));

    assertTrue(compare.equalsDeep(pos));
  }

  /** Tests that the transformer sets the expected line item product/service extension. */
  @Test
  public void shouldHaveLineItemProductOrServiceExtension() {
    assertNotNull(eob.getItemFirstRep().getProductOrService());
    assertEquals(1, eob.getItemFirstRep().getProductOrService().getExtension().size());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "http://hl7.org/fhir/sid/ndc",
            eob.getItemFirstRep().getProductOrService().getExtension());

    Extension compare =
        new Extension(
            "http://hl7.org/fhir/sid/ndc",
            new Coding("http://hl7.org/fhir/sid/ndc", "000000000", "Fake Diluent - WATER"));

    assertTrue(compare.equalsDeep(ex));
  }

  /**
   * Tests that the transformer sets the expected number of line item modifiers and the entries are
   * correct.
   */
  @Test
  public void shouldHaveLineItemModifier() {
    assertEquals(2, eob.getItemFirstRep().getModifier().size());

    CodeableConcept modifier = eob.getItemFirstRep().getModifierFirstRep();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "XX", null)));

    assertTrue(compare.equalsDeep(modifier));
  }

  /** Tests that the transformer sets the expected line item serviced date. */
  @Test
  public void shouldHaveLineItemServicedDate() {
    DateType servicedDate = eob.getItemFirstRep().getServicedDateType();

    DateType compare = new DateType("1942-01-03");

    assertEquals(servicedDate.toString(), compare.toString());
  }

  /** Tests that the transformer sets the expected line location (address). */
  @Test
  public void shouldHaveLineItemLocation() {
    Address address = eob.getItemFirstRep().getLocationAddress();

    Address compare = new Address().setState("KY");

    assertTrue(compare.equalsDeep(address));
  }

  /** Tests that the transformer sets the expected line item quantity. */
  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();

    Quantity compare = new Quantity(77);

    assertTrue(compare.equalsDeep(quantity));
  }

  /** Tests that the transformer sets the expected number of line item adjudications. */
  @Test
  public void shouldHaveLineItemAdjudications() {
    assertEquals(15, eob.getItemFirstRep().getAdjudication().size());
  }

  /** Tests that the transformer sets the expected adjudication denial code. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected revenue center denial reason. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntr2ndAnsiCd() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByReason(
            "CR121", eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_2nd_ansi_cd",
                                "CR121",
                                null))));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected adjudication center rate amount. */
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
            .setAmount(new Money().setValue(5).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected adjudication revenue center to charge amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrTotChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_tot_chrg_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_tot_chrg_amt",
                                "Revenue Center Total Charge Amount"))))
            .setAmount(
                new Money().setValue(999.85).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected adjudication revenue center non-covered charge
   * amount.
   */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrNcvrdChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_ncvrd_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

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
                    .setValue(new BigDecimal("134.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected revenue center blood deductible amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrBloddDdctblAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_blood_ddctbl_amt",
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_blood_ddctbl_amt",
                                "Revenue Center Blood Deductible Amount"))))
            .setAmount(
                new Money().setValue(10.45).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected revenue center cash deductible amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrCashDdctblAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_cash_ddctbl_amt",
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_cash_ddctbl_amt",
                                "Revenue Center Cash Deductible Amount"))))
            .setAmount(
                new Money().setValue(12.89).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected revenue center coinsurance/wage adjusted
   * coinsurance amount.
   */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrCoinsrncWgeAdjstdC() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_coinsrnc_wge_adjstd_c",
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_coinsrnc_wge_adjstd_c",
                                "Revenue Center Coinsurance/Wage Adjusted Coinsurance Amount"))))
            .setAmount(
                new Money().setValue(15.23).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected revenue center reduced coinsurance amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrRdcdCoinsrncAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_rdcd_coinsrnc_amt",
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_rdcd_coinsrnc_amt",
                                "Revenue Center Reduced Coinsurance Amount"))))
            .setAmount(
                // Because of the .00, this has to be specified as a string or it will only have
                // .0 and fail
                new Money()
                    .setValueElement(new DecimalType("11.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected revenue center first medicare secondary payer
   * (MSP) paid amount.
   */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntr1stMspPdAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_1st_msp_pd_amt",
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_1st_msp_pd_amt",
                                "Revenue Center 1st Medicare Secondary Payer (MSP) Paid Amount"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected revenue center second medicare secondary payer
   * (MSP) paid amount.
   */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntr2ndMspPdAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_2nd_msp_pd_amt",
            eob.getItemFirstRep().getAdjudication());

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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_2nd_msp_pd_amt",
                                "Revenue Center 2nd Medicare Secondary Payer (MSP) Paid Amount"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item revenue center provider amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrPrvdrPmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
                                "Revenue Center (Medicare) Provider Payment Amount"))))
            .setAmount(new Money().setValue(200).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected revenue center payment amount to bene. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrBenePmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_bene_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

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
            .setAmount(new Money().setValue(300).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected revenue center patient responsibility payment
   * amount.
   */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrPtntRspnbltyPmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_ptnt_rspnsblty_pmt",
            eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "paidbypatient",
                                "Paid by patient"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_ptnt_rspnsblty_pmt",
                                "Revenue Center Patient Responsibility Payment Amount"))))
            .setAmount(new Money().setValue(500).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected adjudication revenue center payment amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrPmtAmtAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_pmt_amt_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_pmt_amt_amt",
                                "Revenue Center (Medicare) Payment Amount"))))
            .setAmount(
                new Money()
                    .setValue(new BigDecimal("5000.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected claim total charge amount entries. */
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
                new Money().setValue(8888.85).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(total));
  }

  /** Tests that the transformer sets the expected payment value. */
  @Test
  public void shouldHavePayment() {
    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(
                new Money().setValue(693.11).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(eob.getPayment()));
  }

  /** Tests that the transformer sets the expected number of total entries. */
  @Test
  public void shouldHaveTotal() {
    assertEquals(1, eob.getTotal().size());
  }

  /**
   * Tests that the transformer sets the expected number of benefit balance entries and the correct
   * values.
   */
  @Test
  public void shouldHaveBenefitBalance() {
    assertEquals(1, eob.getBenefitBalance().size());

    // Test Category here
    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/ex-benefitcategory",
                        "1",
                        "Medical Care")));

    assertTrue(compare.equalsDeep(eob.getBenefitBalanceFirstRep().getCategory()));
  }

  /** Tests that the transformer sets the expected number of benefit balance financial entries. */
  @Test
  public void shouldHaveBenefitBalanceFinancial() {
    assertEquals(7, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  /** Tests that the transformer sets the expected professional component charge amount code. */
  @Test
  public void shouldHaveNchProfnlCmpntChrgAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_profnl_cmpnt_chrg_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_profnl_cmpnt_chrg_amt",
                                "Professional Component Charge Amount"))))
            .setUsed(new Money().setValue(66.89).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH beneficiary part B deductible amount. */
  @Test
  public void shouldHaveNchBenePtbDdctblAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_bene_ptb_ddctbl_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_bene_ptb_ddctbl_amt",
                                "NCH Beneficiary Part B Deductible Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("112.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH beneficiary part B coinsurance amount. */
  @Test
  public void shouldHaveNchBenePtbCoinsrncAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_bene_ptb_coinsrnc_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_bene_ptb_coinsrnc_amt",
                                "NCH Beneficiary Part B Coinsurance Amount"))))
            .setUsed(
                new Money().setValue(175.73).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected outpatient provider payment amount. */
  @Test
  public void shouldHaveClmOpPrvdrPmtAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_op_prvdr_pmt_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_op_prvdr_pmt_amt",
                                "Claim Outpatient Provider Payment Amount"))))
            .setUsed(
                new Money().setValue(693.92).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected outpatient payment amount to beneficiary. */
  @Test
  public void shouldHaveClmOpBenePmtAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_op_bene_pmt_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_op_bene_pmt_amt",
                                "Claim Outpatient Payment Amount to Beneficiary"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("44.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected NCH beneficiary blood deductible liability amount
   * code.
   */
  @Test
  public void shouldHaveNchBeneBloodDdctblLbltyAmFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_bene_blood_ddctbl_lblty_am",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_bene_blood_ddctbl_lblty_am",
                                "NCH Beneficiary Blood Deductible Liability Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("6.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH primary payer claim paid amount. */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Ensure that when the revenue status code exists in the claim, it should be mapped to an
   * extension.
   *
   * <p>The specific code value of the extension is tested in {@link
   * TransformerUtilsV2Test#mapEobCommonItemRevenueStatusCodeWhenStatusCodeExistsExpectExtensionOnItem()}
   */
  @Test
  public void shouldHaveRevenueStatusCode() {

    String expectedExtensionUrl =
        "https://bluebutton.cms.gov/resources/variables/rev_cntr_stus_ind_cd";

    assertNotNull(eob.getItem());
    assertTrue(eob.getItem().size() > 0);
    ExplanationOfBenefit.ItemComponent item = eob.getItem().get(0);
    assertNotNull(item);
    assertNotNull(item.getRevenue());
    assertNotNull(item.getRevenue().getExtension());
    assertEquals(1, item.getRevenue().getExtension().size());
    Extension ext = item.getRevenue().getExtensionByUrl(expectedExtensionUrl);
    assertNotNull(ext);
    assertEquals(expectedExtensionUrl, ext.getUrl());
    assertTrue(ext.getValue() instanceof Coding);
    assertNotNull(((Coding) ext.getValue()).getCode());
  }

  /**
   * Ensures the rev_cntr_unit_cnt is correctly mapped to an eob item as an extension when the unit
   * quantity is not zero.
   */
  @Test
  public void shouldHaveRevenueCenterUnit() {
    TransformerTestUtilsV2.assertExtensionQuantityEquals(
        CcwCodebookMissingVariable.REV_CNTR_UNIT_CNT, BigDecimal.valueOf(111), eob.getItem());
  }

  /**
   * Ensures the fi_num is correctly mapped to an eob as an extension when the
   * fiscalIntermediaryNumber is present.
   */
  @Test
  public void shouldHaveFiNumberExtension() {

    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_num";

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiNumExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNotNull(fiNumExtension);
    assertEquals("15444", ((Coding) fiNumExtension.getValue()).getCode());
  }

  /**
   * Ensures the Fi_Clm_Proc_Dt is correctly mapped to an eob as an extension when the
   * fiscalIntermediaryClaimProcessDate is present.
   */
  @Test
  public void shouldHaveFiClaimProcessDateExtension() {
    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt", eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt",
            new DateType("2011-02-18"));

    assertTrue(compare.equalsDeep(ex));
  }
}
