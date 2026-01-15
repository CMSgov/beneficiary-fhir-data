package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the {@link CarrierClaimTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CarrierClaimTransformerV2Test {
  /** The claim under test. */
  CarrierClaim claim;

  /** The eob loaded before each test from a file. */
  ExplanationOfBenefit eob;

  /** The fhir context for parsing the file data. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** The transformer under test. */
  CarrierClaimTransformerV2 carrierClaimTransformer;

  /** The mock metric registry. */
  @Mock MetricRegistry metricRegistry;

  /** The mock metric timer. */
  @Mock Timer metricsTimer;

  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context metricsTimerContext;

  Set<String> securityTags = new HashSet<>();

  /**
   * Generates the sample A claim object to be used in multiple tests.
   *
   * @return the claim object
   * @throws FHIRException if there is an issue parsing the claim
   */
  public CarrierClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    CarrierClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
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
    SecurityTagManager securityTagManager = mock(SecurityTagManager.class);

    carrierClaimTransformer =
        new CarrierClaimTransformerV2(metricRegistry, securityTagManager, false);

    claim = generateClaim();
    ExplanationOfBenefit genEob =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
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
    String expectedTimerName = carrierClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link ClaimTransformerInterfaceV2#transform}
   * works as expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER} {@link
   * CarrierClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    CarrierClaim claim = generateClaim();

    assertMatches(
        claim,
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags)));
  }

  /** Tests that the transformer sets the provider (CARR_CLM_BLG_NPI_NUM). */
  @Test
  public void shouldSetProvider() {
    // Make sure the field was in the claim data
    assertTrue(claim.getCarrierClaimBlgNpiNumber().isPresent());

    // Check the value was mapped correctly
    assertNotNull(eob.getProvider());
    assertEquals(
        claim.getCarrierClaimBlgNpiNumber().get(), eob.getProvider().getIdentifier().getValue());
    assertEquals(
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CARR_CLM_BLG_NPI_NUM),
        eob.getProvider().getIdentifier().getSystem());
  }

  /**
   * Tests that the transformer sets the provider to the default "UNKNOWN" when CARR_CLM_BLG_NPI_NUM
   * is not present in the claim.
   */
  @Test
  public void shouldSetProviderDefaultWhenNoClaimBlgNumberPresent() {
    // Remove CARR_CLM_BLG_NPI_NUM from the claim
    claim = generateClaim();
    claim.setCarrierClaimBlgNpiNumber(Optional.empty());
    ExplanationOfBenefit genEob =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);

    // Make sure the field is no longer present
    assertFalse(claim.getCarrierClaimBlgNpiNumber().isPresent());

    // Check the value was set to UNKNOWN
    assertNotNull(eob.getProvider());
    assertEquals("UNKNOWN", eob.getProvider().getIdentifier().getValue());
    assertEquals(
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CARR_CLM_BLG_NPI_NUM),
        eob.getProvider().getIdentifier().getSystem());
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
        (new SimpleDateFormat("yyy-MM-dd")).parse("1999-10-27"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("1999-10-27"), eob.getBillablePeriod().getEnd());
  }

  /** Verifies that a {@link CarrierClaim} has a Billing NPI Number. */
  @Test
  public void shouldHaveBillingNPINum() throws Exception {
    // We just want to make sure it is set
    assertNotNull(eob.getProvider().getIdentifier());
    assertEquals(
        "https://bluebutton.cms.gov/resources/variables/carr_clm_blg_npi_num",
        eob.getProvider().getIdentifier().getSystem());
    assertEquals("1234567890", eob.getProvider().getIdentifier().getValue());
  }

  /** Tests that the transformer sets the expected identifiers. */
  @Test
  public void shouldHaveIdentifiers() {
    assertEquals(2, eob.getIdentifier().size());

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

    assertTrue(compare1.equalsDeep(clmGrp1));

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

    assertTrue(compare2.equalsDeep(clmGrp2));
  }

  /** Tests that the transformer sets the expected number of extensions for this claim type. */
  @Test
  public void shouldHaveExtensions() {
    assertEquals(7, eob.getExtension().size());
  }

  /** Tests that the transformer sets the expected "near line" extensions. */
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

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected carrier number extensions. */
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

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected carrier claim control number extensions. */
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

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected payment download code extensions. */
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

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected assigned claim extensions. */
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

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected clinical trial number extensions. */
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

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected claim entry code extensions. */
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

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected number of supporting info entries. */
  @Test
  public void shouldHaveSupportingInfoList() {
    assertEquals(2, eob.getSupportingInfo().size());
  }

  /** Tests that the transformer sets the expected supporting info claim received date. */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /**
   * Tests that the transformer sets the expected supporting info line observation info.
   *
   * <p>TODO: Is this test named correctly?
   */
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

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected creation date. */
  @Test
  public void shouldHaveCreatedDate() {
    assertNotNull(eob.getCreated());
  }

  /** Tests that the transformer sets the expected patient reference. */
  @Test
  public void shouldReferencePatient() {
    assertNotNull(eob.getPatient());
    assertEquals("Patient/567834", eob.getPatient().getReference());
  }

  /** Tests that the transformer sets the expected insurance coverage reference. */
  @Test
  public void shouldInsuranceCoverage() {
    assertNotNull(eob.getInsurance());
    assertEquals("Coverage/part-b-567834", eob.getInsurance().get(0).getCoverage().getReference());
  }

  /** Tests that the transformer sets the expected final action status. */
  @Test
  public void shouldSetFinalAction() {
    assertEquals(ExplanationOfBenefitStatus.ACTIVE, eob.getStatus());
  }

  /** Tests that the transformer sets the expected 'nature of request' value. */
  @Test
  public void shouldSetUse() {
    assertEquals(Use.CLAIM, eob.getUse());
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals("ExplanationOfBenefit/carrier-" + claim.getClaimId(), eob.getId());
  }

  /** Tests that the transformer sets the expected last updated date in the metadata. */
  @Test
  public void shouldSetLastUpdated() {
    assertNotNull(eob.getMeta().getLastUpdated());
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
                        "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "92999", null)));

    compare.setExtension(
        Arrays.asList(
            new Extension(
                "http://hl7.org/fhir/sid/ndc",
                new Coding("http://hl7.org/fhir/sid/ndc", "000000000", "Fake Diluent - WATER"))));

    assertTrue(compare.equalsDeep(pos));
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
            .setTiming(new DateType("1999-11-06"));

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
        TransformerTestUtilsV2.findDiagnosisByCode("A02", eob.getDiagnosis());

    DiagnosisComponent cmp1 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag1.getSequence(),
            List.of(
                new Coding(
                    "http://hl7.org/fhir/sid/icd-10-cm", "A02", "OTHER SALMONELLA INFECTIONS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A02", "OTHER SALMONELLA INFECTIONS")),
            new Coding(
                "http://terminology.hl7.org/CodeSystem/ex-diagnosistype", "principal", "principal"),
            null,
            null);

    assertTrue(cmp1.equalsDeep(diag1));

    DiagnosisComponent diag2 =
        TransformerTestUtilsV2.findDiagnosisByCode("A06", eob.getDiagnosis());

    DiagnosisComponent cmp2 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag2.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A06", "AMEBIASIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A06", "AMEBIASIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    assertTrue(cmp2.equalsDeep(diag2));

    DiagnosisComponent diag3 =
        TransformerTestUtilsV2.findDiagnosisByCode("B04", eob.getDiagnosis());

    DiagnosisComponent cmp3 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag3.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B04", "MONKEYPOX"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B04", "MONKEYPOX")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    assertTrue(cmp3.equalsDeep(diag3));

    DiagnosisComponent diag4 =
        TransformerTestUtilsV2.findDiagnosisByCode("B05", eob.getDiagnosis());

    DiagnosisComponent cmp4 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag4.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "B05", "MEASLES"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "B05", "MEASLES")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    assertTrue(cmp4.equalsDeep(diag4));

    DiagnosisComponent diag5 =
        TransformerTestUtilsV2.findDiagnosisByCode("A52", eob.getDiagnosis());

    DiagnosisComponent cmp5 =
        TransformerTestUtilsV2.createDiagnosis(
            // Order doesn't matter
            diag5.getSequence(),
            List.of(
                new Coding("http://hl7.org/fhir/sid/icd-10-cm", "A52", "LATE SYPHILIS"),
                new Coding("http://hl7.org/fhir/sid/icd-10", "A52", "LATE SYPHILIS")),
            new Coding(
                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimDiagnosisType",
                "secondary",
                "Secondary"),
            null,
            null);

    assertTrue(cmp5.equalsDeep(diag5));
  }

  /** Tests that the transformer sets the expected number of top level category type codings. */
  @Test
  public void shouldHaveExpectedTypeCoding() {
    assertEquals(3, eob.getType().getCoding().size());
  }

  /** Tests that the transformer sets the expected values for the top level type codings. */
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

    assertTrue(compare.equalsDeep(eob.getType()));
  }

  /** Tests that the transformer sets the expected number of care team entries. */
  @Test
  public void shouldHaveCareTeamList() {
    assertEquals(4, eob.getCareTeam().size());
  }

  /**
   * Tests that the transformer sets the expected values for the care team member entries, and does
   * not contain duplicate entries.
   */
  @Test
  public void shouldHaveCareTeamMembers() {

    // Load a claim with multiple lines, to test we dont get duplicate entries
    List<Object> parsedRecords =
        ServerTestUtils.parseData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_A_MULTIPLE_CARRIER_LINES.getResources()));

    CarrierClaim loadedClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();
    loadedClaim.setLastUpdated(Instant.now());

    ExplanationOfBenefit genEob =
        carrierClaimTransformer.transform(
            new ClaimWithSecurityTags<>(loadedClaim, securityTags));

    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    // First member
    CareTeamComponent member1 =
        TransformerTestUtilsV2.findCareTeamBySequence(1, genEob.getCareTeam());
    CareTeamComponent compare1 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            1,
            "8765676",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "referring",
            "Referring");

    assertTrue(compare1.equalsDeep(member1));

    // Second member
    CareTeamComponent member2 =
        TransformerTestUtilsV2.findCareTeamBySequence(2, genEob.getCareTeam());
    CareTeamComponent compare2 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            2,
            "K25852",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "referring",
            "Referring");

    assertTrue(compare2.equalsDeep(member2));

    // Third member
    CareTeamComponent member3 =
        TransformerTestUtilsV2.findCareTeamBySequence(3, genEob.getCareTeam());
    CareTeamComponent compare3 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            3,
            "1923124",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "performing",
            "Performing provider");

    compare3.setResponsible(true);
    compare3.setQualification(
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding()
                        .setSystem("http://nucc.org/provider-taxonomy")
                        .setDisplay("Health Care")
                        .setCode("390200000X"),
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

    assertTrue(compare3.equalsDeep(member3));

    // Fourth member
    CareTeamComponent member4 =
        TransformerTestUtilsV2.findCareTeamBySequence(4, genEob.getCareTeam());
    CareTeamComponent compare4 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            4,
            "0000000000",
            "http://terminology.hl7.org/CodeSystem/claimcareteamrole",
            "primary",
            "Primary provider");
    compare4.getProvider().setDisplay(RDATestUtils.FAKE_NPI_ORG_NAME);

    assertTrue(compare4.equalsDeep(member4));

    assertEquals(4, genEob.getCareTeam().size());
  }

  /**
   * Tests that the transformer sets the expected values for the care team member extensions and
   * does not error when only the required care team values exist.
   */
  @Test
  public void testCareTeamExtensionsWhenOptionalValuesAbsent() {

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    CarrierClaim loadedClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();
    loadedClaim.setLastUpdated(Instant.now());

    // Set the optional care team fields to empty
    for (CarrierClaimLine line : loadedClaim.getLines()) {
      line.setProviderParticipatingIndCode(Optional.empty());
    }

    ExplanationOfBenefit genEob =
        carrierClaimTransformer.transform(
            new ClaimWithSecurityTags<>(loadedClaim, securityTags));

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

  /**
   * Tests the care team entries have the expected number of entries.
   *
   * <p>TODO: This seems like a removable duplicate of the above test(s)
   */
  @Test
  public void shouldHaveFourCareTeamEntries() throws IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_A_MULTIPLE_CARRIER_LINES.getResources()));

    CarrierClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());

    ExplanationOfBenefit genEob =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    eob = parser.parseResource(ExplanationOfBenefit.class, json);

    assertEquals(4, eob.getCareTeam().size());
  }

  /** Tests that the transformer sets the expected line item quantity. */
  @Test
  public void shouldHaveLineItemQuantity() {
    Quantity quantity = eob.getItemFirstRep().getQuantity();

    Quantity compare = new Quantity().setValue(new BigDecimal("1.0"));

    assertTrue(compare.equalsDeep(quantity));
  }

  /**
   * Tests that the transformer sets the expected line item extensions and has the correct number of
   * them.
   */
  @Test
  public void shouldHaveLineItemExtension() {
    assertNotNull(eob.getItemFirstRep().getExtension());
    assertEquals(7, eob.getItemFirstRep().getExtension().size());

    Extension ex1 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
            eob.getItemFirstRep().getExtension());

    Extension compare1 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
            new Quantity().setValue(new BigDecimal("1")));

    assertTrue(compare1.equalsDeep(ex1));

    Extension ex2 =
        TransformerTestUtilsV2.findExtensionByUrlAndSystem(
            "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
            "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
            eob.getItemFirstRep().getExtension());

    Extension compare2 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt",
            new Coding()
                .setSystem("https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cnt")
                .setCode("3"));

    assertTrue(compare2.equalsDeep(ex2));

    Extension ex3 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare3 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cd",
            new Coding()
                .setSystem("https://bluebutton.cms.gov/resources/variables/carr_line_mtus_cd")
                .setCode("3")
                .setDisplay("Services"));

    assertTrue(compare3.equalsDeep(ex3));

    Extension ex4 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/betos_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare4 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/betos_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/betos_cd",
                "T2D",
                "Other tests - other"));

    assertTrue(compare4.equalsDeep(ex4));

    Extension ex5 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare5 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/line_bene_prmry_pyr_cd",
                "E",
                "Workers' compensation"));

    assertTrue(compare5.equalsDeep(ex5));

    Extension ex6 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/line_prcsg_ind_cd",
            eob.getItemFirstRep().getExtension());

    Extension compare6 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/line_prcsg_ind_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/line_prcsg_ind_cd",
                "A",
                "Allowed"));

    assertTrue(compare6.equalsDeep(ex6));

    Extension ex7 =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/line_service_deductible",
            eob.getItemFirstRep().getExtension());

    Extension compare7 =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/line_service_deductible",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/line_service_deductible",
                "0",
                "Service Subject to Deductible"));

    assertTrue(compare7.equalsDeep(ex7));
  }

  /** Tests that the transformer sets the expected number of line item adjudications. */
  @Test
  public void shouldHaveLineItemAdjudications() {
    assertEquals(9, eob.getItemFirstRep().getAdjudication().size());
  }

  /** Tests that the transformer sets the expected line item denial reason adjudication entries. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item paid to patient adjudication entries.
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item benefit adjudication entries. */
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
    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected line item paid to provider adjudication entries.
   */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item deductible adjudication. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected prior payer paid adjudication. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item co-insurance adjudication entries. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item submitted adjudication entries. */
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

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected line item eligible adjudication entries. */
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
            .setAmount(
                new Money().setValue(47.84).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected number of benefit balance financial entries. */
  @Test
  public void shouldHaveBenefitBalanceFinancial() {
    assertEquals(5, eob.getBenefitBalanceFirstRep().getFinancial().size());
  }

  /**
   * Tests that the transformer sets the expected claim pass thru cash deductible financial entries.
   */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected claim pass thru claim provider payment amount
   * entries.
   */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected claim pass thru claim provider payment amount to
   * beneficiary entries.
   */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected claim pass thru submitted charge financial
   * entries.
   */
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

    assertTrue(compare.equalsDeep(benefit));
  }

  /**
   * Tests that the transformer sets the expected claim pass thru allowed charge financial entries.
   */
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

    assertTrue(compare.equalsDeep(benefit));
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

  /** Tests that the transformer sets the expected number of procedure entries. */
  @Test
  public void shouldHaveProcedureList() {
    assertEquals(0, eob.getProcedure().size());
  }

  /** Test that should not have a npi entry for organization. */
  @Test
  public void shouldNotHaveNpiEntryForOrgWhenNoOrganizationNpi() throws IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    CarrierClaim claimWithoutNpi =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();
    claimWithoutNpi.setLastUpdated(Instant.now());

    claimWithoutNpi.getLines().get(0).setOrganizationNpi(Optional.empty());
    ExplanationOfBenefit genEob =
        carrierClaimTransformer.transform(
            new ClaimWithSecurityTags<>(claimWithoutNpi, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    ExplanationOfBenefit eobWithoutNpi = parser.parseResource(ExplanationOfBenefit.class, json);

    C4BBPractitionerIdentifierType type = C4BBPractitionerIdentifierType.NPI;
    C4BBClaimProfessionalAndNonClinicianCareTeamRole role =
        C4BBClaimProfessionalAndNonClinicianCareTeamRole.PRIMARY;

    CareTeamComponent careTeamEntry =
        eobWithoutNpi.getCareTeam().stream()
            .filter(
                ctc ->
                    ctc.getProvider().getIdentifier().getType().getCoding().stream()
                        .anyMatch(
                            c ->
                                c.getSystem().equalsIgnoreCase(type.getSystem())
                                    && c.getCode().equalsIgnoreCase(type.toCode())))
            .filter(
                ctc ->
                    role.toCode().equalsIgnoreCase(ctc.getRole().getCodingFirstRep().getCode())
                        && role.getSystem()
                            .equalsIgnoreCase(ctc.getRole().getCodingFirstRep().getSystem()))
            .findAny()
            .orElse(null);

    assertEquals(null, careTeamEntry);
  }

  /** Test that should have a npi entry for organization. */
  @Test
  public void shouldHaveAnNpiEntryForOrganizationWhenTheClaimsOrganizationNpiIsPresent()
      throws IOException {

    C4BBPractitionerIdentifierType type = C4BBPractitionerIdentifierType.NPI;
    C4BBClaimProfessionalAndNonClinicianCareTeamRole role =
        C4BBClaimProfessionalAndNonClinicianCareTeamRole.PRIMARY;

    CareTeamComponent careTeamEntry =
        eob.getCareTeam().stream()
            .filter(
                ctc ->
                    ctc.getProvider().getIdentifier().getType().getCoding().stream()
                        .anyMatch(
                            c ->
                                c.getSystem().equalsIgnoreCase(type.getSystem())
                                    && c.getCode().equalsIgnoreCase(type.toCode())))
            .filter(
                ctc ->
                    role.toCode().equalsIgnoreCase(ctc.getRole().getCodingFirstRep().getCode())
                        && role.getSystem()
                            .equalsIgnoreCase(ctc.getRole().getCodingFirstRep().getSystem()))
            .findAny()
            .orElse(null);
    assertEquals("primary", careTeamEntry.getRole().getCoding().get(0).getCode());
    assertEquals(RDATestUtils.FAKE_NPI_ORG_NAME, careTeamEntry.getProvider().getDisplay());
    assertEquals(
        "npi", careTeamEntry.getProvider().getIdentifier().getType().getCoding().get(0).getCode());
    assertEquals(
        "National Provider Identifier",
        careTeamEntry.getProvider().getIdentifier().getType().getCoding().get(0).getDisplay());
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link CarrierClaim}.
   *
   * @param claim the {@link CarrierClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     CarrierClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(CarrierClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.CARRIER,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());
  }
}
