package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PatientSearchIT extends IntegrationTestBase {
  private IQuery<Bundle> searchBundle() {
    return getFhirClient().search().forResource(Patient.class).returnBundle(Bundle.class);
  }

  @Test
  void patientSearchById() {
    var patientBundle =
        searchBundle()
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("1"))
            .execute();
    assertEquals(1, patientBundle.getEntry().size());
    expect.serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @Test
  void patientSearchByIdEmpty() {
    var patientBundle =
        searchBundle()
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("999"))
            .execute();
    assertEquals(0, patientBundle.getEntry().size());
    expect.serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @Test
  void patientSearchByIdentifier() {
    var patientBundle =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(SystemUrls.CMS_MBI, "1S000000000"))
            .execute();
    expect.serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @Test
  void patientSearchByIdentifierEmpty() {
    var patient =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(SystemUrls.CMS_MBI, "999"))
            .execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientSearchByIdentifierMissingSystem() {
    assertThrows(
        InvalidRequestException.class,
        () ->
            searchBundle()
                .where(new TokenClientParam(Patient.SP_IDENTIFIER).exactly().identifier("1"))
                .execute());
  }

  private static Stream<Arguments> patientSearchByDate() {
    return Stream.of(
        Arguments.of(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("1")),
        Arguments.of(
            new TokenClientParam(Patient.SP_IDENTIFIER)
                .exactly()
                .systemAndIdentifier(SystemUrls.CMS_MBI, "1S000000000")));
  }

  @ParameterizedTest
  @MethodSource
  void patientSearchByDate(ICriterion<TokenClientParam> searchCriteriaId) {
    // Search date exact
    var patientBundle =
        searchBundle()
            .where(searchCriteriaId)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .exactly()
                    .day(DateUtil.toDate(LocalDateTime.parse("2024-01-01T00:00:00"))))
            .execute();
    assertEquals(1, patientBundle.getEntry().size());

    // Search date greater than
    patientBundle =
        searchBundle()
            .where(searchCriteriaId)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .after()
                    .day(DateUtil.toDate(LocalDateTime.parse("2024-01-01T00:00:00"))))
            .execute();
    assertEquals(0, patientBundle.getEntry().size());

    // Search date less than
    patientBundle =
        searchBundle()
            .where(searchCriteriaId)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .before()
                    .day(DateUtil.toDate(LocalDateTime.parse("2024-01-01T00:00:00"))))
            .execute();
    assertEquals(0, patientBundle.getEntry().size());

    // Search date greater than or equal
    patientBundle =
        searchBundle()
            .where(searchCriteriaId)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .afterOrEquals()
                    .day(DateUtil.toDate(LocalDateTime.parse("2024-01-01T00:00:00"))))
            .execute();
    assertEquals(1, patientBundle.getEntry().size());

    // Search date less than or equal
    patientBundle =
        searchBundle()
            .where(searchCriteriaId)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .beforeOrEquals()
                    .day(DateUtil.toDate(LocalDateTime.parse("2024-01-01T00:00:00"))))
            .execute();
    assertEquals(1, patientBundle.getEntry().size());
  }
}
