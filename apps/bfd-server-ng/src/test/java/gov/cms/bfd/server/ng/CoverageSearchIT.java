package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

public class CoverageSearchIT extends IntegrationTestBase {

  @Autowired private EntityManager entityManager;

  private IQuery<Bundle> searchBundle() {
    return getFhirClient().search().forResource(Coverage.class).returnBundle(Bundle.class);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchById(SearchStyleEnum searchStyle) {
    String validId = "part-a-405764107";

    Bundle coverageBundle =
        searchBundle()
            .where(new TokenClientParam(Coverage.SP_RES_ID).exactly().identifier(validId))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        1,
        coverageBundle.getEntry().size(),
        "Should find exactly one Coverage for this composite ID");
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchByIdEmpty(SearchStyleEnum searchStyle) {
    String nonExistentId = "part-a-9999999";

    Bundle coverageBundle =
        searchBundle()
            .where(new TokenClientParam(Coverage.SP_RES_ID).exactly().identifier(nonExistentId))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        0, coverageBundle.getEntry().size(), "Should find no Coverage for a non-existent ID");
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchByBeneficiary(SearchStyleEnum searchStyle) {
    long beneficiaryId = 405764107;

    Bundle coverageBundle =
        searchBundle()
            .where(
                new ReferenceClientParam(Coverage.SP_BENEFICIARY).hasId("Patient/" + beneficiaryId))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        2,
        coverageBundle.getEntry().size(),
        "Should find all Coverage resources for the given beneficiary");
    coverageBundle
        .getEntry()
        .sort(Comparator.comparing(entry -> entry.getResource().getIdElement().getIdPart()));
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchByBeneficiaryEmpty(SearchStyleEnum searchStyle) {
    long beneficiaryIdWithNoCoverage = 9999999;

    Bundle coverageBundle =
        searchBundle()
            .where(
                new ReferenceClientParam(Coverage.SP_BENEFICIARY)
                    .hasId("Patient/" + beneficiaryIdWithNoCoverage))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        0,
        coverageBundle.getEntry().size(),
        "Should find no Coverage for a beneficiary with no coverage data");
    expect.scenario(searchStyle.name()).serializer("fhir+json").toMatchSnapshot(coverageBundle);
  }

  private static Stream<Arguments> coverageSearchWithLastUpdated() {
    return Stream.of(
        Arguments.of(
            new TokenClientParam(Coverage.SP_RES_ID).exactly().identifier("part-a-405764107"),
            405764107L),
        Arguments.of(
            new ReferenceClientParam(Coverage.SP_BENEFICIARY).hasId("Patient/405764107"),
            405764107L));
  }

  @ParameterizedTest
  @MethodSource
  void coverageSearchWithLastUpdated(ICriterion<?> searchCriteria, long beneSk) {
    ZonedDateTime lastUpdated =
        entityManager
            .createQuery(
                "SELECT b.meta.updatedTimestamp FROM Beneficiary b WHERE b.beneSk = :beneSk",
                ZonedDateTime.class)
            .setParameter("beneSk", beneSk)
            .getSingleResult();
    assertNotNull(lastUpdated);

    // inclusive range : one millisecond before and one millisecond after.
    ZonedDateTime lowerBound = lastUpdated.minusNanos(1_000_000); // 1 millisecond before
    ZonedDateTime upperBound = lastUpdated.plusNanos(1_000_000); // 1 millisecond after

    // Search using an inclusive range that is guaranteed to contain the value
    System.out.println("Testing inclusive range for bene_sk: " + beneSk);
    Bundle coverageBundle =
        searchBundle()
            .where(searchCriteria)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .afterOrEquals()
                    .millis(DateUtil.toDate(lowerBound))) // 'ge' one millisecond before
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .beforeOrEquals()
                    .millis(DateUtil.toDate(upperBound))) // 'le' one millisecond after
            .execute();
    assertEquals(
        true,
        coverageBundle.getEntry().size() > 0,
        "A small inclusive range around the exact timestamp should find a match");

    // Search for strictly greater than (gt) the exact timestamp
    System.out.println("Testing 'gt' for bene_sk: " + beneSk);
    coverageBundle =
        searchBundle()
            .where(searchCriteria)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .after()
                    .millis(DateUtil.toDate(lastUpdated)))
            .execute();
    assertEquals(
        0,
        coverageBundle.getEntry().size(),
        "_lastUpdated=gt with full precision should NOT find a match");

    // Search for strictly less than (lt) the exact timestamp ---
    System.out.println("Testing 'lt' for bene_sk: " + beneSk);
    coverageBundle =
        searchBundle()
            .where(searchCriteria)
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .before()
                    .millis(DateUtil.toDate(lastUpdated)))
            .execute();
    assertEquals(
        0,
        coverageBundle.getEntry().size(),
        "_lastUpdated=lt with full precision should NOT find a match");
  }

  @Test
  void coverageSearchWithNoParametersBadRequest() {
    assertThrows(InvalidRequestException.class, () -> searchBundle().execute());
  }
}
