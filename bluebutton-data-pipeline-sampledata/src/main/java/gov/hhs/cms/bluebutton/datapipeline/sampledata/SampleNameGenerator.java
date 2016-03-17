package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.util.Random;

/**
 * Provides methods for generating {@link SampleName}s.
 */
public final class SampleNameGenerator {
	/**
	 * Used to generate random numbers to include in the names. Note that the
	 * seed is fixed so that the sequence produced is stable. This should make
	 * the sample data more predictable, which is a good thing for our use
	 * cases.
	 */
	private final Random rng = new Random(42L);

	/**
	 * @return a generated {@link SampleName} instance
	 */
	public SampleName generateName() {
		String firstName = String.format("f%d", rng.nextInt(10 ^ 7));
		String lastName = String.format("l%d", rng.nextInt(10 ^ 7));
		return new SampleName(firstName, lastName);
	}
}
