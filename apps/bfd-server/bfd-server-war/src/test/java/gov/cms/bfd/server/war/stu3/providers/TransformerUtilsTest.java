package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.List;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.TransformerUtils}. Not to be confused
 * with {@link TransformerTestUtils}, which are test utilities.
 */
public final class TransformerUtilsTest {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#createExtensionCoding(org.hl7.fhir.instance.model.api.IAnyResource,
   * CcwCodebookVariable, String)} works as expected.
   */
  @Test
  public void createExtensionCoding() {
    Patient patientA = new Patient();
    patientA.setId("12345");

    Extension extension =
        TransformerUtils.createExtensionCoding(patientA, CcwCodebookVariable.RACE, "4");
    assertNotNull(extension);
    assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", extension.getUrl());
    assertTrue(extension.getValue() instanceof Coding);

    Coding coding = (Coding) extension.getValue();
    assertNotNull(coding);
    assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
    assertEquals("4", coding.getCode());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#createCodeableConcept(org.hl7.fhir.instance.model.api.IAnyResource,
   * CcwCodebookVariable, String)} works as expected.
   */
  @Test
  public void createCodeableConcept() {
    Patient patientA = new Patient();
    patientA.setId("12345");

    CodeableConcept concept =
        TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "4");
    assertNotNull(concept);
    assertEquals(1, concept.getCoding().size());

    Coding coding = concept.getCodingFirstRep();
    assertNotNull(coding);
    assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
    assertEquals("4", coding.getCode());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#createCodeableConcept(org.hl7.fhir.instance.model.api.IAnyResource,
   * CcwCodebookVariable, String)} sets {@link Coding#getDisplay()} correctly.
   */
  @Test
  public void createCodeableConcept_display() {
    Patient patientA = new Patient();
    patientA.setId("12345");

    CodeableConcept raceConcept_4 =
        TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "4");
    assertEquals("Asian", raceConcept_4.getCodingFirstRep().getDisplay());

    // This code isn't valid and shouldn't end up with a matching display.
    CodeableConcept raceConcept_12 =
        TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "12");
    assertNull(raceConcept_12.getCodingFirstRep().getDisplay());

    /*
     * The REV_CNTR_PMT_MTHD_IND_CD Variable has value collisions. Verify that those
     * are handled correctly.
     */
    CodeableConcept paymentMethodConcept_1 =
        TransformerUtils.createCodeableConcept(
            patientA, CcwCodebookVariable.REV_CNTR_PMT_MTHD_IND_CD, "1");
    assertNull(paymentMethodConcept_1.getCodingFirstRep().getDisplay());
  }

  @Test
  public void addCareTeamPractitioner() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.PRIMARY);
    assertEquals(1, eob.getCareTeam().size(), "Expect there to be one care team member");
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.ASSIST);
    assertEquals(2, eob.getCareTeam().size(), "Expect there to be two care team members");
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.ASSIST);
    assertEquals(2, eob.getCareTeam().size(), "Expect there to be two care team members");
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#createIdentifierReference(String,
   * String)} sets {@link Reference)} correctly.
   */
  @Test
  public void createReferenceTest() {
    String identifierSystem = "identifierSystem";
    String identifierValue = "identifierValue";

    Reference reference =
        TransformerUtils.createIdentifierReference(identifierSystem, identifierValue);

    assertEquals(identifierSystem, reference.getIdentifier().getSystem());
    assertEquals(identifierValue, reference.getIdentifier().getValue());
    assertTrue(isCodingListNullOrEmpty(reference.getIdentifier().getType().getCoding()));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#careTeamHasMatchingExtension(
   * (org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent, String, String)} verifies if
   * an extension is found
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsTrueWhenFound() {
    String referenceUrl = "http://test.url";
    String codeValue = "code";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertTrue(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#careTeamHasMatchingExtension(
   * (org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent, String, String)} verifies it
   * returns false when a reference url is empty.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithEmptyReferenceUrl() {
    String referenceUrl = "";
    String codeValue = "code";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#careTeamHasMatchingExtension(
   * (org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent, String, String)} verifies it
   * returns false when a reference url is null.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithNullReferenceUrl() {
    String referenceUrl = null;
    String codeValue = "code";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#careTeamHasMatchingExtension(
   * (org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent, String, String)} verifies it
   * returns false when a code value is empty.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithEmptyCodeValue() {
    String referenceUrl = "http://test.url";
    String codeValue = "";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#careTeamHasMatchingExtension(
   * (org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent, String, String)} verifies it
   * returns false when a code value is null.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithNullCodeValue() {
    String referenceUrl = "http://test.url";
    String codeValue = null;
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.TransformerUtils#createIdentifierReference
   * (gov.cms.bfd.server.war.stu3.providers.IdentifierType, String)} sets {@link Reference)}
   * correctly.
   */
  @Test
  public void createReferenceForIdentifierTypeTest() {
    String identifierValue = "identifierValue";
    IdentifierType identifierType = IdentifierType.NPI;
    Reference reference =
        TransformerUtils.createIdentifierReference(identifierType, identifierValue);

    assertEquals(identifierType.getSystem(), reference.getIdentifier().getSystem());
    assertEquals(identifierValue, reference.getIdentifier().getValue());
    assertEquals(
        identifierType.getCode(), reference.getIdentifier().getType().getCoding().get(0).getCode());
    assertEquals(
        identifierType.getDisplay(),
        reference.getIdentifier().getType().getCoding().get(0).getDisplay());
    assertEquals(
        identifierType.getSystem(),
        reference.getIdentifier().getType().getCoding().get(0).getSystem());
  }

  private boolean isCodingListNullOrEmpty(List<Coding> coding) {
    if (coding == null || coding.isEmpty() || coding.size() == 0) return true;

    return false;
  }
}
