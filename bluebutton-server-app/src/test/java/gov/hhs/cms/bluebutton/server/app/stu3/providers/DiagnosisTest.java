package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

/*
 * Unit tests for {@link Diagnosis}.
 */
public class DiagnosisTest {

	/**
	 * Verifies that {@link Diagnosis(Object)} works as expected.
	 * {@link Diagnosis}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void testDiagnosis() throws FHIRException {

		Character v9 = '9';
		String sys9 = "http://hl7.org/fhir/sid/icd-9-cm";
		assertMatches(v9, sys9);

		Character v0 = '0';
		String sys0 = "http://hl7.org/fhir/sid/icd-10";
		assertMatches(v0, sys0);

		Character vUnk = 'U';
		String sysUnk = String.format("http://hl7.org/fhir/sid/unknown-icd-version/%s", vUnk);
		assertMatches(vUnk, sysUnk);

		assertDiagnosisLabelsMatch();
	}

	static void assertMatches(Character version, String system) {

		Optional<String> code = Optional.of("code");
		Optional<Character> prsntOnAdmsn = Optional.of('Y');

		Optional<Diagnosis> diag = Diagnosis.from(code, Optional.of(version), prsntOnAdmsn);

		Assert.assertEquals(prsntOnAdmsn, diag.get().getPresentOnAdmission());
		Assert.assertEquals(system, diag.get().getFhirSystem());

		TransformerTestUtils.assertHasCoding(system, code.get(), diag.get().toCodeableConcept());

		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setSystem(system).setCode(code.get());

		Assert.assertTrue(diag.get().isContainedIn(codeableConcept));
	}

	static void assertDiagnosisLabelsMatch() {

		Optional<String> code = Optional.of("code");
		Optional<Character> version = Optional.of('9');

		Set<DiagnosisLabel> set1 = new HashSet<>(Arrays.asList(DiagnosisLabel.ADMITTING));
		Set<DiagnosisLabel> set2 = new HashSet<>(Arrays.asList(DiagnosisLabel.FIRSTEXTERNAL));
		Set<DiagnosisLabel> set3 = new HashSet<>(Arrays.asList(DiagnosisLabel.PRINCIPAL));

		Optional<Diagnosis> diag1 = Diagnosis.from(code, version, DiagnosisLabel.ADMITTING);
		Assert.assertEquals(set1, diag1.get().getLabels());

		Optional<Diagnosis> diag2 = Diagnosis.from(code, version, DiagnosisLabel.FIRSTEXTERNAL);
		Assert.assertEquals(set2, diag2.get().getLabels());

		Optional<Diagnosis> diag3 = Diagnosis.from(code, version, DiagnosisLabel.PRINCIPAL);
		Assert.assertEquals(set3, diag3.get().getLabels());
	}
}
