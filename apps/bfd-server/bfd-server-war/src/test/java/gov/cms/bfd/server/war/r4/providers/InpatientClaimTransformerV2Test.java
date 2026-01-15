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
import ca.uhn.fhir.parser.IParser;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookMissingVariable;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
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
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
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
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link InpatientClaimTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class InpatientClaimTransformerV2Test {
  /** The parsed claim used to generate the EOB and for validating with. */
  InpatientClaim claim;

  /** The EOB under test created from the {@link #claim}. */
  ExplanationOfBenefit eob;

  /** The transformer under test. */
  InpatientClaimTransformerV2 inpatientClaimTransformer;

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

  Set<String> securityTags = new HashSet<>();

  /**
   * Generates the Claim object to be used in multiple tests.
   *
   * @return the claim object
   * @throws FHIRException if there was an issue creating the claim
   */
  public InpatientClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(InpatientClaim.class::cast)
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

    inpatientClaimTransformer =
        new InpatientClaimTransformerV2(metricRegistry, securityTagManager, false);
    claim = generateClaim();
    ExplanationOfBenefit genEob =
        inpatientClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
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
    String expectedTimerName = inpatientClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals("inpatient-" + claim.getClaimId(), eob.getIdElement().getIdPart());
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
        (new SimpleDateFormat("yyy-MM-dd")).parse("2016-01-15"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2016-01-27"), eob.getBillablePeriod().getEnd());
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
            "161999999",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "attending",
            "Attending");

    assertTrue(compare1.equalsDeep(member1));

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());
    CareTeamComponent compare2 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            2,
            "3333444555",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "operating",
            "Operating");

    assertTrue(compare2.equalsDeep(member2));

    // Third member
    CareTeamComponent member3 = TransformerTestUtilsV2.findCareTeamBySequence(3, eob.getCareTeam());
    CareTeamComponent compare3 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            3,
            "161943433",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "otheroperating",
            "Other Operating");

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
    assertEquals(11, eob.getSupportingInfo().size());
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
  public void shouldHaveAdmissionPeriodSupInfo() {
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
                        "Admission Period")))
            .setTiming(
                // Period
                new Period()
                    .setStartElement(new DateTimeType("2016-01-15"))
                    .setEndElement(new DateTimeType("2016-01-27")));

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
                "1",
                "Emergency - The patient required immediate medical intervention as a result of"
                    + " severe, life threatening, or potentially disabling conditions. Generally,"
                    + " the patient was admitted through the emergency room."));

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

  /** Tests that the transformer sets the expected diagnosis related group (MS-DRG) codes. */
  @Test
  public void shouldHaveClmDrgCdInfo() throws IOException {
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
            new Coding("https://bluebutton.cms.gov/resources/variables/clm_drg_cd", "695", null));
    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests to make sure a four digit DiagnosisRelatedGroupCd exists for inpatient claims. */
  @Test
  public void shouldHaveFourCharacterClmDrgCdInfo() throws IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_A_FOUR_CHARACTER_DRG_CODE.getResources()));

    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(InpatientClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());

    ExplanationOfBenefit genEob =
        inpatientClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
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
            new Coding("https://bluebutton.cms.gov/resources/variables/clm_drg_cd", "6955", null));
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

  /** Tests that the transformer sets the expected blood pints furnished supporting info. */
  @Test
  public void shouldHaveNchBloodPntsFrnshedQtyInfo() {
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
            // Quantity
            .setValue(
                new Quantity()
                    .setValue(19)
                    .setSystem(TransformerConstants.CODING_SYSTEM_UCUM)
                    .setCode(TransformerConstants.CODING_SYSTEM_UCUM_PINT_CODE)
                    .setUnit(TransformerConstants.CODING_SYSTEM_UCUM_PINT_DISPLAY));

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
                "51",
                "Discharged/transferred to a Hospice – medical facility."));

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
            .setTiming(new DateType("2016-02-26"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected local organization reference. */
  @Test
  public void shouldHaveLocalOrganizationReference() {
    assertNotNull(eob.getProvider());
    assertEquals("#provider-org", eob.getProvider().getReference());
  }

  /** Tests that the transformer sets the expected number of extensions for this claim type. */
  @Test
  public void shouldHaveKnownExtensions() {
    assertEquals(10, eob.getExtension().size());
  }

  /** Tests that the transformer sets the expected "near line" extensions. */
  @Test
  public void shouldContainNchNearLineRecIdentCdExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
                "V",
                "Part A institutional claim record (inpatient [IP], skilled nursing facility"
                    + " [SNF], hospice [HOS], or home health agency [HHA])"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected IME claim value amount extension. */
  @Test
  public void shouldContainImeOpClmValAmtExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/ime_op_clm_val_amt",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/ime_op_clm_val_amt",
            new Money().setValue(66125.51).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected DSH claim value amount extension. */
  @Test
  public void shouldContainDshOpClmValAmtExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/dsh_op_clm_val_amt",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/dsh_op_clm_val_amt",
            new Money().setValue(25).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected covered works compensation extension. */
  @Test
  public void shouldContainClmMdcrNonPmtRsnCdExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
                "A",
                "Covered worker's compensation (Obsolete)"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected clm_srvc_clsfctn_type_cd extension. */
  @Test
  public void shouldContainClmSrvcClsfctnTypeCdExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
                "1",
                null));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected number of identifiers. */
  @Test
  public void shouldHaveKnownIdentifiers() {
    assertEquals(2, eob.getIdentifier().size());
  }

  /** Tests that the transformer sets the expected claim id identifier. */
  @Test
  public void shouldIncludeClaimIdIdentifier() {
    Identifier clmId =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "https://bluebutton.cms.gov/resources/variables/clm_id", eob.getIdentifier());

    Identifier compare =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/variables/clm_id",
            "333333222222",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
            "uc",
            "Unique Claim ID");

    assertTrue(compare.equalsDeep(clmId));
  }

  /** Tests that the transformer sets the expected claim group identifier. */
  @Test
  public void shouldIncludeClaimGroupIdentifier() {
    Identifier clmGrp =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "https://bluebutton.cms.gov/resources/identifier/claim-group", eob.getIdentifier());

    Identifier compare =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/identifier/claim-group",
            "900",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
            "uc",
            "Unique Claim ID");

    assertTrue(compare.equalsDeep(clmGrp));
  }

  /** Tests that the transformer sets the expected number of diagnosis. */
  @Test
  public void shouldHaveDiagnosesList() {
    assertEquals(8, eob.getDiagnosis().size());
  }

  /** Tests that the transformer sets the expected diagnosis entries. */
  @Test
  public void shouldHaveDiagnosesMembers() {
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("A37", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A37", "WHOOPING COUGH"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A37", "WHOOPING COUGH")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                "admitting",
                "Admitting Diagnosis"),
            null,
            null);

    assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("A40", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A40", "STREPTOCOCCAL SEPSIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A40", "STREPTOCOCCAL SEPSIS")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                "principal",
                "Principal Diagnosis"),
            1,
            "Y");

    assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("A52", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A52", "LATE SYPHILIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A52", "LATE SYPHILIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            2,
            "N");

    assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("A06", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A06", "AMEBIASIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A06", "AMEBIASIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            3,
            "N");

    assertTrue(cmp4.equalsDeep(diag4));

    DiagnosisComponent diag5 =
        TransformerTestUtilsV2.findDiagnosisByCode("A15", eob.getDiagnosis());

    DiagnosisComponent cmp5 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag5.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A15", "RESPIRATORY TUBERCULOSIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A15", "RESPIRATORY TUBERCULOSIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            4,
            "N");

    assertTrue(cmp5.equalsDeep(diag5));

    DiagnosisComponent diag6 =
        TransformerTestUtilsV2.findDiagnosisByCode("B01", eob.getDiagnosis());

    DiagnosisComponent cmp6 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag6.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B01", "VARICELLA [CHICKENPOX]"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B01", "VARICELLA [CHICKENPOX]")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            5,
            "N");

    assertTrue(cmp6.equalsDeep(diag6));

    DiagnosisComponent diag7 =
        TransformerTestUtilsV2.findDiagnosisByCode("A01", eob.getDiagnosis());

    DiagnosisComponent cmp7 =
        TransformerTestUtilsV2.createExDiagnosis(
            // Order doesn't matter
            diag7.getSequence(),
            List.of(
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10-cm", "A01", "TYPHOID AND PARATYPHOID FEVERS"),
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10", "A01", "TYPHOID AND PARATYPHOID FEVERS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            1,
            "N");

    assertTrue(cmp7.equalsDeep(diag7));

    DiagnosisComponent diag8 =
        TransformerTestUtilsV2.findDiagnosisByCode("A02", eob.getDiagnosis());

    DiagnosisComponent cmp8 =
        TransformerTestUtilsV2.createExDiagnosis(
            // Order doesn't matter
            diag8.getSequence(),
            List.of(
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10-cm", "A02", "OTHER SALMONELLA INFECTIONS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A02", "OTHER SALMONELLA INFECTIONS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            2,
            "Y");

    assertTrue(cmp8.equalsDeep(diag8));
  }

  /** Tests that the transformer sets the expected number of procedures. */
  @Test
  public void shouldHaveProcedureList() {
    assertEquals(6, eob.getProcedure().size());
  }

  /** Tests that the transformer sets the expected procedure entries. */
  @Test
  public void shouldHaveProcedureMembers() {
    ProcedureComponent proc1 =
        TransformerTestUtilsV2.findProcedureByCode("BQ0HZZZ", eob.getProcedure());

    ProcedureComponent cmp1 =
        TransformerTestUtilsV2.createProcedure(
            proc1.getSequence(),
            List.of(
                new Coding(
                    "http://www.cms.gov/Medicare/Coding/ICD10",
                    "BQ0HZZZ",
                    "PLAIN RADIOGRAPHY OF LEFT ANKLE"),
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10",
                    "BQ0HZZZ",
                    "PLAIN RADIOGRAPHY OF LEFT ANKLE")),
            "2016-01-16T00:00:00Z");

    assertTrue(cmp1.equalsDeep(proc1), "Comparing Procedure code BQ0HZZZ");

    ProcedureComponent proc2 =
        TransformerTestUtilsV2.findProcedureByCode("CD1YYZZ", eob.getProcedure());

    ProcedureComponent cmp2 =
        TransformerTestUtilsV2.createProcedure(
            proc2.getSequence(),
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

    assertTrue(cmp2.equalsDeep(proc2), "Comparing Procedure code CD1YYZZ");

    ProcedureComponent proc3 =
        TransformerTestUtilsV2.findProcedureByCode("2W52X6Z", eob.getProcedure());

    ProcedureComponent cmp3 =
        TransformerTestUtilsV2.createProcedure(
            proc3.getSequence(),
            List.of(
                new Coding(
                    "http://www.cms.gov/Medicare/Coding/ICD10",
                    "2W52X6Z",
                    "REMOVAL OF PRESSURE DRESSING ON NECK"),
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10",
                    "2W52X6Z",
                    "REMOVAL OF PRESSURE DRESSING ON NECK")),
            "2016-01-15T00:00:00Z");

    assertTrue(cmp3.equalsDeep(proc3), "Comparing Procedure code 2W52X6Z");

    ProcedureComponent proc4 =
        TransformerTestUtilsV2.findProcedureByCode("BP17ZZZ", eob.getProcedure());

    ProcedureComponent cmp4 =
        TransformerTestUtilsV2.createProcedure(
            proc4.getSequence(),
            List.of(
                new Coding(
                    "http://www.cms.gov/Medicare/Coding/ICD10",
                    "BP17ZZZ",
                    "FLUOROSCOPY OF LEFT SCAPULA"),
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10", "BP17ZZZ", "FLUOROSCOPY OF LEFT SCAPULA")),
            "2016-01-17T00:00:00Z");

    assertTrue(cmp4.equalsDeep(proc4), "Comparing Procedure code BP17ZZZ");

    ProcedureComponent proc5 =
        TransformerTestUtilsV2.findProcedureByCode("D9YD8ZZ", eob.getProcedure());

    ProcedureComponent cmp5 =
        TransformerTestUtilsV2.createProcedure(
            proc5.getSequence(),
            List.of(
                new Coding(
                    "http://www.cms.gov/Medicare/Coding/ICD10",
                    "D9YD8ZZ",
                    "HYPERTHERMIA OF NASOPHARYNX"),
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10", "D9YD8ZZ", "HYPERTHERMIA OF NASOPHARYNX")),
            "2016-01-24T00:00:00Z");

    assertTrue(cmp5.equalsDeep(proc5), "Comparing Procedure code D9YD8ZZ");

    ProcedureComponent proc6 =
        TransformerTestUtilsV2.findProcedureByCode("F00ZCKZ", eob.getProcedure());

    ProcedureComponent cmp6 =
        TransformerTestUtilsV2.createProcedure(
            proc6.getSequence(),
            List.of(
                new Coding(
                    "http://www.cms.gov/Medicare/Coding/ICD10",
                    "F00ZCKZ",
                    "APHASIA ASSESSMENT USING AUDIOVISUAL EQUIPMENT"),
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10",
                    "F00ZCKZ",
                    "APHASIA ASSESSMENT USING AUDIOVISUAL EQUIPMENT")),
            "2016-01-24T00:00:00Z");

    assertTrue(cmp6.equalsDeep(proc6), "Comparing Procedure code F00ZCKZ");
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
            .setCoverage(new Reference().setReference("Coverage/part-a-567834"));

    assertTrue(compare.equalsDeep(insurance));
  }

  /** Tests that the transformer sets the expected number of codings. */
  @Test
  public void shouldHaveExpectedTypeCoding() {
    assertEquals(3, eob.getType().getCoding().size());
  }

  /** Tests that the transformer sets the expected coding entities. */
  @Test
  public void shouldHaveExpectedCodingValues() {
    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/nch_clm_type_cd",
                        "60",
                        "Inpatient claim"),
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/eob-type",
                        "INPATIENT",
                        null),
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/claim-type",
                        "institutional",
                        "Institutional")));

    assertTrue(compare.equalsDeep(eob.getType()));
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
                        "https://bluebutton.cms.gov/resources/variables/rev_cntr", "6767", null),
                    new Coding("https://www.nubc.org/CodeSystem/RevenueCodes", "A", null),
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/rev_cntr_ddctbl_coinsrnc_cd",
                        "A",
                        null)));

    assertTrue(compare.equalsDeep(revenue));
  }

  /** Tests that the transformer sets the expected line item product/service. */
  @Test
  public void shouldHaveDataAbsentLineItemProductOrService() {
    CodeableConcept pos = eob.getItemFirstRep().getProductOrService();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        TransformerConstants.CODING_DATA_ABSENT,
                        TransformerConstants.DATA_ABSENT_REASON_NULL_CODE,
                        TransformerConstants.DATA_ABSENT_REASON_DISPLAY)));

    assertTrue(compare.equalsDeep(pos));
  }

  /**
   * Tests that the transformer sets the expected number of line item modifiers and the entries are
   * correct.
   */
  @Test
  public void shouldHaveLineItemModifier() {
    assertEquals(1, eob.getItemFirstRep().getModifier().size());

    CodeableConcept modifier = eob.getItemFirstRep().getModifierFirstRep();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(Arrays.asList(new Coding("http://unitsofmeasure.org", "GG", null)));

    assertTrue(compare.equalsDeep(modifier));
  }

  /** Tests that the transformer sets the expected line location (address). */
  @Test
  public void shouldHaveLineItemLocation() {
    Address address = eob.getItemFirstRep().getLocationAddress();

    Address compare = new Address().setState("IA");

    assertTrue(compare.equalsDeep(address));
  }

  /** Tests that the transformer sets the expected number of line item adjudications. */
  @Test
  public void shouldHaveLineItemAdjudications() {
    assertEquals(3, eob.getItemFirstRep().getAdjudication().size());
  }

  /** Tests that the transformer sets the expected revenue center rate amount. */
  @Test
  public void shouldHaveLineItemRevCentrRateAmtAdjudication() {
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

  /** Tests that the transformer sets the expected revenue center total charge amount. */
  @Test
  public void shouldHaveLineItemRevCntrTotChrgAmtAdjudication() {
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
                new Money().setValue(84888.88).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected adjudication revenue center non-covered charge
   * amount.
   */
  @Test
  public void shouldHaveLineItemRevCentrNcvrdChrgAmtAdjudication() {
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
                    // Because of the .00, this has to be specified as a string or it will only have
                    // .0 and fail
                    .setValueElement(new DecimalType("3699.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected number of total entries. */
  @Test
  public void shouldHaveTotal() {
    assertEquals(1, eob.getTotal().size());
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
                new Money().setValue(84999.37).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(total));
  }

  /** Tests that the transformer sets the expected payment value. */
  @Test
  public void shouldHavePayment() {
    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(
                new Money().setValue(7699.48).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(eob.getPayment()));
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
    assertEquals(21, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  /** Tests that the transformer sets the expected pass thru per diem amount code. */
  @Test
  public void shouldHaveClmPassThruPerDiemAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pass_thru_per_diem_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pass_thru_per_diem_amt",
                                "Claim Pass Thru Per Diem Amount"))))
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("10.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected professional component charge amount code. */
  @Test
  public void shouldHaveNonProfnlCmpntChrgAmtFinancial() {
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
            .setUsed(
                new Money()
                    .setValueElement(new DecimalType("4.00"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected claim total PPS capital amount code. */
  @Test
  public void shouldHaveClmTotPpsCptlAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_tot_pps_cptl_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_tot_pps_cptl_amt",
                                "Claim Total PPS Capital Amount"))))
            .setUsed(
                new Money().setValue(646.23).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected beneficiary total coinsurance days count code. */
  @Test
  public void shouldHaveTotCoinsrncDaysCntFinancial() {
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
            .setUsed(new UnsignedIntType(0));

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
            .setUsed(new UnsignedIntType(0));

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
                                "NCH Inpatient (or other Part A) Total Deductible/Coinsurance"
                                    + " Amount"))))
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
            .setUsed(new Money().setValue(25.09).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
            .setUsed(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
                new Money().setValue(552.56).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
            .setUsed(new Money().setValue(68.58).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
            .setUsed(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected PPS old capital hold harmless amount code. */
  @Test
  public void shouldHaveClmPpsOldCptlHldHrmlsAmtFinancial() {
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
            .setUsed(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH DRG outlier approved payment amount code. */
  @Test
  public void shouldHaveNchDrgOutlierAprvdPmtAmtFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/nch_drg_outlier_aprvd_pmt_amt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/nch_drg_outlier_aprvd_pmt_amt",
                                "NCH DRG Outlier Approved Payment Amount"))))
            .setUsed(new Money().setValue(23.99).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected NCH beneficiary blood deductible liability amount
   * code.
   */
  @Test
  public void shouldHaveNchBeneBloodDdctlbLbltyAmtFinancial() {
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
  public void shouldHavePrPayAmtFinancial() {
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
            .setUsed(new UnsignedIntType(12));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected PPS capital DRG weight number code. */
  @Test
  public void shouldHaveClmPpsCptlDrgWtNumFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_drg_wt_num",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/clm_pps_cptl_drg_wt_num",
                                "Claim PPS Capital DRG Weight Number"))))
            .setUsed(new UnsignedIntType(1));

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected beneficiary medicare lifetime reserve days (LRD)
   * used count code.
   */
  @Test
  public void shouldHaveBeneLrdUsedCntFinancial() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/bene_lrd_used_cnt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    BenefitComponent compare =
        new BenefitComponent()
            .setType(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/benefit-balance",
                                "https://bluebutton.cms.gov/resources/variables/bene_lrd_used_cnt",
                                "Beneficiary Medicare Lifetime Reserve Days (LRD) Used Count"))))
            .setUsed(new UnsignedIntType(0));

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected clm_uncompd_care_pmt_amt. */
  @Test
  public void shouldHaveClaimUncompensatedCarePmtAmt() {

    Optional<BigDecimal> expectedValue = claim.getClaimUncompensatedCareAmount();
    assertTrue(expectedValue.isPresent());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_uncompd_care_pmt_amt",
            eob.getExtension());

    Extension expectedExtension =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_uncompd_care_pmt_amt",
            new Money()
                .setValue(expectedValue.get())
                .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(expectedExtension.equalsDeep(ex));
  }

  /**
   * Ensures the rev_cntr_unit_cnt is correctly mapped to an eob item as an extension when the unit
   * quantity is not zero.
   *
   * <p>TODO: Is this correct? says non-0 but tests 0
   */
  @Test
  public void shouldHaveRevenueCenterUnit() {
    TransformerTestUtilsV2.assertExtensionQuantityEquals(
        CcwCodebookMissingVariable.REV_CNTR_UNIT_CNT, BigDecimal.valueOf(0), eob.getItem());
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
    assertEquals("8299", ((Coding) fiNumExtension.getValue()).getCode());
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
            new DateType("2016-02-19"));

    assertTrue(compare.equalsDeep(ex));
  }
}
