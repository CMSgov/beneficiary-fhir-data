package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.NPIOrgLookup;
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
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link SNFClaimTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SNFClaimTransformerV2Test {
  /** The parsed claim used to generate the EOB and for validating with. */
  SNFClaim claim;

  /** The EOB under test created from the {@link #claim}. */
  ExplanationOfBenefit eob;

  /** The transformer under test. */
  SNFClaimTransformerV2 snfClaimTransformer;

  /** The fhir context for parsing the test file. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** The metrics registry. */
  @Mock MetricRegistry metricRegistry;

  /** The SamhsaSecurityTag lookup. */
  @Mock SecurityTagManager securityTagManager;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  NPIOrgLookup npiOrgLookup;

  Set<String> securityTags = new HashSet<>();

  /**
   * Generates the Claim object to be used in multiple tests.
   *
   * @return the claim object
   * @throws FHIRException if there was an issue creating the claim
   */
  public SNFClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    SNFClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(SNFClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());
    // Mock HCPCS Code missing
    claim.getLines().getFirst().setHcpcsCode(Optional.empty());
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
    npiOrgLookup = RDATestUtils.createTestNpiOrgLookup();

    snfClaimTransformer = new SNFClaimTransformerV2(metricRegistry, securityTagManager, false);
    claim = generateClaim();
    ExplanationOfBenefit genEob =
        snfClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());

    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);
  }

  /** Releases the static mock NPIOrgLookup. */
  @AfterEach
  public void after() {}

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    String expectedTimerName = snfClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals("ExplanationOfBenefit/snf-" + claim.getClaimId(), eob.getId());
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
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_EOB_INPATIENT_PROFILE_URL)));
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
        (new SimpleDateFormat("yyy-MM-dd")).parse("2013-12-01"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2013-12-18"), eob.getBillablePeriod().getEnd());
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
                "2",
                "Skilled Nursing Facility (SNF)"));

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
            "4444444444",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "otheroperating",
            "Other Operating",
            "207R00000X",
            "Internal Medicine Physician");

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
    assertEquals(15, eob.getSupportingInfo().size());
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
            .setTiming(new DateType("2014-02-14"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected claim inpatient admission type codes. */
  @Test
  public void shouldHaveClmIpAdmsnTypeCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_ip_admsn_type_cd",
            eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                    "admtype",
                    "Information"),
                new Coding(
                    "https://bluebutton.cms.gov/resources/codesystem/information",
                    "https://bluebutton.cms.gov/resources/variables/clm_ip_admsn_type_cd",
                    "Claim Inpatient Admission Type Code")),
            // Code
            new Coding(
                "https://www.nubc.org/CodeSystem/PriorityTypeOfAdmitOrVisit",
                "3",
                "Elective - The patient's condition permitted adequate time to schedule the availability of suitable accommodations."));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected claim source admission type codes. */
  @Test
  public void shouldHaveClmSrcIpAdmsnCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_src_ip_admsn_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/clm_src_ip_admsn_cd",
                    "Claim Source Inpatient Admission Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_src_ip_admsn_cd", "4", null));

    assertTrue(compare.equalsDeep(sic));
  }

  /**
   * Tests that the transformer sets the expected NCH verified non-covered stay from date supporting
   * info.
   */
  @Test
  public void shouldHaveNchVrfdNcvrdStayFromDtSupInfo() throws Exception {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nch_vrfd_ncvrd_stay_from_dt",
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
                    "https://bluebutton.cms.gov/resources/variables/nch_vrfd_ncvrd_stay_from_dt",
                    "NCH Verified Non-covered Stay From Date")));
    // timingPeriod
    Period period = new Period();
    period.setStart(
        new SimpleDateFormat("yyy-MM-dd").parse("2002-01-11"), TemporalPrecisionEnum.DAY);
    period.setEnd(new SimpleDateFormat("yyy-MM-dd").parse("2002-01-21"), TemporalPrecisionEnum.DAY);

    compare.setTiming(period);

    assertTrue(compare.equalsDeep(sic));
  }

  /**
   * Tests that the transformer sets the expected NCH beneficiary medicare benefits exhausted date
   * supporting info.
   */
  @Test
  public void shouldHaveNchBeneMdcrBnftsExhtdDtISupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nch_bene_mdcr_bnfts_exhtd_dt_i",
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
                        "https://bluebutton.cms.gov/resources/variables/nch_bene_mdcr_bnfts_exhtd_dt_i",
                        "NCH Beneficiary Medicare Benefits Exhausted Date")))
            // timingDate
            .setTiming(new DateType("2002-01-31"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected diagnosis related group (MS-DRG) codes. */
  @Test
  public void shouldHaveClmDrgCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_drg_cd", eob.getSupportingInfo());

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
                    "https://bluebutton.cms.gov/resources/variables/clm_drg_cd",
                    "Claim Diagnosis Related Group Code (or MS-DRG Code)")),
            // Code
            new Coding("https://bluebutton.cms.gov/resources/variables/clm_drg_cd", "645", null));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests to make sure a four digit DiagnosisRelatedGroupCd exists for snf claims. */
  @Test
  public void shouldHaveFourCharacterDRGClmDrgCdSupInfo() throws IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_A_FOUR_CHARACTER_DRG_CODE.getResources()));

    SNFClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(SNFClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());
    ExplanationOfBenefit genEob =
        snfClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_drg_cd", eob.getSupportingInfo());

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
                    "https://bluebutton.cms.gov/resources/variables/clm_drg_cd",
                    "Claim Diagnosis Related Group Code (or MS-DRG Code)")),
            // Code
            new Coding("https://bluebutton.cms.gov/resources/variables/clm_drg_cd", "6455", null));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected NCH patient status indicator codes. */
  @Test
  public void shouldHaveNchPtntStusIndCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nch_ptnt_stus_ind_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/nch_ptnt_stus_ind_cd",
                    "NCH Patient Status Indicator Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/nch_ptnt_stus_ind_cd",
                "A",
                "Discharged"));

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
        new SimpleDateFormat("yyy-MM-dd").parse("2013-11-05"), TemporalPrecisionEnum.DAY);
    period.setEnd(new SimpleDateFormat("yyy-MM-dd").parse("2013-12-18"), TemporalPrecisionEnum.DAY);

    compare.setTiming(period);

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected NCH qualified stay from date supporting info. */
  @Test
  public void shouldHaveNchQlfydStayFromDtSupInfo() throws Exception {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nch_qlfyd_stay_from_dt",
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
                    "https://bluebutton.cms.gov/resources/variables/nch_qlfyd_stay_from_dt",
                    "NCH Qualified Stay From Date")));
    // timingPeriod
    Period period = new Period();
    period.setStart(
        new SimpleDateFormat("yyy-MM-dd").parse("2013-09-23"), TemporalPrecisionEnum.DAY);
    period.setEnd(new SimpleDateFormat("yyy-MM-dd").parse("2013-11-05"), TemporalPrecisionEnum.DAY);

    compare.setTiming(period);

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected claim PPS indicator code supporting info. */
  @Test
  public void shouldHaveClmPpsIndCdSupInfo() {
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

  /** Tests that the transformer sets the expected blood pints furnished supporting info. */
  @Test
  public void shouldHaveNchBloodPntsFrnshdQtySupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nch_blood_pnts_frnshd_qty",
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
                        "https://bluebutton.cms.gov/resources/variables/nch_blood_pnts_frnshd_qty",
                        "NCH Blood Pints Furnished Quantity")))
            // valueQuantity
            .setValue(
                new Quantity()
                    .setValue(19)
                    .setUnit("pint")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("[pt_us]"));

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
        TransformerTestUtilsV2.findDiagnosisByCode("6202", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            List.of(new Coding("http://hl7.org/fhir/sid/icd-9-cm", "6202", "OVARIAN CYST NEC/NOS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            null,
            null);

    assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("3736", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-9-cm", "3736", "PARASITIC INFEST EYELID")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                "principal",
                "Principal Diagnosis"),
            null,
            null);

    assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("V0182", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            List.of(new Coding("http://hl7.org/fhir/sid/icd-9-cm", "V0182", "EXPOSURE TO SARS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            null,
            null);

    assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("E9281", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            List.of(new Coding("http://hl7.org/fhir/sid/icd-9-cm", "E9281", "EXPOSURE TO NOISE")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            null,
            null);

    assertTrue(cmp4.equalsDeep(diag4));

    DiagnosisComponent diag5 =
        TransformerTestUtilsV2.findDiagnosisByCode("3310", eob.getDiagnosis());

    DiagnosisComponent cmp5 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag5.getSequence(),
            List.of(new Coding("http://hl7.org/fhir/sid/icd-9-cm", "3310", "ALZHEIMER'S DISEASE")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
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
        TransformerTestUtilsV2.findProcedureByCode("9214", eob.getProcedure());

    ProcedureComponent cmp1 =
        TransformerTestUtilsV2.createProcedure(
            proc1.getSequence(),
            List.of(new Coding("http://hl7.org/fhir/sid/icd-9-cm", "9214", "BONE SCAN")),
            "2016-01-16T00:00:00Z");

    assertTrue(cmp1.equalsDeep(proc1), "Comparing Procedure code 9214");
  }

  /**
   * Tests that the transformer sets the expected number of insurance entries with the expected
   * values.
   */
  @Test
  public void shouldReferenceCoverageInInsurance() {
    // Only one insurance object if there is more than we need to fix the focal set
    // to point to the
    // correct insurance
    assertEquals(false, eob.getInsurance().size() > 1);
    assertEquals(1, eob.getInsurance().size());

    InsuranceComponent insurance = eob.getInsuranceFirstRep();

    InsuranceComponent compare =
        new InsuranceComponent()
            .setFocal(true)
            .setCoverage(new Reference().setReference("Coverage/part-a-567834"));

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

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/rev_cntr", "22", null),
                    new Coding("https://www.nubc.org/CodeSystem/RevenueCodes", "A", null),
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/rev_cntr_ddctbl_coinsrnc_cd",
                        "A",
                        null)));

    assertTrue(compare.equalsDeep(revenue));
  }

  /** Tests that the transformer sets the expected Coding for line item produce/service. */
  @Test
  public void shouldHaveDataAbsentLineItemProductOrServiceCoding() {
    CodeableConcept pos = eob.getItemFirstRep().getProductOrService();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        TransformerConstants.CODING_DATA_ABSENT,
                        TransformerConstants.DATA_ABSENT_REASON_NULL_CODE,
                        null)));

    assertTrue(compare.equalsDeep(pos));
  }

  /** Tests that the transformer sets the expected line location (address). */
  @Test
  public void shouldHaveLineItemLocation() {
    Address address = eob.getItemFirstRep().getLocationAddress();

    Address compare = new Address().setState("FL");

    assertTrue(compare.equalsDeep(address));
  }

  /** Tests that the transformer sets the expected line item quantity. */
  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();
    Quantity compare = new Quantity().setValue(new BigDecimal("234.567"));

    assertTrue(compare.equalsDeep(quantity));
  }

  /** Tests that the transformer sets the expected number of line item adjudications. */
  @Test
  public void shouldHaveLineItemAdjudications() {
    assertEquals(3, eob.getItemFirstRep().getAdjudication().size());
  }

  /** Tests that the transformer sets the expected adjudication center rate amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRevCntrRateAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_rate_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(5.00);
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
                                "https://bluebutton.cms.gov/resources/variables/rev_cntr_rate_amt",
                                "Revenue Center Rate Amount"))))
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
    BigDecimal amt = new BigDecimal(95.00);
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
  public void shouldHaveLineItemAdjudicationRevCntrNcvrdChrgAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_ncvrd_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(88.00);
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
                new Money().setValue(5555.03).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(total));
  }

  /** Tests that the transformer sets the expected payment value. */
  @Test
  public void shouldHavePayment() {
    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(
                new Money().setValue(3333.33).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
    assertEquals(15, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  /** Tests that the transformer sets the expected medicare utilization day count. */
  @Test
  public void shouldHaveClmUtlztnDayCntFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_utlztn_day_cnt",
            eob.getBenefitBalanceFirstRep().getFinancial());

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
            .setUsed(new UnsignedIntType().setValue(17));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected beneficiary total coinsurance days count code. */
  @Test
  public void shouldHaveBeneTotCoinsrncDaysCntFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/bene_tot_coinsrnc_days_cnt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/bene_tot_coinsrnc_days_cnt",
                                "Beneficiary Total Coinsurance Days Count"))))
            .setUsed(new UnsignedIntType().setValue(17));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected medicare non utilization days count code. */
  @Test
  public void shouldHaveClmNonUtlztnDaysCntFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_non_utlztn_days_cnt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_non_utlztn_days_cnt",
                                "Claim Medicare Non Utilization Days Count"))))
            .setUsed(new UnsignedIntType().setValue(0));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected NCH beneficiary inpatient deductible amount code.
   */
  @Test
  public void shouldHaveNchBeneIpDdctblAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_bene_ip_ddctbl_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_bene_ip_ddctbl_amt",
                                "NCH Beneficiary Inpatient (or other Part A) Deductible Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("112.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected NCH beneficiary part A coinsurance liability
   * amount code.
   */
  @Test
  public void shouldHaveNchBenePtaCoinsrncLbltyAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_bene_pta_coinsrnc_lblty_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_bene_pta_coinsrnc_lblty_amt",
                                "NCH Beneficiary Part A Coinsurance Liability Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("5.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH inpatient non-covered charge amount code. */
  @Test
  public void shouldHaveNchIpNcvrdChrgAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_ip_ncvrd_chrg_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_ip_ncvrd_chrg_amt",
                                "NCH Inpatient(or other Part A) Non-covered Charge Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("33.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH inpatient total deductible amount code. */
  @Test
  public void shouldHaveNchIpTotDdctnAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_ip_tot_ddctn_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_ip_tot_ddctn_amt",
                                "NCH Inpatient (or other Part A) Total Deductible/Coinsurance Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("14.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected PPS capital disproportionate share amount code.
   */
  @Test
  public void shouldHaveClmPpsCptlDsprprtntShrAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_dsprprtnt_shr_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_dsprprtnt_shr_amt",
                                "Claim PPS Capital Disproportionate Share Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("7.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected PPS capital exception amount code. */
  @Test
  public void shouldHaveClmPpsCptlExcptnAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_excptn_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_excptn_amt",
                                "Claim PPS Capital Exception Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("5.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected PPS capital federal specific portion amount code.
   */
  @Test
  public void shouldHaveClmPpsCptlFspAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_fsp_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_fsp_amt",
                                "Claim PPS Capital Federal Specific Portion (FSP) Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("9.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected PPS capital indirect medical education amount
   * code.
   */
  @Test
  public void shouldHaveClmPpsCptlImeAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_ime_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_ime_amt",
                                "Claim PPS Capital Indirect Medical Education (IME) Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("6.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected PPS capital outlier amount code. */
  @Test
  public void shouldHaveClmPpsCptlOutlierAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_outlier_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_outlier_amt",
                                "Claim PPS Capital Outlier Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("8.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected PPS old capital hold harmless amount code. */
  @Test
  public void shouldHaveClmPpsCptlHldHrmlsAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_old_cptl_hld_hrmls_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pps_old_cptl_hld_hrmls_amt",
                                "Claim PPS Old Capital Hold Harmless Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("4.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected NCH beneficiary blood deductible liability amount
   * code.
   */
  @Test
  public void shouldHaveNchBeneBloodDdcblLbltyAmtFinancial() {
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
  public void shouldHavePrpayamtFinancial() {
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
   * Ensures the rev_cntr_unit_cnt is correctly mapped to an eob item as an extension when the unit
   * quantity is not zero.
   */
  @Test
  public void shouldHaveRevenueCenterUnit() {
    TransformerTestUtilsV2.assertExtensionQuantityEquals(
        CcwCodebookMissingVariable.REV_CNTR_UNIT_CNT, BigDecimal.valueOf(477), eob.getItem());
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
    assertEquals("11111", ((Coding) fiNumExtension.getValue()).getCode());
  }

  /**
   * Ensures the fiClmActnCd is correctly mapped to an eob as an extension when the
   * fiscalIntermediaryClaimActionCode is present.
   */
  @Test
  public void shouldHaveFiClaimActionCdExtension() {

    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_clm_actn_cd";

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiClmActCdExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNotNull(fiClmActCdExtension);
    assertEquals("1", ((Coding) fiClmActCdExtension.getValue()).getCode());
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
            new DateType("2014-02-07"));

    assertTrue(compare.equalsDeep(ex));
  }
}
