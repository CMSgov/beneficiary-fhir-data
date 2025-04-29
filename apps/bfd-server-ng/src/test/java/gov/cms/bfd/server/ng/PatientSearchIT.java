package gov.cms.bfd.server.ng;

import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PatientSearchIT extends IntegrationTestBase {
  @Test
  void patientSearchById() {
    var patient =
        getFhirClient()
            .search()
            .forResource("Patient")
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("1"))
            .execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientSearchByIdEmpty() {
    var patient =
        getFhirClient()
            .search()
            .forResource("Patient")
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("999"))
            .execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientSearchByIdentifier() {
    var patient =
        getFhirClient()
            .search()
            .forResource("Patient")
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(SystemUrls.CMS_MBI, "1S000000000"))
            .execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientSearchByIdentifierEmpty() {
    var patient =
        getFhirClient()
            .search()
            .forResource("Patient")
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(SystemUrls.CMS_MBI, "999"))
            .execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientSearchByIdentifierMissingSystem() {
    Assertions.assertThrows(
        InvalidRequestException.class,
        () ->
            getFhirClient()
                .search()
                .forResource("Patient")
                .where(new TokenClientParam(Patient.SP_IDENTIFIER).exactly().identifier("1"))
                .execute());
  }
}
