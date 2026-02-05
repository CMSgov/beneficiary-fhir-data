package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import gov.cms.bfd.server.ng.eob.EobResourceProvider;
import gov.cms.bfd.server.ng.testUtil.ThreadSafeAppender;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EobPharmacyIT extends IntegrationTestBase {

  @Autowired private EobResourceProvider eobResourceProvider;

  @Mock HttpServletRequest request;

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @Test
  void eobReadPharmacyQueryCount() {
    var events = ThreadSafeAppender.startRecord();
    eobResourceProvider.find(new IdType(CLAIM_ID_RX), request);
    assertEquals(1, queryCount(events));
  }

  @Test
  void eobReadPharmacy() {
    var eob = eobRead().withId(CLAIM_ID_RX).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);

    assertEquals("#1548226988", eob.getProvider().getReference());
    var containedResources = eob.getContained();
    var hasPractitioner =
        containedResources.stream().filter(r -> r.getId().equals("1548226988")).findFirst();
    assertTrue(hasPractitioner.isPresent());
    Practitioner practitioner = (Practitioner) hasPractitioner.get();
    var familyName =
        practitioner.getName().stream().filter(p -> p.getFamily().equals("Garcia")).findFirst();
    assertTrue(familyName.isPresent());

    var productOrService = eob.getItem().getFirst().getProductOrService();
    assertFalse(productOrService.isEmpty());
    assertEquals("compound", productOrService.getCoding().get(0).getCode());

    var itemQuantity = eob.getItem().getFirst().getQuantity();
    assertFalse(itemQuantity.isEmpty());
    // This is due to compound meds being weird + patterning the qualifier to go in
    // detail.
    assertNull(itemQuantity.getUnit());

    var itemDetail = eob.getItem().getFirst().getDetailFirstRep();
    assertFalse(itemDetail.isEmpty());
    assertEquals("00338004904", itemDetail.getProductOrService().getCoding().get(0).getCode());

    var supportingInfo = eob.getSupportingInfo();
    assertFalse(supportingInfo.isEmpty());
    assertTrue(
        supportingInfo.size()
            >= 4); // C4BB profile requires at least 4 supporting info, good litmus test

    var hasBadDateTime =
        supportingInfo.stream()
            .anyMatch(
                s ->
                    s.hasTiming()
                        && s.getTiming() instanceof DateTimeType dt
                        && dt.getValue().toString().startsWith("9999-12-31"));
    assertFalse(hasBadDateTime);

    var careTeam = eob.getCareTeam();
    assertFalse(careTeam.isEmpty());
    for (ExplanationOfBenefit.CareTeamComponent careTeamMember : careTeam) {
      var provider = careTeamMember.getProvider();
      assertFalse(provider.isEmpty());
      assertEquals("#1730548868", provider.getReference());

      var role = careTeamMember.getRole();
      assertFalse(role.isEmpty());
      assertEquals("prescribing", role.getCoding().get(0).getCode());
      var hasPrescribingRole =
          role.getCoding().stream().filter(r -> r.getCode().equals("prescribing")).findFirst();
      assertTrue(hasPrescribingRole.isPresent());
    }

    var hasContainedPrescriber =
        containedResources.stream().filter(r -> r.getId().equals("1730548868")).findFirst();
    assertTrue(hasContainedPrescriber.isPresent());
    Practitioner prescriber = (Practitioner) hasContainedPrescriber.get();
    var hasPrescriberRogers =
        prescriber.getName().stream()
            .filter(humanName -> humanName.getFamily().equals("Rogers"))
            .findFirst();
    assertTrue(hasPrescriberRogers.isPresent());

    var partDInsurance =
        eob.getInsurance().stream()
            .filter(i -> i.getCoverage().getDisplay().equals("Part D"))
            .findFirst();
    assertTrue(partDInsurance.isPresent());
    var insuranceExtensions = partDInsurance.get().getExtension();
    assertTrue(insuranceExtensions.isEmpty());
  }

  @Test
  void eobReadPharmacyWithOrganization() {
    var eob = eobRead().withId(CLAIM_ID_RX_ORGANIZATION).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);

    assertEquals("#1649041195", eob.getProvider().getReference());
    var containedResources = eob.getContained();
    var hasOrganization =
        containedResources.stream()
            .filter(r -> r.getResourceType().toString().equals("Organization"))
            .findFirst();
    assertTrue(hasOrganization.isPresent());
    Organization organization = (Organization) hasOrganization.get();
    assertEquals("1649041195", organization.getId());
    assertEquals("CBS Health Corporation", organization.getName());

    var careTeam = eob.getCareTeam();
    assertFalse(careTeam.isEmpty());
    for (ExplanationOfBenefit.CareTeamComponent careTeamMember : careTeam) {
      var provider = careTeamMember.getProvider();
      assertFalse(provider.isEmpty());
      assertEquals("#1437702123", provider.getReference());

      var role = careTeamMember.getRole();
      assertFalse(role.isEmpty());
      assertEquals("prescribing", role.getCoding().get(0).getCode());
      var hasPrescribingRole =
          role.getCoding().stream().filter(r -> r.getCode().equals("prescribing")).findFirst();
      assertTrue(hasPrescribingRole.isPresent());
    }

    var hasContainedPrescriber =
        containedResources.stream().filter(r -> r.getId().equals("1437702123")).findFirst();
    assertTrue(hasContainedPrescriber.isPresent());
    Practitioner prescriber = (Practitioner) hasContainedPrescriber.get();
    var hasPrescriberStark =
        prescriber.getName().stream()
            .filter(humanName -> humanName.getFamily().equals("Stark"))
            .findFirst();
    assertTrue(hasPrescriberStark.isPresent());

    var partDInsurance =
        eob.getInsurance().stream()
            .filter(i -> i.getCoverage().getDisplay().equals("Part D"))
            .findFirst();
    assertTrue(partDInsurance.isPresent());
    var insuranceExtensions = partDInsurance.get().getExtension();
    assertTrue(insuranceExtensions.isEmpty());
  }
}
