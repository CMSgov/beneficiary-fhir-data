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

		Character v9 = '9';
		String sys9 = "http://hl7.org/fhir/sid/icd-9-cm";
		assertMatches(v9, sys9);

		Character v0 = '0';
		String sys0 = "http://hl7.org/fhir/sid/icd-10";
		assertMatches(v0, sys0);

		Character vUnk = 'U';
		String sysUnk = String.format("http://hl7.org/fhir/sid/unknown-icd-version/%s", vUnk);
		assertMatches(vUnk, sysUnk);
	}

	static void assertMatches(Character version, String system) {

		Optional<String> code = Optional.of("code");
		Optional<LocalDate> procDate = Optional.of(LocalDate.now());

		Optional<CCWProcedure> diag = CCWProcedure.from(code, Optional.of(version), procDate);

		Assert.assertEquals(procDate.get(), diag.get().getProcedureDate());
		Assert.assertEquals(system, diag.get().getFhirSystem());

		TransformerTestUtils.assertHasCoding(system, code.get(), diag.get().toCodeableConcept());

		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setSystem(system).setCode(code.get());

		Assert.assertTrue(diag.get().isContainedIn(codeableConcept));
	}
}
