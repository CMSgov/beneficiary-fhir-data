package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.ng.eob.EobHandler;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration test to verify SAMHSA claim filtering logging is working correctly. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EobSamhsaFilterLoggingIT extends IntegrationTestBase {

  private static final long CLAIM_ID_WITH_SAMHSA_DIAGNOSIS = 4146709784142L;
  private static final long CLAIM_ID_WITH_SAMHSA_PROCEDURE = 6647624169509L;
  private static final long CLAIM_ID_WITH_SAMHSA_HCPCS = 7095549187112L;
  private static final long CLAIM_ID_WITH_SAMHSA_DRG = 9644464937468L;
  private static final long CLAIM_ID_WITH_NO_SAMHSA = 566745788569L;

  @Autowired private EobHandler eobHandler;

  /** Test that filtering a claim with SAMHSA diagnosis code logs appropriately. */
  @Test
  void testSamhsaDiagnosisCodeLogging() {
    var resultExclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_DIAGNOSIS,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.EXCLUDE);

    assertTrue(
        resultExclude.getEntry().isEmpty(), "Claim with SAMHSA diagnosis code should be filtered");

    var resultInclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_DIAGNOSIS,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.INCLUDE);

    assertFalse(
        resultInclude.getEntry().isEmpty(), "Claim should be present when SAMHSA is INCLUDED");
  }

  /** Test that filtering a claim with SAMHSA procedure code logs appropriately. */
  @Test
  void testSamhsaProcedureCodeLogging() {
    var resultExclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_PROCEDURE,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.EXCLUDE);

    assertTrue(
        resultExclude.getEntry().isEmpty(), "Claim with SAMHSA procedure code should be filtered");

    var resultInclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_PROCEDURE,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.INCLUDE);

    assertFalse(
        resultInclude.getEntry().isEmpty(), "Claim should be present when SAMHSA is INCLUDED");
  }

  /** Test that filtering a claim with SAMHSA HCPCS code logs appropriately. */
  @Test
  void testSamhsaHcpcsCodeLogging() {
    var resultExclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_HCPCS,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.EXCLUDE);

    assertTrue(
        resultExclude.getEntry().isEmpty(), "Claim with SAMHSA HCPCS code should be filtered");

    var resultInclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_HCPCS,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.INCLUDE);

    assertFalse(
        resultInclude.getEntry().isEmpty(), "Claim should be present when SAMHSA is INCLUDED");
  }

  /** Test that filtering a claim with SAMHSA DRG code logs appropriately. */
  @Test
  void testSamhsaDrgCodeLogging() {
    var resultExclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_DRG,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.EXCLUDE);

    assertTrue(resultExclude.getEntry().isEmpty(), "Claim with SAMHSA DRG code should be filtered");

    var resultInclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_SAMHSA_DRG,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.INCLUDE);

    assertFalse(
        resultInclude.getEntry().isEmpty(), "Claim should be present when SAMHSA is INCLUDED");
  }

  /** Test that non-SAMHSA claims are not filtered and no logging occurs for filtering. */
  @Test
  void testNonSamhsaClaimNotFiltered() {
    var resultExclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_NO_SAMHSA,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.EXCLUDE);

    assertFalse(
        resultExclude.getEntry().isEmpty(),
        "Non-SAMHSA claim should not be filtered even with EXCLUDE mode");

    var resultInclude =
        eobHandler.searchById(
            CLAIM_ID_WITH_NO_SAMHSA,
            new DateTimeRange(),
            new DateTimeRange(),
            SamhsaFilterMode.INCLUDE);

    assertFalse(resultInclude.getEntry().isEmpty(), "Non-SAMHSA claim should be present");
  }
}
