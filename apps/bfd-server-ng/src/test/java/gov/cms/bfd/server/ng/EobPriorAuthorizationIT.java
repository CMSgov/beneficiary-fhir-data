package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.cms.bfd.server.ng.claim.model.ClaimFinalAction;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import gov.cms.bfd.server.ng.testUtil.SamhsaCertType;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EobPriorAuthorizationIT extends IntegrationTestBase {

  private IQuery<Bundle> searchBundle(SamhsaCertType samhsaCertType) {
    final var fhirClient = getFhirClient();

    if (samhsaCertType != SamhsaCertType.NO_CERT) {
      final var headersInterceptor = new AdditionalRequestHeadersInterceptor();
      headersInterceptor.addHeaderValue("X-Amzn-Mtls-Clientcert", samhsaCertType.getCertValue());
      fhirClient.registerInterceptor(headersInterceptor);
    }

    return fhirClient.search().forResource(ExplanationOfBenefit.class).returnBundle(Bundle.class);
  }

  @Test
  void eobSearchPriorAuthorizationByPatient() {
    var bundle =
        searchBundle(SamhsaCertType.SAMHSA_NOT_ALLOWED_CERT)
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_WITH_PRIOR_AUTH))
            .execute();

    var eobs = getEobFromBundle(bundle);
    assertFalse(eobs.isEmpty());
    var priorAuths = getPriorAuths(bundle);
    assertFalse(priorAuths.isEmpty(), "Expected a Prior Authorization EOB");

    expectFhir().toMatchSnapshot(bundle);

    var eob = priorAuths.getFirst();

    assertEquals(ExplanationOfBenefit.RemittanceOutcome.PARTIAL, eob.getOutcome());
    assertFalse(eob.getPreAuthRefPeriod().isEmpty());
    assertEquals(DateUtil.toDate(LocalDate.of(2026, 1, 26)), eob.getCreated());
    assertFalse(eob.getInsurance().isEmpty());
    assertNotNull(eob.getPatient());
    assertNotNull(eob.getMeta());

    validateIdentifiers(eob);
    validateFacilityOrganization(eob);
    validateInsurer(eob);
    validateCareTeam(eob);
    validateItems(eob);
    validateExtensions(eob);
  }

  private void validateIdentifiers(ExplanationOfBenefit eob) {
    var identifiers = eob.getIdentifier();

    var hasUtn =
        identifiers.stream()
            .anyMatch(id -> SystemUrls.BLUE_BUTTON_UNIQUE_TRACKING_NUMBER.equals(id.getSystem()));
    assertTrue(hasUtn);

    var hasIcn =
        identifiers.stream()
            .anyMatch(
                id -> SystemUrls.BLUE_BUTTON_INTERNAL_CONTROL_NUMBER_OR_DCN.equals(id.getSystem()));
    assertNotNull(eob.getType());
    var isPartB =
        eob.getType().getCoding().stream().anyMatch(coding -> "B".equals(coding.getCode()));
    if (isPartB) {
      assertTrue(hasIcn);
    } else {
      assertFalse(hasIcn);
    }
  }

  private void validateFacilityOrganization(ExplanationOfBenefit eob) {
    assertTrue(eob.getProvider().getReference().startsWith("#"));

    var organization =
        eob.getContained().stream()
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .filter(o -> !o.getId().equals("insurer-org"))
            .findFirst();

    assertTrue(organization.isPresent());
    assertFalse(organization.get().getIdentifier().isEmpty());
    assertNotNull(organization.get().getName());
  }

  private void validateInsurer(ExplanationOfBenefit eob) {
    assertEquals("#insurer-org", eob.getInsurer().getReference());

    var insurer =
        eob.getContained().stream()
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .filter(o -> "insurer-org".equals(o.getId()))
            .findFirst();

    assertTrue(insurer.isPresent());
    assertEquals("Centers for Medicare and Medicaid Services", insurer.get().getName());
  }

  private void validateCareTeam(ExplanationOfBenefit eob) {
    assertFalse(eob.getCareTeam().isEmpty());

    eob.getCareTeam()
        .forEach(
            ct -> {
              assertTrue(ct.hasRole());
              assertTrue(ct.hasProvider());
            });
  }

  private void validateItems(ExplanationOfBenefit eob) {
    assertFalse(eob.getItem().isEmpty());

    for (var item : eob.getItem()) {
      assertTrue(item.hasProductOrService());
      assertFalse(item.getModifier().isEmpty());
      assertFalse(item.getExtension().isEmpty());
    }
  }

  private void validateExtensions(ExplanationOfBenefit eob) {
    var extensionsAlwaysPopulated =
        Set.of(
            SystemUrls.EXT_PA_DECISION_URL,
            SystemUrls.EXT_PA_DT_ADDED_URL,
            SystemUrls.EXT_PA_REQ_SUB_DT_URL,
            SystemUrls.EXT_PA_REQ_REC_DT_URL,
            SystemUrls.EXT_PA_SERVICE_CNTS_URL,
            SystemUrls.EXT_PA_MR_COUNT_INDICATOR_URL,
            SystemUrls.EXT_SVC_RENDER_ST_URL);

    for (var item : eob.getItem()) {
      var urls = item.getExtension().stream().map(Extension::getUrl).collect(Collectors.toSet());
      assertTrue(urls.containsAll(extensionsAlwaysPopulated));
    }
  }

  static Stream<Arguments> provideSourceParameterScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            style ->
                Stream.of(
                    Arguments.of(
                        "WithSource_CWF",
                        List.of(List.of(MetaSourceSk.CWF.getDisplay())),
                        2,
                        true,
                        style),
                    Arguments.of(
                        "WithCombinedSourceOr_CWF_NCH",
                        List.of(
                            List.of(MetaSourceSk.CWF.getDisplay(), MetaSourceSk.NCH.getDisplay())),
                        3,
                        true,
                        style),
                    Arguments.of(
                        "WithCombinedSourceAnd_CWF_NCH",
                        List.of(
                            List.of(MetaSourceSk.CWF.getDisplay()),
                            List.of(MetaSourceSk.NCH.getDisplay())),
                        0,
                        false,
                        style),
                    Arguments.of(
                        "WithSource_NCH",
                        List.of(List.of(MetaSourceSk.NCH.getDisplay())),
                        1,
                        false,
                        style)));
  }

  @ParameterizedTest
  @MethodSource("provideSourceParameterScenarios")
  void eobSearchBySources(
      String scenarioName,
      List<List<String>> sourceScenarios,
      int expectedCount,
      boolean expectsPriorAuth,
      SearchStyleEnum searchStyle) {
    var query =
        searchBundle(SamhsaCertType.SAMHSA_ALLOWED_CERT)
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_WITH_PRIOR_AUTH));

    for (List<String> sources : sourceScenarios) {
      if (sources.size() == 1) {
        query =
            query.and(
                new TokenClientParam(Constants.PARAM_SOURCE).exactly().code(sources.getFirst()));
      } else {
        query = query.and(new TokenClientParam(Constants.PARAM_SOURCE).exactly().codes(sources));
      }
    }

    var bundle = query.usingStyle(searchStyle).execute();
    expectFhir().scenario(searchStyle.name() + "_" + scenarioName).toMatchSnapshot(bundle);

    assertEquals(
        expectedCount,
        bundle.getEntry().size(),
        "Should find " + expectedCount + " EOBs for scenario " + scenarioName);

    var hasPriorAuth = !getPriorAuths(bundle).isEmpty();
    if (expectedCount > 0) {
      assertEquals(expectsPriorAuth, hasPriorAuth);
    }
  }

  static Stream<Arguments> provideTagScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            style ->
                Stream.of(
                    Arguments.of(
                        "WithTag_SharedSystem",
                        List.of(List.of(tag(SystemUrls.BLUE_BUTTON_SYSTEM_TYPE, "SharedSystem"))),
                        true,
                        style),
                    Arguments.of(
                        "WithTag_DDPS",
                        List.of(List.of(tag(SystemUrls.BLUE_BUTTON_SYSTEM_TYPE, "DDPS"))),
                        false,
                        style),
                    Arguments.of(
                        "WithTagFinalActionAndNch",
                        List.of(
                            List.of(systemType(MetaSourceSk.NCH)),
                            List.of(finalAction(ClaimFinalAction.YES))),
                        false,
                        style),
                    Arguments.of(
                        "WithTagFinalActionAndSharedSystem",
                        List.of(
                            List.of(systemType(MetaSourceSk.FISS)),
                            List.of(finalAction(ClaimFinalAction.YES))),
                        true,
                        style),
                    Arguments.of(
                        "WithIncompatibleTags",
                        List.of(
                            List.of(systemType(MetaSourceSk.FISS)),
                            List.of(systemType(MetaSourceSk.NCH))),
                        false,
                        style),
                    Arguments.of(
                        "WithCombinedTagOr",
                        List.of(
                            List.of(
                                systemType(MetaSourceSk.NCH), finalAction(ClaimFinalAction.YES))),
                        true,
                        style),
                    Arguments.of(
                        "WithCombinedTagOrDDPSNCH",
                        List.of(
                            List.of(systemType(MetaSourceSk.NCH), systemType(MetaSourceSk.DDPS))),
                        false,
                        style),
                    Arguments.of(
                        "WithSystemTag_FinalAction",
                        List.of(
                            List.of(
                                tag(SystemUrls.BLUE_BUTTON_FINAL_ACTION_STATUS, "FinalAction"))),
                        true,
                        style)));
  }

  @ParameterizedTest
  @MethodSource("provideTagScenarios")
  void shouldQueryPriorAuthForTagScenarios(
      String scenarioName,
      List<List<Coding>> tagScenarios,
      boolean expectedPriorAuth,
      SearchStyleEnum searchStyle) {
    var query =
        searchBundle(SamhsaCertType.SAMHSA_ALLOWED_CERT)
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_WITH_PRIOR_AUTH));

    for (List<Coding> tags : tagScenarios) {
      if (tags.size() == 1) {
        query =
            query.and(
                new TokenClientParam(Constants.PARAM_TAG)
                    .exactly()
                    .systemAndCode(tags.getFirst().getSystem(), tags.getFirst().getCode()));
      } else {
        query =
            query.and(
                new TokenClientParam(Constants.PARAM_TAG)
                    .exactly()
                    .codings(tags.toArray(new Coding[0])));
      }
    }

    var bundle = query.usingStyle(searchStyle).execute();
    expectFhir().scenario(searchStyle.name() + "_" + scenarioName).toMatchSnapshot(bundle);

    var hasPriorAuth = !getPriorAuths(bundle).isEmpty();
    assertEquals(expectedPriorAuth, hasPriorAuth);
  }

  private List<ExplanationOfBenefit> getPriorAuths(Bundle bundle) {
    return getEobFromBundle(bundle).stream()
        .filter(eob -> eob.hasUse() && eob.getUse() == ExplanationOfBenefit.Use.PREAUTHORIZATION)
        .toList();
  }
}
