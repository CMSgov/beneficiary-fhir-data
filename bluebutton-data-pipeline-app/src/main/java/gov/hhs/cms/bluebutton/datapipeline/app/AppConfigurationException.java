package gov.hhs.cms.bluebutton.datapipeline.app;

/**
 * A {@link RuntimeException} that indicates that the application was not
 * launched with correct configuration.
 */
public final class AppConfigurationException extends RuntimeException {
	private static final long serialVersionUID = -7541188402652747741L;

	/**
	 * Constructs a new {@link AppConfigurationException} instance.
	 * 
	 * @param message
	 *            the value to use for {@link #getMessage()}
	 */
	public AppConfigurationException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link AppConfigurationException} instance.
	 * 
	 * @param message
	 *            the value to use for {@link #getMessage()}
	 * @param cause
	 *            the value to use for {@link #getCause()}
	 */
	public AppConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
