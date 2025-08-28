package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
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
  IGenericClient client = context.newRestfulGenericClient(getServerUrl());

  protected long claimUniqueId = 566745788569L;
  protected long beneSk = 412457726;

  // Code: F10.10 System: icd-10-cm [clm_dgns_cd]
  private static final String ICD_X = "F10.10";

  // Code: H0005 System: HCPCS [clm_line_hcpcs_cd]
  private static final String HCPCS = "H0005";

  // Code: 99408 System: CPT [clm_line_hcpcs_cd]
  private static final String CPT = "99408";

  // Code: HZ2ZZZZ System: ICD10 [clm_prcdr_cd]
  private static final String ICDX = "HZ2ZZZZ";

  // Code: 522 System: DRG [dgns_drg_cd]
  private static final int DRG = 522;

  protected final List<String> codeToCheck = List.of(DRG + "", HCPCS, ICD_X, ICDX);

  private Bundle bundle;
  private List<ExplanationOfBenefit> eob;

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  private IQuery<Bundle> bundleRead(long id) {
    var url = getServerUrl() + "/ExplanationOfBenefit?patient=" + id;
    return getFhirClient().search().byUrl(url).returnBundle(Bundle.class);
  }

  @BeforeAll
  void beforeAll() {
    // v3/fhir/ExplanationOfBenefit?patient=00000001
    bundle = bundleRead(beneSk).execute();
    eob = bundleToEob(bundle);
  }

  @Test
  void eobWithIdShouldThrow() {
    // Enabling samhsa filtering should cause
    // v3/fhir/ExplanationOfBenefit/566745788569 calls to throw
    assertThrows(ResourceNotFoundException.class, () -> eobRead().withId(claimUniqueId).execute());
  }

  @Test
  void eobReadIdNotFound() {
    assertThrows(ResourceNotFoundException.class, () -> eobRead().withId("999").execute());
  }

  @Test
  void eobReadInvalidIdBadRequest() {
    assertThrows(InvalidRequestException.class, () -> eobRead().withId("abc").execute());
  }

  @Test
  void bundleCallResultShouldNotBeEmpty() {
    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(codeToCheck.stream().anyMatch(e -> containsDiagnosisCode(eob, e)));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaICD_x() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsDiagnosisCode(eob, ICD_X));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaDrg() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsDiagnosisCode(eob, DRG + ""));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaHcpcs() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsDiagnosisCode(eob, HCPCS));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaCpt() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsDiagnosisCode(eob, CPT));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void bundleCallResultShouldNotContainSamhsaICDx() {

    expect.scenario(BUNDLE_SEARCH_CALL).serializer(FHIR_JSON).toMatchSnapshot(bundle);

    assertFalse(containsDiagnosisCode(eob, ICDX));
    assertFalse(bundle.isEmpty());
  }

  @Test
  void beneSkIdNotFound() {
    var call = bundleRead(00000).execute();
    var eOfB = bundleToEob(call);

    assertThrows(NoSuchElementException.class, eOfB::getFirst);
  }

  private List<ExplanationOfBenefit> bundleToEob(Bundle bundle) {
    List<ExplanationOfBenefit> fhirClaims =
        bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof ExplanationOfBenefit)
            .map(resource -> (ExplanationOfBenefit) resource)
            .collect(Collectors.toList());
    return fhirClaims;
  }

  private boolean containsDiagnosisCode(List<ExplanationOfBenefit> fhirClaims, String code) {
    return fhirClaims.stream()
        .anyMatch(
            d ->
                d.getDiagnosis().stream()
                    .anyMatch(
                        e ->
                            e.getDiagnosis().getNamedProperty("coding").getValues().stream()
                                    .anyMatch(
                                        t ->
                                            t.getNamedProperty("code").getValues().stream()
                                                .anyMatch(c -> c.toString().equals(code)))
                                || e.getDiagnosisCodeableConcept().getCoding().stream()
                                    .anyMatch(f -> code.equals(f.getCode()))));
  }
}
