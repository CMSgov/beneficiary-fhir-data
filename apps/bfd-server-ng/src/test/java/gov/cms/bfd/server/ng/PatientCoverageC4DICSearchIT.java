package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

class PatientCoverageC4DICSearchIT extends IntegrationTestBase {
  private Bundle searchBundle(String beneSK) {
    return getFhirClient()
        .operation()
        .onInstance(new IdType("Patient", beneSK))
        .named("$generate-insurance-card")
        .withNoParameters(Parameters.class)
        .returnResourceType(Bundle.class)
        .execute();
  }

  @Test
  void patientCoverageSearchByIdPartA() {
    var response = searchBundle(BENE_ID_PART_A_ONLY);

    // Assert the response structure
    assertNotNull(response);
    assertEquals(Bundle.BundleType.COLLECTION, response.getType());
    assertFalse(response.getEntry().isEmpty());

    assertEquals(3, response.getEntry().size());
    long patientCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Patient).count();
    long orgCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Organization).count();
    long coverageCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Coverage).count();

    assertEquals(1, patientCount);
    assertEquals(1, orgCount);
    assertEquals(1, coverageCount);
    assertAllResourcesHaveValidProfilesAndIds(response);
    expectFhir().toMatchSnapshot(response);
  }

  @Test
  void patientCoverageSearchByIdPartB() {
    var response = searchBundle(BENE_ID_PART_B_ONLY);

    // Assert the response structure
    assertNotNull(response);
    assertEquals(Bundle.BundleType.COLLECTION, response.getType());
    assertFalse(response.getEntry().isEmpty());

    assertEquals(3, response.getEntry().size());
    long patientCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Patient).count();
    long orgCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Organization).count();
    long coverageCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Coverage).count();

    assertEquals(1, patientCount);
    assertEquals(1, orgCount);
    assertEquals(1, coverageCount);
    assertAllResourcesHaveValidProfilesAndIds(response);
    expectFhir().toMatchSnapshot(response);
  }

  @Test
  void patientCoverageSearchByIdDual() {
    var response = searchBundle(BENE_ID_DUAL_ONLY);

    // Assert the response structure
    assertNotNull(response);
    assertEquals(Bundle.BundleType.COLLECTION, response.getType());
    assertFalse(response.getEntry().isEmpty());

    assertEquals(3, response.getEntry().size());
    long patientCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Patient).count();
    long orgCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Organization).count();
    long coverageCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Coverage).count();

    assertEquals(1, patientCount);
    assertEquals(1, orgCount);
    assertEquals(1, coverageCount);
    assertAllResourcesHaveValidProfilesAndIds(response);
    expectFhir().toMatchSnapshot(response);
  }

  @Test
  void patientCoverageSearchAllParts() {
    var response = searchBundle(BENE_ID_ALL_PARTS_WITH_XREF);

    // Assert the response structure
    assertNotNull(response);
    assertEquals(Bundle.BundleType.COLLECTION, response.getType());
    assertFalse(response.getEntry().isEmpty());
    assertEquals(7, response.getEntry().size());

    long patientCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Patient).count();
    long orgCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Organization).count();
    long coverageCount =
        response.getEntry().stream().filter(e -> e.getResource() instanceof Coverage).count();

    assertEquals(1, patientCount);
    assertEquals(1, orgCount);
    assertEquals(5, coverageCount);
    // Checking IDs are present because snapshot is suppressing IDs.
    assertAllResourcesHaveValidProfilesAndIds(response);
    expectFhir().toMatchSnapshot(response);
  }

  private void assertAllResourcesHaveValidProfilesAndIds(Bundle bundle) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();

      boolean skipUuidCheck = false;
      if (resource instanceof Coverage coverage) {
        Optional<Coverage.ClassComponent> classComponent =
            coverage.getClass_().stream().findFirst();
        String coveragePart = classComponent.map(Coverage.ClassComponent::getValue).orElse("");
        skipUuidCheck = "Part C".equals(coveragePart) || "Part D".equals(coveragePart);
      }
      if (!skipUuidCheck) {
        assertTrue(isValidUuidUrn(resource.getId()));
      } else {
        assertTrue(hasCoverageId(resource.getId()));
      }

      String expectedProfile =
          switch (resource) {
            case Patient _ -> SystemUrls.PROFILE_C4DIC_PATIENT_1_1_0;
            case Organization _ -> SystemUrls.PROFILE_C4DIC_ORGANIZATION;
            case Coverage _ -> SystemUrls.PROFILE_C4DIC_COVERAGE_1_1_0;
            default -> fail("Unexpected resource type: " + resource.getClass().getSimpleName());
          };

      assertEquals(expectedProfile, resource.getMeta().getProfile().getFirst().asStringValue());
    }
  }

  private boolean isValidUuidUrn(String id) {
    if (!id.startsWith("urn:uuid:")) {
      return false;
    }
    String uuid = id.substring("urn:uuid:".length());
    return uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  }

  private boolean hasCoverageId(String id) {
    if (!id.startsWith("urn:uuid:")) {
      return false;
    }
    String uuid = id.substring("urn:uuid:".length());
    return uuid.matches("[A-Za-z0-9]+");
  }

  @Test
  void patientSearchByIdEmpty() {
    var patientBundle = searchBundle("999");
    assertEquals(0, patientBundle.getEntry().size());
  }
}
