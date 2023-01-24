package gov.cms.model.dsl.codegen.plugin;

import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration test for the {@link GenerateCsvWritersFromDslMojo}. */
public class GenerateCsvWritersFromDslMojoIT extends AbstractMojoIntegrationTestCase {
  /**
   * All of the expected output file paths relative to the temporary output directory created for us
   * by jupiter.
   */
  private static final List<String> OUTPUT_FILE_PATHS =
      List.of("gov/cms/test/BeneficiaryCsvWriter.java", "gov/cms/test/DMEClaimCsvWriter.java");

  /**
   * Path to directory containing our mappings files relative to our {@link
   * AbstractMojoIntegrationTestCase#baseFilesDir}.
   */
  private final File mappingsDir = new File(baseFilesDir, "mappings");

  /**
   * Path to directory containing our expected output relative to our {@link
   * AbstractMojoIntegrationTestCase#baseFilesDir}.
   */
  private final File expectedDir = new File(baseFilesDir, "expected");

  /**
   * Executes the plugin and verifies that all of the generated files match our expected values.
   *
   * @param outputDirectory temporary output directory managed by jupiter.
   * @throws Exception because we are testing methods with checked exceptions
   */
  @Test
  public void testPlugin(@TempDir File outputDirectory) throws Exception {
    final String mappingsDirectoryPath = mappingsDir.getAbsolutePath();
    final String outputDirectoryPath = outputDirectory.getAbsolutePath();
    final var mojo =
        new GenerateCsvWritersFromDslMojo(mappingsDirectoryPath, outputDirectoryPath, project);
    mojo.execute();
    verify(project).addCompileSourceRoot(outputDirectoryPath);
    for (String outputFilePath : OUTPUT_FILE_PATHS) {
      compareFiles(expectedDir, outputDirectory, outputFilePath);
    }
  }
}
