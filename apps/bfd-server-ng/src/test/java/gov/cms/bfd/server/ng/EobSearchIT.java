package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

public class EobSearchIT extends IntegrationTestBase {
  @Autowired private EntityManager entityManager;

  private IQuery<Bundle> searchBundle() {
    return getFhirClient()
        .search()
        .forResource(ExplanationOfBenefit.class)
        .returnBundle(Bundle.class);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchById(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier("1071939711295"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(1, eobBundle.getEntry().size());
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByIdEmpty(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(new TokenClientParam(ExplanationOfBenefit.SP_RES_ID).exactly().identifier("999"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByPatient(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier("405764107"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(2, eobBundle.getEntry().size());
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByPatientHistorical(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier("181968400"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void eobSearchByPatientLimitOffset(int offset) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier("405764107"))
            .count(1)
            .offset(offset)
            .execute();
    assertEquals(1, eobBundle.getEntry().size());
    expect.scenario("offset" + offset).serializer("fhir+json").toMatchSnapshot(eobBundle);
  }

  @Test
  void eobSearchByDate() {
    var beneSk = "405764107";
    var lastUpdated =
        entityManager
            .createQuery(
                "SELECT c.meta.updatedTimestamp FROM Claim c WHERE c.beneficiary.xrefSk = :beneSk",
                ZonedDateTime.class)
            .setParameter("beneSk", beneSk)
            .getResultList()
            .getFirst();

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT).exactly().identifier(beneSk))
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .afterOrEquals()
                    .day(DateUtil.toDate(lastUpdated)))
            .execute();
    assertEquals(2, eobBundle.getEntry().size());

    eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT).exactly().identifier(beneSk))
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .before()
                    .day(DateUtil.toDate(lastUpdated.minusDays(1))))
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
  }

  @Test
  void eobSearchByServiceDate() {
    var claimId = "1071939711295";
    var serviceDate =
        entityManager
            .createQuery(
                "SELECT billablePeriod.claimThroughDate FROM Claim c WHERE c.claimUniqueId = :id",
                LocalDate.class)
            .setParameter("id", claimId)
            .getResultList()
            .getFirst();

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID).exactly().identifier(claimId))
            .and(
                new DateClientParam("service-date")
                    .afterOrEquals()
                    .day(DateUtil.toDate(serviceDate)))
            .execute();
    assertEquals(1, eobBundle.getEntry().size());

    eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT).exactly().identifier(claimId))
            .and(
                new DateClientParam("service-date")
                    .before()
                    .day(DateUtil.toDate(serviceDate.minusDays(1))))
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
  }

  @Test
  void eobSearchByPatientInvalidResourceType() {
    assertThrows(
        InvalidRequestException.class,
        () ->
            searchBundle()
                .where(
                    new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                        .exactly()
                        .identifier("Blah/181968400"))
                .execute());
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByTag(SearchStyleEnum searchStyle) {
    String beneficiaryId = "405764107";
    String validTag = IdrConstants.ADJUDICATION_STATUS_FINAL;

    Bundle eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(beneficiaryId))
            .and(new TokenClientParam(Constants.PARAM_TAG).exactly().identifier(validTag))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        2, eobBundle.getEntry().size(), "Should find EOBs with the specified adjudication status");

    expect
        .scenario(searchStyle.name() + "_WithTag_" + validTag)
        .serializer("fhir+json")
        .toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByTagEmpty(SearchStyleEnum searchStyle) {
    String beneficiaryId = "405764107";
    String validTagWithNoMatches = IdrConstants.ADJUDICATION_STATUS_PARTIAL;

    Bundle eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(beneficiaryId))
            .and(
                new TokenClientParam(Constants.PARAM_TAG)
                    .exactly()
                    .identifier(validTagWithNoMatches))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        0, eobBundle.getEntry().size(), "Should find no EOBs with this adjudication status");
    expect
        .scenario(searchStyle.name() + "_WithTag_EmptyResult")
        .serializer("fhir+json")
        .toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidStatus", "in-progress", "test"})
  void eobSearchByInvalidTagBadRequest(String invalidTag) {
    String beneficiaryId = "405764107";

    assertThrows(
        InvalidRequestException.class,
        () ->
            searchBundle()
                .where(
                    new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                        .exactly()
                        .identifier(beneficiaryId))
                .and(new TokenClientParam(Constants.PARAM_TAG).exactly().identifier(invalidTag))
                .execute(),
        "Should throw InvalidRequestException for unsupported _tag value: " + invalidTag);
  }
}
