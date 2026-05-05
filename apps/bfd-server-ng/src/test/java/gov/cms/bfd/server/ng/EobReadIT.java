package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.claim.model.ClaimProfessionalNch;
import gov.cms.bfd.server.ng.eob.EobResourceProvider;
import io.restassured.RestAssured;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EobReadIT extends IntegrationTestBase {
  @Autowired private EobResourceProvider eobResourceProvider;
  @Mock HttpServletRequest request;

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @Test
  void eobReadValidLong() {
    var eob = eobRead().withId(Long.parseLong(CLAIM_ID_ADJUDICATED)).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);
  }

  @Test
  void eobReadValidString() {
    var patient = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertFalse(patient.isEmpty());
    expectFhir().toMatchSnapshot(patient);
  }

  @Test
  void eobReadPhase1() {
    var eob = eobRead().withId(CLAIM_ID_PHASE_1).execute();
    assertFalse(eob.isEmpty());
    assertEquals("PARTIAL", eob.getOutcome().name());
    expectFhir().toMatchSnapshot(eob);
  }

  @Test
  void eobReadPhase2() {
    var eob = eobRead().withId(CLAIM_ID_PHASE_2).execute();
    assertFalse(eob.isEmpty());
    assertEquals("QUEUED", eob.getOutcome().name());
    expectFhir().toMatchSnapshot(eob);
  }

  @Test
  void eobReadIdNotFound() {
    var readWithId = eobRead().withId("999");
    assertThrows(ResourceNotFoundException.class, readWithId::execute);
  }

  @Test
  void eobReadInvalidIdBadRequest() {
    var readWithId = eobRead().withId("abc");
    assertThrows(InvalidRequestException.class, readWithId::execute);
  }

  @Test
  void eobReadProfessionalClaim() {
    var eob = eobRead().withId(Long.parseLong(CLAIM_ID_PROFESSIONAL)).execute();
    assertFalse(eob.isEmpty());

    assertTrue(
        eob.getMeta().getProfile().stream()
            .anyMatch(p -> p.getValue().contains("Professional-NonClinician")));

    var insurance =
        eob.getInsurance().stream()
            .filter(i -> i.hasCoverage() && i.getCoverage().hasDisplay())
            .findFirst();
    if (insurance.isPresent()) {
      var display = insurance.get().getCoverage().getDisplay();
      assertEquals("Part B", display);
    }
    expectFhir().toMatchSnapshot(eob);
  }

  @Test
  void eobReadProfessionalClaimBillingOrg() {
    var eob = eobRead().withId(Long.parseLong(CLAIM_ID_PROFESSIONAL_ORG)).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);
  }

  @Test
  void eobReadNonLatestPartDIsReturned() {
    var eob = eobRead().withId(Long.parseLong(CLAIM_ID_RX_NON_LATEST)).execute();
    assertFalse(eob.isEmpty());
  }

  @Test
  @Transactional
  void eobReadNonLatestProfessionalIsNotReturned() {
    TestTransaction.flagForCommit(); // disable rollback and performa commit at end of transaction

    // temporarily set latest claim to non-latest since purge has removed all non-latest claims
    var updateCount =
        entityManager
            .createQuery(
                """
      UPDATE ClaimProfessionalNch c SET
        c.latestClaimIndicator = :latestClaim
      WHERE c.claimUniqueId = :claimId
    """)
            .setParameter("latestClaim", false)
            .setParameter("claimId", CLAIM_ID_PROFESSIONAL_ORG)
            .executeUpdate();
    assertEquals(1, updateCount);

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
            .setParameter("claimId", Long.parseLong(CLAIM_ID_PROFESSIONAL_ORG))
            .getResultList();
    // Precondition - claim is normally not avaiable, but making available to verify server does not
    // return claim
    assertFalse(claims.isEmpty());

    TestTransaction
        .end(); // force a commit on Spring-managed test transaction, so API will observe db changes

    var eobRequest = eobRead().withId(Long.parseLong(CLAIM_ID_PROFESSIONAL_ORG));
    assertThrows(ResourceNotFoundException.class, eobRequest::execute);

    TestTransaction.start();
    TestTransaction.flagForCommit();

    // reset claim back to latest
    updateCount =
        entityManager
            .createQuery(
                """
      UPDATE ClaimProfessionalNch c SET
        c.latestClaimIndicator = :latestClaim
      WHERE c.claimUniqueId = :claimId
    """)
            .setParameter("latestClaim", true)
            .setParameter("claimId", CLAIM_ID_PROFESSIONAL_ORG)
            .executeUpdate();
    assertEquals(1, updateCount);

    TestTransaction.end();
  }

  @ParameterizedTest
  @EmptySource
  void eobReadNoIdBadRequest(String id) {
    // Using RestAssured here because HAPI FHIR doesn't let us send a request with a blank ID
    RestAssured.get(getServerUrl() + "/ExplanationOfBenefit" + id).then().statusCode(400);
  }
}
