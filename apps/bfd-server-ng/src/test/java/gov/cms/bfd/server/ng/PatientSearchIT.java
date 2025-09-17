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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
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
            .where(
                new TokenClientParam(Patient.SP_RES_ID).exactly().identifier(BENE_ID_PART_A_ONLY))
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
            .where(
                new TokenClientParam(Patient.SP_RES_ID)
                    .exactly()
                    .identifier(HISTORICAL_MERGED_BENE_SK))
            .usingStyle(searchStyle)
            .execute();

    var patients = getPatientsFromBundle(patientBundle);

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
            .anyMatch(CURRENT_MERGED_BENE_SK::equals),
        String.format("Expected one link with display '%s'", CURRENT_MERGED_BENE_SK));
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchByIdUnMergedWithHistoricKillCredit(SearchStyleEnum searchStyle) {
    // bene_sk with kill credit set should not contain the link to the merged bene
    var patientBundle =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_RES_ID)
                    .exactly()
                    .identifier(HISTORICAL_MERGED_BENE_SK_KILL_CREDIT))
            .usingStyle(searchStyle)
            .execute();

    var patients = getPatientsFromBundle(patientBundle);
    assertEquals(1, patients.size());
    var patient = patients.getFirst();

    assertEquals(0, patient.getLink().size());
    assertEquals(0, patient.getIdentifier().size());

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
                    .systemAndIdentifier(SystemUrls.CMS_MBI, HISTORICAL_AND_CURRENT_MBI))
            .usingStyle(searchStyle)
            .execute();
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(patientBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void patientSearchByHistoricalMbi(SearchStyleEnum searchStyle) {
    var patientBundle =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(SystemUrls.CMS_MBI, HISTORICAL_MERGED_MBI))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(1, patientBundle.getEntry().size());
    var patient = getPatientsFromBundle(patientBundle).getFirst();
    assertEquals("Patient/" + CURRENT_MERGED_BENE_SK, patient.getId());
    var identifiers =
        patient.getIdentifier().stream().map(Identifier::getValue).collect(Collectors.toSet());
    assertEquals(Set.of(HISTORICAL_MERGED_MBI, HISTORICAL_AND_CURRENT_MBI), identifiers);

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
    var searchWithIdentifier =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .identifier(BENE_ID_PART_A_ONLY));
    assertThrows(InvalidRequestException.class, searchWithIdentifier::execute);
  }

  private static Stream<Arguments> patientSearchByDate() {
    return Stream.of(
        Arguments.of(
            new TokenClientParam(Patient.SP_RES_ID)
                .exactly()
                .identifier(BENE_ID_ALL_PARTS_WITH_XREF),
            BENE_ID_ALL_PARTS_WITH_XREF),
        Arguments.of(
            new TokenClientParam(Patient.SP_IDENTIFIER)
                .exactly()
                .systemAndIdentifier(SystemUrls.CMS_MBI, HISTORICAL_AND_CURRENT_MBI),
            CURRENT_MERGED_BENE_SK));
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
    var searchWithIdentifier =
        searchBundle().where(new TokenClientParam(Patient.SP_RES_ID).exactly().identifier(id));
    assertThrows(InvalidRequestException.class, searchWithIdentifier::execute);
  }

  @ParameterizedTest
  @EmptySource
  void patientSearchEmptyIdentifierBadRequest(String id) {
    var searchWithIdentifier =
        searchBundle()
            .where(
                new TokenClientParam(Patient.SP_IDENTIFIER)
                    .exactly()
                    .systemAndIdentifier(Patient.SP_IDENTIFIER, id));
    assertThrows(InvalidRequestException.class, searchWithIdentifier::execute);
  }

  @Test
  void patientSearchDateOnlyBadRequest() {
    var searchWithDate =
        searchBundle()
            .where(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .afterOrEquals()
                    .day(DateUtil.toDate(ZonedDateTime.parse("2024-01-01T00:00:00Z"))));
    assertThrows(InvalidRequestException.class, searchWithDate::execute);
  }
}
