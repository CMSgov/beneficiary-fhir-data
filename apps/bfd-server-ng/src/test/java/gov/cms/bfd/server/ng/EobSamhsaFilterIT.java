package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.eob.EobHandler;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test that verifies SAMHSA-coded claims are filtered end-to-end.
 *
 * <p>This test loads the security labels YAML from the classpath, finds a matching claim in the
 * test database seeded by the integration test pipeline, and then asserts that requesting that
 * claim via the FHIR EOB endpoint returns no results (i.e. it was filtered).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EobSamhsaFilterIT extends IntegrationTestBase {

  private static final String BUNDLE_SEARCH_CALL = "bundleFromSearchCall";

  private final FhirContext context = FhirContext.forR4(); // Create a FhirContext for R4
  private static final String FHIR_JSON = "fhir+json";
  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS =
      SecurityLabel.securityLabelsMap();

  protected static long claimUniqueIdForIcd10Diagnosis = 4146709784142L; // icd-10
  protected static long claimUniqueIdForIcd10Procedure = 6647624169509L; // icd10
  protected static long claimUniqueIdForIcd9Diagnosis = 5312173004042L; // icd-9
  protected static long claimUniqueIdForIcd9Procedure = 6103633914327L; // icd9
  protected static long claimUniqueIdForHcpcs = 7095549187112L;
  protected static long claimUniqueIdForDrg = 9644464937468L;
  protected static long claimUniqueIdForDrgWithExpiredCode522 = 9688880648059L;
  protected static long claimUniqueIdForDrgWithExpiredCode523 = 3159002171180L;
  protected static long claimUniqueIdForCpt = 4722020775430L;

  protected static long claimUniqueIdWithNoSamhsa = 566745788569L;
  protected static long claimUniqueIdWithMultipleSamhsaCodes = 3233800161009L;
  protected static long claimWithHcpcsInIcd = 6288598524935L;
  protected static long beneSk = 412457726;
  protected static long beneSk2 = 794471559;
  protected static long beneSk3 = 455108118;
  protected static long beneSk4 = 167719446;
  protected static long beneSk5 = 27590072;
  protected static long beneSk6 = 186315498;

  // Code: F10.10 System: icd-10-cm [clm_dgns_cd]
  private static final String ICD10_DIAGNOSIS = "F10.10";

  // Code: HZ2ZZZZ System: ICD10 [clm_prcdr_cd]
  private static final String ICD10_PROCEDURE = "HZ2ZZZZ";

  // Code: F10.10 System: icd-9-cm [clm_dgns_cd]
  private static final String ICD9_DIAGNOSIS = "291";

  // Code: HZ2ZZZZ System: ICD9 [clm_prcdr_cd]
  private static final String ICD9_PROCEDURE = "94.45";

  // Code: H0005 System: HCPCS [clm_line_hcpcs_cd]
  private static final String HCPCS = "H0005";

  // Code: 896 System: DRG [dgns_drg_cd]
  private static final String DRG = "896";

  // Code: 99408 System: CPT [clm_line_hcpcs_cd]
  private static final String CPT = "99408";

  protected final List<String> codesToCheck =
      List.of(DRG, HCPCS, CPT, ICD9_DIAGNOSIS, ICD9_PROCEDURE, ICD10_DIAGNOSIS, ICD10_PROCEDURE);

  @Autowired private EobHandler eobHandler;

  private IQuery<Bundle> searchBundle(long patient) {
    return searchBundle(String.valueOf(patient));
  }

  private IQuery<Bundle> searchBundle(String patient) {
    return getFhirClient()
        .search()
        .forResource(ExplanationOfBenefit.class)
        .returnBundle(Bundle.class)
        .where(new TokenClientParam(ExplanationOfBenefit.SP_PATIENT).exactly().identifier(patient));
  }

  private List<ExplanationOfBenefit> getClaimsByBene(
      long beneSk, SamhsaFilterMode samhsaFilterMode) {
    var claims =
        eobHandler.searchByBene(
            beneSk,
            Optional.empty(),
            new DateTimeRange(),
            new DateTimeRange(),
            Optional.empty(),
            List.of(),
            samhsaFilterMode);
    return getEobFromBundle(claims);
  }

  private List<Long> getClaimIdsByBene(long beneSk, SamhsaFilterMode samhsaFilterMode) {
    return getClaimsByBene(beneSk, samhsaFilterMode).stream()
        .map(c -> Long.parseLong(c.getId()))
        .toList();
  }

  @Test
  void claimWithWrongCode() {
    // Wrongly added HCPCS Samhsa code should not be removed
    // even it is a Samhsa code it is in wrong system
    // Samhsa code H0034 becomes H00.34 in ICD-10
    var codeToCheck = "H00.34";
    assertTrue(isSensitiveCode(SystemUrls.CMS_HCPCS, normalize(codeToCheck)));
    var fetched = eobRead().withId(claimWithHcpcsInIcd).execute();
    var diag =
        fetched.getDiagnosis().stream()
            .flatMap(d -> d.getDiagnosisCodeableConcept().getCoding().stream())
            .filter(d -> d.getCode().equals(codeToCheck))
            .findFirst();
    assertTrue(diag.isPresent());
    assertEquals(codeToCheck, diag.get().getCode());
    assertEquals(SystemUrls.ICD_10_CM_DIAGNOSIS, diag.get().getSystem());
  }

  private static Stream<Arguments> shouldFilterSamhsa() {
    return Stream.of(
        Arguments.of(
            beneSk,
            List.of(claimUniqueIdWithMultipleSamhsaCodes),
            List.of(
                claimUniqueIdForDrgWithExpiredCode522,
                claimUniqueIdWithNoSamhsa,
                claimWithHcpcsInIcd)),
        Arguments.of(
            beneSk2, List.of(claimUniqueIdForHcpcs, claimUniqueIdForIcd10Diagnosis), List.of()),
        Arguments.of(beneSk3, List.of(claimUniqueIdForIcd10Procedure), List.of()),
        Arguments.of(beneSk4, List.of(claimUniqueIdForIcd9Diagnosis), List.of()),
        Arguments.of(
            beneSk5,
            List.of(claimUniqueIdForIcd9Procedure, claimUniqueIdForDrg, claimUniqueIdForCpt),
            List.of()),
        Arguments.of(beneSk6, List.of(), List.of(claimUniqueIdForDrgWithExpiredCode523)));
  }

  @MethodSource
  @ParameterizedTest
  void shouldFilterSamhsa(long beneSk, List<Long> samhsaClaimIds, List<Long> nonSamhsaClaimIds) {
    var bundle = searchBundle(beneSk).execute();

    // Bundle from endpoint should not contain the code
    assertFalse(
        codesToCheck.stream()
            .anyMatch(e -> containsSamhsaCodeAnywhere(getEobFromBundle(bundle), e)));

    // Ensure listed claims are valid
    var foundClaimIds = getClaimIdsByBene(beneSk, SamhsaFilterMode.INCLUDE);
    for (var claimId :
        Stream.concat(samhsaClaimIds.stream(), nonSamhsaClaimIds.stream()).toList()) {
      assertTrue(foundClaimIds.contains(claimId));
    }

    var foundClaims = getClaimsByBene(beneSk, SamhsaFilterMode.INCLUDE);
    var foundSamhsa =
        codesToCheck.stream().anyMatch(e -> containsSamhsaCodeAnywhere(foundClaims, e));
    assertEquals(!samhsaClaimIds.isEmpty(), foundSamhsa);

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

    // expect.serializer(FHIR_JSON).toMatchSnapshot(bundle);
  }

  private static Stream<Arguments> ensureDiagnosis() {
    return Stream.of(
        Arguments.of(
            claimUniqueIdForIcd10Diagnosis, ICD10_DIAGNOSIS, SystemUrls.ICD_10_CM_DIAGNOSIS),
        Arguments.of(claimUniqueIdForIcd9Diagnosis, ICD9_DIAGNOSIS, SystemUrls.ICD_9_CM_DIAGNOSIS));
  }

  @MethodSource
  @ParameterizedTest
  void ensureDiagnosis(long claimId, String code, String system) {
    var eob = eobHandler.find(claimId, SamhsaFilterMode.INCLUDE).get();
    var icd10Diagnosis =
        eob.getDiagnosis().stream()
            .flatMap(d -> d.getDiagnosisCodeableConcept().getCoding().stream())
            .filter(d -> d.getCode().equals(code) && d.getSystem().equals(system))
            .findFirst();
    assertTrue(icd10Diagnosis.isPresent());
  }

  //
  //  /** When SAMHSA filter is active, ICD-10 diagnosis claim is filtered and not returned (404).
  // */
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaICD10DiagnosisCode() {
  //    assertThrows(
  //        ResourceNotFoundException.class,
  //        () -> eobRead().withId(claimUniqueIdForIcd10Diagnosis).execute());
  //  }
  //
  //  /** When SAMHSA filter is active, ICD-9 diagnosis claim is filtered and not returned (404). */
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaICD9DiagnosisCode() {
  //    assertThrows(
  //        ResourceNotFoundException.class,
  //        () -> eobRead().withId(claimUniqueIdForIcd9Diagnosis).execute());
  //  }
  //
  //  /**
  //   * When SAMHSA filter is active, ICD-10 procedure claim is returned but does not contain
  // SAMHSA
  //   * code.
  //   */
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaICD10ProcedureCode() {
  //    var eoBenefits = eobRead().withId(claimUniqueIdForIcd10Procedure).execute();
  //    expect.scenario("ICD10Procedure").serializer(FHIR_JSON).toMatchSnapshot(eoBenefits);
  //    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD10_PROCEDURE));
  //  }
  //
  //  /**
  //   * When SAMHSA filter is active, ICD-9 procedure claim is returned but does not contain SAMHSA
  //   * code.
  //   */
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaICD9ProcedureCode() {
  //    var eoBenefits = eobRead().withId(claimUniqueIdForIcd9Procedure).execute();
  //    expect.scenario("ICD9Procedure").serializer(FHIR_JSON).toMatchSnapshot(eoBenefits);
  //    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD9_PROCEDURE));
  //  }
  //
  //  /** When SAMHSA filter is active, HCPCS claim is filtered and not returned (404). */
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaHcpcsCode() {
  //    assertThrows(
  //        ResourceNotFoundException.class, () ->
  // eobRead().withId(claimUniqueIdForHcpcs).execute());
  //  }
  //
  //  /** When SAMHSA filter is active, CPT claim is filtered and not returned (404). */
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaCptCode() {
  //    assertThrows(
  //        ResourceNotFoundException.class, () -> eobRead().withId(claimUniqueIdForCpt).execute());
  //  }
  //
  //  /** When SAMHSA filter is active, DRG claim is filtered and not returned (404). */
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaDrgCode() {
  //    var eoBenefits = eobRead().withId(claimUniqueIdForDrg);
  //    assertThrows(ResourceNotFoundException.class, eoBenefits::execute);
  //  }
  //
  //  /** When SAMHSA filter is active, DRG claim is filtered and not returned (404). */
  //  @Test
  //  void eobCallResultShouldContainExpiredSamhsaDrgCode1() throws JsonProcessingException {
  //    var eoBenefits = eobRead().withId(claimUniqueIdForDrgWithExpiredCode523).execute();
  //    var json = context.newJsonParser().encodeResourceToString(eoBenefits);
  //    var root = objectMapper.readTree(json);
  //    assertEquals(
  //        true,
  //        root.findValues("supportingInfo")
  //            .getFirst()
  //            .findValues("code")
  //            .getLast()
  //            .findValues("code")
  //            .getFirst()
  //            .asText()
  //            .equals("523"));
  //  }
  //
  //  @Test
  //  void eobCallResultShouldContainExpiredSamhsaDrgCode2() {
  //    var eoBenefits = eobRead().withId(claimUniqueIdForDrgWithExpiredCode522).execute();
  //    expect.scenario("drgExpiredCode").serializer(FHIR_JSON).toMatchSnapshot(eoBenefits);
  //    assertEquals(
  //        "522", eoBenefits.getSupportingInfo().get(14).getCode().getCodingFirstRep().getCode());
  //  }
  //
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaICD_x() {
  //
  //    var eoBenefits = eobRead().withId(claimUniqueIdWithNoSamhsa).execute();
  //
  //    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD10_DIAGNOSIS));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaICDx() {
  //
  //    var eoBenefits = eobRead().withId(claimUniqueIdWithNoSamhsa).execute();
  //
  //    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD10_PROCEDURE));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaDrg() {
  //
  //    var eoBenefits = eobRead().withId(claimUniqueIdWithNoSamhsa).execute();
  //
  //    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), DRG));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaHcpcs() {
  //
  //    var eoBenefits = eobRead().withId(claimUniqueIdWithNoSamhsa).execute();
  //
  //    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), HCPCS));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void eobCallResultShouldNotContainSamhsaCpt() {
  //
  //    var eoBenefits = eobRead().withId(claimUniqueIdWithNoSamhsa).execute();
  //
  //    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), CPT));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void bundleCallResultShouldNotContainSamhsaICD_x() {
  //
  //    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);
  //
  //    assertFalse(containsSamhsaCodeAnywhere(eob, ICD10_DIAGNOSIS));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void bundleCallResultShouldNotContainSamhsaICDx() {
  //
  //    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);
  //
  //    assertFalse(containsSamhsaCodeAnywhere(eob, ICD10_PROCEDURE));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void bundleCallResultShouldNotContainSamhsaDrg() {
  //
  //    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);
  //
  //    assertFalse(containsSamhsaCodeAnywhere(eob, DRG));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void bundleCallResultShouldNotContainSamhsaHcpcs() {
  //
  //    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);
  //
  //    assertFalse(containsSamhsaCodeAnywhere(eob, HCPCS));
  //    assertFalse(bundle.isEmpty());
  //  }
  //
  //  @Test
  //  void bundleCallResultShouldNotContainSamhsaCpt() {
  //
  //    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);
  //
  //    assertFalse(containsSamhsaCodeAnywhere(eob, CPT));
  //    assertFalse(bundle.isEmpty());
  //  }

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

    // security_labels.yml should have the samhsa codes above.
    assertTrue(checkSamhsaCode(SecurityLabel.normalize(ICD10_PROCEDURE), SECURITY_LABELS));
    assertTrue(checkSamhsaCode(SecurityLabel.normalize(ICD10_DIAGNOSIS), SECURITY_LABELS));
    assertTrue(checkSamhsaCode(SecurityLabel.normalize(HCPCS), SECURITY_LABELS));
    assertTrue(checkSamhsaCode(SecurityLabel.normalize(CPT), SECURITY_LABELS));
  }

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  private boolean isSensitiveCode(String system, String code) {
    return SECURITY_LABELS.get(system).stream().anyMatch(s -> s.getCode().equals(code));
  }

  private boolean containsSamhsaCodeAnywhere(List<ExplanationOfBenefit> fhirClaims, String code) {
    var targetNorm = normalize(code);
    return fhirClaims.stream().anyMatch(e -> eobJsonHasCode(e, targetNorm));
  }

  private boolean eobJsonHasCode(ExplanationOfBenefit eob, String targetNorm) {
    // This can produce false positives, but it will be safer to set up the test data to avoid this
    // than to try to limit the fields we check against
    var json = normalize(context.newJsonParser().encodeResourceToString(eob));
    return json.contains(":" + targetNorm) || json.contains(String.format(":\"%s\"", targetNorm));
  }

  private boolean checkSamhsaCode(String samhsaCode, Map<String, List<SecurityLabel>> labelsMap) {
    return labelsMap.values().stream()
        .flatMap(List::stream)
        .anyMatch(entry -> samhsaCode.equals(entry.getCode()));
  }

  private static class EobJsonScanException extends RuntimeException {
    EobJsonScanException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  private String normalize(String val) {
    return val.trim().replace(".", "").toLowerCase();
  }
}
