package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.eob.EobResourceProvider;
import gov.cms.bfd.server.ng.testUtil.ThreadSafeAppender;
import io.restassured.RestAssured;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EobReadIT extends IntegrationTestBase {
  @Autowired private EobResourceProvider eobResourceProvider;
  @Mock HttpServletRequest request;

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @Test
  void eobEnforceInvariants() {
    var claimIds =
        new String[] {
          CLAIM_ID_ADJUDICATED,
          CLAIM_ID_ADJUDICATED_ICD_9,
          CLAIM_ID_PHASE_1,
          CLAIM_ID_PHASE_2,
          CLAIM_ID_PROFESSIONAL,
          CLAIM_ID_RX,
          CLAIM_ID_RX_ORGANIZATION
        };
    // The following represent the minimum conditions for an EOB to be valid
    for (var claimId : claimIds) {
      var eob = eobRead().withId(claimId).execute();
      assertFalse(eob.isEmpty(), "EOB should not be empty.");
      assertTrue(eob.hasInsurance(), "All EOBs should have insurance");
      assertTrue(eob.hasMeta(), "EOB should have meta.");
      assertTrue(eob.hasOutcome(), "EOB should have outcome");
      assertTrue(eob.hasPatient(), "EOB should have patient");
      assertTrue(eob.hasType(), "EOB should have type");
      assertTrue(eob.hasStatus(), "EOB should have status");
      assertTrue(eob.hasCreated(), "EOB should have created");
      assertTrue(eob.hasUse(), "EOB should have use");
      var eobSupportingInfo =
          eob.getSupportingInfo().stream()
              .filter(s -> s.hasCode() && s.getCode().hasCoding())
              .toList();
      for (var supportingInfo : eobSupportingInfo) {
        for (var coding : supportingInfo.getCode().getCoding()) {
          assertTrue(
              coding.hasSystem(), "Coding should have system: " + claimId + coding.toString());
          assertTrue(coding.hasCode(), "Coding should have code: " + claimId);
        }
      }
    }
  }

  @Test
  void eobReadValidLong() {
    var eob = eobRead().withId(Long.parseLong(CLAIM_ID_ADJUDICATED)).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);
  }

  @Test
  void eobReadQueryCount() {
    var events = ThreadSafeAppender.startRecord();
    eobResourceProvider.find(new IdType(CLAIM_ID_ADJUDICATED_ICD_9), request);
    assertEquals(1, queryCount(events));
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
      assertTrue(display.equals("Part B"));
    }
    expectFhir().toMatchSnapshot(eob);
  }

  @Test
  void eobReadProfessionalClaimBillingOrg() {
    var eob = eobRead().withId(Long.parseLong(CLAIM_ID_PROFESSIONAL_ORG)).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);
  }

  @ParameterizedTest
  @EmptySource
  void eobReadNoIdBadRequest(String id) {
    // Using RestAssured here because HAPI FHIR doesn't let us send a request with a
    // blank ID
    RestAssured.get(getServerUrl() + "/ExplanationOfBenefit" + id).then().statusCode(400);
  }
}
