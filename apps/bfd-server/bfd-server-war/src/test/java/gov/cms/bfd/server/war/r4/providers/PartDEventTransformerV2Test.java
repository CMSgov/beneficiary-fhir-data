package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudicationStatus;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.PaymentComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link PartDEventTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class PartDEventTransformerV2Test {
  /** The parsed claim used to generate the EOB and for validating with. */
  PartDEvent claim;

  /** The EOB under test created from the {@link #claim}. */
  ExplanationOfBenefit eob;

  /** The transformer under test. */
  PartDEventTransformerV2 partdEventTransformer;

  /** The metrics registry. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The mock npi lookup. */
  @Mock NPIOrgLookup mockNpiOrgLookup;

  Set<String> securityTags = new HashSet<>();

  /** ndcProductHashMap represents a map of PRODUCTNDC and SUBSTANCENAME for test. */
  Map<String, String> ndcProductHashMap = new HashMap<>();

  /**
   * Generates the Claim object to be used in multiple tests.
   *
   * @return the claim object
   * @throws FHIRException if there was an issue creating the claim
   */
  public PartDEvent generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    PartDEvent claim =
        parsedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(PartDEvent.class::cast)
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

    partdEventTransformer = new PartDEventTransformerV2(metricRegistry);
    claim = generateClaim();
    eob = partdEventTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtilsV2.enrichEob(
        eob, RDATestUtils.createTestNpiOrgLookup(), RDATestUtils.createFdaDrugCodeDisplayLookup());
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    String expectedTimerName = partdEventTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals("pde-" + claim.getEventId(), eob.getId());
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
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_EOB_PHARMACY_PROFILE_URL)));
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
        (new SimpleDateFormat("yyy-MM-dd")).parse("2015-05-12"),
        eob.getBillablePeriod().getStart());
    assertEquals(
        (new SimpleDateFormat("yyy-MM-dd")).parse("2015-05-12"), eob.getBillablePeriod().getEnd());
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
            "https://bluebutton.cms.gov/resources/variables/phrmcy_srvc_type_cd",
            eob.getFacility().getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/phrmcy_srvc_type_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/phrmcy_srvc_type_cd",
                "01",
                "Community/retail pharmacy"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected number of care team entries. */
  @Test
  public void shouldHaveCareTeamList() {
    assertEquals(1, eob.getCareTeam().size());
  }

  /** Tests that the transformer sets the expected values for the care team member entries. */
  @Test
  public void shouldHaveCareTeamMembers() {
    // Single member
    CareTeamComponent member = TransformerTestUtilsV2.findCareTeamBySequence(1, eob.getCareTeam());
    assertEquals("1750384806", member.getProvider().getIdentifier().getValue());
  }

  /** Tests that the transformer sets the expected number of supporting info entries. */
  @Test
  public void shouldHaveSupportingInfoList() {
    assertEquals(14, eob.getSupportingInfo().size());
  }

  /** Tests that the transformer sets the expected compound code supporting info. */
  @Test
  public void shouldHaveCompoundCodeSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("compoundcode", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                    "compoundcode",
                    "Compound Code")),
            // Code
            new Coding(
                "http://terminology.hl7.org/CodeSystem/v3-ActCode",
                "RXDINV",
                "Rx dispense invoice"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected refill number supporting info. */
  @Test
  public void shouldHaveRefillNumberSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("refillnum", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
                // We don't care what the sequence number is here
                sic.getSequence(),
                // Category
                Arrays.asList(
                    new Coding(
                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                        "refillnum",
                        "Refill Number")))
            // valueQuantity
            .setValue(new SimpleQuantity().setValue(3));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected days supply supporting info. */
  @Test
  public void shouldHaveDaysSupplySupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("dayssupply", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
                // We don't care what the sequence number is here
                sic.getSequence(),
                // Category
                Arrays.asList(
                    new Coding(
                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                        "dayssupply",
                        "Days Supply")))
            // valueQuantity
            .setValue(new SimpleQuantity().setValue(30));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected drug coverage status code supporting info. */
  @Test
  public void shouldHaveDrugCvrgStusCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/drug_cvrg_stus_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/drug_cvrg_stus_cd",
                    "Drug Coverage Status Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/drug_cvrg_stus_cd",
                "C",
                "Covered"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected DAW product selection supporting info. */
  @Test
  public void shouldHaveDAWProdSlctnCdCodeSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/daw_prod_slctn_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/daw_prod_slctn_cd",
                    "Dispense as Written (DAW) Product Selection Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/daw_prod_slctn_cd",
                "0",
                "No Product Selection Indicated (may also have missing values)"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected dispensing status code supporting info. */
  @Test
  public void shouldHaveDspnsngStusCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/dspnsng_stus_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/dspnsng_stus_cd",
                    "Dispensing Status Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/dspnsng_stus_cd",
                "P",
                "Partial fill"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected adjustment deletion code supporting info. */
  @Test
  public void shouldHaveAdjstmtDltnCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/adjstmt_dltn_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/adjstmt_dltn_cd",
                    "Adjustment Deletion Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/adjstmt_dltn_cd",
                "A",
                "Adjustment"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected non-standard format code supporting info. */
  @Test
  public void shouldHaveNstdFrmtCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nstd_frmt_cd", eob.getSupportingInfo());

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
                    "https://bluebutton.cms.gov/resources/variables/nstd_frmt_cd",
                    "Non-Standard Format Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/nstd_frmt_cd", "X", "X12 837"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected pricing exception code supporting info. */
  @Test
  public void shouldHavePrcngExcptnCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/prcng_excptn_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/prcng_excptn_cd",
                    "Pricing Exception Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/prcng_excptn_cd",
                "M",
                "Medicare is a secondary payer (MSP)"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected catastrophic coverage code supporting info. */
  @Test
  public void shouldHaveCtstrphcCvrgCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/ctstrphc_cvrg_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/ctstrphc_cvrg_cd",
                    "Catastrophic Coverage Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/ctstrphc_cvrg_cd",
                "C",
                "Above attachment point"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected rx origin code supporting info. */
  @Test
  public void shouldHaveRxOrgnCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode("rxorigincode", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                    "rxorigincode",
                    "Rx Origin Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/rx_orgn_cd", "3", "Electronic"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected brand generic Indicator supporting info. */
  @Test
  public void shouldHaveBrndGnrcCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "brandgenericindicator", eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                    "brandgenericindicator",
                    "Brand Generic Indicator")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/brnd_gnrc_cd",
                "G",
                "Generic Null/missing"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected patient residence code supporting info. */
  @Test
  public void shouldHavePtntRsdncCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/ptnt_rsdnc_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/ptnt_rsdnc_cd",
                    "Patient Residence Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/ptnt_rsdnc_cd",
                "02",
                "Skilled Nursing Facility"));

    assertTrue(compare.equalsDeep(sic));
  }

  /** Tests that the transformer sets the expected submission clarification code supporting info. */
  @Test
  public void shouldHaveSubmsnClrCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/submsn_clr_cd",
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
                    "https://bluebutton.cms.gov/resources/variables/submsn_clr_cd",
                    "Submission clarification code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/submsn_clr_cd",
                "08",
                "Process compound for approved ingredients"));

    assertTrue(compare.equalsDeep(sic));
  }

  /**
   * Tests that the transformer sets the expected number of insurance entries with the expected
   * values.
   */
  @Test
  public void shouldReferenceCoverageInInsurance() {
    // Only one insurance object
    assertEquals(1, eob.getInsurance().size());
    assertNotNull(eob.getInsuranceFirstRep());
    assertEquals("Coverage/part-d-567834", eob.getInsuranceFirstRep().getCoverage().getReference());
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

  /** Tests that the transformer sets the expected Coding for line item produce/service. */
  @Test
  public void shouldHaveLineItemProductOrServiceCoding() {
    CodeableConcept pos = eob.getItemFirstRep().getProductOrService();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "http://hl7.org/fhir/sid/ndc",
                        "000000000",
                        RDATestUtils.FAKE_DRUG_CODE_DISPLAY)));

    assertTrue(compare.equalsDeep(pos));
  }

  /** Tests that the transformer sets the expected line item serviced date. */
  @Test
  public void shouldHaveLineItemServicedDate() {
    DateType servicedDate = eob.getItemFirstRep().getServicedDateType();

    DateType compare = new DateType("2015-05-12");

    assertEquals(servicedDate.toString(), compare.toString());
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
    assertEquals(9, eob.getItemFirstRep().getAdjudication().size());
  }

  /** Tests that the transformer sets the expected part D amount paid for PDE. */
  @Test
  public void shouldHaveLineItemAdjudicationCvrdDPlanPdAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/cvrd_d_plan_pd_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/cvrd_d_plan_pd_amt",
                                "Amount paid by Part D plan for the PDE (drug is covered by Part D)"))))
            .setAmount(
                new Money().setValue(126.99).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected GDBC threshold amount. */
  @Test
  public void shouldHaveLineItemAdjudicationGdcBlwOoptAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/gdc_blw_oopt_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/gdc_blw_oopt_amt",
                                "Gross Drug Cost Below Part D Out-of-Pocket Threshold (GDCB)"))))
            .setAmount(
                new Money().setValue(995.34).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected GDBA threshold amount. */
  @Test
  public void shouldHaveLineItemAdjudicationGdcAbvOoptAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/gdc_abv_oopt_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/gdc_abv_oopt_amt",
                                "Gross Drug Cost Above Part D Out-of-Pocket Threshold (GDCA)"))))
            .setAmount(
                new Money().setValue(15.25).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the expected adjudication amount paid by patient. */
  @Test
  public void shouldHaveLineItemAdjudicationPtntPayAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/ptnt_pay_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/ptnt_pay_amt",
                                "Amount Paid by Patient"))))
            .setAmount(
                new Money().setValue(235.85).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the 'expected other true out-of-pocket' (TrOOP) amount. */
  @Test
  public void shouldHaveLineItemAdjudicationOthrTroopAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/othr_troop_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(17.30);
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
                                "https://bluebutton.cms.gov/resources/variables/othr_troop_amt",
                                "Other True Out-of-Pocket (TrOOP) Amount"))))
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the line item low income subsidy amount. */
  @Test
  public void shouldHaveLineItemAdjudicationLicsAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/lics_amt",
            eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "discount",
                                "Discount"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/lics_amt",
                                "Amount paid for the PDE by Part D low income subsidy"))))
            .setAmount(
                new Money().setValue(122.23).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the line item patient liability due to payments by other payers
   * (PLRO).
   */
  @Test
  public void shouldHaveLineItemAdjudicationPlroAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/plro_amt",
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
                                "https://bluebutton.cms.gov/resources/variables/plro_amt",
                                "Reduction in patient liability due to payments by other payers (PLRO)"))))
            .setAmount(
                new Money().setValue(42.42).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the line item part D total drug cost amount. */
  @Test
  public void shouldHaveLineItemAdjudicationTotRxCstAmt() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/tot_rx_cst_amt",
            eob.getItemFirstRep().getAdjudication());

    // Need to maintain trailing 0s in USD amount
    BigDecimal amt = new BigDecimal(550.00);
    amt = amt.setScale(2, RoundingMode.HALF_DOWN);

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "drugcost",
                                "Drug Cost"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/tot_rx_cst_amt",
                                "Total drug cost (Part D)"))))
            .setAmount(new Money().setValue(amt).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /** Tests that the transformer sets the line item adjudication gap discount amount. */
  @Test
  public void shouldHaveLineItemAdjudicationRptdGapDscntNum() {
    AdjudicationComponent adjudication =
        TransformerTestUtilsV2.findAdjudicationByCategory(
            "https://bluebutton.cms.gov/resources/variables/rptd_gap_dscnt_num",
            eob.getItemFirstRep().getAdjudication());

    AdjudicationComponent compare =
        new AdjudicationComponent()
            .setCategory(
                new CodeableConcept()
                    .setCoding(
                        Arrays.asList(
                            new Coding(
                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                "discount",
                                "Discount"),
                            new Coding(
                                "https://bluebutton.cms.gov/resources/codesystem/adjudication",
                                "https://bluebutton.cms.gov/resources/variables/rptd_gap_dscnt_num",
                                "Gap Discount Amount"))))
            .setAmount(
                new Money().setValue(317.22).setCurrency(TransformerConstants.CODED_MONEY_USD));

    assertTrue(compare.equalsDeep(adjudication));
  }

  /**
   * Tests that the transformer sets the expected payment value.
   *
   * @throws Exception if there is a date parsing error
   */
  @Test
  public void shouldHavePayment() throws Exception {
    PaymentComponent compare =
        new PaymentComponent().setDate(new SimpleDateFormat("yyy-MM-dd").parse("2015-05-27"));

    assertTrue(compare.equalsDeep(eob.getPayment()));
  }

  /**
   * Verifies that the transformer sets the expected provider value.
   *
   * @throws Exception (indicates test failure)
   */
  @Test
  public void shouldHaveProvider() throws Exception {
    Reference compare =
        new Reference()
            .setIdentifier(
                new Identifier()
                    .setSystem(
                        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PRVDR_NPI))
                    .setValue("1023011079"));

    assertTrue(compare.equalsDeep(eob.getProvider()));
  }

  /**
   * Verifies that a claim processed through {@link PartDEventTransformerV2} has an adjudication
   * total slice set. This is required for CARIN compliance.
   */
  @Test
  public void shouldHaveAdjudicationTotalSliceSet() {
    String expectedSystem = C4BBAdjudication.DRUG_COST.getSystem();

    assertNotNull(eob.getTotal());
    assertFalse(eob.getTotal().isEmpty());
    // Get the total from the list with the expected system
    Optional<ExplanationOfBenefit.TotalComponent> total =
        eob.getTotal().stream()
            .filter(t -> t.getCategory().getCoding().get(0).getSystem().equals(expectedSystem))
            .findFirst();
    assertTrue(
        total.isPresent(),
        "Did not find expected total in EOB (expected adjudication total slice for DRUG COST)");
    assertEquals(
        C4BBAdjudication.DRUG_COST.toCode(),
        total.get().getCategory().getCoding().get(0).getCode());
    assertEquals(
        C4BBAdjudication.DRUG_COST.getDisplay(),
        total.get().getCategory().getCoding().get(0).getDisplay());
    // Check total amount
    assertEquals(new BigDecimal("550.00"), total.get().getAmount().getValue());
    assertEquals("USD", total.get().getAmount().getCurrency());
  }

  /**
   * Verifies that a claim processed through {@link PartDEventTransformerV2} has an adjudication
   * status total slice set. This is required for CARIN compliance. Note: In various examples the
   * value is 0, so the value was hardcoded to 0, as adding this was primarily to satisfy CARIN
   * compliance and not due to any specific user ask.
   */
  @Test
  public void shouldHaveAdjudicationStatusTotalSliceSet() {
    String expectedSystem = C4BBAdjudicationStatus.OTHER.getSystem();

    assertNotNull(eob.getTotal());
    assertFalse(eob.getTotal().isEmpty());
    // Get the total from the list with the expected system
    Optional<ExplanationOfBenefit.TotalComponent> total =
        eob.getTotal().stream()
            .filter(t -> t.getCategory().getCoding().get(0).getSystem().equals(expectedSystem))
            .findFirst();
    assertTrue(
        total.isPresent(),
        "Did not find expected total in EOB (expected adjudication status total slice for OTHER)");
    assertEquals(
        C4BBAdjudicationStatus.OTHER.toCode(),
        total.get().getCategory().getCoding().get(0).getCode());
    assertEquals(
        C4BBAdjudicationStatus.OTHER.getDisplay(),
        total.get().getCategory().getCoding().get(0).getDisplay());
    // Check total amount
    assertEquals(BigDecimal.valueOf(0), total.get().getAmount().getValue());
    assertEquals("USD", total.get().getAmount().getCurrency());
  }
}
