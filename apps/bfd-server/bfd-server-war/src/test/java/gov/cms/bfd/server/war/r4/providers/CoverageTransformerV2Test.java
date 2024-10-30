package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link CoverageTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class CoverageTransformerV2Test {

  /** The metrics registry. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The fhir context for parsing the file data. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** The beneficiary parsed from SAMPLE_A data. */
  private static Beneficiary beneficiary = null;

  /**
   * The current medicare segment. TODO: Should likely be moved to a local var and passed around as
   * needed.
   */
  private static MedicareSegment currSegment = null;

  /**
   * Coverage being tested. TODO: Should likely be moved to a local var and passed around as needed
   * to avoid cross-pollution between tests.
   */
  private static Coverage coverage = null;

  /** Date formatter that complies with std RIF date string. */
  private static final DateTimeFormatter RIF_DATE_FORMATTER =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("dd-MMM-yyyy")
          .toFormatter();

  /** The transformer under test. */
  private CoverageTransformerV2 coverageTransformer;

  /** Sets up the test dependencies shared across each test. */
  @BeforeEach
  public void setup() {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);
    coverageTransformer = new CoverageTransformerV2(metricRegistry);
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Calendar calen = Calendar.getInstance();
    calen.set(2021, 3, 17);
    beneficiary.setLastUpdated(calen.getTime().toInstant());
  }

  /** Standalone wrapper to output PART_A. */
  @Test
  public void outputTransformCoveragePartA() throws FHIRException {
    String partA = "part-a-567834";
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    assertNotNull(coverage);
    assertEquals("Coverage", coverage.getIdElement().getResourceType());
    assertEquals(partA, coverage.getIdPart());
    verifyMetrics("part_a");
  }

  /** Standalone wrapper to output PART_B. */
  @Test
  public void outputTransformCoveragePartB() throws FHIRException {
    String partB = "part-b-567834";
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    assertNotNull(coverage);
    assertEquals("Coverage", coverage.getIdElement().getResourceType());
    assertEquals(partB, coverage.getIdPart());
    verifyMetrics("part_b");
  }

  /** Standalone wrapper to output PART_C. */
  @Test
  public void outputTransformCoveragePartC() throws FHIRException {
    String partC = "part-c-567834";
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    assertNotNull(coverage);
    assertEquals("Coverage", coverage.getIdElement().getResourceType());
    assertEquals(partC, coverage.getIdPart());
    verifyMetrics("part_c");
  }

  /** Standalone wrapper to output PART_D. */
  @Test
  public void outputTransformCoveragePartD() throws FHIRException {
    String partD = "part-d-567834";
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    assertNotNull(coverage);
    assertEquals("Coverage", coverage.getIdElement().getResourceType());
    assertEquals(partD, coverage.getIdPart());
    verifyMetrics("part_d");
  }

  /** Standalone wrapper to output C4DIC. */
  @Test
  public void outputTransformCoverageC4Dic() throws FHIRException {
    String c4dicIdPartA = "c4dic-part-a-567834";
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4DIC);
    assertNotNull(coverage);
    assertEquals("Coverage", coverage.getIdElement().getResourceType());
    assertEquals(c4dicIdPartA, coverage.getIdPart());
    verifyMetrics("part_a");

    assertEquals(Coverage.CoverageStatus.CANCELLED, coverage.getStatus());
    assertEquals("Patient/567834", coverage.getSubscriber().getReference());
    assertNotNull(coverage.getPeriod().getStart());
    String mbiIdentifier =
        coverage.getIdentifier().getFirst().getType().getCodingFirstRep().getCode();
    assertEquals("MB", mbiIdentifier);

    List<Extension> extensions = coverage.getExtension();
    Extension baseExtension = extensions.getFirst();
    assertEquals(TransformerConstants.C4DIC_COLOR_PALETTE_EXT_URL, baseExtension.getUrl());
    List<Extension> colorExtensions = baseExtension.getExtension();
    List<String> expectedColors = Arrays.asList("#F4FEFF", "#092E86", "#3B9BFB");
    colorExtensions.stream()
        .allMatch(
            colorExtension -> {
              Coding coding = (Coding) colorExtension.getValue();
              return expectedColors.contains(coding.getCode());
            });

    Set<Extension> addInfoExtension =
        extensions.stream()
            .filter(
                extension -> extension.getUrl().equals(TransformerConstants.C4DIC_ADD_INFO_EXT_URL))
            .collect(Collectors.toSet());
    assertEquals(1, addInfoExtension.size());
    boolean containsAddInfoText =
        addInfoExtension.stream()
            .anyMatch(
                extension -> {
                  Annotation valueAnnotation = (Annotation) extension.getValue();
                  return valueAnnotation.getText().equals(TransformerConstants.C4DIC_ADD_INFO);
                });
    assertTrue(containsAddInfoText);

    boolean containsLogo =
        extensions.stream()
            .anyMatch(
                extension ->
                    extension.getUrl().equals(TransformerConstants.C4DIC_LOGO_EXT_URL)
                        && extension
                            .getValue()
                            .toString()
                            .equals(TransformerConstants.C4DIC_LOGO_URL));
    assertTrue(containsLogo);
  }

  // ==================
  // Begin PART A Tests
  // ==================

  /** Tests that the transformer sets the expected coverage id. */
  @Test
  public void shouldSetIDPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyID("part-a-567834");
  }

  /** Tests that the transformer sets the expected metadata (lastUpdated and profile). */
  @Test
  public void shouldSetCorrectProfileAndDatePartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyMeta(ProfileConstants.C4BB_COVERAGE_URL);
  }

  /** Tests that the transformer sets the expected extension entries. */
  @Test
  public void shouldSetExtensionsPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyExtensionsPartA();
  }

  /** Tests that the expected extensions exist. */
  private static void verifyExtensionsPartA() {
    assertEquals(30, coverage.getExtension().size());

    // ms_cd
    // rfrnc_yr
    // dual_01 thru dual_12
    verifyCommonExtensions();

    // a_trm_cd
    verifyCodedExtension(
        "https://bluebutton.cms.gov/resources/variables/a_trm_cd", "9", "Other Termination");

    // orec
    verifyCodedExtension(
        "https://bluebutton.cms.gov/resources/variables/orec",
        "1",
        "Disability insurance benefits (DIB)");

    // crec
    verifyCodedExtension(
        "https://bluebutton.cms.gov/resources/variables/crec",
        "1",
        "Disability insurance benefits (DIB)");

    // esrd_ind
    verifyCodedExtension("https://bluebutton.cms.gov/resources/variables/esrd_ind", "N", null);

    // buyin01 thru buyin12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/buyin%02d", i);
      verifyCodedExtension(url, "C", "Part A and Part B state buy-in");
    }
  }

  /** Tests that the transformer sets the expected coverage status. */
  @Test
  public void verifyCoverageStatusPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyCoverageStatus("cancelled");
  }

  /** Tests that the transformer sets the expected type coding. */
  @Test
  public void verifyTypePartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyType();
  }

  /** Tests that the transformer sets the expected subscriber information. */
  @Test
  public void verifySubscriberPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifySubscriber();
  }

  /** Tests that the transformer sets the expected relationship data. */
  @Test
  public void verifyRelationshipPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyRelationship();
  }

  /** Tests that the transformer sets the expected period date. */
  @Test
  public void verifyPeriodPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyPeriod(Optional.of("17-Mar-2020"), Optional.of("17-JUN-2020"));
  }

  /** Tests that the transformer sets the expected payor data. */
  @Test
  public void verifyPayorPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyPayor();
  }

  /** Tests that the transformer sets the expected coverage class. */
  @Test
  public void verifyCoverageClassPartA() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    verifyCoverageClass("Part A");
  }

  /**
   * Verifies that {@link CoverageTransformerV2#transform} works as expected when run against the
   * {@link StaticRifResource#SAMPLE_A_BENES} {@link Coverage} with a reference year field not
   * found.
   */
  @Test
  public void verifyPartAWithoutReferenceYear() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    Beneficiary newBeneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Calendar calen = Calendar.getInstance();
    calen.set(2021, 3, 17);
    newBeneficiary.setLastUpdated(calen.getTime().toInstant());
    newBeneficiary.setBeneEnrollmentReferenceYear(Optional.empty());

    Coverage newCoverage =
        coverageTransformer.transform(MedicareSegment.PART_A, newBeneficiary, Profile.C4BB);
    checkForNoYearlyDate(newCoverage);
  }

  // ==================
  // Begin PART B Tests
  // ==================

  /** Tests that the transformer sets the expected coverage id. */
  @Test
  public void shouldSetIDPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyID("part-b-567834");
  }

  /** Tests that the transformer sets the expected metadata (lastUpdated and profile). */
  @Test
  public void shouldSetCorrectProfileAndDatePartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyMeta(ProfileConstants.C4BB_COVERAGE_URL);
  }

  /** Tests that the transformer sets the expected extension entries. */
  @Test
  public void shouldSetExtensionsPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyExtensionsPartB();
  }

  /** Tests that the expected extensions exist. */
  private static void verifyExtensionsPartB() {
    assertEquals(27, coverage.getExtension().size());

    // ms_cd
    // rfrnc_yr
    // dual_01 thru dual_12
    verifyCommonExtensions();

    // b_trm_cd
    verifyCodedExtension(
        "https://bluebutton.cms.gov/resources/variables/b_trm_cd", "0", "Not Terminated");

    // buyin01 thru buyin12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/buyin%02d", i);
      verifyCodedExtension(url, "C", "Part A and Part B state buy-in");
    }
  }

  /** Tests that the transformer sets the expected coverage status. */
  @Test
  public void verifyCoverageStatusPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyCoverageStatus("active");
  }

  /** Tests that the transformer sets the expected type coding. */
  @Test
  public void verifyTypePartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyType();
  }

  /** Tests that the transformer sets the expected subscriber information. */
  @Test
  public void verifySubscriberPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifySubscriber();
  }

  /** Tests that the transformer sets the expected relationship data. */
  @Test
  public void verifyRelationshipPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyRelationship();
  }

  /** Tests that the transformer sets the expected period date. */
  @Test
  public void verifyPeriodPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyPeriod(Optional.of("17-JUL-2021"), Optional.of("17-AUG-2022"));
  }

  /** Tests that the transformer sets the expected payor data. */
  @Test
  public void verifyPayorPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyPayor();
  }

  /** Tests that the transformer sets the expected coverage class. */
  @Test
  public void verifyCoverageClassPartB() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    verifyCoverageClass("Part B");
  }

  // ==================
  // Begin PART C Tests
  // ==================

  /** Tests that the transformer sets the expected coverage id. */
  @Test
  public void shouldSetIDPartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyID("part-c-567834");
  }

  /** Tests that the transformer sets the expected metadata (lastUpdated and profile). */
  @Test
  public void shouldSetCorrectProfileAndDatePartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyMeta(ProfileConstants.C4BB_COVERAGE_URL);
  }

  /** Tests that the transformer sets the expected extension entries. */
  @Test
  public void shouldSetExtensionsPartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyExtensionsPartC();
  }

  /** Tests that the expected extensions exist. */
  private static void verifyExtensionsPartC() {
    assertEquals(59, coverage.getExtension().size());

    // ptc_cntrct_id_01 thru ptc_cntrct_id_12
    for (int i = 1; i < 13; i++) {
      String url =
          String.format("https://bluebutton.cms.gov/resources/variables/ptc_cntrct_id_%02d", i);
      String code = i < 11 ? "H1225" : "0";
      verifyCodedExtension(url, code, null);
    }

    // ptc_pbp_id_01 thru ptc_pbp_id_10
    for (int i = 1; i < 11; i++) {
      String url =
          String.format("https://bluebutton.cms.gov/resources/variables/ptc_pbp_id_%02d", i);
      verifyCodedExtension(url, "003", null);
    }

    // ptc_plan_type_cd_01 thru ptc_plan_type_cd_10
    for (int i = 1; i < 11; i++) {
      String url =
          String.format("https://bluebutton.cms.gov/resources/variables/ptc_plan_type_cd_%02d", i);
      verifyCodedExtension(url, "00", null);
    }

    // hmo_ind_01 thru hmo_ind_12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/hmo_ind_%02d", i);
      String display =
          i < 11 ? "Non-lock-in, CMS to process provider claims" : "Not a member of an HMO";
      String code = i < 11 ? "1" : "0";
      verifyCodedExtension(url, code, display);
    }

    // dual_01 thru dual_12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/dual_%02d", i);
      verifyCodedExtension(
          url,
          "**",
          "Enrolled in Medicare A and/or B, but no Part D enrollment data for the beneficiary. (This status was indicated as 'XX' for 2006-2009)");
    }
  }

  /** Tests that the transformer sets the expected coverage status. */
  @Test
  public void verifyCoverageStatusPartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyCoverageStatus("active");
  }

  /** Tests that the transformer sets the expected type coding. */
  @Test
  public void verifyTypePartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyType();
  }

  /** Tests that the transformer sets the expected subscriber information. */
  @Test
  public void verifySubscriberPartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifySubscriber();
  }

  /** Tests that the transformer sets the expected relationship data. */
  @Test
  public void verifyRelationshipPartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyRelationship();
  }

  /** Tests that the transformer sets the expected payor data. */
  @Test
  public void verifyPayorPartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyPayor();
  }

  /** Tests that the transformer sets the expected coverage class. */
  @Test
  public void verifyCoverageClassPartC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    verifyCoverageClass("Part C");
  }

  // ==================
  // Begin PART D Tests
  // ==================

  /** Tests that the transformer sets the expected coverage id. */
  @Test
  public void shouldSetIDPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyID("part-d-567834");
  }

  /** Tests that the transformer sets the expected metadata (lastUpdated and profile). */
  @Test
  public void shouldSetCorrectProfileAndDatePartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyMeta(ProfileConstants.C4BB_COVERAGE_URL);
  }

  /** Tests that the transformer sets the expected period date. */
  @Test
  public void verifyPeriodPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyPeriod(Optional.of("17-FEB-2021"), Optional.of("17-NOV-2022"));
  }

  /** Tests that the transformer sets the expected extension entries. */
  @Test
  public void shouldSetExtensionsPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyExtensionsPartD(72);
  }

  /**
   * Tests that the expected extensions exist.
   *
   * @param expectedSize the expected number of extensions expected
   */
  private static void verifyExtensionsPartD(int expectedSize) {
    assertTrue(coverage.getExtension().size() == expectedSize);

    // ms_cd
    // rfrnc_yr
    // dual_01 thru dual_12
    verifyCommonExtensions();

    // ptdcntrct01 thru ptdcntrct12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/ptdcntrct%02d", i);
      String code = i < 12 ? "S4607" : "0";
      String display = i < 12 ? null : "Not Medicare enrolled for the month";
      verifyCodedExtension(url, code, display);
    }

    // ptdpbpid01 thru ptdpbpid11
    for (int i = 1; i < 12; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/ptdpbpid%02d", i);
      verifyCodedExtension(url, "003", null);
    }

    // sgmtid01 thru sgmtid11
    for (int i = 1; i < 12; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/sgmtid%02d", i);
      verifyCodedExtension(url, "000", null);
    }

    // cstshr01 thru cstshr12
    ArrayList<String> displayList =
        new ArrayList<String>(
            Arrays.asList(
                "",
                "Not Medicare enrolled for the month",
                "Not Medicare enrolled for the month",
                "Beneficiary enrolled in Parts A and/or B, and Part D; deemed eligible for LIS with 100% premium subsidy and no copayment",
                "Beneficiary enrolled in Parts A and/or B, and Part D; deemed eligible for LIS with 100% premium subsidy and no copayment",
                "Beneficiary enrolled in Parts A and/or B, and Part D; enrolled in LIS with 100% premium subsidy and 15% copayment",
                "Beneficiary enrolled in Parts A and/or B, and Part D; enrolled in LIS with 75% premium subsidy and 15% copayment",
                "Beneficiary enrolled in Parts A and/or B, and Part D; enrolled in LIS with 50% premium subsidy and 15% copayment",
                "Beneficiary enrolled in Parts A and/or B, and Part D; enrolled in LIS with 25% premium subsidy and 15% copayment",
                "Beneficiary enrolled in Parts A and/or B, and Part D; deemed eligible for LIS with 100% premium subsidy and no copayment",
                "Beneficiary enrolled in Parts A and/or B, and Part D; deemed eligible for LIS with 100% premium subsidy and low copayment",
                "Not Medicare enrolled for the month",
                "Not Medicare enrolled for the month"));

    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/cstshr%02d", i);
      String code =
          i < 3
              ? "00"
              : i < 5
                  ? "01"
                  : i < 9 ? String.format("%02d", i) : i == 9 ? "01" : i == 10 ? "02" : "00";
      verifyCodedExtension(url, code, displayList.get(i));
    }

    // rdsind01 thru rdsind10
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/rdsind%02d", i);
      String code = i > 11 ? "N" : "Y";
      String display =
          i > 11
              ? "No employer subsidization for the retired beneficiary"
              : "Employer subsidized for the retired beneficiary";
      verifyCodedExtension(url, code, display);
    }
  }

  /** Tests that the transformer sets the expected coverage status. */
  @Test
  public void verifyCoverageStatusPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyCoverageStatus("active");
  }

  /** Tests that the transformer sets the expected type coding. */
  @Test
  public void verifyTypePartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyType();
  }

  /** Tests that the transformer sets the expected subscriber information. */
  @Test
  public void verifySubscriberPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifySubscriber();
  }

  /** Tests that the transformer sets the expected relationship data. */
  @Test
  public void verifyRelationshipPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyRelationship();
  }

  /** Tests that the transformer sets the expected payor data. */
  @Test
  public void verifyPayorPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyPayor();
  }

  /** Tests that the transformer sets the expected coverage class. */
  @Test
  public void verifyCoverageClassPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    verifyCoverageClass("Part D");
  }

  // ==================
  // Begin C4DIC tests
  // ==================

  /** Tests that the transformer sets the expected coverage id. */
  @Test
  public void shouldSetC4DICPartBID() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4DIC);
    verifyID("c4dic-part-b-567834");
  }

  /** Tests that the transformer sets the expected metadata (lastUpdated and profile). */
  @Test
  public void shouldSetCorrectProfileAndDateC4DIC() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4DIC);
    verifyMeta(ProfileConstants.C4DIC_COVERAGE_URL + ProfileConstants.C4DIC_VERSION_SUFFIX);
  }

  // ==================
  // Begin Common tests
  // ==================

  /** Tests that the transformer filters out the C4DIC profile. */
  @Test
  public void shouldFilterOutC4DicProfile() {
    List<IBaseResource> coverages = transformCoverageAll(Profile.C4BB, true);
    assertEquals(4, coverages.size());
  }

  /** Tests that the transformer separates each plan into a Coverage resource. */
  @Test
  public void shouldSeparatePlansPerResource() {
    List<IBaseResource> coverages = transformCoverageAll(Profile.C4DIC, true);
    assertEquals(4, coverages.size());
  }

  /** Tests that the transformer returns all profiles. */
  @Test
  public void shouldFilterOutC4BBProfile() {
    List<IBaseResource> coverages = transformCoverageAll(Profile.C4DIC, true);
    assertEquals(4, coverages.size());
  }

  /**
   * Verifies the coverage id.
   *
   * @param idRef the expected id
   */
  private static void verifyID(String idRef) {
    assertEquals("Coverage", coverage.getIdElement().getResourceType());
    assertEquals("Coverage/" + idRef, coverage.getIdElement().toString());
  }

  /**
   * Verifies the metadata has some last updated value and the profile contains {@link
   * ProfileConstants#C4BB_COVERAGE_URL}.
   *
   * @param coverageUrl coverage URL
   */
  private static void verifyMeta(String coverageUrl) {
    assertNotNull(coverage.getMeta().getLastUpdated());

    // The base CanonicalType doesn't seem to compare correctly so lets convert it to a string
    assertTrue(
        coverage.getMeta().getProfile().stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(coverageUrl)));
  }

  /** Verify that various expected extensions exist. */
  private static void verifyCommonExtensions() {
    // ms_cd
    verifyCodedExtension(
        "https://bluebutton.cms.gov/resources/variables/ms_cd", "20", "Disabled without ESRD");

    // rfrnc_yr
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/rfrnc_yr", coverage.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/rfrnc_yr", new DateType("2018"));

    assertTrue(compare.equalsDeep(ex));

    // dual_01 thru dual_12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/dual_%02d", i);
      verifyCodedExtension(
          url,
          "**",
          "Enrolled in Medicare A and/or B, but no Part D enrollment data for the beneficiary. (This status was indicated as 'XX' for 2006-2009)");
    }
  }

  /**
   * Verify that a coded extension exists with the given values.
   *
   * @param url the url to look up the extension by
   * @param code the code the extension should have
   * @param display the display the extension should have
   */
  private static void verifyCodedExtension(String url, String code, String display) {
    Extension ex = TransformerTestUtilsV2.findExtensionByUrl(url, coverage.getExtension());

    Extension compare = new Extension(url, new Coding(url, code, display));

    assertTrue(compare.equalsDeep(ex));
  }

  /**
   * Verifies coverage status.
   *
   * @param status the expected status
   */
  private static void verifyCoverageStatus(String status) {
    assertEquals(status, coverage.getStatus().toCode());
  }

  /** Verifies the type has the expected coding. */
  private static void verifyType() {
    // Test Category here
    CodeableConcept typ = coverage.getType();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/v3-ActCode", "SUBSIDIZ", null)));
    assertTrue(compare.equalsDeep(typ));
  }

  /** Verifies the subscriber has the expected patient reference information and id. */
  private static void verifySubscriber() {
    assertEquals("3456789", coverage.getSubscriberId());
    Reference ex = coverage.getBeneficiary();
    Reference compare = TransformerUtilsV2.referencePatient(567834L);
    assertTrue(compare.equalsDeep(ex));
  }

  /** Verifies the relationship coding is as expected. */
  private static void verifyRelationship() {
    CodeableConcept typ = coverage.getRelationship();
    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/subscriber-relationship",
                        "self",
                        "Self")));
    assertTrue(compare.equalsDeep(typ));
  }

  /**
   * Verifies the period date is as expected.
   *
   * @param startDate option {@link String} denoting FHIR {@link Period} start date
   * @param endDate option {@link String} denoting FHIR {@link Period} end date
   */
  private static void verifyPeriod(Optional<String> startDate, Optional<String> endDate) {
    Period per = coverage.getPeriod();
    Period compare = new Period();
    startDate.ifPresent(value -> TransformerUtilsV2.setPeriodStart(compare, parseDate(value)));
    endDate.ifPresent(value -> TransformerUtilsV2.setPeriodEnd(compare, parseDate(value)));
    assertTrue(compare.equalsDeep(per));
  }

  /** Verifies the number of payers and the identifier for the payor is as expected. */
  private static void verifyPayor() {
    List<Reference> payers = coverage.getPayor();
    assertNotNull(payers);
    assertEquals(1, payers.size());
    Coverage compare = new Coverage();
    compare
        .addPayor()
        .setIdentifier(new Identifier().setValue("Centers for Medicare and Medicaid Services"));
    assertTrue(compare.getPayor().get(0).equalsDeep(coverage.getPayor().get(0)));
  }

  /**
   * Verifies the correct number of coverage class entries and the entries have the correct data.
   *
   * @param className the class name expected for the second coverage entry
   */
  private static void verifyCoverageClass(String className) {
    assertEquals(2, coverage.getClass_().size());

    Coverage compare = new Coverage();
    compare
        .addClass_()
        .setValue("Medicare")
        .getType()
        .addCoding()
        .setCode("group")
        .setDisplay("Group")
        .setSystem("http://terminology.hl7.org/CodeSystem/coverage-class");
    compare
        .addClass_()
        .setValue(className)
        .getType()
        .addCoding()
        .setCode("plan")
        .setDisplay("Plan")
        .setSystem("http://terminology.hl7.org/CodeSystem/coverage-class");

    assertTrue(compare.getClass_().get(0).equalsDeep(coverage.getClass_().get(0)));
    assertTrue(compare.getClass_().get(1).equalsDeep(coverage.getClass_().get(1)));
  }

  /**
   * Standalone wrapper to create and optionally print out a MedicareSegment coverage.
   *
   * @param medSeg the medicare segment
   * @param showJson {@code true} if the json should be printed to stdout
   * @param enabledProfile the CARIN {@link Profile} to use
   * @throws FHIRException if there is an issue transforming the coverage
   */
  public void transformCoverage(MedicareSegment medSeg, boolean showJson, Profile enabledProfile)
      throws FHIRException {
    if (currSegment == null || currSegment != medSeg) {
      coverage = coverageTransformer.transform(medSeg, beneficiary, enabledProfile);
      currSegment = medSeg;
    }
    if (showJson && coverage != null) {
      System.out.println(fhirContext.newJsonParser().encodeResourceToString(coverage));
    }
  }

  /**
   * Wrapper to transform all coverages for the included profiles.
   *
   * @param profile profile to include
   * @param showJson {@code true} if the json should be printed to stdout
   * @return list of coverages
   * @throws FHIRException if there is an issue transforming the coverage
   */
  private List<IBaseResource> transformCoverageAll(Profile profile, boolean showJson)
      throws FHIRException {
    List<IBaseResource> coverages = coverageTransformer.transform(beneficiary, profile);
    if (showJson) {
      for (IBaseResource coverage : coverages) {
        System.out.println(fhirContext.newJsonParser().encodeResourceToString(coverage));
      }
    }

    return coverages;
  }

  /**
   * Verifies that the specified {@link MedicareSegment#PART_A} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   */
  @Disabled("Test only used to verify support for Integration Test")
  @Test
  public void verifyIntegrationPartA() {
    // transformCoverage(MedicareSegment.PART_A, false, Profile.C4BB);
    coverage = coverageTransformer.transform(MedicareSegment.PART_A, beneficiary, Profile.C4BB);
    assertPartAMatches(beneficiary, coverage);
  }

  /**
   * Verifies that the specified {@link MedicareSegment#PART_B} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   */
  @Disabled("Test only used to verify support for Integration Test")
  @Test
  public void verifyIntegrationPartB() {
    // transformCoverage(MedicareSegment.PART_B, false, Profile.C4BB);
    coverage = coverageTransformer.transform(MedicareSegment.PART_B, beneficiary, Profile.C4BB);
    assertPartBMatches(beneficiary, coverage);
  }

  /**
   * Verifies that the specified {@link MedicareSegment#PART_C} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   */
  @Disabled("Test only used to verify support for Integration Test")
  @Test
  public void verifyIntegrationPartC() {
    // transformCoverage(MedicareSegment.PART_C, false, Profile.C4BB);
    coverage = coverageTransformer.transform(MedicareSegment.PART_C, beneficiary, Profile.C4BB);
    assertPartCMatches(beneficiary, coverage);
  }

  /**
   * Verifies that the specified {@link MedicareSegment#PART_D} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   */
  @Disabled("Test only used to verify support for Integration Test")
  @Test
  public void verifyIntegrationPartD() {
    coverage = coverageTransformer.transform(MedicareSegment.PART_D, beneficiary, Profile.C4BB);
    // transformCoverage(MedicareSegment.PART_D, false, Profile.C4BB);
    assertPartDMatches(beneficiary, coverage);
  }

  /**
   * Asserts that the data for a part A beneficiary matches the expected values.
   *
   * <p>The following 4 aggregated tests will be called from the R4CoverageResourceProviderIT
   * (integration tests); as such they may have different results from the standalone
   * CoverageTransformerV2.
   *
   * <p>TODO: Move the shared items to a base test or util instead of having a test call another
   * test
   *
   * @param inBeneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param inCoverage the {@link Coverage} to verify
   */
  static void assertPartAMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_A;
    assertNotNull(coverage);
    assertNotNull(beneficiary);

    verifyCoverageClass("Part A");
    verifyMeta(ProfileConstants.C4BB_COVERAGE_URL);
    verifyExtensionsPartA();
    verifyCoverageStatus("cancelled");
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPeriod(Optional.of("17-Mar-2020"), Optional.of("17-JUN-2020"));
    verifyPayor();
  }

  /**
   * Asserts that the data for a part B beneficiary matches the expected values.
   *
   * @param inBeneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param inCoverage the {@link Coverage} to verify
   */
  static void assertPartBMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_B;
    assertNotNull(coverage);
    assertNotNull(beneficiary);
    verifyCoverageClass("Part B");

    verifyMeta(ProfileConstants.C4BB_COVERAGE_URL);
    verifyExtensionsPartB();
    verifyCoverageStatus("active");
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPeriod(Optional.of("17-JUL-2021"), Optional.of("17-AUG-2022"));
    verifyPayor();
  }

  /**
   * Asserts that the data for a part C beneficiary matches the expected values.
   *
   * @param inBeneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param inCoverage the {@link Coverage} to verify
   */
  static void assertPartCMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_C;
    assertNotNull(coverage);
    assertNotNull(beneficiary);
    verifyCoverageClass("Part C");
    verifyExtensionsPartC();
    verifyCoverageStatus("active");
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPayor();
  }

  /**
   * Asserts that the data for a part D beneficiary matches the expected values.
   *
   * @param inBeneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param inCoverage the {@link Coverage} to verify
   */
  static void assertPartDMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_D;
    assertNotNull(coverage);
    assertNotNull(beneficiary);
    verifyCoverageClass("Part D");
    verifyExtensionsPartD(84);
    verifyCoverageStatus("active");
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPayor();
  }

  /**
   * Verifies the specified extension does not exist for the specified coverage object.
   *
   * @param inCoverage the coverage to check the extensions of
   * @param url the extension url (to identify it) that should not exist
   */
  private static void verifyCodedExtensionDoesNotExist(Coverage inCoverage, String url) {
    Optional<Extension> ex =
        inCoverage.getExtension().stream().filter(e -> url.equals(e.getUrl())).findFirst();

    assertEquals(true, ex.isEmpty());
  }

  /**
   * Verifies that a number of yearly extensions do not exist for the specified coverage object.
   *
   * @param inCoverage the coverage object to check
   */
  private static void checkForNoYearlyDate(Coverage inCoverage) {
    // dual_01 thru dual_12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/dual_%02d", i);
      verifyCodedExtensionDoesNotExist(inCoverage, url);
    }

    // buyin01 thru buyin12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/buyin%02d", i);
      verifyCodedExtensionDoesNotExist(inCoverage, url);
    }

    // ptdcntrct01 thru ptdcntrct12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/ptdcntrct%02d", i);
      verifyCodedExtensionDoesNotExist(inCoverage, url);
    }

    // ptdpbpid01 thru ptdpbpid11
    for (int i = 1; i < 12; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/ptdpbpid%02d", i);
      verifyCodedExtensionDoesNotExist(inCoverage, url);
    }

    // sgmtid01 thru sgmtid11
    for (int i = 1; i < 12; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/sgmtid%02d", i);
      verifyCodedExtensionDoesNotExist(inCoverage, url);
    }

    // cstshr01 thru cstshr12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/cstshr%02d", i);
      verifyCodedExtensionDoesNotExist(inCoverage, url);
    }

    // rdsind01 thru rdsind12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/rdsind%02d", i);
      verifyCodedExtensionDoesNotExist(inCoverage, url);
    }
  }

  /**
   * Parse an {@link Optional} {@link LocalDate} from a {@link String}.
   *
   * @param dateText the date string to parse
   * @return an {@link Optional} populated with a {@link LocalDate} if the input has data, or an
   *     empty Optional if not
   */
  public static Optional<LocalDate> parseDate(String dateText) {
    if (dateText.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(dateText, RIF_DATE_FORMATTER));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Verify metrics were called with the specified part as part of the metric name.
   *
   * @param partSubstring the part substring
   */
  private void verifyMetrics(String partSubstring) {
    String expectedTimerName =
        coverageTransformer.getClass().getSimpleName() + ".transform." + partSubstring;
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
  }
}
