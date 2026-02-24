package gov.cms.bfd.server.ng;

import static gov.cms.bfd.server.ng.util.SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import gov.cms.bfd.server.ng.claim.model.ProviderIdQualifierCode;
import gov.cms.bfd.server.ng.eob.EobResourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
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
  void eobReadPharmacy() {
    var eob = eobRead().withId(CLAIM_ID_RX).execute();
    assertFalse(eob.isEmpty());
    expectFhir().toMatchSnapshot(eob);

    validateProviderAndPractitioner(eob);
    validatePharmacyItems(eob);
    validateSupportingInfo(eob);
    validateCareTeamAndPrescriber(eob, "Rogers, Gromit", "1730548868");
    validateInsurance(eob);
    validateFinancialFieldRedaction(eob);
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

    validateCareTeamAndPrescriber(eob, "Stark, Tony", "1437702123");
    validateSupportingInfo(eob);
    validateInsurance(eob);
    validateFinancialFieldRedaction(eob);
  }

  private void validateProviderAndPractitioner(ExplanationOfBenefit eob) {
    assertEquals("#1548226988", eob.getProvider().getReference());

    var hasPractitioner =
        eob.getContained().stream().filter(r -> r.getId().equals("1548226988")).findFirst();
    assertTrue(hasPractitioner.isPresent());
    Practitioner practitioner = (Practitioner) hasPractitioner.get();
    var familyName =
        practitioner.getName().stream().filter(p -> p.getFamily().equals("Garcia")).findFirst();
    assertTrue(
        familyName.isPresent(), "Practitioner 'Garcia' should be present in contained resources");
  }

  private void validatePharmacyItems(ExplanationOfBenefit eob) {
    var item = eob.getItem().getFirst();
    var detail = item.getDetailFirstRep();

    assertEquals("compound", item.getProductOrService().getCoding().get(0).getCode());
    assertNull(item.getQuantity().getUnit(), "Compound meds should be null");
    assertEquals("00338004904", detail.getProductOrService().getCoding().get(0).getCode());
  }

  private void validateSupportingInfo(ExplanationOfBenefit eob) {
    var supportingInfo = eob.getSupportingInfo();
    assertTrue(supportingInfo.size() >= 4, "C4BB profile requires >= 4 supporting info");

    var hasBadDateTime =
        supportingInfo.stream()
            .anyMatch(
                s ->
                    s.hasTiming()
                        && s.getTiming() instanceof DateTimeType dt
                        && dt.getValue().toString().startsWith("9999-12-31"));
    assertFalse(hasBadDateTime, "Should not contain placeholder date times");
  }

  private void validateCareTeamAndPrescriber(
      ExplanationOfBenefit eob, String expectedName, String expectedNpi) {
    var hasPrescriber =
        eob.getCareTeam().stream()
            .filter(
                ct ->
                    ct.hasRole()
                        && ct.getRole()
                            .hasCoding(CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE, "prescribing"))
            .findFirst();

    assertTrue(hasPrescriber.isPresent());
    var provider = hasPrescriber.get().getProvider();

    assertEquals(expectedName, provider.getDisplay());
    assertEquals(expectedNpi, provider.getIdentifier().getValue());

    var codes = provider.getIdentifier().getType().getCoding();
    var hasNpi = codes.stream().anyMatch(c -> "NPI".equals(c.getCode()));
    var hasQual =
        codes.stream().anyMatch(c -> ProviderIdQualifierCode._01.getCode().equals(c.getCode()));

    assertTrue(hasNpi && hasQual, "Provider identifier must have NPI and Qualifier codes");
  }

  private void validateInsurance(ExplanationOfBenefit eob) {
    var partD =
        eob.getInsurance().stream()
            .filter(i -> "Part D".equals(i.getCoverage().getDisplay()))
            .findFirst();

    assertTrue(partD.isPresent());
    assertTrue(partD.get().getExtension().isEmpty(), "Part D insurance should not have extensions");
  }

  private void validateFinancialFieldRedaction(ExplanationOfBenefit eob) {
    var restrictedFields =
        Set.of(
            "CLM_LINE_INGRDNT_CST_AMT",
            "CLM_LINE_SRVC_CST_AMT",
            "CLM_LINE_SLS_TAX_AMT",
            "CLM_LINE_VCCN_ADMIN_FEE_AMT",
            "CLM_LINE_GRS_CVRD_CST_TOT_AMT",
            "CLM_LINE_REBT_PASSTHRU_POS_AMT",
            "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT",
            "CLM_CMS_CALCD_MFTR_DSCNT_AMT",
            "CLM_RPTD_MFTR_DSCNT_AMT",
            "CLM_LINE_TROOP_TOT_AMT");

    var adjudicationElements =
        FhirContext.forR4Cached()
            .newTerser()
            .getAllPopulatedChildElementsOfType(
                eob, ExplanationOfBenefit.AdjudicationComponent.class);

    for (var adjudication : adjudicationElements) {
      for (var coding : adjudication.getCategory().getCoding()) {
        assertFalse(
            restrictedFields.contains(coding.getCode()),
            "Adjudication field " + coding.getCode() + " should have been removed");
      }
    }
  }
}
