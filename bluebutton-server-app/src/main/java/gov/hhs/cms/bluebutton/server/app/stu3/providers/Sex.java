package gov.hhs.cms.bluebutton.server.app.stu3.providers;

public enum Sex {
	UNKNOWN('0'), MALE('1'), FEMALE('2');

	private char code;

	public char getCode() {
		return this.code;
	}

	private Sex(char code) {
		this.code = code;
	}
}