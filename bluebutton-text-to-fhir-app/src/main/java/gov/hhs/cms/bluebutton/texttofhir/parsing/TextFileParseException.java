package gov.hhs.cms.bluebutton.texttofhir.parsing;

import java.io.IOException;

/**
 * Indicates that the file being parsed could not be read or did not conform to
 * the format expected of CMS/MyMedicare.gov BlueButton files.
 */
public final class TextFileParseException extends Exception {
	private static final long serialVersionUID = -8396169570620002541L;

	/**
	 * Constructs a new {@link TextFileParseException} instance.
	 * 
	 * @param message
	 *            the value to use for {@link #getMessage()}
	 */
	public TextFileParseException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link TextFileParseException} instance.
	 * 
	 * @param cause
	 *            the value to use for {@link #getCause()}
	 */
	public TextFileParseException(IOException cause) {
		super(cause);
	}
}
