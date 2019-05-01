package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * Enumerates the value codeset indicating the sex of the {@link Beneficiary}.
 */
public enum Sex {
	UNKNOWN('0'), MALE('1'), FEMALE('2');

	private char code;

	/**
	 * @return the code representing the sex of the {@link Beneficiary}
	 */
	public char getCode() {
		return this.code;
	}

	private Sex(char code) {
		this.code = code;
	}
}