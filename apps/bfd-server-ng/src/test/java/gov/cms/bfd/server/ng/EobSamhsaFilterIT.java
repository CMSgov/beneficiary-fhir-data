package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.eob.EobHandler;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final FhirContext context = FhirContext.forR4();

  private static final Map<String, List<SecurityLabel>> SECURITY_LABELS =
      SecurityLabel.getSecurityLabels();

  protected static long claimUniqueIdForIcd10Diagnosis = 4146709784142L; // icd-10
  protected static long claimUniqueIdForIcd10Procedure = 6647624169509L; // icd10
  protected static long claimUniqueIdForIcd9Diagnosis = 5312173004042L; // icd-9
  protected static long claimUniqueIdForIcd9Procedure = 6103633914327L; // icd9
  protected static long claimUniqueIdForHcpcs = 7095549187112L;
  protected static long claimUniqueIdForDrg = 9644464937468L;
  protected static long claimUniqueIdForDrgWithExpiredCode522 = 9688880648059L;
  protected static long claimUniqueIdForDrgWithExpiredCode523 = 3159002171180L;
  protected static long claimUniqueIdForCpt = 4722020775430L;
  protected static long claimUniqueIdForFutureHcpcs = 6871761612138L;
  protected static long claimUniqueIdForFutureHcpcsAfterStart = 6871761612139L;

  protected static long claimUniqueIdWithNoSamhsa = 566745788569L;
  protected static long claimUniqueIdWithMultipleSamhsaCodes = 3233800161009L;
  protected static long claimWithHcpcsInIcd = 6288598524935L;
  protected static long beneSk = 412457726;
  protected static long beneSk2 = 794471559;
  protected static long beneSk3 = 455108118;
  protected static long beneSk4 = 167719446;
  protected static long beneSk5 = 27590072;
  protected static long beneSk6 = 186315498;

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
    // SAMHSA code with a mismatched system should not be removed
    var codeToCheck = "H00.34";
    assertTrue(isSensitiveCode(SystemUrls.CMS_HCPCS, normalize(codeToCheck)));
    var fetched = eobRead().withId(claimWithHcpcsInIcd).execute();
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
            beneSk,
            List.of(claimUniqueIdWithMultipleSamhsaCodes),
            List.of(
                claimUniqueIdForDrgWithExpiredCode522,
                claimUniqueIdWithNoSamhsa,
                claimWithHcpcsInIcd),
            List.of(claimWithHcpcsInIcd)),
        Arguments.of(
            beneSk2,
            List.of(claimUniqueIdForHcpcs, claimUniqueIdForIcd10Diagnosis),
            List.of(),
            List.of()),
        Arguments.of(beneSk3, List.of(claimUniqueIdForIcd10Procedure), List.of(), List.of()),
        Arguments.of(beneSk4, List.of(claimUniqueIdForIcd9Diagnosis), List.of(), List.of()),
        Arguments.of(
            beneSk5,
            List.of(claimUniqueIdForIcd9Procedure, claimUniqueIdForDrg, claimUniqueIdForCpt),
            List.of(),
            List.of()),
        Arguments.of(
            beneSk6,
            List.of(claimUniqueIdForFutureHcpcsAfterStart),
            List.of(claimUniqueIdForDrgWithExpiredCode523, claimUniqueIdForFutureHcpcs),
            List.of()));
  }

  @MethodSource
  @ParameterizedTest
  void shouldFilterSamhsa(
      long beneSk,
      List<Long> samhsaClaimIds,
      List<Long> nonSamhsaClaimIds,
      List<Long> skipBundleVerification) {
    var bundle = searchBundle(beneSk).execute();
    // Before checking for SAMHSA codes, filter any cases that won't pass verification due to
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

  // The following group of tests is used to ensure the validity of the test data.
  // Since the tests above are largely checking for the absence of some codes,
  // the only way to ensure the data is set up correctly is to make sure the relevant codes
  // do appear in the responses when filtering is not enabled.

  private static Stream<Arguments> ensureDiagnosis() {
    return Stream.of(
        Arguments.of(
            claimUniqueIdForIcd10Diagnosis, ICD10_DIAGNOSIS, SystemUrls.ICD_10_CM_DIAGNOSIS),
        Arguments.of(claimUniqueIdForIcd9Diagnosis, ICD9_DIAGNOSIS, SystemUrls.ICD_9_CM_DIAGNOSIS),
        Arguments.of(
            claimUniqueIdWithMultipleSamhsaCodes, ICD10_DIAGNOSIS2, SystemUrls.ICD_10_CM_DIAGNOSIS),
        Arguments.of(
            claimUniqueIdWithMultipleSamhsaCodes, ICD9_DIAGNOSIS2, SystemUrls.ICD_9_CM_DIAGNOSIS));
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
            claimUniqueIdForIcd10Procedure, ICD10_PROCEDURE, SystemUrls.CMS_ICD_10_PROCEDURE),
        Arguments.of(claimUniqueIdForIcd9Procedure, ICD9_PROCEDURE, SystemUrls.CMS_ICD_9_PROCEDURE),
        Arguments.of(
            claimUniqueIdWithMultipleSamhsaCodes,
            ICD10_PROCEDURE2,
            SystemUrls.CMS_ICD_10_PROCEDURE),
        Arguments.of(
            claimUniqueIdWithMultipleSamhsaCodes, ICD9_PROCEDURE2, SystemUrls.CMS_ICD_9_PROCEDURE));
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
        Arguments.of(claimUniqueIdForHcpcs, HCPCS, SystemUrls.CMS_HCPCS),
        Arguments.of(claimUniqueIdForCpt, CPT, SystemUrls.AMA_CPT),
        Arguments.of(claimUniqueIdWithMultipleSamhsaCodes, HCPCS2, SystemUrls.CMS_HCPCS),
        Arguments.of(claimUniqueIdWithMultipleSamhsaCodes, CPT2, SystemUrls.AMA_CPT),
        Arguments.of(claimUniqueIdForFutureHcpcs, FUTURE_HCPCS, SystemUrls.CMS_HCPCS),
        Arguments.of(claimUniqueIdForFutureHcpcsAfterStart, FUTURE_HCPCS, SystemUrls.CMS_HCPCS));
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
        Arguments.of(claimUniqueIdForDrg, DRG, SystemUrls.CMS_MS_DRG),
        Arguments.of(claimUniqueIdWithMultipleSamhsaCodes, DRG2, SystemUrls.CMS_MS_DRG),
        Arguments.of(claimUniqueIdForDrgWithExpiredCode522, DRG_EXPIRED1, SystemUrls.CMS_MS_DRG),
        Arguments.of(claimUniqueIdForDrgWithExpiredCode523, DRG_EXPIRED2, SystemUrls.CMS_MS_DRG));
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
      if (validateDates && eobEnd.after(DateUtil.toDate(entry.getEndDateAsDate()))) {
        continue;
      }
      if (validateDates && eobEnd.before(DateUtil.toDate(entry.getStartDateAsDate()))) {
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
}
