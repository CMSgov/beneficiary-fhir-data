package gov.cms.bfd.server.war.r4.providers;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.hamcrest.collection.IsEmptyCollection;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2V2}. */
public final class BeneficiaryTransformerV2Test {

  private static final FhirContext fhirContext = FhirContext.forR4();
  private static Beneficiary beneficiary = null;
  private static Patient patient = null;

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

    beneficiary.setLastUpdated(new Date());
    beneficiary.setMbiHash(Optional.of("someMBIhash"));

    // Add the history records to the Beneficiary, but nill out the HICN fields.
    Set<BeneficiaryHistory> beneficiaryHistories =
        parsedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId()))
            .collect(Collectors.toSet());

    beneficiary.getBeneficiaryHistories().addAll(beneficiaryHistories);

    // Add the MBI history records to the Beneficiary.
    Set<MedicareBeneficiaryIdHistory> beneficiaryMbis =
        parsedRecords.stream()
            .filter(r -> r instanceof MedicareBeneficiaryIdHistory)
            .map(r -> (MedicareBeneficiaryIdHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId().orElse(null)))
            .collect(Collectors.toSet());
    beneficiary.getMedicareBeneficiaryIdHistories().addAll(beneficiaryMbis);
    assertThat(beneficiary, is(notNullValue()));

    createPatient(RequestHeaders.getHeaderWrapper());
  }

  private void createPatient(RequestHeaders reqHeaders) {
    patient = BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, reqHeaders);
  }

  @After
  public void tearDown() {
    beneficiary = null;
    patient = null;
  }

  /** Common top level Patient values */
  @Test
  public void shouldSetID() {
    Assert.assertEquals(patient.getId(), beneficiary.getBeneficiaryId());
  }

  @Test
  public void shouldSetLastUpdated() {
    Assert.assertNotNull(patient.getMeta().getLastUpdated());
  }

  @Test
  public void shouldSetCorrectProfile() {
    // The base CanonicalType doesn't seem to compare correctly so lets convert it to a string
    Assert.assertTrue(
        patient.getMeta().getProfile().stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_PATIENT_URL)));
  }

  /** Top level Identifiers */
  @Test
  public void shouldHaveKnownIdentifiers() {
    Assert.assertEquals(1, patient.getIdentifier().size());
  }

  @Test
  public void shouldIncludeMemberIdentifier() {
    Identifier mbId =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "http://terminology.hl7.org/CodeSystem/v2-0203", patient.getIdentifier());

    Identifier compare =
        TransformerTestUtilsV2.createIdentifier(
            "http://terminology.hl7.org/CodeSystem/v2-0203",
            "3456789",
            "http://terminology.hl7.org/CodeSystem/v2-0203",
            "MB",
            "");

    Assert.assertTrue(compare.equalsDeep(mbId));
  }

  /** Top level Extension(s) */
  @Test
  public void shouldHaveRaceExtension() {
    Assert.assertNotNull(beneficiary.getRace());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/race", patient.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/race",
            new Coding("https://bluebutton.cms.gov/resources/variables/race", "1", "White"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  /**
   * test to verify that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}
   * hanldes patient race Extension.
   */
  @Test
  public void shouldHaveOmbCategoryExtension() {
    Extension core =
        TransformerTestUtilsV2.findExtensionByUrl(
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race", patient.getExtension());

    Assert.assertNotNull(core);
    Assert.assertEquals(2, core.getExtension().size());

    Extension ombCateg =
        TransformerTestUtilsV2.findExtensionByUrl("ombCategory", core.getExtension());
    Extension txt = TransformerTestUtilsV2.findExtensionByUrl("text", core.getExtension());

    Assert.assertNotNull(ombCateg);
    Assert.assertNotNull(txt);

    Extension cmp1 =
        new Extension(
            "ombCategory",
            new Coding("http://terminology.hl7.org/CodeSystem/v3-NullFlavor", "UNK", "Unknown"));

    Extension cmp2 = new Extension("text", new StringType().setValue("Unknown"));

    Extension compare =
        new Extension().setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
    compare.addExtension(cmp1);
    compare.addExtension(cmp2);

    Assert.assertTrue(compare.equalsDeep(core));
  }

  /**
   * test to verify that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}
   * hanldes patient reference year extension.
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

    Assert.assertTrue(compare.equalsDeep(ex));
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
   * gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} correctly handles patient Part D
   * attributee.
   */
  private void verifyDualResourceExtension(String dualId) {
    String uri = "https://bluebutton.cms.gov/resources/variables/" + dualId;

    Extension ex = TransformerTestUtilsV2.findExtensionByUrl(uri, patient.getExtension());
    Assert.assertNotNull(ex);

    Extension compare =
        new Extension(
            uri,
            new Coding(
                uri,
                "**",
                "Enrolled in Medicare A and/or B, but no Part D enrollment data for the beneficiary. (This status was indicated as 'XX' for 2006-2009)"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  /** Top level beneficiary info */
  @Test
  public void shouldMatchBeneficiaryName() {
    List<HumanName> name = patient.getName();
    Assert.assertNotNull(name);
    Assert.assertEquals(1, name.size());
    HumanName hn = name.get(0);
    Assert.assertEquals(HumanName.NameUse.USUAL, hn.getUse());
    Assert.assertEquals("Doe", hn.getFamily().toString());
    Assert.assertEquals("John", hn.getGiven().get(0).toString());
    Assert.assertEquals("A", hn.getGiven().get(1).toString());
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} correctly
   * handles patient gender attribute.
   */
  @Test
  public void shouldMatchBeneficiaryGender() {
    Assert.assertEquals(AdministrativeGender.MALE, patient.getGender());
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} correctly
   * handles patient birth date attribute.
   */
  @Test
  public void shouldMatchBeneficiaryBirthDate() {
    Assert.assertEquals(parseDate("1981-03-17"), patient.getBirthDate());
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} correctly
   * handles patient deceased attributes.
   */
  @Test
  public void shouldMatchBeneficiaryDeathDate() {
    DateTimeType deceasedDate = patient.getDeceasedDateTimeType();
    if (deceasedDate != null) {
      Assert.assertEquals("1981-03-17", deceasedDate.getValueAsString());
    } else {
      Assert.assertEquals(new BooleanType(false), patient.getDeceasedBooleanType());
    }
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} works
   * correctly when passed a {@link Beneficiary} without includeAddressFields header values.
   */
  @Test
  public void shouldMatchAddressNoAddrHeader() {
    List<Address> addrList = patient.getAddress();
    Assert.assertEquals(1, addrList.size());
    Address compare = new Address();
    compare.setPostalCode("12345");
    compare.setState("MO");
    Assert.assertTrue(compare.equalsDeep(addrList.get(0)));
  }

  /**
   * Verifies that {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2} works
   * correctly when passed a {@link Beneficiary} with includeAddressFields header values.
   */
  @Test
  public void shouldMatchAddressWithAddrHeader() {
    RequestHeaders reqHdr = getRHwithIncldAddrFldHdr("true");
    createPatient(reqHdr);
    Assert.assertNotNull(patient);
    List<Address> addrList = patient.getAddress();
    Assert.assertEquals(1, addrList.size());
    Assert.assertEquals(6, addrList.get(0).getLine().size());

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
    Assert.assertTrue(compare.equalsDeep(addrList.get(0)));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary}.
   */
  @Ignore
  @Test
  public void transformSampleARecord() {
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(patient));
    assertThat(patient.getIdentifier(), not(IsEmptyCollection.empty()));
  }

  /**
   * test helper
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
   * test helper
   *
   * @param value of all include identifier values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldIdentityHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, value);
  }

  /**
   * test helper
   *
   * @param value of all include address fields values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldAddrFldHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, value);
  }
}
