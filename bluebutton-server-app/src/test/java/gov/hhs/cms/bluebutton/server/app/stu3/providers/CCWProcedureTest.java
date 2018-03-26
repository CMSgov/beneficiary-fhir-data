package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.time.LocalDate;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

/*
 * Unit tests for {@link CCWProcedure}.
 */
public class CCWProcedureTest {

	/**
	 * Verifies that {@link CCWProcedure(Object)} works as expected.
	 * {@link CCWProcedure}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void testCCWProcedure() throws FHIRException {

		Character versionIcd9 = '9';
		String systemIcd9 = "http://hl7.org/fhir/sid/icd-9-cm";
		assertMatches(versionIcd9, systemIcd9);

		Character versionIcd10 = '0';
		String systemIcd10 = "http://hl7.org/fhir/sid/icd-10";
		assertMatches(versionIcd10, systemIcd10);

		Character versionIcdUnknown = 'U';
		String systemIcdUnknown = String.format("http://hl7.org/fhir/sid/unknown-icd-version/%s", versionIcdUnknown);
		assertMatches(versionIcdUnknown, systemIcdUnknown);
	}

	static void assertMatches(Character version, String system) {

		Optional<String> code = Optional.of("code");
		Optional<LocalDate> procDate = Optional.of(LocalDate.now());

		Optional<CCWProcedure> diagnosis = CCWProcedure.from(code, Optional.of(version), procDate);

		Assert.assertEquals(procDate.get(), diagnosis.get().getProcedureDate().get());
		Assert.assertEquals(system, diagnosis.get().getFhirSystem());

		TransformerTestUtils.assertHasCoding(system, code.get(), diagnosis.get().toCodeableConcept().getCoding());

		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setSystem(system).setCode(code.get());

		Assert.assertTrue(diagnosis.get().isContainedIn(codeableConcept));
	}
}
