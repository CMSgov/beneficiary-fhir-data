package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

/**
 * Models a diagnosis code entry in a claim.
 */
final class Diagnosis {
	private final String icdCode;
	private final Character icdVersionCode;
	private final String presentOnAdmission;
	private final Set<DiagnosisLabel> labels;

	/**
	 * Constructs a new {@link Diagnosis}.
	 * 
	 * @param icdCode
	 *            the ICD code of the diagnosis, if any
	 * @param icdVersionCode
	 *            the CCW encoding (per <a href=
	 *            "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
	 *            CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other
	 *            similar fields) of the code's ICD version, if any
	 * @param labels
	 *            the value to use for {@link #getLabels()}
	 */
	private Diagnosis(Optional<String> icdCode, Optional<Character> icdVersionCode, DiagnosisLabel... labels) {
		Objects.requireNonNull(icdCode);
		Objects.requireNonNull(icdVersionCode);
		Objects.requireNonNull(labels);

		this.icdCode = icdCode.get();
		this.icdVersionCode = icdVersionCode.orElse(null);
		this.presentOnAdmission = null;
		this.labels = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(labels)));
	}

	/**
	 * @return TODO
	 */
	Set<DiagnosisLabel> getLabels() {
		return labels;
	}

	/**
	 * @return TODO
	 */
	Optional<String> getPresentOnAdmission() {
		return Optional.ofNullable(presentOnAdmission);
	}

	/**
	 * @param codeableConcept
	 *            the {@link CodeableConcept} to check
	 * @return <code>true</code> if the specified {@link CodeableConcept}
	 *         contains a {@link Coding} that matches this {@link Diagnosis},
	 *         <code>false</code> if not
	 */
	boolean isContainedIn(CodeableConcept codeableConcept) {
		return codeableConcept.getCoding().stream().filter(c -> icdCode.equals(c.getCode()))
				.filter(c -> getFhirSystem().equals(c.getSystem())).count() != 0;
	}

	/**
	 * @return a {@link CodeableConcept} that contains this {@link Diagnosis}
	 */
	CodeableConcept toCodeableConcept() {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();

		String system = getFhirSystem();
		coding.setSystem(system);

		coding.setCode(icdCode);

		return codeableConcept;
	}

	/**
	 * @return the
	 *         <a href= "https://www.hl7.org/fhir/terminologies-systems.html">
	 *         FHIR Coding system</a> value for this {@link Diagnosis}'
	 *         {@link #icdVersionCode} value
	 */
	private String getFhirSystem() {
		String system;
		if (icdVersionCode == null || icdVersionCode.equals("9"))
			system = "http://hl7.org/fhir/sid/icd-9-cm";
		else if (icdVersionCode.equals("0"))
			system = "http://hl7.org/fhir/sid/icd-10";
		else
			system = String.format("http://hl7.org/fhir/sid/unknown-icd-version/%s", icdVersionCode);
		return system;
	}

	/**
	 * Constructs a new {@link Diagnosis}, if the specified <code>code</code> is
	 * present.
	 * 
	 * @param icdCode
	 *            the ICD code of the diagnosis, if any
	 * @param icdVersionCode
	 *            the CCW encoding (per <a href=
	 *            "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
	 *            CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other
	 *            similar fields) of the code's ICD version, if any
	 * @param labels
	 *            the value to use for {@link #getLabels()}
	 * @return the new {@link Diagnosis}, or {@link Optional#empty()} if no
	 *         <code>code</code> was present
	 */
	static Optional<Diagnosis> from(Optional<String> icdCode, Optional<Character> icdVersionCode,
			DiagnosisLabel... labels) {
		if (!icdCode.isPresent())
			return Optional.empty();
		return Optional.of(new Diagnosis(icdCode, icdVersionCode, labels));
	}

	/**
	 * Enumerates the various labels/tags that are used to distinguish between
	 * the various diagnoses in a claim.
	 */
	static enum DiagnosisLabel {
		PRINCIPAL;
	}
}
