package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

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

  protected long claimUniqueIdForIcd10Diagnosis = 4146709784142L; // icd-10
  protected long claimUniqueIdForIcd10Procedure = 6647624169509L; // icd10
  protected long claimUniqueIdForIcd9Diagnosis = 5312173004042L; // icd-9
  protected long claimUniqueIdForIcd9Procedure = 6103633914327L; // icd9
  protected long claimUniqueIdForHcpcs = 7095549187112L;
  protected long claimUniqueIdForDrg = 9644464937468L;
  protected long claimUniqueIdForDrgWithExpiredCode522 = 9688880648059L;
  protected long claimUniqueIdForDrgWithExpiredCode523 = 3159002171180L;
  protected long claimUniqueIdForCpt = 4722020775430L;

  protected long claimUniqueId = 566745788569L;
  protected long claimUniqueId2 = 3233800161009L;
  protected long claimWithHcpcsInIcd = 6288598524935L;
  protected long beneSk = 412457726;

  // Code: F10.10 System: icd-10-cm [clm_dgns_cd]
  private static final String ICD10_DIAGNOSIS = "F10.10";

  // Code: HZ2ZZZZ System: ICD10 [clm_prcdr_cd]
  private static final String ICD10_PROCEDURE = "HZ2ZZZZ";

  // Code: F10.10 System: icd-9-cm [clm_dgns_cd]
  private static final String ICD9_DIAGNOSIS = "291";

  // Code: HZ2ZZZZ System: ICD9 [clm_prcdr_cd]
  private static final String ICD9_PROCEDURE = "94.53";

  // Code: H0005 System: HCPCS [clm_line_hcpcs_cd]
  private static final String HCPCS = "H0005";

  // Code: 896 System: DRG [dgns_drg_cd]
  private static final String DRG = "896";

  // Code: 99408 System: CPT [clm_line_hcpcs_cd]
  private static final String CPT = "99408";

  protected final List<String> codeToCheck =
      List.of(DRG, HCPCS, CPT, ICD9_DIAGNOSIS, ICD9_PROCEDURE, ICD10_DIAGNOSIS, ICD10_PROCEDURE);

  private Bundle bundle;
  private List<ExplanationOfBenefit> eob;

  @BeforeAll
  void beforeAll() {
    bundle = bundleSearch(beneSk).execute();
    eob = bundleToEob(bundle);
  }

  @Test
  void eobWithIdShouldThrow() {
    // Enabling samhsa filtering should cause
    // v3/fhir/ExplanationOfBenefit/0000000001 calls to throw
    var exception =
        assertThrows(
            ResourceNotFoundException.class, () -> eobRead().withId(claimUniqueId2).execute());
    assertTrue(exception.getMessage().contains("HTTP 404 Not Found: HAPI-0971:"));
  }

  @Test
  void eobWithId2ShouldNotThrow() {
    // v3/fhir/ExplanationOfBenefit/0000000001
    var eoBenefits = eobRead().withId(claimUniqueId).execute();
    assertFalse(eoBenefits.isEmpty());
  }

  @Test
  void eobWithId2ShouldNotHaveSamhsaCode() {
    // v3/fhir/ExplanationOfBenefit/0000000001
    var eoBenefits = eobRead().withId(claimUniqueId).execute();
    assertFalse(
        codeToCheck.stream().anyMatch(e -> containsSamhsaCodeAnywhere(List.of(eoBenefits), e)));
  }

  @Test
  void claimWithWrongCode() {
    // Wrongly added HCPCS Samhsa code should not be removed
    // even it is a Samhsa code it is in wrong system
    // Samhsa code H0036 becomes H00.36 in ICD-10
    var fetched = eobRead().withId(claimWithHcpcsInIcd).execute();
    var diag = fetched.getDiagnosis().get(4).getDiagnosisCodeableConcept().getCoding().getFirst();

    assertEquals("H00.36", diag.getCode());
    assertEquals(SystemUrls.ICD_10_CM, diag.getSystem());
  }

  @Test
  void eobReadInvalidIdBadRequest() {
    assertThrows(InvalidRequestException.class, () -> eobRead().withId("abc").execute());
  }

  @Test
  void bundleCallResultShouldNotBeEmpty() {
    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(codeToCheck.stream().anyMatch(e -> containsSamhsaCodeAnywhere(eob, e)));
    assertFalse(bundle.isEmpty());
  }

  /** When SAMHSA filter is active, ICD-10 diagnosis claim is filtered and not returned (404). */
  @Test
  void eobCallResultShouldNotContainSamhsaICD10DiagnosisCode() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> eobRead().withId(claimUniqueIdForIcd10Diagnosis).execute());
  }

  /** When SAMHSA filter is active, ICD-9 diagnosis claim is filtered and not returned (404). */
  @Test
  void eobCallResultShouldNotContainSamhsaICD9DiagnosisCode() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> eobRead().withId(claimUniqueIdForIcd9Diagnosis).execute());
  }

  /**
   * When SAMHSA filter is active, ICD-10 procedure claim is returned but does not contain SAMHSA
   * code.
   */
  @Test
  void eobCallResultShouldNotContainSamhsaICD10ProcedureCode() {
    var eoBenefits = eobRead().withId(claimUniqueIdForIcd10Procedure).execute();
    expect.scenario("ICD10Procedure").serializer(FHIR_JSON).toMatchSnapshot(eoBenefits);
    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD10_PROCEDURE));
  }

  /**
   * When SAMHSA filter is active, ICD-9 procedure claim is returned but does not contain SAMHSA
   * code.
   */
  @Test
  void eobCallResultShouldNotContainSamhsaICD9ProcedureCode() {
    var eoBenefits = eobRead().withId(claimUniqueIdForIcd9Procedure).execute();
    expect.scenario("ICD9Procedure").serializer(FHIR_JSON).toMatchSnapshot(eoBenefits);
    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD9_PROCEDURE));
  }

  /** When SAMHSA filter is active, HCPCS claim is filtered and not returned (404). */
  @Test
  void eobCallResultShouldNotContainSamhsaHcpcsCode() {
    assertThrows(
        ResourceNotFoundException.class, () -> eobRead().withId(claimUniqueIdForHcpcs).execute());
  }

  /** When SAMHSA filter is active, CPT claim is filtered and not returned (404). */
  @Test
  void eobCallResultShouldNotContainSamhsaCptCode() {
    assertThrows(
        ResourceNotFoundException.class, () -> eobRead().withId(claimUniqueIdForCpt).execute());
  }

  /** When SAMHSA filter is active, DRG claim is filtered and not returned (404). */
  @Test
  void eobCallResultShouldNotContainSamhsaDrgCode() {
    var eoBenefits = eobRead().withId(claimUniqueIdForDrg).execute();
    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), DRG));
  }

  /** When SAMHSA filter is active, DRG claim is filtered and not returned (404). */
  @Test
  void eobCallResultShouldContainExpiredSamhsaDrgCode1() throws JsonProcessingException {
    var eoBenefits = eobRead().withId(claimUniqueIdForDrgWithExpiredCode523).execute();
    var json = context.newJsonParser().encodeResourceToString(eoBenefits);
    var root = objectMapper.readTree(json);
    assertEquals(
        true,
        root.findValues("supportingInfo")
            .getFirst()
            .findValues("code")
            .getLast()
            .findValues("code")
            .getFirst()
            .asText()
            .equals("523"));
  }

  @Test
  void eobCallResultShouldContainExpiredSamhsaDrgCode2() {
    var eoBenefits = eobRead().withId(claimUniqueIdForDrgWithExpiredCode522).execute();
    expect.scenario("drgExpiredCode").serializer(FHIR_JSON).toMatchSnapshot(eoBenefits);
    assertEquals(
        "522", eoBenefits.getSupportingInfo().get(14).getCode().getCodingFirstRep().getCode());
  }

  @Test
  void eobCallResultShouldNotContainSamhsaICD_x() {

    var eoBenefits = eobRead().withId(claimUniqueId).execute();

    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD10_DIAGNOSIS));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void eobCallResultShouldNotContainSamhsaICDx() {

    var eoBenefits = eobRead().withId(claimUniqueId).execute();

    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), ICD10_PROCEDURE));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void eobCallResultShouldNotContainSamhsaDrg() {

    var eoBenefits = eobRead().withId(claimUniqueId).execute();

    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), DRG));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void eobCallResultShouldNotContainSamhsaHcpcs() {

    var eoBenefits = eobRead().withId(claimUniqueId).execute();

    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), HCPCS));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void eobCallResultShouldNotContainSamhsaCpt() {

    var eoBenefits = eobRead().withId(claimUniqueId).execute();

    assertFalse(containsSamhsaCodeAnywhere(List.of(eoBenefits), CPT));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaICD_x() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsSamhsaCodeAnywhere(eob, ICD10_DIAGNOSIS));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaICDx() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsSamhsaCodeAnywhere(eob, ICD10_PROCEDURE));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaDrg() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsSamhsaCodeAnywhere(eob, DRG));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaHcpcs() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsSamhsaCodeAnywhere(eob, HCPCS));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaCpt() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsSamhsaCodeAnywhere(eob, CPT));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void beneSkIdNotFound() {
    var call = bundleSearch(0).execute();
    var eOfB = bundleToEob(call);

    assertThrows(NoSuchElementException.class, eOfB::getFirst);
  }

  /**
   * Verifies that the security_labels.yml file loads, serializes, and contains expected SAMHSA
   * codes. Ensures the file has the correct number of items and key codes are present.
   */
  @Test
  void securityLabelsYamlSerializationTest() {
    // Load and deserialize security_labels.yml using SecurityLabels utility
    var labelsMap = SecurityLabels.securityLabelsMap();
    var totalItems = labelsMap.values().stream().mapToInt(List::size).sum();
    // Serialize back to YAML
    var yaml = new org.yaml.snakeyaml.Yaml();
    var serialized = yaml.dump(labelsMap.values().stream().flatMap(List::stream).toList());

    // Check that serialization is not empty
    assertFalse(serialized.isEmpty(), "Serialized YAML should not be empty");

    // security_labels.yml has 680 active and 2 inactive items
    assertEquals(682, totalItems, "Expected 682 items, got: " + totalItems);

    // security_labels.yml should have the samhsa codes above.
    assertTrue(checkSamhsaCode(ICD10_PROCEDURE, labelsMap));
    assertTrue(checkSamhsaCode(ICD10_DIAGNOSIS, labelsMap));
    assertTrue(checkSamhsaCode(HCPCS, labelsMap));
    assertTrue(checkSamhsaCode(CPT, labelsMap));
  }

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  private IQuery<Bundle> bundleSearch(long id) {
    var url = getServerUrl() + "/ExplanationOfBenefit?patient=" + id;
    return getFhirClient().search().byUrl(url).returnBundle(Bundle.class);
  }

  private List<ExplanationOfBenefit> bundleToEob(Bundle bundle) {
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(ExplanationOfBenefit.class::isInstance)
        .map(ExplanationOfBenefit.class::cast)
        .toList();
  }

  private boolean containsSamhsaCodeAnywhere(List<ExplanationOfBenefit> fhirClaims, String code) {
    var targetNorm = normalize(code);
    return fhirClaims.stream().anyMatch(e -> eobJsonHasCode(e, targetNorm));
  }

  private final ObjectMapper objectMapper = new ObjectMapper();

  private boolean eobJsonHasCode(ExplanationOfBenefit eob, String targetNorm) {
    try {
      var json = context.newJsonParser().encodeResourceToString(eob);
      var root = objectMapper.readTree(json);
      // Find every JSON node with field name "code" and compare normalized values
      return root.findValues("code").stream()
          .map(JsonNode::asText)
          .map(this::normalize)
          .anyMatch(val -> val.equals(targetNorm));
    } catch (DataFormatException | JsonProcessingException ex) {
      throw new EobJsonScanException("Failed to parse EOB JSON", ex);
    }
  }

  private boolean checkSamhsaCode(
      String samhsaCode, Map<String, List<Map<String, Object>>> labelsMap) {
    return labelsMap.values().stream()
        .flatMap(List::stream)
        .anyMatch(entry -> samhsaCode.equals(entry.get("code") + ""));
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
