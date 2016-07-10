package gov.hhs.cms.bluebutton.datapipeline.fhir.sampledata;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Parses command line arguments into {@link Options} instances.
 */
final class OptionsParser {
	/**
	 * @param args
	 *            the arguments to parse
	 * @return the {@link Options} instance that was parsed from the specified
	 *         arguments
	 * @throws CmdLineException
	 *             Any {@link CmdLineException}s encountered will be bubbled up.
	 */
	Options parse(String... args) throws CmdLineException {
		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);
		return options;
	}

	/**
	 * @return the help/usage text for the application
	 */
	public String getUsageText() {
		CmdLineParser parser = new CmdLineParser(new Options());
		ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
		parser.printUsage(bytesOutputStream);
		try {
			return bytesOutputStream.toString(StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			// Won't happen: charset is supported on all platforms.
			throw new IllegalStateException(e);
		}
	}
}
