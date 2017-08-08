package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Models a ccwProcedure code entry in a claim.
 */
final class CCWProcedure extends IcdCode {

	private LocalDate procedureDate;

	/**
	 * Constructs a new {@link CCWProcedure}.
	 * 
	 * @param icdCode
	 *            the ICD code of the ccwProcedure, if any
	 * @param icdVersionCode
	 *            the CCW encoding (per <a href=
	 *            "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
	 *            CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other
	 *            similar fields) of the code's ICD version, if any
	 * @param present
	 *            the value to use for {@link #getLabels()}
	 */
	public CCWProcedure(Optional<String> icdCode, Optional<Character> icdVersionCode, LocalDate procedureDate) {
		super(icdCode, icdVersionCode);
		Objects.requireNonNull(icdCode);
		Objects.requireNonNull(icdVersionCode);
		Objects.requireNonNull(procedureDate);

		this.icdCode = icdCode.get();
		this.icdVersionCode = icdVersionCode.orElse(null);
		this.procedureDate = procedureDate;

	}

	/**
	 * @return the ICD procedure date
	 */
	public LocalDate getProcedureDate() {
		return procedureDate;
	}

	/**
	 * Constructs a new {@link CCWProcedure}, if the specified <code>code</code>
	 * is present.
	 * 
	 * @param icdCode
	 *            the ICD code of the ccwProcedure, if any
	 * @param icdVersionCode
	 *            the CCW encoding (per <a href=
	 *            "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
	 *            CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other
	 *            similar fields) of the code's ICD version, if any
	 * @param labels
	 *            the value to use for {@link #getLabels()}
	 * @return the new {@link CCWProcedure}, or {@link Optional#empty()} if no
	 *         <code>code</code> was present
	 */
	static Optional<CCWProcedure> from(Optional<String> icdCode, Optional<Character> icdVersionCode,
			LocalDate procedureDate) {
		if (!icdCode.isPresent())
			return Optional.empty();
		return Optional.of(new CCWProcedure(icdCode, icdVersionCode, procedureDate));
	}

}

