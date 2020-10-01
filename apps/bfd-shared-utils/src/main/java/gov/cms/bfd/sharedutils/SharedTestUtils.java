package gov.cms.bfd.sharedutils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contains utility code for tests that is (potentially) useful across all of the other BFD modules.
 *
 * <p><strong>Note:</strong> This class is in <code>src/main/java</code> on purpose, as code in
 * <code>src/test/java</code> is not available to dependent modules. Use of this class outside of
 * JUnit tests is not supported.
 */
public final class SharedTestUtils {

  /**
   * <strong>Note:</strong> This method should only be used by unit/integration tests, and will not
   * work in any other context.
   *
   * @return the local {@link Path} to the root project's/module's <code>target/</code> directory,
   *     e.g. <code>/foo/beneficiary-fhir-data.git/apps/target/</code>, which will be created if it
   *     does not exist
   */
  public static Path getBuildRootTargetDirectory() {
    /*
     * The working directory for tests will be somewhere in the overall 'apps/' tree. If we run a
     * single test manually from Eclipse, it should be in the module directory for whatever test is
     * being run. If we run it from the overall Maven build, it should be the root of the 'apps/'
     * tree. Regardless of where we end up, what we want here is `apps/target/` (which Maven won't
     * create by default).
     */
    String rootProjectDirectoryName = "apps";
    try {
      /*
       * Start with the current directory and look "up" no more than two times until the 'apps/'
       * directory is found.
       */
      Path projectDir = Paths.get(".");

      if (!projectDir.toRealPath().endsWith(rootProjectDirectoryName)) {
        projectDir = projectDir.resolve("..");
      }

      if (!projectDir.toRealPath().endsWith(rootProjectDirectoryName)) {
        projectDir = projectDir.resolve("..");
      }

      if (!projectDir.toRealPath().endsWith(rootProjectDirectoryName)) {
        throw new IllegalStateException(
            String.format(
                "Unable to find '%s' directory, starting from '%s', and ending at '%s'.",
                rootProjectDirectoryName,
                Paths.get(".").toAbsolutePath(),
                projectDir.toAbsolutePath()));
      }

      Path targetDir = projectDir.resolve("target");
      Files.createDirectories(targetDir);

      return targetDir.toRealPath();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
