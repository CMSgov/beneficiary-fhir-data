package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.CoverageTransformerV2}. */
public final class CoverageTransformerV2Test {

  private static final FhirContext fhirContext = FhirContext.forR4();
  private static Beneficiary beneficiary = null;
  private static MedicareSegment currSegment = null;
  private static Coverage coverage = null;

  @Before
  public void setup() {
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

  /** Standalone wrapper to output PART_A */
  @Ignore
  @Test
  public void outputTransformCoveragePartA() throws FHIRException {
    transformCoverage(MedicareSegment.PART_A, true);
    Assert.assertNotNull(coverage);
  }

  /** Standalone wrapper to output PART_B */
  @Ignore
  @Test
  public void outputTransformCoveragePartB() throws FHIRException {
    transformCoverage(MedicareSegment.PART_B, true);
    Assert.assertNotNull(coverage);
  }

  /** Standalone wrapper to output PART_C */
  @Ignore
  @Test
  public void outputTransformCoveragePartC() throws FHIRException {
    transformCoverage(MedicareSegment.PART_C, true);
    Assert.assertNotNull(coverage);
  }

  /** Standalone wrapper to output PART_D */
  @Ignore
  @Test
  public void outputTransformCoveragePartD() throws FHIRException {
    transformCoverage(MedicareSegment.PART_D, true);
    Assert.assertNotNull(coverage);
  }

  // ==================
  // Begin PART A Tests
  // ==================
  @Test
  public void shouldSetIDPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyID("part-a-" + TransformerTestUtilsV2.getGoldenBeneId());
  }

  @Test
  public void shouldSetCorrectProfileAndDatePartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyMeta();
  }

  @Test
  public void shouldSetExtensionsPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyExtensionsPartA();
  }

  private static void verifyExtensionsPartA() {
    Assert.assertEquals(30, coverage.getExtension().size());

    // ms_cd
    // rfrnc_yr
    // dual_01 thru dual_12
    verifyCommonExtensions();

    // a_trm_cd
    verifyCodedExtension(
        "https://bluebutton.cms.gov/resources/variables/a_trm_cd", "0", "Not Terminated");

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

  @Test
  public void verifyCoverageStatusPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyCoverageStatus();
  }

  @Test
  public void verifyTypePartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyType();
  }

  @Test
  public void verifySubscriberPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifySubscriber();
  }

  @Test
  public void verifyRelationshipPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyRelationship();
  }

  @Test
  public void verifyPeriodPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyPeriod();
  }

  @Test
  public void verifyPayorPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyPayor();
  }

  @Test
  public void verifyCoverageClassPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyCoverageClass("Part A");
  }

  @Test
  public void verifyCoverageContractPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    verifyCoverageContract("part-a");
  }

  // ==================
  // Begin PART B Tests
  // ==================
  @Test
  public void shouldSetIDPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyID("part-b-" + TransformerTestUtilsV2.getGoldenBeneId());
  }

  @Test
  public void shouldSetCorrectProfileAndDatePartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyMeta();
  }

  @Test
  public void shouldSetExtensionsPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyExtensionsPartB();
  }

  private static void verifyExtensionsPartB() {
    Assert.assertEquals(27, coverage.getExtension().size());

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

  @Test
  public void verifyCoverageStatusPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyCoverageStatus();
  }

  @Test
  public void verifyTypePartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyType();
  }

  @Test
  public void verifySubscriberPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifySubscriber();
  }

  @Test
  public void verifyRelationshipPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyRelationship();
  }

  @Test
  public void verifyPeriodPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyPeriod();
  }

  @Test
  public void verifyPayorPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyPayor();
  }

  @Test
  public void verifyCoverageClassPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    verifyCoverageClass("Part B");
  }

  // ==================
  // Begin PART C Tests
  // ==================
  @Test
  public void shouldSetIDPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyID("part-c-" + TransformerTestUtilsV2.getGoldenBeneId());
  }

  @Test
  public void shouldSetCorrectProfileAndDatePartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyMeta();
  }

  @Test
  public void shouldSetExtensionsPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyExtensionsPartC();
  }

  private static void verifyExtensionsPartC() {
    Assert.assertEquals(57, coverage.getExtension().size());

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

  @Test
  public void verifyCoverageStatusPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyCoverageStatus();
  }

  @Test
  public void verifyTypePartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyType();
  }

  @Test
  public void verifySubscriberPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifySubscriber();
  }

  @Test
  public void verifyRelationshipPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyRelationship();
  }

  @Test
  public void verifyPayorPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyPayor();
  }

  @Test
  public void verifyCoverageClassPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    verifyCoverageClass("Part C");
  }

  // ==================
  // Begin PART D Tests
  // ==================
  @Test
  public void shouldSetIDPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyID("part-d-" + TransformerTestUtilsV2.getGoldenBeneId());
  }

  @Test
  public void shouldSetCorrectProfileAndDatePartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyMeta();
  }

  @Test
  public void shouldSetExtensionsPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyExtensionsPartD(72);
  }

  private static void verifyExtensionsPartD(int expectedSize) {
    Assert.assertTrue(coverage.getExtension().size() == expectedSize);

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

  @Test
  public void verifyCoverageStatusPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyCoverageStatus();
  }

  @Test
  public void verifyTypePartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyType();
  }

  @Test
  public void verifySubscriberPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifySubscriber();
  }

  @Test
  public void verifyRelationshipPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyRelationship();
  }

  @Test
  public void verifyPayorPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyPayor();
  }

  @Test
  public void verifyCoverageClassPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    verifyCoverageClass("Part D");
  }

  // ==================
  // Begin Common tests
  // ==================
  private static void verifyID(String idRef) {
    Assert.assertEquals("Coverage", coverage.getIdElement().getResourceType());
    Assert.assertEquals("Coverage/" + idRef, coverage.getIdElement().toString());
  }

  private static void verifyMeta() {
    Assert.assertNotNull(coverage.getMeta().getLastUpdated());

    // The base CanonicalType doesn't seem to compare correctly so lets convert it to a string
    Assert.assertTrue(
        coverage.getMeta().getProfile().stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_COVERAGE_URL)));
  }

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

    Assert.assertTrue(compare.equalsDeep(ex));

    // dual_01 thru dual_12
    for (int i = 1; i < 13; i++) {
      String url = String.format("https://bluebutton.cms.gov/resources/variables/dual_%02d", i);
      verifyCodedExtension(
          url,
          "**",
          "Enrolled in Medicare A and/or B, but no Part D enrollment data for the beneficiary. (This status was indicated as 'XX' for 2006-2009)");
    }
  }

  private static void verifyCodedExtension(String url, String code, String display) {
    Extension ex = TransformerTestUtilsV2.findExtensionByUrl(url, coverage.getExtension());

    Extension compare = new Extension(url, new Coding(url, code, display));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  private static void verifyCoverageStatus() {
    Assert.assertEquals("active", coverage.getStatus().toCode());
  }

  private static void verifyType() {
    // Test Category here
    CodeableConcept typ = coverage.getType();

    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/v3-ActCode", "SUBSIDIZ", null)));
    Assert.assertTrue(compare.equalsDeep(typ));
  }

  private static void verifySubscriber() {
    Assert.assertEquals("3456789", coverage.getSubscriberId());
    Reference ex = coverage.getBeneficiary();
    Reference compare =
        TransformerUtilsV2.referencePatient(TransformerTestUtilsV2.getGoldenBeneId());
    Assert.assertTrue(compare.equalsDeep(ex));
  }

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
    Assert.assertTrue(compare.equalsDeep(typ));
  }

  private static void verifyPeriod() {
    Period per = coverage.getPeriod();
    Period compare = new Period();
    TransformerUtilsV2.setPeriodStart(compare, LocalDate.parse("1963-10-03"));
    Assert.assertTrue(compare.equalsDeep(per));
  }

  private static void verifyPayor() {
    List<Reference> payers = coverage.getPayor();
    Assert.assertNotNull(payers);
    Assert.assertEquals(1, payers.size());
    Coverage compare = new Coverage();
    compare
        .addPayor()
        .setIdentifier(new Identifier().setValue("Centers for Medicare and Medicaid Services"));
    Assert.assertTrue(compare.getPayor().get(0).equalsDeep(coverage.getPayor().get(0)));
  }

  private static void verifyCoverageClass(String className) {
    Assert.assertEquals(2, coverage.getClass_().size());

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

    Assert.assertTrue(compare.getClass_().get(0).equalsDeep(coverage.getClass_().get(0)));
    Assert.assertTrue(compare.getClass_().get(1).equalsDeep(coverage.getClass_().get(1)));
  }

  private static void verifyCoverageContract(String partId) {
    Assert.assertEquals(2, coverage.getContract().size());
    Coverage compare = new Coverage();
    compare.addContract().setId("contract1");
    compare.addContract().setReference("Coverage/" + partId + "-contract1");
    Assert.assertTrue(compare.getContract().get(0).equalsDeep(coverage.getContract().get(0)));
    Assert.assertTrue(compare.getContract().get(1).equalsDeep(coverage.getContract().get(1)));
  }

  /** Standalone wrapper to create and optionall printout a MedicareSegment coverage */
  public static void transformCoverage(MedicareSegment medSeg, boolean showJson)
      throws FHIRException {
    if (currSegment == null || currSegment != medSeg) {
      coverage = CoverageTransformerV2.transform(new MetricRegistry(), medSeg, beneficiary);
      currSegment = medSeg;
    }
    if (showJson && coverage != null) {
      System.out.println(fhirContext.newJsonParser().encodeResourceToString(coverage));
    }
  }

  /**
   * Verifies that the specified {@link
   * gov.cms.bfd.server.war.stu3.providers.MedicareSegment#PART_A} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param coverage the {@link Coverage} to verify
   */
  @Ignore // test only used to verify support for IT (intgration Test)
  @Test
  public void verifyIntegrationPartA() {
    transformCoverage(MedicareSegment.PART_A, false);
    assertPartAMatches(beneficiary, coverage);
  }

  @Ignore // test only used to verify support for IT (intgration Test)
  @Test
  public void verifyIntegrationPartB() {
    transformCoverage(MedicareSegment.PART_B, false);
    assertPartBMatches(beneficiary, coverage);
  }

  @Ignore // test only used to verify support for IT (intgration Test)
  @Test
  public void verifyIntegrationPartC() {
    transformCoverage(MedicareSegment.PART_C, false);
    assertPartCMatches(beneficiary, coverage);
  }

  @Ignore // test only used to verify support for IT (intgration Test)
  @Test
  public void verifyIntegrationPartD() {
    transformCoverage(MedicareSegment.PART_D, false);
    assertPartDMatches(beneficiary, coverage);
  }

  /*
   ** The following 4 aggregated tests will be called from the R4CoverageResourceProviderIT
   ** (integration tests); as such they may have different results from the standalone
   ** CoverageTransformerV2.
   */
  static void assertPartAMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_A;
    Assert.assertNotNull(coverage);
    Assert.assertNotNull(beneficiary);

    verifyCoverageClass("Part A");
    verifyMeta();
    verifyExtensionsPartA();
    verifyCoverageStatus();
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPeriod();
    verifyPayor();
    verifyCoverageContract("part-a");
  }

  static void assertPartBMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_B;
    Assert.assertNotNull(coverage);
    Assert.assertNotNull(beneficiary);
    verifyCoverageClass("Part B");

    verifyMeta();
    verifyExtensionsPartB();
    verifyCoverageStatus();
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPeriod();
    verifyPayor();
  }

  static void assertPartCMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_C;
    Assert.assertNotNull(coverage);
    Assert.assertNotNull(beneficiary);
    verifyCoverageClass("Part C");
    verifyExtensionsPartC();
    verifyCoverageStatus();
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPayor();
  }

  static void assertPartDMatches(Beneficiary inBeneficiary, Coverage inCoverage) {
    beneficiary = inBeneficiary;
    coverage = inCoverage;
    currSegment = MedicareSegment.PART_D;
    Assert.assertNotNull(coverage);
    Assert.assertNotNull(beneficiary);
    verifyCoverageClass("Part D");
    verifyExtensionsPartD(84);
    verifyCoverageStatus();
    verifyType();
    verifySubscriber();
    verifyRelationship();
    verifyPayor();
  }
}
