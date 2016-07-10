package gov.hhs.cms.bluebutton.datapipeline.fhir.sampledata;

import java.net.URI;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Models the command line arguments. Intended to be constructed via
 * {@link CmdLineParser}.
 */
final class Options {
	/*
	 * In order to support parsing via CmdLineParser, the following is required:
	 * 1) a non-private default constructor must be present, and 2) none of the
	 * fields here may be final.
	 */

	@Option(name = "--server", aliases = {
			"-s" }, required = true, metaVar = "FHIR_SERVER", usage = "specifies the URL "
					+ "of the FHIR server to publish to")
	private URI server;

	@Option(name = "--help", aliases = { "-h" }, required = false, usage = "displays this help text")
	private boolean helpRequested;

	/**
	 * Default constructor (required by {@link CmdLineParser}).
	 */
	Options() {
		this.server = null;
	}

	/**
	 * @return the {@link URI} of the FHIR server to publish to
	 */
	public URI getServer() {
		return server;
	}

	/**
	 * @return <code>true</code> if the user requested usage help
	 */
	public boolean isHelpRequested() {
		return helpRequested;
	}
}
