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
    var testBene =
        entityManager
            .createQuery("SELECT b FROM Beneficiary b WHERE b.beneSk = :beneSk", Beneficiary.class)
            .setParameter("beneSk", "-300428640")
            .getResultList()
            .getFirst();
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
        // Scenario 1 - first name, last name, DOB, address
        Arguments.of(
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.of(address),
            Optional.empty(),
            Optional.empty(),
            true),
        // Scenario 1 - invalid/should fail
        Arguments.of(
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
            false),
        // Scenario 4 - first name, last name, DOB, SSN last 4
        Arguments.of(
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.empty(),
            Optional.of(ssnLastFour),
            true),
        // Scenario 4 - invalid/should fail
        Arguments.of(
            testBene,
            Optional.of(firstName),
            Optional.of(lastName),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.empty(),
            Optional.of("8347"),
            false),
        // Scenario 8 - first name, DOB, MBI
        Arguments.of(
            testBene,
            Optional.of(firstName),
            Optional.empty(),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.of(mbi),
            Optional.empty(),
            true),
        // Scenario 8 - invalid/should fail
        Arguments.of(
            testBene,
            Optional.of(firstName),
            Optional.empty(),
            Optional.of(birthDate),
            Optional.empty(),
            Optional.of("5I50JT9WX61"),
            Optional.empty(),
            false));
  }

  @ParameterizedTest
  @MethodSource
  void verifyPatientMatch(
      Beneficiary beneficiary,
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<Date> birthDate,
      Optional<Address> address,
      Optional<String> mbi,
      Optional<String> ssnLastFour,
      boolean shouldMatch) {

    var patient = buildRequest(firstName, lastName, birthDate, address, mbi, ssnLastFour);
    var bundle = searchBundle(patient);
    assertEquals(shouldMatch ? 2 : 1, bundle.getEntry().size());
    if (!shouldMatch) {
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
