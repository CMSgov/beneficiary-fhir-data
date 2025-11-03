package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import gov.cms.bfd.server.ng.eob.EobResourceProvider;
import gov.cms.bfd.server.ng.testUtil.ThreadSafeAppender;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EobPharmacyIT extends IntegrationTestBase {

  @Autowired private EobResourceProvider eobResourceProvider;

  @Mock HttpServletRequest request;

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @Test
  void eobReadPharmacyQueryCount() {
    var events = ThreadSafeAppender.startRecord();
    eobResourceProvider.find(new IdType(CLAIM_ID_RX), request);
    assertEquals(3, queryCount(events));
  }

  @Test
  void eobReadPharmacy() {
    var eob = eobRead().withId(CLAIM_ID_RX).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);

    var careTeam = eob.getCareTeam();
    assertFalse(careTeam.isEmpty());
    for (ExplanationOfBenefit.CareTeamComponent careTeamMember : careTeam) {
      var provider = careTeamMember.getProvider();
      assertFalse(provider.isEmpty());
      assertEquals("#careteam-prescriber-practitioner-1", provider.getReference());

      var role = careTeamMember.getRole();
      assertFalse(role.isEmpty());
      assertEquals("prescribing", role.getCoding().get(0).getCode());
      var hasPrescribingRole =
          role.getCoding().stream().filter(r -> r.getCode().equals("prescribing")).findFirst();
      assertTrue(hasPrescribingRole.isPresent());
    }

    var containedResources = eob.getContained();
    var hasPractitioner =
        containedResources.stream()
            .filter(r -> r.getResourceType().toString().equals("Practitioner"))
            .findFirst();
    assertTrue(hasPractitioner.isPresent());
    Practitioner practitioner = (Practitioner) hasPractitioner.get();
    var familyName =
        practitioner.getName().stream()
            .filter(p -> p.getFamily().equals("LAST NAME HERE"))
            .findFirst();
    assertTrue(familyName.isPresent());

    var partDInsurance =
        eob.getInsurance().stream()
            .filter(i -> i.getCoverage().getDisplay().equals("Part D"))
            .findFirst();
    assertTrue(partDInsurance.isPresent());
    var extensions = partDInsurance.get().getExtension();
    List<String> systems =
        List.of(
            SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_SUBMITTER_CONTRACT_NUMBER,
            SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_SUBMITTER_CONTRACT_PBP_NUMBER);
    var hasContractSystems =
        extensions.stream().allMatch(extension -> systems.contains(extension.getUrl()));
    assertTrue(hasContractSystems);
  }
}
