package gov.hhs.cms.bluebutton.datapipeline.benchmarks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A set of shared utilities for the benchmark code.
 */
final class BenchmarkUtilities {
	/**
	 * @return the {@link Path} to this project's <code>target</code> directory
	 */
	static Path findProjectTargetDir() {
		Path targetDir = Paths.get(".", "bluebutton-data-pipeline-benchmarks", "target");
		if (!Files.isDirectory(targetDir))
			targetDir = Paths.get(".", "target");
		if (!Files.isDirectory(targetDir))
			throw new IllegalStateException();

		return targetDir;
	}
}
