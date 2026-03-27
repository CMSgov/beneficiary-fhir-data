package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

  @Test
  void emptyRequestReturnsEmptyBundle() {
    var res = searchBundle(new Patient());
    assertEquals(1, res.getEntry().size());
  }

  private static Stream<Arguments> verifyPatientMatch() {
    return Stream.of(
        Arguments.of(
            // Scenario 1 - first name, last name, DOB, address
            Optional.of("Joey"),
            Optional.of("Erdapfel"),
            Optional.of("2024-07-02"),
            Optional.of(
                new Address()
                    .addLine("3728 Broadway")
                    .setCity("Galveston")
                    .setState("TX")
                    .setPostalCode("77550")),
            Optional.empty(),
            Optional.empty(),
            false),
        // Scenario 4 - first name, last name, DOB, SSN last 4
        Arguments.of(
            Optional.of("Joey"),
            Optional.of("Erdapfel"),
            Optional.of("1925-08-16"),
            Optional.empty(),
            Optional.empty(),
            Optional.of("8346"),
            true),
        // Scenario 4 - invalid/should fail
        Arguments.of(
            Optional.of("Joe"),
            Optional.of("Erdapfel"),
            Optional.of("1925-08-16"),
            Optional.empty(),
            Optional.empty(),
            Optional.of("8346"),
            false),
        // Scenario 8 - first name, DOB, SSN
        Arguments.of(
            Optional.of("Joey"),
            Optional.empty(),
            Optional.of("1925-08-16"),
            Optional.empty(),
            Optional.of("5I50JT9WX60"),
            Optional.empty(),
            true),
        // Scenario 8 - invalid/should fail
        Arguments.of(
            Optional.of("Joey"),
            Optional.empty(),
            Optional.of("1925-08-17"),
            Optional.empty(),
            Optional.of("5I50JT9WX60"),
            Optional.empty(),
            false));
  }

  @ParameterizedTest
  @MethodSource
  void verifyPatientMatch(
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<String> birthDateStr,
      Optional<Address> address,
      Optional<String> mbi,
      Optional<String> ssnLastFour,
      boolean shouldMatch) {
    var birthDate =
        birthDateStr.map(
            d -> {
              var parser = new SimpleDateFormat("yyyy-MM-dd");
              try {
                return parser.parse(d);
              } catch (ParseException e) {
                throw new RuntimeException(e);
              }
            });

    var patient = buildRequest(firstName, lastName, birthDate, address, mbi, ssnLastFour);
    var res = searchBundle(patient);
    assertEquals(shouldMatch ? 2 : 1, res.getEntry().size());
  }
}
