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
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
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
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link DMEClaimTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class DMEClaimTransformerV2Test {
  /** The parsed claim used to generate the EOB and for validating with. */
  DMEClaim claim;

  /** The EOB under test created from the {@link #claim}. */
  ExplanationOfBenefit eob;

  /** The transformer under test. */
  DMEClaimTransformerV2 dmeClaimTransformer;

  /** The fhir context for parsing the test file. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** The metrics registry. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The SamhsaSecurityTag lookup. */
  @Mock SecurityTagManager securityTagManager;

  Set<String> securityTags = new HashSet<>();

  /**
   * Generates the Claim object to be used in multiple tests.
   *
   * @return the claim object
   * @throws FHIRException if there was an issue creating the claim
   */
  public DMEClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    DMEClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(DMEClaim.class::cast)
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
    NPIData npiData =
        NPIData.builder()
            .npi("0000000000")
            .taxonomyCode("207X00000X")
            .taxonomyDisplay("Orthopaedic Surgery")
            .build();

    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);
    dmeClaimTransformer = new DMEClaimTransformerV2(metricRegistry, securityTagManager, false);
    claim = generateClaim();
    ExplanationOfBenefit genEob =
        dmeClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
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
    String expectedTimerName = dmeClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals("ExplanationOfBenefit/dme-" + claim.getClaimId(), eob.getId());
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
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-02-03"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-02-03"), eob.getBillablePeriod().getEnd());
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

  /** Tests that the transformer sets the expected referral id. */
  @Test
  public void shouldHaveReferral() {
    assertNotNull(eob.getReferral());
    assertEquals("1306849450", eob.getReferral().getIdentifier().getValue());
  }

  /** Tests that the transformer sets the expected disposition code. */
  @Test
  public void shouldHaveDisposition() {
    assertEquals("01", claim.getClaimDispositionCode());
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
    assertEquals("1306849450", member1.getProvider().getIdentifier().getValue());
    assertEquals(
        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
        member1.getRole().getCoding().get(0).getSystem());
    assertEquals("referring", member1.getRole().getCoding().get(0).getCode());

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());
    assertEquals("1244444444", member2.getProvider().getIdentifier().getValue());
    assertEquals(
        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
        member2.getRole().getCoding().get(0).getSystem());
    assertEquals("performing", member2.getRole().getCoding().get(0).getCode());
    // Check PRTCPTNG_IND_CD extension
    assertEquals(
        "https://bluebutton.cms.gov/resources/variables/prtcptng_ind_cd",
        member2.getExtension().get(0).getUrl());
    assertEquals(
        "https://bluebutton.cms.gov/resources/variables/prtcptng_ind_cd",
        member2.getExtension().get(0).getUrl());
    assertEquals("1", ((Coding) member2.getExtension().get(0).getValue()).getCode());
    assertEquals("Participating", ((Coding) member2.getExtension().get(0).getValue()).getDisplay());
    // Check Qualification
    assertEquals(
        "http://nucc.org/provider-taxonomy",
        member2.getQualification().getCoding().getFirst().getSystem());
    assertEquals(
        "Hematology (Pathology) Physician",
        member2.getQualification().getCoding().getFirst().getDisplay());
    assertEquals("207ZH0000X", member2.getQualification().getCoding().get(0).getCode());

    assertEquals(2, eob.getCareTeam().size());
  }

  /**
   * Tests that the transformer sets the expected values for the care team member extensions and
   * does not error when only the required care team values exist.
   */
  @Test
  public void testCareTeamExtensionsWhenOptionalValuesAbsent() {

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    DMEClaim loadedClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(DMEClaim.class::cast)
            .findFirst()
            .get();
    loadedClaim.setLastUpdated(Instant.now());

    // Set the optional care team fields to empty
    for (DMEClaimLine line : loadedClaim.getLines()) {
      line.setProviderParticipatingIndCode(Optional.empty());
      line.setProviderSpecialityCode(Optional.empty());
    }

    ExplanationOfBenefit genEob =
        dmeClaimTransformer.transform(new ClaimWithSecurityTags<>(loadedClaim, securityTags));

    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    // Ensure the extension for PRTCPTNG_IND_CD wasnt added
    // Also the qualification coding should be empty if specialty code is not set
    String prtIndCdUrl =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PRTCPTNG_IND_CD);
    for (CareTeamComponent careTeam : genEob.getCareTeam()) {
      assertFalse(careTeam.getExtension().stream().anyMatch(i -> i.getUrl().equals(prtIndCdUrl)));
    }
  }

  /** Tests that the transformer sets the expected number of supporting info entries. */
  @Test
  public void shouldHaveSupportingInfoList() {
    assertEquals(2, eob.getSupportingInfo().size());
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

  /** Tests that the transformer sets the expected codings for supporting info. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected number of diagnosis. */
  @Test
  public void shouldHaveDiagnosesList() {
    assertEquals(3, eob.getDiagnosis().size());
  }

  /** Tests that the transformer sets the expected diagnosis entries. */
  @Test
  public void shouldHaveDiagnosesMembers() {
    DiagnosisComponent diag1 =
        TransformerTestUtilsV2.findDiagnosisByCode("B04", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B04", "MONKEYPOX"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B04", "MONKEYPOX")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                "principal",
                "Principal Diagnosis"),
            null,
            null);

    assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("A37", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A37", "WHOOPING COUGH"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A37", "WHOOPING COUGH")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "other",
                "Other"),
            null,
            null);

    assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("A25", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A25", "RAT-BITE FEVERS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A25", "RAT-BITE FEVERS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
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

  /**
   * Tests that the transformer sets the expected line item extensions and has the correct number of
   * them.
   */
  @Test
  public void shouldHaveLineItemExtension() {
    assertNotNull(eob.getItemFirstRep().getExtension());
    assertEquals(9, eob.getItemFirstRep().getExtension().size());

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

    assertTrue(compare1.equalsDeep(ex1));

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

    assertTrue(compare2.equalsDeep(ex2));

    Extension ex3 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_mtus_cnt",
            eob.getItemFirstRep().getExtension());

    Extension compare3 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/dmerc_line_mtus_cnt",
            new Quantity()
                .setUnit("Number of services")
                .setValue(new BigDecimal("60.234"))
                .setSystem("https://bluebutton.cms.gov/resources/variables/dmerc_line_mtus_cd")
                .setCode("3"));

    assertTrue(compare3.equalsDeep(ex3));

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

    assertTrue(compare4.equalsDeep(ex4));

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

    assertTrue(compare5.equalsDeep(ex5));

    Extension ex6 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/betos_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare6 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/betos_cd",
            new Coding("https://bluebutton.cms.gov/resources/variables/betos_cd", "D9Z", null));

    assertTrue(compare6.equalsDeep(ex6));

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

    assertTrue(compare7.equalsDeep(ex7));

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

    assertTrue(compare8.equalsDeep(ex8));

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

    assertTrue(compare9.equalsDeep(ex9));

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

    assertTrue(compare10.equalsDeep(ex10));
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

  /** Tests that the transformer sets the expected line item diagnosis reference. */
  @Test
  public void shouldHaveLineItemDiagnosisRef() {
    assertNotNull(eob.getItemFirstRep().getDiagnosisSequence());
    assertEquals(1, eob.getItemFirstRep().getDiagnosisSequence().size());
  }

  /** Tests that the transformer sets the expected line item information reference. */
  @Test
  public void shouldHaveLineItemInformationRef() {
    assertNotNull(eob.getItemFirstRep().getInformationSequence());
    assertEquals(1, eob.getItemFirstRep().getInformationSequence().size());
  }

  /** Tests that the transformer sets the expected line item category entry. */
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

    assertTrue(compare.equalsDeep(category));
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
            new Coding(
                "http://hl7.org/fhir/sid/ndc", "000000000", RDATestUtils.FAKE_DRUG_CODE_DISPLAY));

    assertTrue(compare.equalsDeep(ex));
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
            .setCoding(
                Arrays.asList(
                    new Coding("https://bluebutton.cms.gov/resources/codesystem/hcpcs", "YY", null)
                        .setVersion("3")));

    assertTrue(compare.equalsDeep(modifier));
  }

  /**
   * Tests that the transformer sets the expected line item serviced period dates.
   *
   * @throws Exception if there is a date parse error
   */
  @Test
  public void shouldHaveLineItemServicedPeriod() throws Exception {
    assertNotNull(eob.getItemFirstRep().getServicedPeriod().getStart());
    assertNotNull(eob.getItemFirstRep().getServicedPeriod().getEnd());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-02-03"),
        eob.getItemFirstRep().getServicedPeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2014-02-03"),
        eob.getItemFirstRep().getServicedPeriod().getEnd());
  }

  /** Tests that the transformer sets the expected line item service location extension. */
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

    assertTrue(compare.equalsDeep(location));
  }

  /** Tests that the transformer sets the expected line item quantity. */
  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();

    Quantity compare = new Quantity(60);

    assertTrue(compare.equalsDeep(quantity));
  }

  /** Tests that the transformer sets the expected number of line item adjudications. */
  @Test
  public void shouldHaveLineItemAdjudications() {
    assertEquals(13, eob.getItemFirstRep().getAdjudication().size());
  }

  /** Tests that the transformer sets the expected line item revenue center provider amount. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item adjudication primary charge amount. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item adjudication DME purchase price amount.
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item NCH medicare payment amount. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item adjudication line bene payment amount
   * (paid to patient).
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item adjudication line provider payment
   * amount.
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item adjudication bene deductible amount. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item adjudication bene primary payer paid
   * amount.
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item adjudication coinsurance amount. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item adjudication submitted charge amount.
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item adjudication line allowed charge amount.
   */
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
                new Money().setValue(129.45).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item adjudication line bene payment amount
   * (paid to provider).
   */
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
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "priorpayerpaid",
                                "Prior payer paid"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/clm_tot_chrg_amt",
                                "Claim Total Charge Amount"))))
            .setAmount(new Money().setValue(0).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(total));
  }

  /** Tests that the transformer sets the expected payment value. */
  @Test
  public void shouldHavePayment() {
    PaymentComponent compare =
        new PaymentComponent()
            .setAmount(
                new Money().setValue(777.75).setCurrency(TransformerConstants.CODED_MONEY_USD));

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
    assertEquals(5, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  /**
   * Tests that the transformer sets the expected carrier claim cash deductible applied amount
   * coding.
   */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH claim provider payment amount coding. */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH claim payment amount to bene coding. */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH carrier claim submitted charge amount. */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /** Tests that the transformer sets the expected NCH carrier claim allowed charge amount. */
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

    assertTrue(compare.equalsDeep(benefit));
  }
}
