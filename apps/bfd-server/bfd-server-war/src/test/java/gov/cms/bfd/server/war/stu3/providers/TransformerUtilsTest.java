package gov.cms.bfd.server.war.stu3.providers;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.List;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertNotNull(extension);
    Assert.assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", extension.getUrl());
    Assert.assertTrue(extension.getValue() instanceof Coding);

    Coding coding = (Coding) extension.getValue();
    Assert.assertNotNull(coding);
    Assert.assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
    Assert.assertEquals("4", coding.getCode());
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
    Assert.assertNotNull(concept);
    Assert.assertEquals(1, concept.getCoding().size());

    Coding coding = concept.getCodingFirstRep();
    Assert.assertNotNull(coding);
    Assert.assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
    Assert.assertEquals("4", coding.getCode());
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
    Assert.assertEquals("Asian", raceConcept_4.getCodingFirstRep().getDisplay());

    // This code isn't valid and shouldn't end up with a matching display.
    CodeableConcept raceConcept_12 =
        TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "12");
    Assert.assertNull(raceConcept_12.getCodingFirstRep().getDisplay());

    /*
     * The REV_CNTR_PMT_MTHD_IND_CD Variable has value collisions. Verify that those
     * are handled correctly.
     */
    CodeableConcept paymentMethodConcept_1 =
        TransformerUtils.createCodeableConcept(
            patientA, CcwCodebookVariable.REV_CNTR_PMT_MTHD_IND_CD, "1");
    Assert.assertNull(paymentMethodConcept_1.getCodingFirstRep().getDisplay());
  }

  @Test
  public void addCareTeamPractitioner() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.PRIMARY);
    Assert.assertEquals("Expect there to be one care team member", 1, eob.getCareTeam().size());
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.ASSIST);
    Assert.assertEquals("Expect there to be two care team members", 2, eob.getCareTeam().size());
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.ASSIST);
    Assert.assertEquals("Expect there to be two care team members", 2, eob.getCareTeam().size());
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

    Assert.assertEquals(identifierSystem, reference.getIdentifier().getSystem());
    Assert.assertEquals(identifierValue, reference.getIdentifier().getValue());
    Assert.assertTrue(isCodingListNullOrEmpty(reference.getIdentifier().getType().getCoding()));
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

    Assert.assertEquals(identifierType.getSystem(), reference.getIdentifier().getSystem());
    Assert.assertEquals(identifierValue, reference.getIdentifier().getValue());
    Assert.assertEquals(
        identifierType.getCode(), reference.getIdentifier().getType().getCoding().get(0).getCode());
    Assert.assertEquals(
        identifierType.getDisplay(),
        reference.getIdentifier().getType().getCoding().get(0).getDisplay());
    Assert.assertEquals(
        identifierType.getSystem(),
        reference.getIdentifier().getType().getCoding().get(0).getSystem());
  }

  private boolean isCodingListNullOrEmpty(List<Coding> coding) {
    if (coding == null || coding.isEmpty() || coding.size() == 0) return true;

    return false;
  }
}
