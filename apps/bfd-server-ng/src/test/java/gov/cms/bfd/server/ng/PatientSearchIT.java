package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientSearchIT extends IntegrationTestBase {
  @Autowired private EntityManager entityManager;

  private IQuery<Bundle> searchBundle() {
    return getFhirClient().search().forResource(Patient.class).returnBundle(Bundle.class);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchById(SearchStyleEnum searchStyle) {
    var patientBundle =
        searchBundle()
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("405764107"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(1, patientBundle.getEntry().size());

    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchByIdMergedBene(SearchStyleEnum searchStyle) {
    var patientBundle =
        searchBundle()
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("792872340"))
            .usingStyle(searchStyle)
            .execute();

    var patients =
        patientBundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(Patient.class::isInstance)
            .map(Patient.class::cast)
            .toList();

    assertEquals(1, patientBundle.getEntry().size());
    assertTrue(
        patients.stream()
            .flatMap(patient -> patient.getLink().stream())
            .allMatch(link -> Patient.LinkType.REPLACEDBY.equals(link.getType())),
        "Expected all Patient.link.type values to be 'replaced by'");

    assertTrue(
        patients.stream()
            .flatMap(patient -> patient.getLink().stream())
            .map(link -> link.getOther().getDisplay())
            .anyMatch("178083966"::equals),
        "Expected one link with display '178083966'");
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchByIdUnMergedWithHistoricKillCredit(SearchStyleEnum searchStyle) {
    var patientBundle =
        searchBundle()
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("178083966"))
            .usingStyle(searchStyle)
            .execute();

    var patients =
        patientBundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(Patient.class::isInstance)
            .map(Patient.class::cast)
            .toList();

    assertEquals(1, patientBundle.getEntry().size());
    assertTrue(
        patients.stream()
            .flatMap(patient -> patient.getLink().stream())
            .allMatch(link -> Patient.LinkType.REPLACES.equals(link.getType())),
        "Expected all Patient.link.type values to be 'replaces'");

    assertTrue(
        patients.stream()
            .flatMap(patient -> patient.getLink().stream())
            .map(link -> link.getOther().getDisplay())
            .anyMatch("792872340"::equals),
        "Expected one link with display '792872340'");

    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchByIdEmpty(SearchStyleEnum searchStyle) {
    var patientBundle =
        searchBundle()
            .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("999"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(0, patientBundle.getEntry().size());
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchByIdentifier(SearchStyleEnum searchStyle) {
    var patientBundle =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(SystemUrls.CMS_MBI, "2B19C89AA35"))
            .usingStyle(searchStyle)
            .execute();
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchByIdentifierEmpty(SearchStyleEnum searchStyle) {
    var patientBundle =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(SystemUrls.CMS_MBI, "999"))
            .execute();
    assertEquals(0, patientBundle.getEntry().size());
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @Test
  void patientSearchByIdentifierMissingSystem() {
    assertThrows(
        InvalidRequestException.class,
        () ->
            searchBundle()
                .where(
                    new TokenClientParam(Patient.SP_IDENTIFIER).exactly().identifier("405764107"))
                .execute());
  }

  private static Stream<Arguments> patientSearchByDate() {
    return Stream.of(
        Arguments.of(
            new TokenClientParam(Patient.SP_RES_ID).exactly().identifier("405764107"), "405764107"),
        Arguments.of(
            new TokenClientParam(Patient.SP_IDENTIFIER)
                .exactly()
                .systemAndIdentifier(SystemUrls.CMS_MBI, "2B19C89AA35"),
            "517782585"));
  }

  @ParameterizedTest
  @MethodSource
  void patientSearchByDate(ICriterion<TokenClientParam> searchCriteriaId, String beneSk) {
    var lastUpdated =
        entityManager
            .createQuery(
                "SELECT b.meta.updatedTimestamp FROM Beneficiary b WHERE b.beneSk=:beneSk",
                ZonedDateTime.class)
            .setParameter("beneSk", beneSk)
            .getResultList()
            .getFirst();

    for (var searchStyle : SearchStyleEnum.values()) {
      var patientBundle =
          searchBundle()
              .where(searchCriteriaId)
              .and(
                  new DateClientParam(Constants.PARAM_LASTUPDATED)
                      .exactly()
                      .day(DateUtil.toDate(lastUpdated)))
              .usingStyle(searchStyle)
              .execute();
      assertEquals(1, patientBundle.getEntry().size());

      // Search date greater than
      patientBundle =
          searchBundle()
              .where(searchCriteriaId)
              .and(
                  new DateClientParam(Constants.PARAM_LASTUPDATED)
                      .after()
                      .day(DateUtil.toDate(lastUpdated.plusDays(1))))
              .usingStyle(searchStyle)
              .execute();
      assertEquals(0, patientBundle.getEntry().size());

      // Search date less than
      patientBundle =
          searchBundle()
              .where(searchCriteriaId)
              .and(
                  new DateClientParam(Constants.PARAM_LASTUPDATED)
                      .before()
                      .millis(DateUtil.toDate(lastUpdated)))
              .usingStyle(searchStyle)
              .execute();
      assertEquals(0, patientBundle.getEntry().size());

      // Search date greater than or equal
      patientBundle =
          searchBundle()
              .where(searchCriteriaId)
              .and(
                  new DateClientParam(Constants.PARAM_LASTUPDATED)
                      .afterOrEquals()
                      .millis(DateUtil.toDate(lastUpdated)))
              .usingStyle(searchStyle)
              .execute();
      assertEquals(1, patientBundle.getEntry().size());

      // Search date less than or equal
      patientBundle =
          searchBundle()
              .where(searchCriteriaId)
              .and(
                  new DateClientParam(Constants.PARAM_LASTUPDATED)
                      .beforeOrEquals()
                      .millis(DateUtil.toDate(lastUpdated.plusDays(1))))
              .usingStyle(searchStyle)
              .execute();
      assertEquals(1, patientBundle.getEntry().size());
    }
  }

  @ParameterizedTest
  @EmptySource
  void patientSearchEmptyIdBadRequest(String id) {
    assertThrows(
        InvalidRequestException.class,
        () ->
            searchBundle()
                .where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier(id))
                .execute());
  }

  @ParameterizedTest
  @EmptySource
  void patientSearchEmptyIdentifierBadRequest(String id) {
    assertThrows(
        InvalidRequestException.class,
        () ->
            searchBundle()
                .where(
                    new TokenClientParam(Patient.SP_IDENTIFIER)
                        .exactly()
                        .systemAndIdentifier(Patient.SP_IDENTIFIER, id))
                .execute());
  }

  @Test
  void patientSearchDateOnlyBadRequest() {
    assertThrows(
        InvalidRequestException.class,
        () ->
            searchBundle()
                .where(
                    new DateClientParam(Constants.PARAM_LASTUPDATED)
                        .afterOrEquals()
                        .day(DateUtil.toDate(ZonedDateTime.parse("2024-01-01T00:00:00Z"))))
                .execute());
  }
}
