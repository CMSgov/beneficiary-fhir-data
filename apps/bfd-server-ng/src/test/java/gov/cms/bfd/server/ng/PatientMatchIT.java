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
      Optional<Address> address,
      Optional<String> mbi,
      Optional<String> ssnLastFour) {
    var patient = new Patient();
    var name = new HumanName();
    firstName.ifPresent(name::addGiven);
    lastName.ifPresent(name::setFamily);
    patient.setName(List.of(name));
    birthDate.ifPresent(patient::setBirthDate);
    address.ifPresent(patient::addAddress);
    mbi.ifPresent(
        m -> patient.addIdentifier(new Identifier().setSystem(SystemUrls.CMS_MBI).setValue(m)));
    ssnLastFour.ifPresent(
        s -> patient.addIdentifier(new Identifier().setSystem(SystemUrls.US_SSN).setValue(s)));

    return patient;
  }

  private String normalizeAddress(String address) {
    return address.replace("Avenue", "Ave");
  }

  @Test
  void emptyRequestReturnsEmptyBundle() {
    var res = searchBundle(new Patient());
    assertEquals(1, res.getEntry().size());
  }

  private Stream<Arguments> verifyPatientMatch() {
    var testBene = getBeneficiaryFromBeneSk("-300428640");

    var firstName = testBene.getBeneficiaryName().getFirstName();
    var lastName = testBene.getBeneficiaryName().getLastName();
    var birthDate = DateUtil.toDate(testBene.getBirthDate());
    var addressIn = testBene.getAddress();
    var combinedLines =
        List.of(addressIn.getAddressLine1().get(), addressIn.getAddressLine2().get()).stream()
            .map(this::normalizeAddress)
            .collect(Collectors.joining(" "));
    var address =
        new Address()
            .addLine(combinedLines)
            .setCity(addressIn.getCity().get())
            .setState(addressIn.getStateCode().get())
            .setPostalCode(addressIn.getZipCode().get());
    var ssnLastFour = testBene.getSsnLastFourDigits();
    var mbi = testBene.getIdentifier().getMbi();
    return Stream.of(
        Arguments.of(
            "Scenario 1 - first name, last name, DOB, address",
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.of(address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - invalid/should fail",
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.of(
                new Address()
                    .addLine("3728 Broadway Avenue J")
                    .setCity(address.getCity())
                    .setState(address.getState())
                    .setPostalCode(address.getPostalCode())),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 4 - first name, last name, DOB, SSN last 4",
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.empty(),
            Optional.of(ssnLastFour),
            Optional.of(4)),
        //
        Arguments.of(
            "Scenario 4 - invalid/should fail",
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.empty(),
            Optional.of("8347"),
            Optional.empty()),
        Arguments.of(
            "Scenario 8 - first name, DOB, MBI",
            testBene,
            Optional.of(firstName),
            Optional.empty(),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.of(mbi),
            Optional.empty(),
            Optional.of(8)),
        Arguments.of(
            " Scenario 8 - invalid/should fail",
            testBene,
            Optional.of(firstName),
            Optional.empty(),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.of("5I50JT9WX61"),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            " Scenario 1 failure, scenario 4 success",
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.of(new Address().addLine("fake")),
            Optional.empty(),
            Optional.of(ssnLastFour),
            Optional.of(4)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void verifyPatientMatch(
      String testName,
      Beneficiary beneficiary,
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<Date> birthDate,
      Optional<Address> address,
      Optional<String> mbi,
      Optional<String> ssnLastFour,
      Optional<Integer> expectedMatchNumber) {
    var patient = buildRequest(firstName, lastName, birthDate, address, mbi, ssnLastFour);
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
}
