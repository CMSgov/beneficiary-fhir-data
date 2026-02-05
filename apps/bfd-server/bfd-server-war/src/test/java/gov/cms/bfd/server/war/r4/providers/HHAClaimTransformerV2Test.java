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
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.IParser;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookMissingVariable;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the {@link HHAClaimTransformerV2Test}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HHAClaimTransformerV2Test {
  /** The claim under test. */
  HHAClaim claim;

  /** The eob loaded before each test from a file. */
  ExplanationOfBenefit eob;

  /** The transformer under test. */
  HHAClaimTransformerV2 hhaClaimTransformer;

  /** The fhir context for parsing the file data. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** The metrics registry. */
  @Mock MetricRegistry metricRegistry;

  /** The SamhsaSecurityTag lookup. */
  @Mock SecurityTagManager securityTagManager;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  Set<String> securityTags = new HashSet<>();

  /**
   * Generates the sample A claim object to be used in multiple tests.
   *
   * @return the claim object
   * @throws FHIRException if there is an issue parsing the claim
   */
  public HHAClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    HHAClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(HHAClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());
    return claim;
  }

  /**
   * Loads the test data needed for each test.
   *
   * @throws IOException if there is an issue loading the file
   */
  @BeforeEach
  public void before() throws IOException {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    hhaClaimTransformer = new HHAClaimTransformerV2(metricRegistry, securityTagManager, false);
    claim = generateClaim();
    ExplanationOfBenefit genEob =
        hhaClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
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
    String expectedTimerName = hhaClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals("ExplanationOfBenefit/hha-" + claim.getClaimId(), eob.getId());
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
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_EOB_NONCLINICIAN_PROFILE_URL)));
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
        "https://bluebutton.cms.gov/resources/variables/claim_query_cd", valueCoding.getSystem());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2015-06-23"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2015-06-23"), eob.getBillablePeriod().getEnd());
  }

  /**
   * Tests that the billable period is not set if claim query code is null.
   *
   * @throws Exception should not be thrown
   */
  @Test
  public void shouldNotSetBillablePeriodWithNullClaimQueryCode() throws Exception {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    HHAClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(HHAClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());
    claim.setClaimQueryCode(Optional.empty());
    claim.setLastUpdated(Instant.now());

    ExplanationOfBenefit genEob =
        hhaClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);

    Extension extension =
        eob.getBillablePeriod()
            .getExtensionByUrl("https://bluebutton.cms.gov/resources/variables/claim_query_cd");
    assertNull(extension);
  }

  /**
   * Tests that the billable period is set if optional claim query code is not empty.
   *
   * @throws Exception should not be thrown
   */
  @Test
  public void shouldSetBillablePeriodWithNonEmptyClaimQueryCode() throws Exception {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    HHAClaim claim = generateClaim();
    claim.setClaimQueryCode(Optional.of('3'));
    claim.setLastUpdated(Instant.now());

    ExplanationOfBenefit genEob =
        hhaClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);

    // We just want to make sure it is set
    Extension extension =
        eob.getBillablePeriod()
            .getExtensionByUrl("https://bluebutton.cms.gov/resources/variables/claim_query_cd");
    assertNotNull(extension);
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
                "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
                "3",
                "Home Health Agency (HHA)"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected number of care team entries. */
  @Test
  public void shouldHaveCareTeamList() {
    assertEquals(2, eob.getCareTeam().size());
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
            "345345345",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "performing",
            "Performing provider",
            "207ZH0000X",
            "Hematology (Pathology) Physician");

    assertTrue(compare2.equalsDeep(member2));
  }

  /** Tests that the transformer sets the expected number of supporting info entries. */
  @Test
  public void shouldHaveSupportingInfoList() {
    assertEquals(9, eob.getSupportingInfo().size());
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
            .setTiming(new DateType("2015-11-06"));

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
                "https://bluebutton.cms.gov/resources/variables/ptnt_dschrg_stus_cd",
                "30",
                "Still patient."));

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

  /** Tests that the transformer sets the expected claim PPS indicator code supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /**
   * Tests that the transformer sets the expected low utilization payment adjustment supporting
   * info.
   */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected claim HHA referral code supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected claim HHA total visit count supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected claim HHA total visit count supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected number of diagnosis. */
  @Test
  public void shouldHaveDiagnosesList() {
    assertEquals(4, eob.getDiagnosis().size());
  }

  /** Tests that the transformer sets the expected diagnosis entries. */
  @Test
  public void shouldHaveDiagnosesMembers() {
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("53081", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            List.of(new Coding("http://hl7.org/fhir/sid/icd-9-cm", "53081", "ESOPHAGEAL REFLUX")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype", "principal", "principal"),
            null,
            null);

    assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("B01", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B01", "VARICELLA [CHICKENPOX]"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B01", "VARICELLA [CHICKENPOX]")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("B05", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B05", "MEASLES"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B05", "MEASLES")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("B30", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B30", "VIRAL CONJUNCTIVITIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B30", "VIRAL CONJUNCTIVITIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    assertTrue(cmp4.equalsDeep(diag4));
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
    assertEquals(1, eob.getItemFirstRep().getSequence());
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
    Coding code2 =
        revenue.getCoding().stream()
            .filter(
                coding -> coding.getSystem().equals("https://www.nubc.org/CodeSystem/RevenueCodes"))
            .findFirst()
            .orElse(null);
    Coding code3 =
        revenue.getCoding().stream()
            .filter(
                coding ->
                    coding
                        .getSystem()
                        .equals(
                            "https://bluebutton.cms.gov/resources/variables/rev_cntr_ddctbl_coinsrnc_cd"))
            .findFirst()
            .orElse(null);

    assertNotNull(code1, "Missing expected rev_cntr coding");
    assertEquals("0023", code1.getCode());
    assertEquals(
        "Home Health services paid under PPS submitted as TOB 32X and 33X, effective 10/00. This code may appear multiple times on a claim to identify different HIPPS/Home Health Resource Groups (HRG).",
        code1.getDisplay());

    assertNotNull(code2, "Missing expected RevenueCodes coding");
    assertEquals("4", code2.getCode());
    assertNull(code2.getDisplay());

    assertNotNull(code3, "Missing expected rev_cntr_ddctbl_coinsrnc_cd coding");
    assertEquals("4", code3.getCode());
    assertEquals(
        "No charge or units associated with this revenue center code. (For multiple HCPCS per single revenue center code) For revenue center code 0001, the following MSP override values may be present:",
        code3.getDisplay());
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
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "2GGGG", null)));

    assertTrue(compare.equalsDeep(pos));
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
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "KO", null)));

    assertTrue(compare.equalsDeep(modifier));
  }

  /** Tests that the transformer sets the expected line item serviced date. */
  @Test
  public void shouldHaveLineItemServicedDate() {
    DateType servicedDate = eob.getItemFirstRep().getServicedDateType();

    DateType compare = new DateType("2015-06-23");

    assertEquals(servicedDate.toString(), compare.toString());
  }

  /** Tests that the transformer sets the expected line location (address). */
  @Test
  public void shouldHaveLineItemLocation() {
    Address address = eob.getItemFirstRep().getLocationAddress();

    Address compare = new Address().setState("UT");

    assertTrue(compare.equalsDeep(address));
  }

  /** Tests that the transformer sets the expected line item quantity. */
  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();

    Quantity compare = new Quantity(666);

    assertTrue(compare.equalsDeep(quantity));
  }

  /** Tests that the transformer sets the expected number of line item adjudications. */
  @Test
  public void shouldHaveLineItemAdjudications() {
    assertEquals(5, eob.getItemFirstRep().getAdjudication().size());
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
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected adjudication revenue center to charge amount. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected adjudication revenue center non-covered charge
   * amount.
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected adjudication revenue center payment amount. */
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
                new Money().setValue(199.99).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(total));
  }

  /** Tests that the transformer sets the expected payment value. */
  @Test
  public void shouldHavePayment() {
    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(188.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(eob.getPayment()));
  }

  /** Tests that the transformer sets the expected number of total entries. */
  @Test
  public void shouldHaveTotal() {
    assertEquals(1, eob.getTotal().size());
  }

  /**
   * Ensures the rev_cntr_unit_cnt is correctly mapped to an eob item as an extension when the unit
   * quantity is not zero.
   */
  @Test
  public void shouldHaveRevenueCenterUnit() {
    TransformerTestUtilsV2.assertExtensionQuantityEquals(
        CcwCodebookMissingVariable.REV_CNTR_UNIT_CNT, BigDecimal.valueOf(1), eob.getItem());
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
    assertEquals(1, eob.getBenefitBalanceFirstRep().getFinancial().size());
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
    assertEquals("15999", ((Coding) fiNumExtension.getValue()).getCode());
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
            new DateType("2015-10-30"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Should have organization with correct fake display. */
  @Test
  public void shouldHaveOrganizationWithFakeDisplay() {
    Optional<Resource> organization =
        eob.getContained().stream()
            .filter(o -> o.getResourceType().equals(ResourceType.Organization))
            .findFirst();

    Organization org = (Organization) organization.get();
    Optional<Identifier> identifier =
        org.getIdentifier().stream()
            .filter(i -> i.getValue().equals(RDATestUtils.FAKE_NPI_NUMBER))
            .findFirst();
    assertEquals(RDATestUtils.FAKE_NPI_NUMBER, identifier.get().getValue());
    assertEquals(RDATestUtils.FAKE_NPI_ORG_NAME, org.getName());
  }
}
