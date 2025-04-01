package gov.cms.bfd.server.war.r4.providers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class BeneficiaryTransformerV2Test {

  /** Fhir context for parsing the test file. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** Beneficiary under test. */
  private static Beneficiary beneficiary = null;

  /** Patient under test. */
  private static Patient patient = null;

  /** The metrics registry. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The class under test. */
  private static BeneficiaryTransformerV2 beneficiaryTransformerV2;

  /**
   * Sets up the test, including parsing the beneficiary from a file and adjusting some of its
   * values for use in each test.
   */
  @BeforeEach
  public void setup() {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);
    beneficiaryTransformerV2 = new BeneficiaryTransformerV2(metricRegistry, false, true);
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    beneficiary.setLastUpdated(Instant.now());
    beneficiary.setMbiHash(Optional.of("someMBIhash"));

    // Add the history records to the Beneficiary, but nill out the HICN fields.
    Set<BeneficiaryHistory> beneficiaryHistories =
        parsedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId() == r.getBeneficiaryId())
            .collect(Collectors.toSet());

    beneficiary.getBeneficiaryHistories().addAll(beneficiaryHistories);

    assertThat(beneficiary, is(notNullValue()));

    createPatient(RequestHeaders.getHeaderWrapper());
  }

  /**
   * Creates a patient to use in the tests.
   *
   * @param reqHeaders the request headers
   */
  private void createPatient(RequestHeaders reqHeaders) {
    Patient genPatient = beneficiaryTransformerV2.transform(beneficiary, reqHeaders, true);
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genPatient);
    patient = parser.parseResource(Patient.class, json);
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    String expectedTimerName = beneficiaryTransformerV2.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
  }

  /** Tests that the transformer sets the expected id. */
  @Test
  public void shouldSetID() {
    assertEquals(patient.getId(), "Patient/" + beneficiary.getBeneficiaryId());
  }

  /** Tests that the transformer sets the expected last updated date in the metadata. */
  @Test
  public void shouldSetLastUpdated() {
    assertNotNull(patient.getMeta().getLastUpdated());
  }

  /** Tests that the transformer sets the expected profile metadata. */
  @Test
  public void shouldSetCorrectProfile() {
    List<CanonicalType> profile = patient.getMeta().getProfile();
    assertEquals(0, profile.size());
    // The base CanonicalType doesn't seem to compare correctly so lets convert it
    // to a string
    assertFalse(
        profile.stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_PATIENT_URL)));
  }

  /** Tests that the transformer sets the expected profile when C4DIC is enabled. */
  @Test
  public void shouldSetCorrectProfilesWithC4DicEnabled() {
    beneficiaryTransformerV2 = new BeneficiaryTransformerV2(metricRegistry, true, true);
    createPatient(RequestHeaders.getHeaderWrapper());
    List<CanonicalType> profile = patient.getMeta().getProfile();
    assertEquals(1, profile.size());
    assertFalse(
        profile.stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_PATIENT_URL)));
    assertTrue(
        profile.stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(Profile.C4DIC.getVersionedPatientUrl())));
  }

  /** Tests that the transformer sets the expected member identifier values. */
  @Test
  public void shouldIncludeMemberIdentifier() {
    Identifier mbId =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "https://bluebutton.cms.gov/resources/variables/bene_id", patient.getIdentifier());

    Identifier compare =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/variables/bene_id",
            "567834",
            "http://terminology.hl7.org/CodeSystem/v2-0203",
            "MB",
            "Member Number");

    assertTrue(compare.equalsDeep(mbId));
  }

  /** Tests that the transformer sets the expected medicare extensions with ids. */
  @Test
  public void shouldIncludeMedicareExtensionIdentifierCurrent() throws ParseException {
    Identifier mcId =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "http://hl7.org/fhir/sid/us-mbi", patient.getIdentifier());

    Extension extension =
        new Extension(
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
            new Coding(
                "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                "current",
                "Current"));

    Period period = new Period();

    Date start = (new SimpleDateFormat("yyyy-MM-dd")).parse("2020-07-30");
    period.setStart(start, TemporalPrecisionEnum.DAY);

    Identifier compare = new Identifier();
    compare
        .setValue("3456789")
        .setSystem("http://hl7.org/fhir/sid/us-mbi")
        .setPeriod(period)
        .getType()
        .addCoding()
        .setCode("MC")
        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
        .setDisplay("Patient's Medicare number")
        .addExtension(extension);

    assertTrue(compare.equalsDeep(mcId));
  }

  /**
   * Tests that the transformer sets the expected medicare extensions and values when mbi history
   * exists.
   */
  @Test
  public void shouldIncludeMedicareExtensionIdentifierWithHistory() throws ParseException {
    List<Identifier> patientIdentList = patient.getIdentifier();
    assertEquals(5, patientIdentList.size());

    ArrayList<Identifier> compareIdentList = new ArrayList<Identifier>();

    Identifier ident =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/variables/bene_id",
            "567834",
            "http://terminology.hl7.org/CodeSystem/v2-0203",
            "MB",
            "Member Number");

    compareIdentList.add(ident);

    Extension extension =
        new Extension(
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
            new Coding(
                "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                "current",
                "Current"));

    Period period = new Period();

    Date start = (new SimpleDateFormat("yyyy-MM-dd")).parse("2020-07-30");
    period.setStart(start, TemporalPrecisionEnum.DAY);

    ident = new Identifier();
    ident
        .setValue("3456789")
        .setSystem("http://hl7.org/fhir/sid/us-mbi")
        .setPeriod(period)
        .getType()
        .addCoding()
        .setCode("MC")
        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
        .setDisplay("Patient's Medicare number")
        .addExtension(extension);

    compareIdentList.add(ident);

    extension =
        new Extension(
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
            new Coding(
                "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                "historic",
                "Historic"));

    ident = new Identifier();
    ident
        .setValue("9AB2WW3GR44")
        .setSystem("http://hl7.org/fhir/sid/us-mbi")
        .getType()
        .addCoding()
        .setCode("MC")
        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
        .setDisplay("Patient's Medicare number")
        .addExtension(extension);
    compareIdentList.add(ident);

    ident = new Identifier();
    ident
        .setValue("543217066")
        .setSystem("http://hl7.org/fhir/sid/us-mbi")
        .getType()
        .addCoding()
        .setCode("MC")
        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
        .setDisplay("Patient's Medicare number")
        .addExtension(extension);
    compareIdentList.add(ident);

    ident = new Identifier();
    ident
        .setValue("3456689")
        .setSystem("http://hl7.org/fhir/sid/us-mbi")
        .getType()
        .addCoding()
        .setCode("MC")
        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
        .setDisplay("Patient's Medicare number")
        .addExtension(extension);
    compareIdentList.add(ident);

    assertEquals(compareIdentList.size(), patientIdentList.size());
    for (int i = 0; i < compareIdentList.size(); i++) {
      assertTrue(compareIdentList.get(i).equalsDeep(patientIdentList.get(i)));
    }
  }

  /** Tests that the transformer sets the expected race extension entries. */
  @Test
  public void shouldHaveRaceExtension() {
    assertNotNull(beneficiary.getRace());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/race", patient.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/race",
            new Coding("https://bluebutton.cms.gov/resources/variables/race", "1", "White"));

    assertTrue(compare.equalsDeep(ex));
  }

  /**
   * Test to verify that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}
   * handles patient race Extension.
   */
  @Test
  public void shouldHaveOmbCategoryExtension() {
    Extension core =
        TransformerTestUtilsV2.findExtensionByUrl(
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race", patient.getExtension());

    assertNotNull(core);
    assertEquals(2, core.getExtension().size());

    Extension ombCateg =
        TransformerTestUtilsV2.findExtensionByUrl("ombCategory", core.getExtension());
    Extension txt = TransformerTestUtilsV2.findExtensionByUrl("text", core.getExtension());

    assertNotNull(ombCateg);
    assertNotNull(txt);

    Extension cmp1 =
        new Extension(
            "ombCategory",
            new Coding("http://terminology.hl7.org/CodeSystem/v3-NullFlavor", "UNK", "Unknown"));

    Extension cmp2 = new Extension("text", new StringType().setValue("Unknown"));

    Extension compare =
        new Extension().setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
    compare.addExtension(cmp1);
    compare.addExtension(cmp2);

    assertTrue(compare.equalsDeep(core));
  }

  /**
   * Test to verify that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}
   * handles patient reference year extension.
   */
  @Test
  public void shouldHaveReferenceYearExtension() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/rfrnc_yr", patient.getExtension());

    DateType yearValue = null;
    try {
      Date dt = new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-01");
      yearValue = new DateType(dt, TemporalPrecisionEnum.YEAR);
    } catch (Exception e) {
    }

    Extension compare =
        new Extension()
            .setValue(yearValue)
            .setUrl("https://bluebutton.cms.gov/resources/variables/rfrnc_yr");

    assertTrue(compare.equalsDeep(ex));
  }

  /**
   * Verifies that {@link BeneficiaryTransformerV2#transform(Beneficiary, RequestHeaders)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary} with
   * a reference year field not found.
   */
  @Test
  public void shouldNotHaveReferenceYearExtension() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    Beneficiary newBeneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    newBeneficiary.setLastUpdated(Instant.now());
    newBeneficiary.setMbiHash(Optional.of("someMBIhash"));
    newBeneficiary.setBeneEnrollmentReferenceYear(Optional.empty());

    Patient genPatient =
        beneficiaryTransformerV2.transform(newBeneficiary, RequestHeaders.getHeaderWrapper());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genPatient);
    Patient newPatient = parser.parseResource(Patient.class, json);

    String url = "https://bluebutton.cms.gov/resources/variables/rfrnc_yr";
    Optional<Extension> ex =
        newPatient.getExtension().stream().filter(e -> url.equals(e.getUrl())).findFirst();

    assertEquals(false, ex.isPresent());
    assertEquals(true, ex.isEmpty());
  }

  /**
   * test to verify that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} sets a
   * valid extension date.
   */
  @Test
  public void shouldSetExtensionDate() {

    IBaseDatatype ex =
        TransformerUtilsV2.createExtensionDate(
                CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear().get())
            .getValue();

    IBaseDatatype compare =
        TransformerUtilsV2.createExtensionDate(CcwCodebookVariable.RFRNC_YR, new BigDecimal(3))
            .getValue();

    assertEquals(ex.toString().length(), compare.toString().length());
  }

  /**
   * helper function to test {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}
   * patient Part D attributee.
   */
  @Test
  public void shouldResourceDualVariablesExtension() {
    verifyDualResourceExtension("dual_01");
    verifyDualResourceExtension("dual_02");
    verifyDualResourceExtension("dual_03");
    verifyDualResourceExtension("dual_04");
    verifyDualResourceExtension("dual_05");
    verifyDualResourceExtension("dual_06");
    verifyDualResourceExtension("dual_07");
    verifyDualResourceExtension("dual_08");
    verifyDualResourceExtension("dual_09");
    verifyDualResourceExtension("dual_10");
    verifyDualResourceExtension("dual_11");
    verifyDualResourceExtension("dual_12");
  }

  /**
   * helper function to verify that {@link
   * gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}* correctly handles patient Part D
   * attributee.
   *
   * @param dualId the dual id to verify
   */
  private void verifyDualResourceExtension(String dualId) {
    String uri = "https://bluebutton.cms.gov/resources/variables/" + dualId;

    Extension ex = TransformerTestUtilsV2.findExtensionByUrl(uri, patient.getExtension());
    assertNotNull(ex);

    Extension compare =
        new Extension(
            uri,
            new Coding(
                uri,
                "**",
                "Enrolled in Medicare A and/or B, but no Part D enrollment data for the beneficiary. (This status was indicated as 'XX' for 2006-2009)"));

    assertTrue(compare.equalsDeep(ex));
  }

  /** Tests that the transformer sets the expected patient name. */
  @Test
  public void shouldMatchBeneficiaryName() {
    List<HumanName> name = patient.getName();
    assertNotNull(name);
    assertEquals(1, name.size());
    HumanName hn = name.get(0);
    assertEquals(HumanName.NameUse.USUAL, hn.getUse());
    assertEquals("Doe", hn.getFamily().toString());
    assertEquals("John", hn.getGiven().get(0).toString());
    assertEquals("A", hn.getGiven().get(1).toString());
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} correctly
   * handles patient sex attribute.
   */
  @Test
  public void shouldMatchBeneficiarySex() {
    assertEquals(
        TransformerConstants.US_CORE_SEX_MALE,
        patient.getExtensionByUrl(TransformerConstants.US_CORE_SEX_URL).getValue().toString());
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} correctly
   * handles patient birth date attribute.
   */
  @Test
  public void shouldMatchBeneficiaryBirthDate() {
    assertEquals(parseDate("1981-03-17"), patient.getBirthDate());
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} correctly
   * handles patient deceased attributes.
   */
  @Test
  public void shouldMatchBeneficiaryDeathDate() {
    DateTimeType deceasedDate = patient.getDeceasedDateTimeType();
    if (deceasedDate != null) {
      assertEquals("1981-03-17", deceasedDate.getValueAsString());
    } else {
      assertEquals(new BooleanType(false), patient.getDeceasedBooleanType());
    }
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} works
   * correctly when passed a {@link Beneficiary} without includeAddressFields header values.
   */
  @Test
  public void shouldMatchAddressNoAddrHeader() {
    List<Address> addrList = patient.getAddress();
    assertEquals(1, addrList.size());
    Address compare = new Address();
    compare.setPostalCode("12345");
    compare.setState("MO");
    assertTrue(compare.equalsDeep(addrList.get(0)));
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} works
   * correctly when passed a {@link Beneficiary} with includeAddressFields header values.
   */
  @Test
  public void shouldMatchAddressWithAddrHeader() {
    RequestHeaders reqHdr = getHeaderWithAddressFieldsSetTo("true");
    createPatient(reqHdr);
    assertNotNull(patient);
    List<Address> addrList = patient.getAddress();
    assertEquals(1, addrList.size());
    assertEquals(6, addrList.get(0).getLine().size());

    Address compare = new Address();
    compare.setPostalCode("12345");
    compare.setState("MO");
    compare.setCity("PODUNK");
    ArrayList<StringType> lineList =
        new ArrayList<>(
            Arrays.asList(
                new StringType("204 SOUTH ST"),
                new StringType("7560 123TH ST"),
                new StringType("SURREY"),
                new StringType("DAEJEON SI 34867"),
                new StringType("COLOMBIA"),
                new StringType("SURREY")));
    compare.setLine(lineList);
    assertTrue(compare.equalsDeep(addrList.get(0)));
  }

  /**
   * Parses a date from a string.
   *
   * @param value of date string
   * @return Date instance derived from value
   */
  public static Date parseDate(String value) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd").parse(value);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets a header wrapper with {@link R4PatientResourceProvider#HEADER_NAME_INCLUDE_ADDRESS_FIELDS}
   * set to the given value.
   *
   * @param value of all include address fields values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getHeaderWithAddressFieldsSetTo(String value) {
    return RequestHeaders.getHeaderWrapper(
        R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, value);
  }
}
