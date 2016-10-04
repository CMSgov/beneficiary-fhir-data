package gov.hhs.cms.bluebutton.datapipeline.benchmarks;

/**
 * Indicates that a benchmark failed due to an unexpected error in one of its
 * iterations.
 */
public final class BenchmarkError extends RuntimeException {
	private static final long serialVersionUID = 7116829896231404163L;

	/**
	 * Constructs a new {@link BenchmarkError} instance.
	 * 
	 * @param message
	 *            the value to use for {@link #getMessage()}
	 * @param cause
	 *            the value to use for {@link #getCause()}
	 */
	public BenchmarkError(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new {@link BenchmarkError} instance.
	 * 
	 * @param message
	 *            the value to use for {@link #getMessage()}
	 */
	public BenchmarkError(String message) {
		super(message);
	}
}
