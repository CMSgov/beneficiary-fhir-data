package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.eob.EobHandler;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test that verifies SAMHSA-coded claims are filtered end-to-end.
 *
 * <p>This test loads the security labels YAML from the classpath, finds a matching claim in the
 * test database seeded by the integration test pipeline, and then asserts that requesting that
 * claim via the FHIR EOB endpoint returns no results (i.e. it was filtered).
 */
class EobSamhsaFilterIT extends IntegrationTestBase {
  private final FhirContext context = FhirContext.forR4();

  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS =
      SecurityLabel.getSecurityLabels();

  private static final long CLAIM_UNIQUE_ID_FOR_ICD_10_DIAGNOSIS = 4146709784142L;
  private static final long CLAIM_UNIQUE_ID_FOR_ICD_10_PROCEDURE = 6647624169509L;
  private static final long CLAIM_UNIQUE_ID_FOR_ICD_9_DIAGNOSIS = 5312173004042L;
  private static final long CLAIM_UNIQUE_ID_FOR_ICD_9_PROCEDURE = 6103633914327L;
  private static final long CLAIM_UNIQUE_ID_FOR_HCPCS = 7095549187112L;
  private static final long CLAIM_UNIQUE_ID_FOR_DRG = 9644464937468L;
  private static final long CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522 = 9688880648059L;
  private static final long CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_523 = 3159002171180L;
  private static final long CLAIM_UNIQUE_ID_FOR_CPT = 4722020775430L;
  private static final long CLAIM_UNIQUE_ID_FOR_FUTURE_HCPCS = 6871761612138L;
  private static final long CLAIM_UNIQUE_ID_FOR_FUTURE_HCPCS_AFTER_CODE_START = 6871761612139L;

  private static final long CLAIM_UNIQUE_ID_WITH_NO_SAMHSA = 566745788569L;
  private static final long CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES = 3233800161009L;
  private static final long CLAIM_WITH_HCPCS_IN_ICD = 6288598524935L;
  private static final long BENE_SK = 412457726;
  private static final long BENE_SK2 = 794471559;
  private static final long BENE_SK3 = 455108118;
  private static final long BENE_SK4 = 167719446;
  private static final long BENE_SK5 = 27590072;
  private static final long BENE_SK6 = 186315498;

  // System: icd-10-cm [clm_dgns_cd]
  private static final String ICD10_DIAGNOSIS = "F10.10";
  private static final String ICD10_DIAGNOSIS2 = "F55.2";

  // System: ICD10 [clm_prcdr_cd]
  private static final String ICD10_PROCEDURE = "HZ2ZZZZ";
  private static final String ICD10_PROCEDURE2 = "HZ57ZZZ";

  // System: icd-9-cm [clm_dgns_cd]
  private static final String ICD9_DIAGNOSIS = "291";
  private static final String ICD9_DIAGNOSIS2 = "V65.42";

  // System: ICD9 [clm_prcdr_cd]
  private static final String ICD9_PROCEDURE = "94.45";
  private static final String ICD9_PROCEDURE2 = "94.66";

  // System: HCPCS [clm_line_hcpcs_cd]
  private static final String HCPCS = "H0005";
  private static final String HCPCS2 = "H0050";
  private static final String FUTURE_HCPCS = "G0534";

  // System: DRG [dgns_drg_cd]
  private static final String DRG = "896";
  private static final String DRG2 = "897";
  private static final String DRG_EXPIRED1 = "522";
  private static final String DRG_EXPIRED2 = "523";

  // System: CPT [clm_line_hcpcs_cd]
  private static final String CPT = "99408";
  private static final String CPT2 = "0078U";

  protected final List<List<String>> samhsaCodesToCheck =
      List.of(
          List.of(SystemUrls.CMS_MS_DRG, DRG),
          List.of(SystemUrls.CMS_MS_DRG, DRG2),
          List.of(SystemUrls.CMS_MS_DRG, DRG_EXPIRED1),
          List.of(SystemUrls.CMS_MS_DRG, DRG_EXPIRED2),
          List.of(SystemUrls.CMS_HCPCS, HCPCS),
          List.of(SystemUrls.CMS_HCPCS, HCPCS2),
          List.of(SystemUrls.CMS_HCPCS, FUTURE_HCPCS),
          List.of(SystemUrls.AMA_CPT, CPT),
          List.of(SystemUrls.AMA_CPT, CPT2),
          List.of(SystemUrls.ICD_9_CM_DIAGNOSIS, ICD9_DIAGNOSIS),
          List.of(SystemUrls.ICD_9_CM_DIAGNOSIS, ICD9_DIAGNOSIS2),
          List.of(SystemUrls.ICD_10_CM_DIAGNOSIS, ICD10_DIAGNOSIS),
          List.of(SystemUrls.ICD_10_CM_DIAGNOSIS, ICD10_DIAGNOSIS2),
          List.of(SystemUrls.CMS_ICD_9_PROCEDURE, ICD9_PROCEDURE),
          List.of(SystemUrls.CMS_ICD_9_PROCEDURE, ICD9_PROCEDURE2),
          List.of(SystemUrls.CMS_ICD_10_PROCEDURE, ICD10_PROCEDURE),
          List.of(SystemUrls.CMS_ICD_10_PROCEDURE, ICD10_PROCEDURE2));

  @Autowired private EobHandler eobHandler;

  private IQuery<Bundle> searchBundle(long patient, SamhsaCertType samhsaCertType) {
    return searchBundle(String.valueOf(patient), samhsaCertType);
  }

  private IQuery<Bundle> searchBundle(String patient, SamhsaCertType samhsaCertType) {
    final var fhirClient = getFhirClient();

    if (samhsaCertType != SamhsaCertType.NO_CERT) {
      final var headersInterceptor = new AdditionalRequestHeadersInterceptor();
      headersInterceptor.addHeaderValue("X-Amzn-Mtls-Clientcert", samhsaCertType.certValue);

      fhirClient.registerInterceptor(headersInterceptor);
    }

    return fhirClient
        .search()
        .forResource(ExplanationOfBenefit.class)
        .returnBundle(Bundle.class)
        .where(new TokenClientParam(ExplanationOfBenefit.SP_PATIENT).exactly().identifier(patient));
  }

  private List<ExplanationOfBenefit> getClaimsByBene(
      long beneSk, SamhsaFilterMode samhsaFilterMode) {
    var criteria =
        new ClaimSearchCriteria(
            beneSk,
            new DateTimeRange(),
            new DateTimeRange(),
            Optional.empty(),
            Optional.empty(),
            Collections.emptyList(),
            List.of(),
            Collections.emptyList());
    var claims = eobHandler.searchByBene(criteria, samhsaFilterMode);
    return getEobFromBundle(claims);
  }

  private List<Long> getClaimIdsByBene(long beneSk, SamhsaFilterMode samhsaFilterMode) {
    return getClaimsByBene(beneSk, samhsaFilterMode).stream()
        .map(c -> Long.parseLong(c.getId()))
        .toList();
  }

  @Test
  void claimWithWrongCode() {
    // SAMHSA code with a mismatched system should not be removed
    var codeToCheck = "H00.34";
    assertTrue(isSensitiveCode(SystemUrls.CMS_HCPCS, normalize(codeToCheck)));
    var fetched = eobRead().withId(CLAIM_WITH_HCPCS_IN_ICD).execute();
    var diagnosis =
        fetched.getDiagnosis().stream()
            .flatMap(d -> d.getDiagnosisCodeableConcept().getCoding().stream())
            .filter(d -> d.getCode().equals(codeToCheck))
            .findFirst();
    assertTrue(diagnosis.isPresent());
    assertEquals(codeToCheck, diagnosis.get().getCode());
    assertEquals(SystemUrls.ICD_10_CM_DIAGNOSIS, diagnosis.get().getSystem());
  }

  private static Stream<Arguments> shouldFilterSamhsa() {
    return Stream.of(
        Arguments.of(
            BENE_SK,
            List.of(CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES),
            List.of(
                CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522,
                CLAIM_UNIQUE_ID_WITH_NO_SAMHSA,
                CLAIM_WITH_HCPCS_IN_ICD),
            List.of(CLAIM_WITH_HCPCS_IN_ICD)),
        Arguments.of(
            BENE_SK2,
            List.of(CLAIM_UNIQUE_ID_FOR_HCPCS, CLAIM_UNIQUE_ID_FOR_ICD_10_DIAGNOSIS),
            List.of(),
            List.of()),
        Arguments.of(BENE_SK3, List.of(CLAIM_UNIQUE_ID_FOR_ICD_10_PROCEDURE), List.of(), List.of()),
        Arguments.of(BENE_SK4, List.of(CLAIM_UNIQUE_ID_FOR_ICD_9_DIAGNOSIS), List.of(), List.of()),
        Arguments.of(
            BENE_SK5,
            List.of(
                CLAIM_UNIQUE_ID_FOR_ICD_9_PROCEDURE,
                CLAIM_UNIQUE_ID_FOR_DRG,
                CLAIM_UNIQUE_ID_FOR_CPT),
            List.of(),
            List.of()),
        Arguments.of(
            BENE_SK6,
            List.of(CLAIM_UNIQUE_ID_FOR_FUTURE_HCPCS_AFTER_CODE_START),
            List.of(
                CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_523, CLAIM_UNIQUE_ID_FOR_FUTURE_HCPCS),
            List.of()));
  }

  private void shouldFilterSamhsa(
      SamhsaCertType samhsaCertType,
      long beneSk,
      List<Long> samhsaClaimIds,
      List<Long> nonSamhsaClaimIds,
      List<Long> skipBundleVerification) {
    var bundle = searchBundle(beneSk, samhsaCertType).execute();
    // Before checking for SAMHSA codes, filter any cases that won't pass
    // verification due to
    // system/code mismatches.
    var bundleClaimsToCheck =
        getEobFromBundle(bundle).stream()
            .filter(c -> !skipBundleVerification.contains(Long.parseLong(c.getIdPart())))
            .toList();
    // Bundle from endpoint should not contain any sensitive codes
    assertFalse(anyClaimsContainSamhsaCode(bundleClaimsToCheck, true));

    // Ensure listed claims are valid
    var foundClaimIds = getClaimIdsByBene(beneSk, SamhsaFilterMode.INCLUDE);
    for (var claimId :
        Stream.concat(samhsaClaimIds.stream(), nonSamhsaClaimIds.stream()).toList()) {
      assertTrue(foundClaimIds.contains(claimId));
    }

    var foundSamhsaClaims =
        getClaimsByBene(beneSk, SamhsaFilterMode.INCLUDE).stream()
            .filter(c -> samhsaClaimIds.contains(Long.parseLong(c.getIdPart())))
            .toList();
    assertFalse(foundSamhsaClaims.isEmpty());
    // All claims marked as SAMHSA should contain at least one valid code.
    assertTrue(allClaimsContainSamhsaCode(foundSamhsaClaims, false));

    var foundClaimIdsNoSamhsa = getClaimIdsByBene(beneSk, SamhsaFilterMode.EXCLUDE);
    // Ensure non-SAMHSA claims don't contain SAMHSA claim IDs
    for (var claimId : samhsaClaimIds) {
      assertFalse(foundClaimIdsNoSamhsa.contains(claimId));
      var read = eobRead().withId(claimId);
      assertThrows(ResourceNotFoundException.class, read::execute);
    }

    // Ensure non-SAMHSA claims contain all valid non-SAMHSA claim IDs.
    for (var claimId : nonSamhsaClaimIds) {
      assertTrue(foundClaimIdsNoSamhsa.contains(claimId));
      var read = eobRead().withId(claimId).execute();
      assertEquals(claimId.toString(), read.getIdPart());
    }

    expectFhir().scenario(String.valueOf(beneSk)).toMatchSnapshot(bundle);
  }

  @MethodSource({"shouldFilterSamhsa"})
  @ParameterizedTest
  void shouldFilterSamhsaNoCert(
      long beneSk,
      List<Long> samhsaClaimIds,
      List<Long> nonSamhsaClaimIds,
      List<Long> skipBundleVerification) {
    shouldFilterSamhsa(
        SamhsaCertType.NO_CERT, beneSk, samhsaClaimIds, nonSamhsaClaimIds, skipBundleVerification);
  }

  @MethodSource({"shouldFilterSamhsa"})
  @ParameterizedTest
  void shouldFilterSamhsaSamhsaNotAllowedCert(
      long beneSk,
      List<Long> samhsaClaimIds,
      List<Long> nonSamhsaClaimIds,
      List<Long> skipBundleVerification) {
    shouldFilterSamhsa(
        SamhsaCertType.SAMHSA_NOT_ALLOWED_CERT,
        beneSk,
        samhsaClaimIds,
        nonSamhsaClaimIds,
        skipBundleVerification);
  }

  @ValueSource(longs = {BENE_SK, BENE_SK2, BENE_SK3, BENE_SK4, BENE_SK5, BENE_SK6})
  @ParameterizedTest
  void shouldNotFilterSamhsaIfAllowedCert(long beneSk) {
    var bundle = searchBundle(beneSk, SamhsaCertType.SAMHSA_ALLOWED_CERT).execute();
    // Bundle from endpoint _should_ contain sensitive codes because the "cert" provided is allowed
    // to get SAMHSA data
    assertTrue(anyClaimsContainSamhsaCode(getEobFromBundle(bundle), true));
  }

  @Test
  void samhsaClaimsIncludeSecurityTagsWhenAllowed() {
    var beneSk = BENE_SK;
    var bundle = searchBundle(beneSk, SamhsaCertType.SAMHSA_ALLOWED_CERT).execute();

    var samhsaEob =
        getEobFromBundle(bundle).stream()
            .filter(
                eob ->
                    eob.getIdPart()
                        .equals(String.valueOf(CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES)))
            .findFirst();

    assertTrue(samhsaEob.isPresent(), "Expected SAMHSA EOB found in bundle.");

    Predicate<Coding> isSamhsaSecurityTag =
        tag ->
            SystemUrls.SAMHSA_ACT_CODE_SYSTEM_URL.equals(tag.getSystem())
                && IdrConstants.SAMHSA_SECURITY_CODE.equals(tag.getCode());

    samhsaEob.get().getMeta().getSecurity().stream()
        .filter(isSamhsaSecurityTag)
        .findFirst()
        .ifPresentOrElse(
            tag -> assertEquals(IdrConstants.SAMHSA_SECURITY_DISPLAY, tag.getDisplay()),
            () ->
                Assertions.fail(
                    "Expected SAMHSA security tag not found in EOB meta or had incorrect code/system."));

    // Snapshot only the SAMHSA EOB to avoid storing irrelevant objects.
    var snapshotBundle = new Bundle();
    snapshotBundle.setType(bundle.getType());
    snapshotBundle.addEntry().setResource(samhsaEob.get());
    expectFhir().scenario(String.valueOf(beneSk)).toMatchSnapshot(snapshotBundle);
  }

  // The following group of tests is used to ensure the validity of the test data.
  // Since the tests above are largely checking for the absence of some codes,
  // the only way to ensure the data is set up correctly is to make sure the relevant codes
  // do appear in the responses when filtering is not enabled.
  private static Stream<Arguments> ensureDiagnosis() {
    return Stream.of(
        Arguments.of(
            CLAIM_UNIQUE_ID_FOR_ICD_10_DIAGNOSIS, ICD10_DIAGNOSIS, SystemUrls.ICD_10_CM_DIAGNOSIS),
        Arguments.of(
            CLAIM_UNIQUE_ID_FOR_ICD_9_DIAGNOSIS, ICD9_DIAGNOSIS, SystemUrls.ICD_9_CM_DIAGNOSIS),
        Arguments.of(
            CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES,
            ICD10_DIAGNOSIS2,
            SystemUrls.ICD_10_CM_DIAGNOSIS),
        Arguments.of(
            CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES,
            ICD9_DIAGNOSIS2,
            SystemUrls.ICD_9_CM_DIAGNOSIS));
  }

  @MethodSource
  @ParameterizedTest
  void ensureDiagnosis(long claimId, String code, String system) {
    var eob = eobHandler.find(claimId, SamhsaFilterMode.INCLUDE).get();
    var icdDiagnosis =
        eob.getDiagnosis().stream()
            .flatMap(d -> d.getDiagnosisCodeableConcept().getCoding().stream())
            .filter(
                d -> normalize(d.getCode()).equals(normalize(code)) && d.getSystem().equals(system))
            .findFirst();
    assertTrue(icdDiagnosis.isPresent());
  }

  private static Stream<Arguments> ensureProcedure() {
    return Stream.of(
        Arguments.of(
            CLAIM_UNIQUE_ID_FOR_ICD_10_PROCEDURE, ICD10_PROCEDURE, SystemUrls.CMS_ICD_10_PROCEDURE),
        Arguments.of(
            CLAIM_UNIQUE_ID_FOR_ICD_9_PROCEDURE, ICD9_PROCEDURE, SystemUrls.CMS_ICD_9_PROCEDURE),
        Arguments.of(
            CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES,
            ICD10_PROCEDURE2,
            SystemUrls.CMS_ICD_10_PROCEDURE),
        Arguments.of(
            CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES,
            ICD9_PROCEDURE2,
            SystemUrls.CMS_ICD_9_PROCEDURE));
  }

  @MethodSource
  @ParameterizedTest
  void ensureProcedure(long claimId, String code, String system) {
    var eob = eobHandler.find(claimId, SamhsaFilterMode.INCLUDE).get();
    var icdProcedure =
        eob.getProcedure().stream()
            .flatMap(p -> p.getProcedureCodeableConcept().getCoding().stream())
            .filter(
                p -> normalize(p.getCode()).equals(normalize(code)) && p.getSystem().equals(system))
            .findFirst();
    assertTrue(icdProcedure.isPresent());
  }

  private static Stream<Arguments> ensureHcpcs() {
    return Stream.of(
        Arguments.of(CLAIM_UNIQUE_ID_FOR_HCPCS, HCPCS, SystemUrls.CMS_HCPCS),
        Arguments.of(CLAIM_UNIQUE_ID_FOR_CPT, CPT, SystemUrls.AMA_CPT),
        Arguments.of(CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES, HCPCS2, SystemUrls.CMS_HCPCS),
        Arguments.of(CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES, CPT2, SystemUrls.AMA_CPT),
        Arguments.of(CLAIM_UNIQUE_ID_FOR_FUTURE_HCPCS, FUTURE_HCPCS, SystemUrls.CMS_HCPCS),
        Arguments.of(
            CLAIM_UNIQUE_ID_FOR_FUTURE_HCPCS_AFTER_CODE_START, FUTURE_HCPCS, SystemUrls.CMS_HCPCS));
  }

  @MethodSource
  @ParameterizedTest
  void ensureHcpcs(long claimId, String code, String system) {
    var eob = eobHandler.find(claimId, SamhsaFilterMode.INCLUDE).get();
    var hcpcs =
        eob.getItem().stream()
            .flatMap(i -> i.getProductOrService().getCoding().stream())
            .filter(c -> c.getCode().equals(code) && c.getSystem().equals(system))
            .findFirst();
    assertTrue(hcpcs.isPresent());
  }

  private static Stream<Arguments> ensureDrg() {
    return Stream.of(
        Arguments.of(CLAIM_UNIQUE_ID_FOR_DRG, DRG, SystemUrls.CMS_MS_DRG),
        Arguments.of(CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES, DRG2, SystemUrls.CMS_MS_DRG),
        Arguments.of(
            CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522, DRG_EXPIRED1, SystemUrls.CMS_MS_DRG),
        Arguments.of(
            CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_523, DRG_EXPIRED2, SystemUrls.CMS_MS_DRG));
  }

  @MethodSource
  @ParameterizedTest
  void ensureDrg(long claimId, String code, String system) {
    var eob = eobHandler.find(claimId, SamhsaFilterMode.INCLUDE).get();
    var drg =
        eob.getSupportingInfo().stream()
            .flatMap(i -> i.getCode().getCoding().stream())
            .filter(c -> c.getCode().equals(code) && c.getSystem().equals(system))
            .findFirst();
    assertTrue(drg.isPresent());
  }

  /**
   * Verifies that the security_labels.yml file loads, serializes, and contains expected SAMHSA
   * codes. Ensures the file has the correct number of items and key codes are present.
   */
  @Test
  void securityLabelsYamlSerializationTest() {
    var totalItems = SECURITY_LABELS.values().stream().mapToInt(List::size).sum();
    // Serialize back to YAML
    var yaml = new org.yaml.snakeyaml.Yaml();
    var serialized = yaml.dump(SECURITY_LABELS.values().stream().flatMap(List::stream).toList());

    // Check that serialization is not empty
    assertFalse(serialized.isEmpty(), "Serialized YAML should not be empty");

    // security_labels.yml has 680 active and 2 inactive items
    assertEquals(682, totalItems, "Expected 682 items, got: " + totalItems);

    for (var code : samhsaCodesToCheck) {
      assertTrue(checkSamhsaCode(SecurityLabel.normalize(code.get(1))));
    }

    // Ensure the security labels file doesn't contain any unexpected systems
    var allowedSystems =
        Set.of(
            SystemUrls.ICD_9_CM_DIAGNOSIS,
            SystemUrls.CMS_ICD_9_PROCEDURE,
            SystemUrls.ICD_10_CM_DIAGNOSIS,
            SystemUrls.CMS_ICD_10_PROCEDURE,
            SystemUrls.AMA_CPT,
            SystemUrls.CMS_HCPCS,
            SystemUrls.CMS_MS_DRG);
    var samhsaSystems = SECURITY_LABELS.keySet();

    assertEquals(allowedSystems, samhsaSystems);
  }

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  private boolean isSensitiveCode(String system, String code) {
    return SECURITY_LABELS.get(system).stream().anyMatch(s -> s.getCode().equals(code));
  }

  private boolean allClaimsContainSamhsaCode(
      List<ExplanationOfBenefit> fhirClaims, boolean validateDates) {
    return fhirClaims.stream().allMatch(e -> containsAnySamhsaCode(e, validateDates));
  }

  private boolean anyClaimsContainSamhsaCode(
      List<ExplanationOfBenefit> fhirClaims, boolean validateDates) {
    return fhirClaims.stream().anyMatch(e -> containsAnySamhsaCode(e, validateDates));
  }

  private boolean containsAnySamhsaCode(ExplanationOfBenefit eob, boolean validateDates) {
    var eobEnd = eob.getBillablePeriod().getEnd();
    var json = normalize(context.newJsonParser().encodeResourceToString(eob));
    for (var entry : SECURITY_LABELS.values().stream().flatMap(Collection::stream).toList()) {
      var eobAfter = eobEnd.after(DateUtil.toDate(entry.getEndDateAsDate()));
      var eobBefore = eobEnd.before(DateUtil.toDate(entry.getStartDateAsDate()));
      if (validateDates && (eobAfter || eobBefore)) {
        continue;
      }

      var target = normalize(entry.getCode());
      // This can produce false positives, but it will be safer to set up the test data to avoid
      // this than to try to limit the fields we check against
      if (json.contains("\"code\":" + target)
          || json.contains(String.format("\"code\":\"%s\"", target))) {
        return true;
      }
    }
    return false;
  }

  private boolean checkSamhsaCode(String samhsaCode) {
    return SECURITY_LABELS.values().stream()
        .flatMap(List::stream)
        .anyMatch(entry -> samhsaCode.equals(entry.getCode()));
  }

  private String normalize(String val) {
    return val.trim().replace(".", "").toLowerCase();
  }

  @AllArgsConstructor
  private enum SamhsaCertType {
    NO_CERT(null),
    SAMHSA_ALLOWED_CERT("samhsa_allowed"),
    SAMHSA_NOT_ALLOWED_CERT("samhsa_not_allowed");

    private final String certValue;
  }

  static Stream<Arguments> provideSecurityScenarios() {
    return Stream.of(SearchStyleEnum.values())
        .flatMap(
            style ->
                Stream.of(
                    Arguments.of(
                        "NoCert_SamhsaRequested_NoSamhsa",
                        List.of(List.of(securityClause(SecurityModifier.NONE, samhsaCodeOnly()))),
                        SamhsaCertType.NO_CERT,
                        List.of(
                            CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522,
                            CLAIM_WITH_HCPCS_IN_ICD,
                            CLAIM_UNIQUE_ID_WITH_NO_SAMHSA),
                        false,
                        style),
                    Arguments.of(
                        "NotAllowedCert_SamhsaRequested_NoSamhsa",
                        List.of(
                            List.of(securityClause(SecurityModifier.NONE, samhsaCodeAndSystem()))),
                        SamhsaCertType.SAMHSA_NOT_ALLOWED_CERT,
                        List.of(
                            CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522,
                            CLAIM_WITH_HCPCS_IN_ICD,
                            CLAIM_UNIQUE_ID_WITH_NO_SAMHSA),
                        false,
                        style),
                    Arguments.of(
                        "AllowedCert_SamhsaCodeRequested_OnlySamhsa",
                        List.of(List.of(securityClause(SecurityModifier.NONE, samhsaCodeOnly()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES),
                        false,
                        style),
                    Arguments.of(
                        "AllowedCert_SamhsaCodeAndSystemRequested_OnlySamhsa",
                        List.of(
                            List.of(securityClause(SecurityModifier.NONE, samhsaCodeAndSystem()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(CLAIM_UNIQUE_ID_WITH_MULTIPLE_SAMHSA_CODES),
                        false,
                        style),
                    Arguments.of(
                        "AllowedCert_InvalidCodeRequest_NoClaims",
                        List.of(
                            List.of(securityClause(SecurityModifier.NONE, invalidSamhsaCode()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(),
                        true,
                        style),
                    Arguments.of(
                        "AllowedCert_SamhsaAndNonSamhsa_NoClaims",
                        List.of(
                            List.of(securityClause(SecurityModifier.NONE, samhsaCodeOnly())),
                            List.of(securityClause(SecurityModifier.NOT, samhsaCodeAndSystem()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(),
                        true,
                        style),
                    Arguments.of(
                        "AllowedCert_SamhsaOrNonSamhsa_AllClaims",
                        List.of(
                            List.of(
                                securityClause(SecurityModifier.NOT, samhsaCodeAndSystem()),
                                securityClause(SecurityModifier.NONE, samhsaCodeAndSystem()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(
                            CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522,
                            CLAIM_WITH_HCPCS_IN_ICD,
                            CLAIM_UNIQUE_ID_WITH_NO_SAMHSA),
                        false,
                        style),
                    Arguments.of(
                        "AllowedCert_ExcludedSamhsaCodeRequested_NoSamhsa",
                        List.of(List.of(securityClause(SecurityModifier.NOT, samhsaCodeOnly()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(
                            CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522,
                            CLAIM_WITH_HCPCS_IN_ICD,
                            CLAIM_UNIQUE_ID_WITH_NO_SAMHSA),
                        false,
                        style),
                    Arguments.of(
                        "AllowedCert_ExcludedSamhsaCodeAndSystemRequested_NoSamhsa",
                        List.of(
                            List.of(securityClause(SecurityModifier.NOT, samhsaCodeAndSystem()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(
                            CLAIM_UNIQUE_ID_FOR_DRG_WITH_EXPIRED_CODE_522,
                            CLAIM_WITH_HCPCS_IN_ICD,
                            CLAIM_UNIQUE_ID_WITH_NO_SAMHSA),
                        false,
                        style),
                    Arguments.of(
                        "AllowedCert_ExcludedSamhsaCodeRequested_InvalidCode_NoClaims",
                        List.of(List.of(securityClause(SecurityModifier.NOT, invalidSamhsaCode()))),
                        SamhsaCertType.SAMHSA_ALLOWED_CERT,
                        List.of(),
                        true,
                        style)));
  }

  @MethodSource({"provideSecurityScenarios"})
  @ParameterizedTest
  void eobSearchBySecurity(
      String scenarioName,
      List<List<SecurityClause>> securityScenarios,
      SamhsaCertType certType,
      List<Long> expectedClaimIds,
      boolean expectException,
      SearchStyleEnum searchStyle) {
    var query = searchBundle(BENE_SK, certType);
    var paramName = new StringBuilder(Constants.PARAM_SECURITY);

    for (var securityScenario : securityScenarios) {
      if (securityScenario.getFirst().modifier() == SecurityModifier.NOT) {
        paramName.append(":not");
      }
      if (securityScenario.size() == 1) {
        var clause = securityScenario.getFirst();
        var coding = clause.coding();

        query.and(
            new TokenClientParam(paramName.toString())
                .exactly()
                .systemAndCode(coding.getSystem(), coding.getCode()));
      } else {
        var joinedOrValues = securityScenario.stream().map(SecurityClause::coding).toList();
        query.and(
            new TokenClientParam(paramName.toString())
                .exactly()
                .codings(joinedOrValues.toArray(new Coding[0])));
      }
    }

    if (expectException) {
      assertThrows(
          InvalidRequestException.class,
          query.usingStyle(searchStyle)::execute,
          "Should throw InvalidRequestException: " + scenarioName);
    } else {
      var eobBundle = query.usingStyle(searchStyle).execute();
      var actualClaimsIds =
          getEobFromBundle(eobBundle).stream()
              .map(ExplanationOfBenefit::getIdPart)
              .map(Long::parseLong)
              .toList();

      assertTrue(
          expectedClaimIds.containsAll(actualClaimsIds)
              && actualClaimsIds.containsAll(expectedClaimIds),
          "Returned claim IDs did not match expected IDs for scenario " + scenarioName);
    }
  }

  private static Coding security(String system, String code) {
    return new Coding(system, code, null);
  }

  private static Coding samhsaCodeOnly() {
    return security(null, IdrConstants.SAMHSA_SECURITY_CODE);
  }

  private static Coding samhsaCodeAndSystem() {
    return security(SystemUrls.SAMHSA_ACT_CODE_SYSTEM_URL, IdrConstants.SAMHSA_SECURITY_CODE);
  }

  private static Coding invalidSamhsaCode() {
    return security(SystemUrls.SAMHSA_ACT_CODE_SYSTEM_URL, "42CFR");
  }

  private enum SecurityModifier {
    NONE,
    NOT
  }

  private record SecurityClause(SecurityModifier modifier, Coding coding) {}

  private static SecurityClause securityClause(SecurityModifier modifier, Coding coding) {
    return new SecurityClause(modifier, coding);
  }
}
