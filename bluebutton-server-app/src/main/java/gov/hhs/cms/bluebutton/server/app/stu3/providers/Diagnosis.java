package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Models a diagnosis code entry in a claim.
 */
final class Diagnosis extends IcdCode {

	private final Character presentOnAdmission;
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
		super(icdCode, icdVersionCode);
		Objects.requireNonNull(icdCode);
		Objects.requireNonNull(icdVersionCode);
		Objects.requireNonNull(labels);

		this.presentOnAdmission = null;
		this.labels = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(labels)));
	}

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
	 * @param present
	 *            the value to use for {@link #getLabels()}
	 */
	private Diagnosis(Optional<String> icdCode, Optional<Character> icdVersionCode,
			Optional<Character> presentOnAdmission) {
		super(icdCode, icdVersionCode);
		Objects.requireNonNull(icdCode);
		Objects.requireNonNull(icdVersionCode);
		Objects.requireNonNull(presentOnAdmission);

		this.presentOnAdmission = presentOnAdmission.orElse(null);
		this.labels = null;


	}

	/**
	 * @return the ICD label
	 */
	Set<DiagnosisLabel> getLabels() {
		return labels;
	}

	/**
	 * @return the ICD presentOnAdmission indicator
	 */
	Optional<Character> getPresentOnAdmission() {
		return Optional.ofNullable(presentOnAdmission);
	}

	/**
	 * Constructs a new {@link Diagnosis}, if the specified <code>icdCode</code>
	 * is present.
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
	 *         <code>icdCode</code> was present
	 */
	static Optional<Diagnosis> from(Optional<String> icdCode, Optional<Character> icdVersionCode,
			DiagnosisLabel... labels) {
		if (!icdCode.isPresent())
			return Optional.empty();
		return Optional.of(new Diagnosis(icdCode, icdVersionCode, labels));
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
	 * @param presentOnAdmission
	 *            the value to use for {@link #getPresentOnAdmission}
	 * @return the new {@link Diagnosis}, or {@link Optional#empty()} if no
	 *         <code>code</code> was present
	 */
	static Optional<Diagnosis> from(Optional<String> icdCode, Optional<Character> icdVersionCode,
			Optional<Character> presentOnAdmission) {
		if (!icdCode.isPresent())
			return Optional.empty();
		return Optional.of(new Diagnosis(icdCode, icdVersionCode, presentOnAdmission));
	}

	/**
	 * Enumerates the various labels/tags that are used to distinguish between
	 * the various diagnoses in a claim.
	 */
	static enum DiagnosisLabel {
		PRINCIPAL, ADMITTING, FIRSTEXTERNAL;
	}
}
