package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.claim.model.ClaimFinalAction;
import gov.cms.bfd.server.ng.claim.model.ClaimProfessionalNch;
import gov.cms.bfd.server.ng.claim.model.ClaimSubtype;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class EobSearchIT extends IntegrationTestBase {

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
                    .identifier(CLAIM_ID_ADJUDICATED))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(1, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  static Stream<Arguments> provideEobSearchByIdOutcomeScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            searchStyle ->
                Stream.of(
                    Arguments.of("MatchingOutcome", OUTCOME_COMPLETE, 1, searchStyle),
                    Arguments.of("NonMatchingOutcome", OUTCOME_PARTIAL, 0, searchStyle)));
  }

  @ParameterizedTest
  @MethodSource("provideEobSearchByIdOutcomeScenarios")
  void eobSearchByIdAndOutcome(
      String scenarioName, String outcome, int expectedCount, SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier(CLAIM_ID_ADJUDICATED))
            .and(new TokenClientParam(OUTCOME).exactly().identifier(outcome))
            .usingStyle(searchStyle)
            .execute();

    var eobs = getEobFromBundle(eobBundle);

    assertEquals(expectedCount, eobs.size(), scenarioName);

    assertTrue(
        eobs.stream()
            .allMatch(eob -> eob.hasOutcome() && outcome.equals(eob.getOutcome().toCode())),
        "All returned EOBs should have outcome " + outcome);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByIdMultiple(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .codes(CLAIM_ID_PROFESSIONAL, CLAIM_ID_RX))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(2, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  @Test
  void eobSearchByIdProfessionalClaimDoesNotIncludeTaxNumberByDefault() {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier(CLAIM_ID_PROFESSIONAL))
            .execute();

    assertEquals(1, eobBundle.getEntry().size());
    assertFalse(hasTaxNumberExtension(getEobFromBundle(eobBundle).getFirst()));
  }

  @Test
  void eobSearchByIdProfessionalClaimIncludesTaxNumberWhenHeaderTrue() {
    var eobBundle =
        searchBundleWithIncludeTaxNumbersHeader("true")
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier(CLAIM_ID_PROFESSIONAL))
            .execute();

    assertEquals(1, eobBundle.getEntry().size());
    assertTrue(hasTaxNumberExtension(getEobFromBundle(eobBundle).getFirst()));
  }

  @Test
  void eobSearchByIdProfessionalClaimDoesNotIncludeTaxNumberWhenHeaderFalse() {
    var eobBundle =
        searchBundleWithIncludeTaxNumbersHeader("false")
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier(CLAIM_ID_PROFESSIONAL))
            .execute();

    assertEquals(1, eobBundle.getEntry().size());
    assertFalse(hasTaxNumberExtension(getEobFromBundle(eobBundle).getFirst()));
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByIdMultipleDuplicate(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .codes(CLAIM_ID_PROFESSIONAL, CLAIM_ID_RX, CLAIM_ID_RX))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(2, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByIdMultipleAndSource(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .codes(CLAIM_ID_PROFESSIONAL, CLAIM_ID_RX))
            .where(new TokenClientParam(SOURCE).exactly().codes(DDPS_SOURCE))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(1, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
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
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  @Test
  void eobSearchByIdTooMany() {
    var ids = IntStream.range(0, 101).mapToObj(String::valueOf).toList();
    var query =
        searchBundle()
            .where(new TokenClientParam(ExplanationOfBenefit.SP_RES_ID).exactly().codes(ids));
    var thrown = assertThrows(InvalidRequestException.class, query::execute);
    assertTrue(thrown.getMessage().contains("maximum of 100 claim IDs"));
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByPatient(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(6, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByPatientHistorical(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(HISTORICAL_MERGED_BENE_SK))
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
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .count(1)
            .offset(offset)
            .execute();
    assertEquals(1, eobBundle.getEntry().size());
    expectFhir().scenario("offset" + offset).toMatchSnapshot(eobBundle);
  }

  @Test
  void eobSearchByDate() {
    var instant =
        (Instant)
            entityManager
                .createNativeQuery(
                    """
                  select max(bfd_claim_updated_ts)
                  from (
                      select bfd_claim_updated_ts from idr.claim_professional_nch where bene_sk = :beneSk
                      union all
                      select bfd_claim_updated_ts from idr.claim_professional_ss where bene_sk = :beneSk
                      union all
                      select bfd_claim_updated_ts from idr.claim_institutional_nch where bene_sk = :beneSk
                      union all
                      select bfd_claim_updated_ts from idr.claim_institutional_ss where bene_sk = :beneSk
                      union all
                      select bfd_claim_updated_ts from idr.claim_rx where bene_sk = :beneSk
                  ) all_claims
                  """)
                .setParameter("beneSk", Long.valueOf(BENE_ID_NON_CURRENT))
                .getSingleResult();
    ZonedDateTime lastUpdated = instant == null ? null : instant.atZone(ZoneOffset.UTC);

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .afterOrEquals()
                    .day(DateUtil.toDate(lastUpdated)))
            .execute();
    assertEquals(6, eobBundle.getEntry().size());

    eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
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
        (LocalDate)
            entityManager
                .createQuery(
                    """
                SELECT billablePeriod.claimThroughDate
                FROM ClaimInstitutionalNch c
                WHERE c.claimUniqueId = :id
                """,
                    Optional.class)
                .setParameter("id", claimId)
                .getResultList()
                .getFirst()
                .get();

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
    var searchWithidentifier =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier("Blah/" + BENE_ID_ALL_PARTS_WITH_XREF));
    assertThrows(InvalidRequestException.class, searchWithidentifier::execute);
  }

  static Stream<Arguments> provideTagScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            style ->
                Stream.of(
                    Arguments.of(
                        "WithTag_SharedSystem",
                        List.of(List.of(tag(SystemUrls.BLUE_BUTTON_SYSTEM_TYPE, "SharedSystem"))),
                        3,
                        style),
                    Arguments.of(
                        "WithTag_DDPS",
                        List.of(List.of(tag(SystemUrls.BLUE_BUTTON_SYSTEM_TYPE, "DDPS"))),
                        1,
                        style),
                    Arguments.of(
                        "WithTagFinalActionAndSharedSystem",
                        List.of(
                            List.of(systemType(MetaSourceSk.FISS)),
                            List.of(finalAction(ClaimFinalAction.YES))),
                        2,
                        style),
                    Arguments.of(
                        "WithIncompatibleTags",
                        List.of(
                            List.of(systemType(MetaSourceSk.FISS)),
                            List.of(systemType(MetaSourceSk.NCH))),
                        0,
                        style),
                    Arguments.of(
                        "WithCombinedTagOr",
                        List.of(
                            List.of(
                                systemType(MetaSourceSk.NCH), finalAction(ClaimFinalAction.YES))),
                        5,
                        style),
                    Arguments.of(
                        "WithCombinedTagOrDDPSNCH",
                        List.of(
                            List.of(systemType(MetaSourceSk.NCH), systemType(MetaSourceSk.DDPS))),
                        3,
                        style),
                    Arguments.of(
                        "WithSystemTag_FinalAction",
                        List.of(
                            List.of(
                                tag(SystemUrls.BLUE_BUTTON_FINAL_ACTION_STATUS, "FinalAction"))),
                        5,
                        style)));
  }

  @ParameterizedTest
  @MethodSource("provideTagScenarios")
  void eobSearchByTags(
      String scenarioName,
      List<List<Coding>> tagScenarios,
      int expectedCount,
      SearchStyleEnum searchStyle) {
    var query =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF));

    for (List<Coding> tags : tagScenarios) {
      if (tags.size() == 1) {
        query =
            query.and(
                new TokenClientParam(Constants.PARAM_TAG)
                    .exactly()
                    .systemAndCode(tags.get(0).getSystem(), tags.get(0).getCode()));
      } else {
        query =
            query.and(
                new TokenClientParam(Constants.PARAM_TAG)
                    .exactly()
                    .codings(tags.toArray(new Coding[0])));
      }
    }

    var eobBundle = query.usingStyle(searchStyle).execute();
    expectFhir().scenario(searchStyle.name() + "_" + scenarioName).toMatchSnapshot(eobBundle);

    assertEquals(
        expectedCount,
        eobBundle.getEntry().size(),
        "Should find " + expectedCount + " EOBs for scenario " + scenarioName);
  }

  private static Coding tag(String system, String code) {
    return new Coding(system, code, null);
  }

  private static Coding systemType(MetaSourceSk metaSourceSk) {
    return tag(SystemUrls.BLUE_BUTTON_SYSTEM_TYPE, metaSourceSk.getSystemType());
  }

  private static Coding finalAction(ClaimFinalAction finalAction) {
    return tag(SystemUrls.BLUE_BUTTON_FINAL_ACTION_STATUS, finalAction.getFinalAction());
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByType(SearchStyleEnum searchStyle) {
    var outpatientType = ClaimSubtype.OUTPATIENT.getCode();

    var eobBundleOutpatient =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam("type").exactly().identifier(outpatientType))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        3,
        eobBundleOutpatient.getEntry().size(),
        "Should find EOBs with the outpatient claim type");

    expectFhir()
        .scenario(searchStyle.name() + "_WithClaimType_" + outpatientType)
        .toMatchSnapshot(eobBundleOutpatient);

    var hhaType = ClaimSubtype.HHA.getCode();

    var eobBundleMultipleTypes =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam("type").exactly().identifier(hhaType))
            .and(new TokenClientParam("type").exactly().identifier(outpatientType))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(
        4,
        eobBundleMultipleTypes.getEntry().size(),
        "Should find EOBs with both HHA and Outpatient claim types");

    expectFhir()
        .scenario(searchStyle.name() + "_WithMultipleClaimTypes_" + hhaType + "," + outpatientType)
        .toMatchSnapshot(eobBundleMultipleTypes);

    var wildcardType = "*";
    var eobBundleWildcard =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam("type").exactly().identifier(wildcardType))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(6, eobBundleWildcard.getEntry().size(), "Should find ALL EOBs for '*' type");
    expectFhir().scenario(searchStyle.name() + "_WithWildcard").toMatchSnapshot(eobBundleWildcard);

    String[] zeroResultClaimTypes = {
      ClaimSubtype.DME.getCode(),
      ClaimSubtype.SNF.getCode(),
      ClaimSubtype.HOSPICE.getCode(),
      ClaimSubtype.INPATIENT.getCode()
    };

    for (var claimType : zeroResultClaimTypes) {
      var eobBundleZero =
          searchBundle()
              .where(
                  new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                      .exactly()
                      .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
              .and(new TokenClientParam("type").exactly().identifier(claimType))
              .usingStyle(searchStyle)
              .execute();

      assertEquals(
          0,
          eobBundleZero.getEntry().size(),
          "Should find 0 EOBs for " + claimType + " claim type for this patient");
    }
  }

  @Test
  void eobSearchInvalidIdBadRequest() {
    var searchWithIdentifier =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier("abc123"));
    assertThrows(InvalidRequestException.class, searchWithIdentifier::execute);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "invalidStatus",
        "in-progress",
        "test",
        "https://bluebutton.cms.gov/fhir/CodeSystem/test"
      })
  void eobSearchByInvalidTagBadRequest(String invalidTag) {
    var searchWithIdentifier =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam(Constants.PARAM_TAG).exactly().identifier(invalidTag));
    assertThrows(
        InvalidRequestException.class,
        searchWithIdentifier::execute,
        "Should throw InvalidRequestException for unsupported _tag value: " + invalidTag);
  }

  @Test
  void returnsCorrectClaimsWhenFilteringWithMergedBenes() {
    // Ensure claim IDs are de-duplicated correctly when using a limit
    // This could have problems with merged benes since they can produce duplicate claim IDs if not
    // filtered properly

    // There should be more than one bene_sk/xref_sk pair here to ensure this test has the
    // correct preconditions.
    var beneCount =
        (Integer)
            entityManager
                .createNativeQuery(
                    """
                SELECT COUNT(*) FROM idr.beneficiary
                WHERE bene_xref_efctv_sk = :beneSk AND bene_sk = :beneSk
                """,
                    Integer.class)
                .setParameter("beneSk", Long.parseLong(CURRENT_MERGED_BENE_SK))
                .getResultList()
                .getFirst();
    assertTrue(beneCount > 1);

    var searchWithIdentifier =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(CURRENT_MERGED_BENE_SK))
            .count(2);
    var bundle = searchWithIdentifier.execute();
    var results =
        bundle.getEntry().stream().map(e -> e.getResource().getId()).collect(Collectors.toSet());
    assertEquals(2, results.size());
  }

  static Stream<Arguments> provideSourceParameterScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            style ->
                Stream.of(
                    Arguments.of(
                        "WithSource_FISS",
                        List.of(List.of(MetaSourceSk.FISS.getDisplay())),
                        2,
                        style),
                    Arguments.of(
                        "WithSource_DDPS",
                        List.of(List.of(MetaSourceSk.DDPS.getDisplay())),
                        1,
                        style),
                    Arguments.of(
                        "WithCombinedSourceAnd",
                        List.of(List.of("DDPS"), List.of("NCH")),
                        0,
                        style),
                    Arguments.of(
                        "WithInvalidSource",
                        List.of(List.of(MetaSourceSk.DDPS.getDisplay(), "NCHH")),
                        0,
                        style),
                    Arguments.of(
                        "WithCombinedTagOr",
                        List.of(
                            List.of(
                                MetaSourceSk.DDPS.getDisplay().toLowerCase(),
                                MetaSourceSk.NCH.getDisplay())),
                        3,
                        style)));
  }

  @ParameterizedTest
  @MethodSource("provideSourceParameterScenarios")
  void eobSearchBySources(
      String scenarioName,
      List<List<String>> sourceScenarios,
      int expectedCount,
      SearchStyleEnum searchStyle) {
    var query =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF));

    for (List<String> sources : sourceScenarios) {
      if (sources.size() == 1) {
        query =
            query.and(
                new TokenClientParam(Constants.PARAM_SOURCE).exactly().code(sources.getFirst()));
      } else {
        query = query.and(new TokenClientParam(Constants.PARAM_SOURCE).exactly().codes(sources));
      }
    }
    if ("WithInvalidSource".equals(scenarioName)) {
      assertThrows(
          InvalidRequestException.class,
          query.usingStyle(searchStyle)::execute,
          "Should throw InvalidRequestException for unsupported _source value in: " + scenarioName);
    } else {
      var eobBundle = query.usingStyle(searchStyle).execute();
      expectFhir().scenario(searchStyle.name() + "_" + scenarioName).toMatchSnapshot(eobBundle);

      assertEquals(
          expectedCount,
          eobBundle.getEntry().size(),
          "Should find " + expectedCount + " EOBs for scenario " + scenarioName);
    }
  }

  static Stream<Arguments> provideSourceOutcomeScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            searchStyle ->
                Stream.of(MetaSourceSk.NCH.getDisplay(), MetaSourceSk.DDPS.getDisplay())
                    .flatMap(
                        source ->
                            Stream.of(
                                    OUTCOME_COMPLETE,
                                    OUTCOME_PARTIAL,
                                    OUTCOME_QUEUED,
                                    OUTCOME_ERROR)
                                .map(
                                    outcome ->
                                        Arguments.of(
                                            source + "_" + outcome,
                                            source,
                                            outcome,
                                            searchStyle))));
  }

  @ParameterizedTest
  @MethodSource("provideSourceOutcomeScenarios")
  void eobSearchBySourceAndOutcome(
      String scenarioName, String source, String outcome, SearchStyleEnum searchStyle) {

    var patientParam =
        new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
            .exactly()
            .identifier(BENE_ID_ALL_PARTS_WITH_XREF);

    var sourceParam = new TokenClientParam(Constants.PARAM_SOURCE).exactly().code(source);
    var outcomeParam = new TokenClientParam(OUTCOME).exactly().identifier(outcome);

    var sourceOnlyBundle =
        searchBundle().where(patientParam).and(sourceParam).usingStyle(searchStyle).execute();

    var sourceOnlyEobs = getEobFromBundle(sourceOnlyBundle);

    assertFalse(sourceOnlyEobs.isEmpty(), "Precondition failed for source " + source);

    var expectedCount =
        (int)
            sourceOnlyEobs.stream()
                .filter(
                    eob -> eob.hasOutcome() && outcome.equalsIgnoreCase(eob.getOutcome().toCode()))
                .count();

    var sourceAndOutcomeBundle =
        searchBundle()
            .where(patientParam)
            .and(sourceParam)
            .and(outcomeParam)
            .usingStyle(searchStyle)
            .execute();

    var sourceAndOutcomeEobs = getEobFromBundle(sourceAndOutcomeBundle);

    assertEquals(
        expectedCount,
        sourceAndOutcomeEobs.size(),
        "Should find matching EOBs for scenario " + scenarioName);

    assertTrue(
        sourceAndOutcomeEobs.stream()
            .allMatch(
                eob -> eob.hasOutcome() && outcome.equalsIgnoreCase(eob.getOutcome().toCode())),
        "All returned EOBs should have outcome " + outcome);
  }

  public static Stream<Arguments> provideOutcomeScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            searchStyle ->
                Stream.of(
                    Arguments.of("complete", OUTCOME_COMPLETE, true, searchStyle),
                    Arguments.of("partial", OUTCOME_PARTIAL, true, searchStyle),
                    Arguments.of("queued", OUTCOME_QUEUED, false, searchStyle),
                    Arguments.of("error", OUTCOME_ERROR, false, searchStyle)));
  }

  @ParameterizedTest
  @MethodSource("provideOutcomeScenarios")
  void eobSearchByOutcome(
      String scenarioName,
      String outcome,
      boolean shouldReturnResults,
      SearchStyleEnum searchStyle) {
    var patientParam =
        new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
            .exactly()
            .identifier(BENE_ID_ALL_PARTS_WITH_XREF);

    var outcomeParam = new TokenClientParam(OUTCOME).exactly().identifier(outcome);

    var eobBundle =
        searchBundle().where(patientParam).and(outcomeParam).usingStyle(searchStyle).execute();

    var eobs = getEobFromBundle(eobBundle);

    if (shouldReturnResults) {
      assertFalse(eobs.isEmpty(), "Should find EOBs for outcome " + scenarioName);

      assertTrue(
          eobs.stream()
              .allMatch(
                  eob ->
                      eob.getOutcome() != null
                          && outcome.equalsIgnoreCase(eob.getOutcome().toCode())),
          "All returned EOBs should have outcome " + scenarioName);
    } else {
      assertEquals(0, eobs.size(), scenarioName + " should have no hits");
    }
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByOutcomeCompleteOrPartial(SearchStyleEnum searchStyle) {
    var patientParam =
        new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
            .exactly()
            .identifier(BENE_ID_ALL_PARTS_WITH_XREF);

    var outcomeParam =
        new TokenClientParam(OUTCOME).exactly().codes(OUTCOME_COMPLETE, OUTCOME_PARTIAL);

    var eobBundle =
        searchBundle().where(patientParam).and(outcomeParam).usingStyle(searchStyle).execute();

    var eobs = getEobFromBundle(eobBundle);

    assertFalse(eobs.isEmpty(), "Should find EOBs for complete or partial outcomes");

    assertTrue(
        eobs.stream()
            .allMatch(
                eob -> {
                  if (eob.getOutcome() == null) {
                    return false;
                  }
                  var outcome = eob.getOutcome().toCode();
                  return OUTCOME_COMPLETE.equals(outcome) || OUTCOME_PARTIAL.equals(outcome);
                }),
        "All returned EOBs should have outcome complete or partial");
  }

  @Test
  void eobSearchNonLatestProfessionalIsNotReturned() {
    var claims =
        entityManager
            .createQuery(
                """
                SELECT c
                FROM ClaimProfessionalNch c
                JOIN FETCH c.beneficiary b
                JOIN FETCH c.claimItems cl
                WHERE c.claimUniqueId = :claimId
                """,
                ClaimProfessionalNch.class)
            .setParameter("claimId", Long.parseLong(CLAIM_ID_PROFESSIONAL_NON_LATEST))
            .getResultList();
    // Precondition - claim should be available in the db
    assertFalse(claims.isEmpty());
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier(CLAIM_ID_PROFESSIONAL_NON_LATEST))
            .execute();

    assertEquals(0, getEobFromBundle(eobBundle).size());
  }

  @Test
  void eobSearchNonLatestPartDIsReturned() {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier(CLAIM_ID_RX_NON_LATEST))
            .execute();

    assertEquals(1, getEobFromBundle(eobBundle).size());
  }
}
