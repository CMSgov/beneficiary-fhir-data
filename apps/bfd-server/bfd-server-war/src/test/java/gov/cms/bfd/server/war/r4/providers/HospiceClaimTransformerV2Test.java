package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.hl7.fhir.r4.model.ExplanationOfBenefit.InsuranceComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.PaymentComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link HospiceClaimTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class HospiceClaimTransformerV2Test {
  /** The EOB under test created from the {@link #claim}. */
  ExplanationOfBenefit eob = null;

  /** The parsed claim used to generate the EOB and for validating with. */
  HospiceClaim claim = null;

  /** The transformer under test. */
  HospiceClaimTransformerV2 hospiceClaimTransformer;

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
   * @throws FHIRException if there was an issue creating the claim
   */
  @BeforeEach
  public void generateClaim() throws FHIRException, IOException {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    hospiceClaimTransformer =
        new HospiceClaimTransformerV2(metricRegistry, securityTagManager, false);
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(HospiceClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());
    createEOB();
  }

  /** Creates an eob for the test. */
  private void createEOB() {
    ExplanationOfBenefit genEob =
        hospiceClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags), false);
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
    String expectedTimerName = hospiceClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link SNFClaimTransformerV2#transform} works as expected when run against the
   * {@link StaticRifResource#SAMPLE_A_SNF} {@link SNFClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    assertMatches(
        claim,
        hospiceClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags), false));
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetId() {
    assertEquals("ExplanationOfBenefit/hospice-" + claim.getClaimId(), eob.getId());
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

  /** Tests that the transformer sets the expected outcome. */
  @Test
  public void shouldSetOutcomeStatus() {
    assertEquals(ExplanationOfBenefit.RemittanceOutcome.COMPLETE, eob.getOutcome());
  }

  /**
   * Tests that the transformer sets the expected number of provider references and correct value.
   */
  @Test
  public void shouldHaveProviderReference() {
    List<Resource> containEntries = eob.getContained();
    assertEquals(1, containEntries.size());
    assertNotNull(eob.getProvider());
    assertEquals("#provider-org", eob.getProvider().getReference());
  }

  /** Tests that the transformer sets the correct values for the contained organization. */
  @Test
  public void shouldHaveOrganizationContainedEntry() {
    Optional<Resource> resource =
        eob.getContained().stream().filter(r -> r.getId().equals("provider-org")).findFirst();
    assertTrue(resource.isPresent());

    Organization actualEobContainedOrganizationResource = (Organization) resource.get();
    assertEquals(RDATestUtils.FAKE_NPI_ORG_NAME, actualEobContainedOrganizationResource.getName());
    assertTrue(actualEobContainedOrganizationResource.hasActive());
    assertTrue(
        actualEobContainedOrganizationResource.getMeta().getProfile().stream()
            .filter(p -> p.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL))
            .findAny()
            .isPresent());
  }

  /** Tests that the transformer sets the expected patient reference. */
  @Test
  public void shouldHavePatientReference() {
    assertNotNull(eob.getPatient());
    assertEquals("Patient/567834", eob.getPatient().getReference());
  }

  /** Tests that the transformer sets the expected creation date. */
  @Test
  public void shouldHaveCreatedDate() {
    assertNotNull(eob.getCreated());
  }

  /**
   * Tests that the transformer sets the expected number of contained identifiers and they have the
   * correct values.
   */
  @Test
  public void shouldHaveContainedIdentifier() {
    List<Resource> actuals = eob.getContained();

    assertEquals(1, actuals.size());
    Organization org = (Organization) actuals.get(0);
    List<Identifier> expected = org.getIdentifier();
    assertEquals(2, expected.size());

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
        .setValue("0000000000")
        .setSystem("http://hl7.org/fhir/sid/us-npi")
        .getType()
        .addCoding()
        .setCode("npi")
        .setSystem("http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType");

    compare.add(ident);

    for (int i = 0; i < compare.size(); i++) {
      assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
  }

  /** Tests that the transformer sets the expected identifiers. */
  @Test
  public void shouldHaveIdentifiers() {
    List<Identifier> expected = eob.getIdentifier();
    assertEquals(2, expected.size());

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
      assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
  }

  /**
   * Tests that the transformer sets the expected number of extensions and they have the correct
   * values.
   */
  @Test
  public void shouldHaveExtensions() {
    List<Extension> expected = eob.getExtension();
    assertEquals(7, expected.size());

    assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntl_num",
            eob.getExtension()));

    assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt", eob.getExtension()));

    assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            eob.getExtension()));

    assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
            eob.getExtension()));

    assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
            eob.getExtension()));

    assertNotNull(
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/bene_hospc_prd_cnt",
            eob.getExtension()));

    Extension hospiceCountExtension =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/bene_hospc_prd_cnt",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/bene_hospc_prd_cnt", null, null));
    hospiceCountExtension.setValue(new Quantity(2));

    List<Extension> compare =
        List.of(
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
                    "V",
                    "Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice [HOS], or home health agency [HHA])")),
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntl_num",
                new Identifier()
                    .setSystem("https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntl_num")
                    .setValue("2718813985998")),
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt",
                new DateType("2014-10-07")),
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
                    null)),
            new Extension(
                "https://bluebutton.cms.gov/resources/variables/fi_num",
                new Coding("https://bluebutton.cms.gov/resources/variables/fi_num", "6666", null)),
            hospiceCountExtension);

    for (int i = 0; i < expected.size(); i++) {
      assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
  }

  /**
   * Tests that the transformer sets the expected number of type codings and they have the correct
   * values.
   */
  @Test
  public void shouldHaveTypeCodings() {
    CodeableConcept cc = eob.getType();
    assertNotNull(cc);
    List<Coding> expected = cc.getCoding();
    assertEquals(3, expected.size());

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
      assertTrue(compare.get(i).equalsDeep(expected.get(i)));
    }
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
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-01-01"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-01-30"), eob.getBillablePeriod().getEnd());
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

    HospiceClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(Instant.now());
    claim.setClaimQueryCode(Optional.empty());
    claim.setLastUpdated(Instant.now());

    ExplanationOfBenefit genEob =
        hospiceClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags), false);
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);

    // We just want to make sure it is not set
    Extension extension =
        eob.getBillablePeriod()
            .getExtensionByUrl("https://bluebutton.cms.gov/resources/variables/claim_query_cd");
    assertNull(extension);
  }

  /** Tests that the transformer sets the expected insurer. */
  @Test
  public void shouldSetInsurer() throws Exception {
    Reference expected = eob.getInsurer();
    assertNotNull(expected);
    Reference compare = new Reference();
    compare.getIdentifier().setValue("CMS");
    assertTrue(compare.equalsDeep(expected));
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
                "8",
                "Ambulatory Surgery Center (ASC) or other special facility (e.g. hospice)"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected values for the care team member entries. */
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

    assertTrue(compare1.equalsDeep(member1));

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

    assertTrue(compare2.equalsDeep(member2));

    // // Third member
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
                                "Performing provider"))))
            .setQualification(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://nucc.org/provider-taxonomy",
                                "207ZH0000X",
                                "Hematology (Pathology) Physician"))));

    assertTrue(compare3.equalsDeep(member3));
  }

  /**
   * Tests that the transformer sets the expected number of diagnosis and they have the correct
   * values.
   */
  @Test
  public void shouldHaveAllDiagnosis() {
    List<DiagnosisComponent> expected = eob.getDiagnosis();
    assertEquals(4, expected.size());
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("72761", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-9-cm", "72761", "ROTATOR CUFF RUPTURE")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                "principal",
                "Principal Diagnosis"),
            null,
            null);

    assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("B30", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B30", "VIRAL CONJUNCTIVITIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B30", "VIRAL CONJUNCTIVITIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            null,
            null);

    assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("B01", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B01", "VARICELLA [CHICKENPOX]"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B01", "VARICELLA [CHICKENPOX]")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            null,
            null);

    assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("A52", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A52", "LATE SYPHILIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A52", "LATE SYPHILIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "externalcauseofinjury",
                "External Cause of Injury"),
            null,
            null);

    assertTrue(cmp3.equalsDeep(diag3));
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

  /** Tests that the transformer sets the expected revenue center rate amount. */
  @Test
  public void shouldHaveLineItemRevCenterRateAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_rate_amt",
            eob.getItemFirstRep().getAdjudication());

    assertNotNull(adjudication);

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
  public void shouldHaveLineItemRevCenterTotalChargeAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_tot_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

    assertNotNull(adjudication);

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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected adjudication revenue center non-covered charge
   * amount.
   */
  @Test
  public void shouldHaveLineItemRevCenterNonRecoverdChargeAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_ncvrd_chrg_amt",
            eob.getItemFirstRep().getAdjudication());

    assertNotNull(adjudication);

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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item revenue center provider amount. */
  @Test
  public void shouldHaveLineItemRevCenterMedicareProviderAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    assertNotNull(adjudication);

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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item revenue center provider amount. */
  @Test
  public void shouldHaveLineItemRevCenterPaidToProviderAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_prvdr_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    assertNotNull(adjudication);

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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected revenue center payment amount to bene. */
  @Test
  public void shouldHaveLineItemRevCenterBenePaymentAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_bene_pmt_amt",
            eob.getItemFirstRep().getAdjudication());

    assertNotNull(adjudication);

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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected adjudication revenue center payment amount. */
  @Test
  public void shouldHaveLineItemRevCenterMedicarePaymentAmtAdjudication() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rev_cntr_pmt_amt_amt",
            eob.getItemFirstRep().getAdjudication());

    assertNotNull(adjudication);

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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected claim total charge amount entries. */
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

    assertTrue(compare.equalsDeep(total));
  }

  /** Tests that the transformer sets the expected payment value. */
  @Test
  public void shouldHavePaymentTotal() {
    PaymentComponent expected = eob.getPayment();
    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(
                new Money()
                    .setValueElement(new DecimalType("130.32"))
                    .setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(expected));
  }

  /** Tests that the transformer sets the expected medicare utilization day count. */
  @Test
  public void shouldHaveCBenefitDayCnt() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/clm_utlztn_day_cnt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    assertNotNull(benefit);

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

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Ensures the rev_cntr_unit_cnt is correctly mapped to an eob item as an extension when the unit
   * quantity is not zero.
   *
   * <p>TODO: Is this test testing the right thing? Says when not zero, but expects value of 0
   */
  @Test
  public void shouldHaveRevenueCenterUnit() {
    TransformerTestUtilsV2.assertExtensionQuantityEquals(
        CcwCodebookMissingVariable.REV_CNTR_UNIT_CNT, BigDecimal.valueOf(0), eob.getItem());
  }

  /** Tests that the transformer sets the expected NCH primary payer claim paid amount. */
  @Test
  public void shouldHaveCBenefitClaimPaidAmt() {
    BenefitComponent benefit =
        TransformerTestUtilsV2.findFinancial(
            "https://bluebutton.cms.gov/resources/variables/prpayamt",
            eob.getBenefitBalanceFirstRep().getFinancial());

    assertNotNull(benefit);

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

    assertTrue(compare.equalsDeep(benefit));
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
        ClaimType.HOSPICE,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());
  }
}
