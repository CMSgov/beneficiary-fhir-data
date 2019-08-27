package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;

/**
 * Unit tests for {@link TransformerUtils}. Not to be confused with
 * {@link TransformerTestUtils}, which are test utilities.
 */
public final class TransformerUtilsTest {
	/**
	 * Verifies that
	 * {@link TransformerUtils#createExtensionCoding(org.hl7.fhir.instance.model.api.IAnyResource, CcwCodebookVariable, String)}
	 * works as expected.
	 */
	@Test
	public void createExtensionCoding() {
		Patient patientA = new Patient();
		patientA.setId("12345");

		Extension extension = TransformerUtils.createExtensionCoding(patientA, CcwCodebookVariable.RACE, "4");
		Assert.assertNotNull(extension);
		Assert.assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", extension.getUrl());
		Assert.assertTrue(extension.getValue() instanceof Coding);

		Coding coding = (Coding) extension.getValue();
		Assert.assertNotNull(coding);
		Assert.assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
		Assert.assertEquals("4", coding.getCode());
	}

	/**
	 * Verifies that
	 * {@link TransformerUtils#createCodeableConcept(org.hl7.fhir.instance.model.api.IAnyResource, CcwCodebookVariable, String)}
	 * works as expected.
	 */
	@Test
	public void createCodeableConcept() {
		Patient patientA = new Patient();
		patientA.setId("12345");

		CodeableConcept concept = TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "4");
		Assert.assertNotNull(concept);
		Assert.assertEquals(1, concept.getCoding().size());

		Coding coding = concept.getCodingFirstRep();
		Assert.assertNotNull(coding);
		Assert.assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
		Assert.assertEquals("4", coding.getCode());
	}

	/**
	 * Verifies that
	 * {@link TransformerUtils#createCodeableConcept(org.hl7.fhir.instance.model.api.IAnyResource, CcwCodebookVariable, String)}
	 * sets {@link Coding#getDisplay()} correctly.
	 */
	@Test
	public void createCodeableConcept_display() {
		Patient patientA = new Patient();
		patientA.setId("12345");

		CodeableConcept raceConcept_4 = TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "4");
		Assert.assertEquals("Asian", raceConcept_4.getCodingFirstRep().getDisplay());

		// This code isn't valid and shouldn't end up with a matching display.
		CodeableConcept raceConcept_12 = TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE,
				"12");
		Assert.assertNull(raceConcept_12.getCodingFirstRep().getDisplay());

		/*
		 * The REV_CNTR_PMT_MTHD_IND_CD Variable has value collisions. Verify that those
		 * are handled correctly.
		 */
		CodeableConcept paymentMethodConcept_1 = TransformerUtils.createCodeableConcept(patientA,
				CcwCodebookVariable.REV_CNTR_PMT_MTHD_IND_CD, "1");
		Assert.assertNull(paymentMethodConcept_1.getCodingFirstRep().getDisplay());
	}
}
