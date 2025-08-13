package gov.cms.bfd.server.ng.eob;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimProcedure;
import gov.cms.bfd.sharedutils.SecurityLabels;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Unit tests for EobHandler#getFhirEobs to check SAMHSA filtering behavior. */
public class EobHandlerGetFhirEobsTest {
  private static Map<String, List<Map<String, Object>>> dictionary;
  private static Set<String> systemKeys;

  @BeforeAll
  public static void beforeAll() {
    dictionary = SecurityLabels.securityLabelsDict();
    systemKeys = List.of(dictionary.keySet()).getFirst();
  }

  @Test
  public void getFhirEobs_filtersOutMatchingSamhsaCodes() {

    var dict = SecurityLabels.securityLabelsDict();
    var firstKey = dict.keySet().stream().findFirst().orElseThrow();
    var firstEntry = dict.get(firstKey).get(0);
    var rawCode = firstEntry.get("code").toString();
    var dictCode = rawCode == null ? "" : rawCode.trim().replace(".", "").toLowerCase();

    // Create a ClaimProcedure that will match the dictionary entry when normalized
    var procMatching = new ClaimProcedure();
    procMatching.setDiagnosisCode(Optional.of(dictCode));

    // Create a ClaimProcedure that will not match
    var procNonMatching = new ClaimProcedure();
    procNonMatching.setDiagnosisCode(Optional.of("not-a-match"));

    // Create two lightweight Claim instances overriding toFhir so we don't need
    // full DB-backed
    // state
    Claim matchingClaim =
        new Claim() {
          @Override
          public ExplanationOfBenefit toFhir() {
            var eob = new ExplanationOfBenefit();
            eob.setId("matching");
            return eob;
          }

          @Override
          public Set<ClaimProcedure> getClaimProcedures() {
            return Set.of(procMatching);
          }
        };

    Claim nonMatchingClaim =
        new Claim() {
          @Override
          public ExplanationOfBenefit toFhir() {
            var eob = new ExplanationOfBenefit();
            eob.setId("nonmatching");
            return eob;
          }

          @Override
          public Set<ClaimProcedure> getClaimProcedures() {
            return Set.of(procNonMatching);
          }
        };

    // Minimal ClaimRepository stub used only for the last-updated supplier
    var repo =
        new ClaimRepository(null) {
          @Override
          public ZonedDateTime claimLastUpdated() {
            return DateUtil.MIN_DATETIME;
          }
        };

    var handler = new EobHandler(null, repo);

    // Act
    Bundle bundle = handler.getFhirEobs(List.of(matchingClaim, nonMatchingClaim), true);

    // Only the non-matching claim should remain
    // (the one that did not have samhsa information)
    assertFalse(bundle.getEntry().isEmpty());
    assertEquals(1, bundle.getEntry().size());
    var resource = bundle.getEntry().get(0).getResource();
    assertTrue(resource instanceof ExplanationOfBenefit);
    assertEquals("nonmatching", ((ExplanationOfBenefit) resource).getId());
  }

  @Test
  public void getFhirEobs_returnsEmptyBundleWhenAllFiltered() {
    var dict = SecurityLabels.securityLabelsDict();
    var firstKey = dict.keySet().stream().findFirst().orElseThrow();
    var firstEntry = dict.get(firstKey).get(0);
    var rawCode = firstEntry.get("code").toString();
    var dictCode = rawCode == null ? "" : rawCode.trim().replace(".", "").toLowerCase();

    var procMatching = new ClaimProcedure();
    procMatching.setDiagnosisCode(Optional.of(dictCode));

    Claim matchingClaim =
        new Claim() {
          @Override
          public ExplanationOfBenefit toFhir() {
            var eob = new ExplanationOfBenefit();
            eob.setId("matching");
            return eob;
          }

          @Override
          public Set<ClaimProcedure> getClaimProcedures() {
            return Set.of(procMatching);
          }
        };

    var repo =
        new ClaimRepository(null) {
          @Override
          public ZonedDateTime claimLastUpdated() {
            return DateUtil.MIN_DATETIME;
          }
        };

    var handler = new EobHandler(null, repo);

    Bundle bundle = handler.getFhirEobs(List.of(matchingClaim), true);

    // When all EOBs are filtered, method returns an empty collection bundle with
    // type set as in code
    assertTrue(bundle.getEntry().isEmpty());
    // The implementation sets the bundle type and then clears the type element;
    // ensure entries empty
    assertEquals(0, bundle.getEntry().size());
  }

  @Test
  public void testSystemShouldContain() {
    Set<String> keys = new HashSet<>();
    keys.addAll(
        List.of(
            "http://www.ama-assn.org/go/cpt",
            "http://hl7.org/fhir/sid/icd-9-cm",
            "http://hl7.org/fhir/sid/icd-10-cm",
            "http://www.cms.gov/Medicare/Coding/ICD9",
            "http://www.cms.gov/Medicare/Coding/ICD10",
            "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets",
            "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software"));
    assertTrue(systemKeys.containsAll(keys));
  }

  @Test
  public void testYmlFileNotFound() {
    URL resourceUrl =
        Thread.currentThread().getContextClassLoader().getResource("non_exist_file_labels.yml");
    assertNull(resourceUrl);
  }

  @Test
  public void testSecurityLabelsDict_ymlFileFound() {
    URL resourceUrl =
        Thread.currentThread().getContextClassLoader().getResource("security_labels.yml");
    assertNotNull(resourceUrl);
  }
}
