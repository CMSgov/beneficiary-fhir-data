package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.coverage.CoverageResourceProvider;
import gov.cms.bfd.server.ng.testUtil.ThreadSafeAppender;
import gov.cms.bfd.server.ng.util.DateUtil;
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

class CoverageSearchIT extends IntegrationTestBase {
  @Autowired private CoverageResourceProvider coverageResourceProvider;

  private IQuery<Bundle> searchBundle() {
    return getFhirClient().search().forResource(Coverage.class).returnBundle(Bundle.class);
  }

  private Bundle searchByBeneficiary(String beneId, SearchStyleEnum searchStyle) {
    return searchBundle()
        .where(new ReferenceClientParam(Coverage.SP_BENEFICIARY).hasId("Patient/" + beneId))
        .usingStyle(searchStyle)
        .execute();
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchById(SearchStyleEnum searchStyle) {
    String validId = String.format("part-a-%s", BENE_ID_ALL_PARTS_WITH_XREF);

    var coverageBundle =
        searchBundle()
            .where(new TokenClientParam(Coverage.SP_RES_ID).exactly().identifier(validId))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        1,
        coverageBundle.getEntry().size(),
        "Should find exactly one Coverage for this composite ID");
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void partCAndDCoverageSearchById(SearchStyleEnum searchStyle) {
    var validId = String.format("part-c-%s", BENE_ID_PART_C_AND_D_ONLY);

    var coverageBundle =
        searchBundle()
            .where(new TokenClientParam(Coverage.SP_RES_ID).exactly().identifier(validId))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        2,
        coverageBundle.getEntry().size(),
        "Should find exactly one Coverage for this composite ID");
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @Test
  void coverageSearchQueryCount() {
    var events = ThreadSafeAppender.startRecord();
    var coverage =
        coverageResourceProvider.searchByBeneficiary(
            new ReferenceParam(BENE_ID_ALL_PARTS_WITH_XREF), new DateRangeParam());
    // This should increase when we map the other coverage types
    assertEquals(5, coverage.getEntry().size());
    assertEquals(1, queryCount(events));
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchByIdEmpty(SearchStyleEnum searchStyle) {
    String nonExistentId = String.format("part-a-%s", BENE_ID_NO_COVERAGE);

    var coverageBundle =
        searchBundle()
            .where(new TokenClientParam(Coverage.SP_RES_ID).exactly().identifier(nonExistentId))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        0, coverageBundle.getEntry().size(), "Should find no Coverage for a non-existent ID");
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchByBeneficiary(SearchStyleEnum searchStyle) {

    var coverageBundle =
        searchBundle()
            .where(
                new ReferenceClientParam(Coverage.SP_BENEFICIARY)
                    .hasId("Patient/" + BENE_ID_ALL_PARTS_WITH_XREF))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        5,
        coverageBundle.getEntry().size(),
        "Should find all Coverage resources for the given beneficiary");

    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchByBeneficiaryEmpty(SearchStyleEnum searchStyle) {

    var coverageBundle =
        searchBundle()
            .where(
                new ReferenceClientParam(Coverage.SP_BENEFICIARY)
                    .hasId("Patient/" + BENE_ID_NO_COVERAGE))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        0,
        coverageBundle.getEntry().size(),
        "Should find no Coverage for a beneficiary with no coverage data");
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  private static Stream<Arguments> coverageSearchWithLastUpdated() {
    return Stream.of(
        Arguments.of(
            new TokenClientParam(Coverage.SP_RES_ID)
                .exactly()
                .identifier(String.format("part-a-%s", BENE_ID_ALL_PARTS_WITH_XREF)),
            Long.parseLong(BENE_ID_ALL_PARTS_WITH_XREF)),
        Arguments.of(
            new ReferenceClientParam(Coverage.SP_BENEFICIARY)
                .hasId(String.format("Patient/%s", BENE_ID_ALL_PARTS_WITH_XREF)),
            Long.parseLong(BENE_ID_ALL_PARTS_WITH_XREF)));
  }

  @ParameterizedTest
  @MethodSource
  void coverageSearchWithLastUpdated(ICriterion<?> searchCriteria, long beneSk) {
    var lastUpdated =
        entityManager
            .createQuery(
                "SELECT b.meta.updatedTimestamp FROM Beneficiary b WHERE b.beneSk = :beneSk",
                ZonedDateTime.class)
            .setParameter("beneSk", beneSk)
            .getSingleResult();
    assertNotNull(lastUpdated);

    // inclusive range : one millisecond before and one millisecond after.
    var lowerBound = lastUpdated.minusNanos(1_000_000); // 1 millisecond before
    var upperBound = lastUpdated.plusNanos(1_000_000); // 1 millisecond after

    var coverageBundle =
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
    assertFalse(
        coverageBundle.getEntry().isEmpty(),
        "A small inclusive range around the exact timestamp should find a match");

    // Search for strictly greater than (gt) the exact timestamp
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
    var bundle = searchBundle();
    assertThrows(InvalidRequestException.class, bundle::execute);
  }

  private static Stream<Arguments> coverageSearchForBeneWithSinglePartShouldReturnOneEntry() {
    return Stream.of(
        Arguments.of(BENE_ID_PART_A_ONLY, "part-a"),
        Arguments.of(BENE_ID_PART_B_ONLY, "part-b"),
        Arguments.of(BENE_ID_DUAL_ONLY, "dual"));
  }

  @ParameterizedTest
  @MethodSource
  void coverageSearchForBeneWithSinglePartShouldReturnOneEntry(String beneSk, String part) {
    var coverageBundle = searchByBeneficiary(beneSk, SearchStyleEnum.GET);
    assertEquals(
        1,
        coverageBundle.getEntry().size(),
        "Should find exactly one Coverage resource for a Part A-only beneficiary");
    assertEquals(
        part + "-" + beneSk,
        coverageBundle.getEntry().getFirst().getResource().getIdElement().getIdPart());
    expectFhir().scenario("singleCoverage" + part).toMatchSnapshot(coverageBundle);
  }

  private static Stream<Arguments>
      partCAndDCoverageSearchForBeneWithSinglePartShouldReturnOneEntry() {
    return Stream.of(
        Arguments.of(BENE_ID_PART_C_ONLY, "part-c"), Arguments.of(BENE_ID_PART_D_ONLY, "part-d"));
  }

  @ParameterizedTest
  @MethodSource
  void partCAndDCoverageSearchForBeneWithSinglePartShouldReturnOneEntry(
      String beneSk, String part) {
    var coverageBundle = searchByBeneficiary(beneSk, SearchStyleEnum.GET);
    assertEquals(
        1,
        coverageBundle.getEntry().size(),
        "Should find exactly one Coverage resource for a Part A-only beneficiary");
    expectFhir().scenario("singleCoverage" + part).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchForNonCurrentBeneShouldReturnEmptyBundle(SearchStyleEnum searchStyle) {
    var coverageBundle = searchByBeneficiary(BENE_ID_NON_CURRENT, searchStyle);
    assertEquals(
        0,
        coverageBundle.getEntry().size(),
        "Should find no Coverage for a beneficiary record that is not the current effective version.");
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchForBeneWithNoCoverageShouldReturnEmptyBundle(SearchStyleEnum searchStyle) {
    var coverageBundle = searchByBeneficiary(BENE_ID_NO_COVERAGE, searchStyle);
    assertEquals(
        0,
        coverageBundle.getEntry().size(),
        "Should find no Coverage for a beneficiary who exists but has no entitlement records.");
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchForBeneWithExpiredCoverage(SearchStyleEnum searchStyle) {
    var coverageBundle = searchByBeneficiary(BENE_ID_EXPIRED_COVERAGE, searchStyle);
    assertEquals(
        2,
        coverageBundle.getEntry().size(),
        "Should find no Coverage for a beneficiary whose entitlement periods are all in the past.");
    for (var coverage : getCoverageFromBundle(coverageBundle)) {
      assertEquals(Coverage.CoverageStatus.CANCELLED, coverage.getStatus());
    }
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchForBeneWithFutureCoverageShouldReturnEmptyBundle(SearchStyleEnum searchStyle) {
    var coverageBundle = searchByBeneficiary(BENE_ID_FUTURE_COVERAGE, searchStyle);
    assertEquals(
        2,
        coverageBundle.getEntry().size(),
        "Should only find Coverage for a beneficiary whose enrollment periods all start in the future.");
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void coverageSearchForBeneWithMissingTplDataShouldStillReturnResources(
      SearchStyleEnum searchStyle) {
    var coverageBundle = searchByBeneficiary(BENE_ID_NO_TP, searchStyle);
    assertEquals(
        2,
        coverageBundle.getEntry().size(),
        "Should find both Part A and B Coverage resources even if TPL data is missing.");
    coverageBundle
        .getEntry()
        .sort(Comparator.comparing(entry -> entry.getResource().getIdElement().getIdPart()));
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(coverageBundle);
  }
}
