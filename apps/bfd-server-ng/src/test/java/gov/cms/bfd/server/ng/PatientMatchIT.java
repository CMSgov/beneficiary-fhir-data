package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatientMatchIT extends IntegrationTestBase {
  private Bundle searchBundle(Patient patient) {
    var ctx = FhirContext.forR4();
    var params = ParametersUtil.newInstance(ctx);
    ParametersUtil.addParameterToParameters(ctx, params, "IDIPatient", patient);

    var res =
        getFhirClient(ctx)
            .operation()
            .onType(Patient.class)
            .named("idi-match")
            .withParameters(params)
            .returnResourceType(Bundle.class)
            .execute();
    // Organization should always be the first entry in the bundle
    assertEquals(
        ResourceType.Organization, res.getEntry().getFirst().getResource().getResourceType());

    return res;
  }

  private Patient buildRequest(
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<Date> birthDate,
      List<Address> addresses,
      Optional<String> mbi,
      Optional<String> ssnLastFour) {
    var patient = new Patient();
    var name = new HumanName();
    firstName.ifPresent(name::addGiven);
    lastName.ifPresent(name::setFamily);
    patient.setName(List.of(name));
    birthDate.ifPresent(patient::setBirthDate);
    addresses.forEach(patient::addAddress);
    mbi.ifPresent(
        m -> patient.addIdentifier(new Identifier().setSystem(SystemUrls.CMS_MBI).setValue(m)));
    ssnLastFour.ifPresent(
        s -> patient.addIdentifier(new Identifier().setSystem(SystemUrls.US_SSN).setValue(s)));

    return patient;
  }

  @Test
  void emptyRequestReturnsEmptyBundle() {
    var res = searchBundle(new Patient());
    assertEquals(1, res.getEntry().size());
  }

  private Stream<Arguments> verifyPatientMatch() {
    var testBene = TestBene.fromBene(getBeneficiaryFromBeneSk("-300428640"));
    var testBeneWithSpaces = TestBene.fromBene(getBeneficiaryFromBeneSk("-18976899"));
    // precondition - should have whitespace in the name
    assertTrue(testBeneWithSpaces.bene.getBeneficiaryName().getFirstName().contains(" "));

    var duplicateBene1 = TestBene.fromBene(getBeneficiaryFromBeneSk("-591866793"));
    var duplicateBene2 = TestBene.fromBene(getBeneficiaryFromBeneSk("-591866794"));
    // precondition - these two should have the same info but different xref sks
    assertNotEquals(duplicateBene1.bene.getXrefSk(), duplicateBene2.bene.getXrefSk());
    assertEquals(
        duplicateBene1.bene.getBeneficiaryName(), duplicateBene2.bene.getBeneficiaryName());
    assertEquals(duplicateBene1.bene.getBirthDate(), duplicateBene2.bene.getBirthDate());
    assertEquals(
        duplicateBene1.bene.getSsnLastFourDigits(), duplicateBene2.bene.getSsnLastFourDigits());

    var historicalBene = TestBene.fromBene(getBeneficiaryFromBeneSk("-415220062", 0));
    var currentBene = TestBene.fromBene(getBeneficiaryFromBeneSk("-415220062", 1));

    // precondition - same bene, different addresses
    assertEquals(historicalBene.bene.getBeneSk(), currentBene.bene.getBeneSk());
    assertNotEquals(historicalBene.bene.getAddress(), currentBene.bene.getAddress());
    return Stream.of(
        Arguments.of(
            "Scenario 1 - first name, last name, DOB, address",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(testBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - name with spaces",
            testBeneWithSpaces.bene,
            Optional.of(testBeneWithSpaces.firstName.replace(" ", "")),
            Optional.of(testBeneWithSpaces.lastName),
            Optional.of(testBeneWithSpaces.birthDate),
            List.of(testBeneWithSpaces.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - invalid/should fail",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(
                new Address()
                    .addLine("3728 Broadway Avenue J")
                    .setCity(testBene.address.getCity())
                    .setState(testBene.address.getState())
                    .setPostalCode(testBene.address.getPostalCode())),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1 - duplicate bene should return no results",
            duplicateBene1.bene,
            Optional.of(duplicateBene1.firstName),
            Optional.of(duplicateBene1.lastName),
            Optional.of(duplicateBene1.birthDate),
            List.of(duplicateBene1.address),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1 - historical address should not match",
            historicalBene.bene,
            Optional.of(historicalBene.firstName),
            Optional.of(historicalBene.lastName),
            Optional.of(historicalBene.birthDate),
            List.of(historicalBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1 - current address should match",
            currentBene.bene,
            Optional.of(currentBene.firstName),
            Optional.of(currentBene.lastName),
            Optional.of(currentBene.birthDate),
            List.of(currentBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - current and historical address should match",
            currentBene.bene,
            Optional.of(currentBene.firstName),
            Optional.of(currentBene.lastName),
            Optional.of(currentBene.birthDate),
            List.of(currentBene.address, historicalBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - current and historical address should match (reverse order)",
            currentBene.bene,
            Optional.of(currentBene.firstName),
            Optional.of(currentBene.lastName),
            Optional.of(currentBene.birthDate),
            List.of(historicalBene.address, currentBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 4 - first name, last name, DOB, SSN last 4",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of(testBene.ssnLastFour),
            Optional.of(4)),
        Arguments.of(
            "Scenario 4 - duplicate bene should return no results",
            duplicateBene1.bene,
            Optional.of(duplicateBene1.firstName),
            Optional.of(duplicateBene1.lastName),
            Optional.of(duplicateBene1.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of(duplicateBene1.ssnLastFour),
            Optional.empty()),
        Arguments.of(
            "Scenario 4 - invalid/should fail",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of("fakeSsn"),
            Optional.empty()),
        Arguments.of(
            " Scenario 1 failure, scenario 4 success",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(new Address().addLine("fake")),
            Optional.empty(),
            Optional.of(testBene.ssnLastFour),
            Optional.of(4)),
        // Note: it's impossible for this to produce two beneficiaries with different xrefs
        // because we already protect against MBIs incorrectly attributed to multiple benes
        Arguments.of(
            "Scenario 8 - first name, DOB, MBI",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.empty(),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.of(testBene.mbi),
            Optional.empty(),
            Optional.of(8)),
        Arguments.of(
            " Scenario 8 - invalid/should fail",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.empty(),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.of("wrongMbi"),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1/4 fail, scenario 8 success",
            duplicateBene1.bene,
            Optional.of(duplicateBene1.firstName),
            Optional.of(duplicateBene1.lastName),
            Optional.of(duplicateBene1.birthDate),
            List.of(duplicateBene1.address),
            Optional.of(duplicateBene1.mbi),
            Optional.of(duplicateBene1.ssnLastFour),
            Optional.of(8)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void verifyPatientMatch(
      String testName,
      Beneficiary beneficiary,
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<Date> birthDate,
      List<Address> addresses,
      Optional<String> mbi,
      Optional<String> ssnLastFour,
      Optional<Integer> expectedMatchNumber) {
    var patient = buildRequest(firstName, lastName, birthDate, addresses, mbi, ssnLastFour);
    var bundle = searchBundle(patient);
    assertEquals(expectedMatchNumber.isPresent() ? 2 : 1, bundle.getEntry().size());
    if (expectedMatchNumber.isEmpty()) {
      return;
    }
    var entry =
        bundle.getEntry().stream()
            .filter(b -> !SystemUrls.CMS_GOV.equals(b.getFullUrl()))
            .findFirst()
            .get();
    var patientResult = (Patient) entry.getResource();
    var name = patientResult.getName().stream().findFirst().get();
    var expectedName = beneficiary.getBeneficiaryName();
    assertEquals(expectedName.getFirstName(), name.getGiven().getFirst().toString());
    assertEquals(expectedName.getLastName(), name.getFamily());

    assertEquals(DateUtil.toDate(beneficiary.getBirthDate()), patientResult.getBirthDate());
    assertEquals(
        beneficiary.getIdentifier().getMbi(),
        patientResult.getIdentifier().stream()
            .filter(i -> i.getSystem().equals(SystemUrls.CMS_MBI))
            .findFirst()
            .get()
            .getValue());
  }

  private record TestBene(
      String firstName,
      String lastName,
      Address address,
      Date birthDate,
      String mbi,
      String ssnLastFour,
      Beneficiary bene) {
    private static TestBene fromBene(Beneficiary testBene) {
      var firstName = testBene.getBeneficiaryName().getFirstName();
      var lastName = testBene.getBeneficiaryName().getLastName();
      var birthDate = DateUtil.toDate(testBene.getBirthDate());
      var addressIn = testBene.getAddress();
      var combinedLines =
          Stream.of(addressIn.getAddressLine1(), addressIn.getAddressLine2())
              .flatMap(Optional::stream)
              .map(TestBene::normalizeAddress)
              .collect(Collectors.joining(" "));
      var address =
          new Address()
              .addLine(combinedLines)
              .setCity(addressIn.getCity().get())
              .setState(addressIn.getStateCode().get())
              .setPostalCode(addressIn.getZipCode().get());
      var ssnLastFour = testBene.getSsnLastFourDigits();
      var mbi = testBene.getIdentifier().getMbi();
      return new TestBene(firstName, lastName, address, birthDate, mbi, ssnLastFour, testBene);
    }

    private static String normalizeAddress(String address) {
      return address.replace("Avenue", "Ave");
    }
  }
}
